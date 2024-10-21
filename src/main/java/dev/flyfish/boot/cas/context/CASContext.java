package dev.flyfish.boot.cas.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

/**
 * cas 上下文
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CASContext {

    @Getter
    private String ticket;

    @Setter
    @Getter
    private WebSession session;

    private final ServerWebExchange exchange;

    private final WebFilterChain chain;

    private final Map<String, Mono<String>> parameters = new ConcurrentReferenceHashMap<>();

    @Setter
    @Getter
    private String username;

    public static Mono<CASContext> create(ServerWebExchange exchange, WebFilterChain chain) {
        return new CASContext(exchange, chain).init();
    }

    private Mono<CASContext> init() {
        Mono<String> ticketMono = getParameter("ticket")
                .filter(StringUtils::hasText)
                .doOnNext(ticket -> this.ticket = ticket);
        // 此处必须保证session不为空
        Mono<WebSession> sessionMono = exchange.getSession()
                .doOnNext(session -> this.session = session);
        return Mono.zipDelayError(ticketMono, sessionMono)
                .onErrorContinue((e, v) -> e.printStackTrace())
                .thenReturn(this);
    }

    public boolean isTokenRequest() {
        return StringUtils.hasText(ticket);
    }

    public Mono<Void> filter() {
        return chain.filter(exchange);
    }

    public Mono<Void> redirect(String url) {
        ServerHttpResponse response = exchange.getResponse();
        response.setRawStatusCode(HttpStatus.FOUND.value());
        response.getHeaders().setLocation(URI.create(url));
        return Mono.empty();
    }

    public ServerHttpRequest getRequest() {
        return exchange.getRequest();
    }

    public ServerHttpResponse getResponse() {
        return exchange.getResponse();
    }

    public String getPath() {
        return exchange.getRequest().getPath().value();
    }

    public HttpMethod getMethod() {
        return exchange.getRequest().getMethod();
    }

    public String getQuery(String key) {
        ServerHttpRequest request = exchange.getRequest();
        return request.getQueryParams().getFirst(key);
    }

    public Mono<String> getFormData(String key) {
        return exchange.getFormData()
                .mapNotNull(formData -> formData.getFirst(key));
    }

    public void setSessionAttribute(String key, Object value) {
        session.getAttributes().put(key, value);
    }

    /**
     * 获取参数
     *
     * @param key 键
     * @return 异步结果
     */
    private Mono<String> getParameter(String key) {
        return parameters.computeIfAbsent(key, this::computeParameter);
    }

    private Mono<String> computeParameter(String key) {
        return this.readParameter(key).cache();
    }

    private Mono<String> readParameter(String key) {
        String query = getQuery(key);
        if (StringUtils.hasText(query)) {
            return Mono.just(query);
        }
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        if (null != mediaType && mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
            return exchange.getFormData().mapNotNull(formData -> formData.getFirst(key));
        }
        return Mono.empty();
    }
}
