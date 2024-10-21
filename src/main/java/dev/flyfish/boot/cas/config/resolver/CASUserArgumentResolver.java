package dev.flyfish.boot.cas.config.resolver;

import dev.flyfish.boot.cas.config.annotation.CASUser;
import dev.flyfish.boot.cas.filter.CASLoginFilter;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CASUserArgumentResolver extends HandlerMethodArgumentResolverSupport {

    public CASUserArgumentResolver() {
        super(ReactiveAdapterRegistry.getSharedInstance());
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return checkAnnotatedParamNoReactiveWrapper(parameter, CASUser.class, (anno, type) -> true);
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        return exchange.getSession().mapNotNull(session -> session.getAttribute(CASLoginFilter.CONST_CAS_USERNAME));
    }
}
