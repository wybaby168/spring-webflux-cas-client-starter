package dev.flyfish.boot.cas.config.session;

import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * web session监听器
 *
 * @author wangyu
 * 基于装饰器增强实现，可灵活处理
 */
public interface WebSessionListener {

    default Mono<Void> onSessionCreated(WebSession session) {
        return Mono.empty();
    }

    default Mono<Void> onSessionInvalidated(WebSession session) {
        return Mono.empty();
    }
}
