package dev.flyfish.boot.cas.config;

import dev.flyfish.boot.cas.config.resolver.CASUserArgumentResolver;
import dev.flyfish.boot.cas.config.session.WebSessionDecorator;
import dev.flyfish.boot.cas.config.session.WebSessionListener;
import dev.flyfish.boot.cas.filter.CASFilter;
import dev.flyfish.boot.cas.filter.CASParameter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.InMemoryWebSessionStore;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.server.session.WebSessionStore;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * cas核心配置
 *
 * @author wangyu
 */
@Configuration
public class CASConfig implements WebFluxConfigurer {

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new CASUserArgumentResolver());
    }

    @Bean
    @ConfigurationProperties("cas.filter")
    public CASParameter casParameter() {
        return new CASParameter();
    }

    @Bean
    public CASFilter casFilter(CASParameter casParameter) {
        return new CASFilter(casParameter);
    }

    @Bean
    @ConditionalOnBean(WebSessionManager.class)
    public WebSessionStore webSessionStore(WebSessionManager webSessionManager, ServerProperties serverProperties,
                                           ObjectProvider<WebSessionListener> listeners) {
        if (webSessionManager instanceof DefaultWebSessionManager defaultWebSessionManager) {
            Duration timeout = serverProperties.getReactive().getSession().getTimeout();
            int maxSessions = serverProperties.getReactive().getSession().getMaxSessions();
            ListenableWebSessionStore sessionStore = new ListenableWebSessionStore(timeout, listeners);
            sessionStore.setMaxSessions(maxSessions);
            defaultWebSessionManager.setSessionStore(sessionStore);
            return sessionStore;
        }
        throw new IllegalStateException("Cannot find web session manager");
    }

    /**
     * 处理session销毁，保证正确退出
     *
     * @param casFilter 过滤器
     * @return 结果
     */
    @Bean
    public WebSessionListener singleSignOutSessionListener(CASFilter casFilter) {
        return new WebSessionListener() {
            @Override
            public Mono<Void> onSessionInvalidated(WebSession session) {
                return casFilter.getSessionMappingStorage().removeBySessionById(session.getId());
            }
        };
    }

    /**
     * 可监听的web session存储
     */
    static final class ListenableWebSessionStore extends InMemoryWebSessionStore {
        private final Duration timeout;
        private final List<WebSessionListener> listeners;

        private ListenableWebSessionStore(Duration timeout, ObjectProvider<WebSessionListener> listeners) {
            this.timeout = timeout;
            this.listeners = listeners.stream().toList();
        }

        public Mono<WebSession> createWebSession() {
            return super.createWebSession()
                    .map(session -> (WebSession) new WebSessionDecorator(session, listeners))
                    .doOnSuccess(this::setMaxIdleTime);
        }

        private void setMaxIdleTime(WebSession session) {
            session.setMaxIdleTime(this.timeout);
        }
    }

}
