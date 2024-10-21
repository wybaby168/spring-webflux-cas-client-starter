package dev.flyfish.boot.cas.filter;

import dev.flyfish.boot.cas.context.CASContext;
import dev.flyfish.boot.cas.context.CASContextInit;
import dev.flyfish.boot.cas.context.CASReceipt;
import dev.flyfish.boot.cas.context.SessionMappingStorage;
import dev.flyfish.boot.cas.exception.CASAuthenticationException;
import dev.flyfish.boot.cas.validator.ProxyTicketValidator;
import dev.flyfish.boot.cas.validator.XmlUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * cas filter的webflux实现
 *
 * @author wangyu
 * 实现相关核心逻辑，完成鉴权信息抽取
 */
@Slf4j
public class CASFilter implements WebFilter {

    public static final String LOGIN_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.loginUrl";
    public static final String VALIDATE_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.validateUrl";
    public static final String SERVICE_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.serviceUrl";
    public static final String SERVERNAME_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.serverName";
    public static final String RENEW_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.renew";
    public static final String AUTHORIZED_PROXY_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.authorizedProxy";
    public static final String PROXY_CALLBACK_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.proxyCallbackUrl";
    public static final String WRAP_REQUESTS_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.wrapRequest";
    public static final String GATEWAY_INIT_PARAM = "edu.yale.its.tp.cas.client.filter.gateway";
    public static final String CAS_FILTER_USER = "edu.yale.its.tp.cas.client.filter.user";
    public static final String CAS_FILTER_RECEIPT = "edu.yale.its.tp.cas.client.filter.receipt";

    static final String CAS_FILTER_GATEWAYED = "edu.yale.its.tp.cas.client.filter.didGateway";
    static final String CAS_FILTER_INITCONTEXTCLASS = "edu.yale.its.tp.cas.client.filter.initContextClass";
    static final String CAS_FILTER_USERLOGINMARK = "edu.yale.its.tp.cas.client.filter.userLoginMark";
    static final String CAS_FILTER_EXCLUSION = "edu.yale.its.tp.cas.client.filter.filterExclusion";

    private final CASParameter parameter;
    private final CASContextInit initializer;
    @Getter
    private final SessionMappingStorage sessionMappingStorage = new SessionMappingStorage.HashMapBackedSessionStorage();

    public CASFilter(CASParameter parameter) {
        this.parameter = parameter.checked();
        this.initializer = createInitializer();
    }

    private CASContextInit createInitializer() {
        if (null != parameter.casInitContextClass) {
            try {
                // 未正确配置类型，抛弃
                Class<? extends CASContextInit> cls = parameter.casInitContextClass.asSubclass(CASContextInit.class);
                // 实例化对象并返回
                return cls.getConstructor().newInstance();
            } catch (ClassCastException e) {
                log.warn("cas context init class not implements CASContextInit", e);
            } catch (IllegalArgumentException | IllegalAccessException | InstantiationException
                     | SecurityException | NoSuchMethodException e) {
                log.error("error when initialize the context init class", e);
            } catch (InvocationTargetException e) {
                log.error("error when create the cas context initializer's instance!", e);
            }
        }
        return null;
    }

    private boolean isReceiptAcceptable(CASReceipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("Cannot evaluate a null receipt.");
        } else if (parameter.casRenew && !receipt.isPrimaryAuthentication()) {
            return false;
        } else {
            return !receipt.isProxied() || parameter.authorizedProxies.contains(receipt.getProxyingService());
        }
    }

    private CASReceipt getAuthenticatedUser(CASContext context) throws CASAuthenticationException {
        log.trace("entering getAuthenticatedUser()");
        ProxyTicketValidator pv = new ProxyTicketValidator();
        pv.setCasValidateUrl(parameter.casValidate);
        pv.setServiceTicket(context.getTicket());
        pv.setService(this.getService(context));
        pv.setRenew(parameter.casRenew);
        if (parameter.casProxyCallbackUrl != null) {
            pv.setProxyCallbackUrl(parameter.casProxyCallbackUrl);
        }

        log.debug("about to validate ProxyTicketValidator: [{}]", pv);

        return CASReceipt.getReceipt(pv);
    }

    private String getService(CASContext context) {
        log.trace("entering getService()");

        if (parameter.casServerName == null && parameter.casServiceUrl == null) {
            throw new IllegalArgumentException("need one of the following configuration parameters: edu.yale.its.tp.cas.client.filter.serviceUrl or edu.yale.its.tp.cas.client.filter.serverName");
        }

        String serviceString;
        if (parameter.casServiceUrl != null) {
            serviceString = URLEncoder.encode(parameter.casServiceUrl, StandardCharsets.UTF_8);
        } else {
            serviceString = computeService(context, parameter.getFullServerUrl());
        }

        if (log.isTraceEnabled()) {
            log.trace("returning from getService() with service [{}]", serviceString);
        }
        return serviceString;
    }

    /**
     * 计算服务地址，主要是替换url中server的部分，并去除ticket
     *
     * @param context 上下文
     * @param server  服务
     * @return 结果
     */
    public String computeService(CASContext context, String server) {
        if (log.isTraceEnabled()) {
            log.trace("entering getService({}, {})", context, server);
        }

        if (server == null) {
            log.error("getService() argument \"server\" was illegally null.");
            throw new IllegalArgumentException("name of server is required");
        }

        URI uri = context.getRequest().getURI();

        StringBuilder sb = new StringBuilder(server).append(uri.getPath());

        if (uri.getQuery() != null) {
            String query = uri.getQuery();

            int ticketLoc = query.indexOf("ticket=");
            if (ticketLoc == -1) {
                sb.append("?").append(query);
            } else if (ticketLoc > 0) {
                ticketLoc = query.indexOf("&ticket=");
                if (ticketLoc == -1) {
                    sb.append("?").append(query);
                } else if (ticketLoc > 0) {
                    sb.append("?").append(query, 0, ticketLoc);
                }
            }
        }

        String encodedService = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
        if (log.isTraceEnabled()) {
            log.trace("returning from getService() with encoded service [{}]", encodedService);
        }

        return encodedService;
    }

    /**
     * 核心，跳转cas服务器鉴权
     *
     * @param context 上下文
     * @return 结果
     */
    private Mono<Void> redirectToCAS(CASContext context) {
        ServerHttpRequest request = context.getRequest();
        String sessionId = context.getSession().getId();

        log.trace("entering redirectToCAS()");

        StringBuilder casLoginString = new StringBuilder()
                .append(parameter.casLogin)
                .append("?service=").append(this.getService(context))
                .append(parameter.casRenew ? "&renew=true" : "")
                .append(parameter.casGateway ? "&gateway=true" : "");

        if (StringUtils.hasText(sessionId)) {
            String appId = parameter.casServerName + request.getPath().contextPath().value();
            casLoginString.append("&appId=").append(URLEncoder.encode(appId, StandardCharsets.UTF_8))
                    .append("&sessionId=").append(sessionId);
        }

        List<HttpCookie> cookies = request.getCookies().get("JSESSIONID");
        if (!CollectionUtils.isEmpty(cookies)) {
            cookies.stream()
                    .filter(Objects::nonNull)
                    .map(HttpCookie::getValue)
                    .filter(cookie -> !cookie.equals("null") && !cookie.equals(sessionId))
                    .peek(cookie -> log.debug("Session is timeout. The timeout session is {}", cookie))
                    .findFirst()
                    .ifPresent(cookie -> casLoginString.append("&timeOut=").append(cookie));
        }

        log.debug("Redirecting browser to [{})", casLoginString);
        log.trace("returning from redirectToCAS()");

        return context.redirect(casLoginString.toString());
    }

    private Mono<Void> redirectToInitFailure(CASContext context, String cause) {
        log.trace("entering redirectToInitFailure()");

        String casLoginString = parameter.casLogin + "?action=initFailure";
        if (cause != null && cause.equals("Illegal user")) {
            casLoginString += "&userIllegal=true";
        }

        String locale = context.getQuery("locale");
        if (locale != null) {
            casLoginString += "&locale=" + locale;
        }

        log.debug("Redirecting browser to [{})", casLoginString);
        log.trace("returning from redirectToInitFailure()");
        return context.redirect(casLoginString);
    }

    private boolean isExclusion(String url) {
        if (parameter.exclusions == null) {
            return false;
        } else {
            return parameter.exclusions.contains(url);
        }
    }

    private Mono<Void> translate(CASContext context) {
        // 是代理回调地址，通过
        if (parameter.casProxyCallbackUrl != null && parameter.casProxyCallbackUrl.endsWith(context.getPath())
                && context.getQuery("pgtId") != null && context.getQuery("pgtIou") != null) {
            log.trace("passing through what we hope is CAS's request for proxy ticket receptor.");
            return context.filter();
        }

        // 请求包装，增强请求并完成自定义功能
        if (parameter.wrapRequest) {
            log.trace("Wrapping request with CASFilterRequestWrapper.");
            // todo 暂时啥也不干，看看有无问题
//                request = new CASFilterRequestWrapper((HttpServletRequest) request);
        }

        WebSession session = context.getSession();
        Map<String, Object> sessionAttributes = session.getAttributes();

        // 使用了用户标记，快速跳过
        if (parameter.userLoginMark != null && session.getAttribute(parameter.userLoginMark) != null) {
            return context.filter();
        }

        // 获取receipt，若存在，则通过
        CASReceipt receipt = session.getAttribute(CAS_FILTER_RECEIPT);
        if (receipt != null && this.isReceiptAcceptable(receipt)) {
            log.trace("CAS_FILTER_RECEIPT attribute was present and acceptable - passing  request through filter..");
            return context.filter();
        }

        // 命中排除地址，跳过请求
        if (this.isExclusion(context.getPath())) {
            return context.filter();
        }

        // 判断票据
        String ticket = context.getTicket();
        // 存在票据时，验证票据
        if (StringUtils.hasText(ticket)) {
            try {
                receipt = this.getAuthenticatedUser(context);
            } catch (CASAuthenticationException e) {
                return this.redirectToCAS(context);
            }

            if (!this.isReceiptAcceptable(receipt)) {
                throw new IllegalStateException("Authentication was technically successful but rejected as a matter of policy. [" + receipt + "]");
            }

            // 记录receipt
            String pt = context.getQuery("pt");
            if (StringUtils.hasText(pt)) {
                context.setSessionAttribute(pt, receipt);
            }

            // 获取到用户名
            String userName = receipt.getUserName();
            // 尝试初始化
            if (null != initializer) {
                try {
                    String translated = initializer.getTranslatorUser(userName);
                    log.debug("translated username: {} to {}", userName, translated);
                    initializer.initContext(context, translated);
                } catch (Exception e) {
                    String cause = e.getCause().getMessage();
                    context.setSessionAttribute("initFailure", cause);
                    return this.redirectToInitFailure(context, cause);
                }
            }

            sessionAttributes.put(CAS_FILTER_USER, userName);
            sessionAttributes.put(CAS_FILTER_RECEIPT, receipt);
            sessionAttributes.remove(CAS_FILTER_GATEWAYED);

            if (log.isTraceEnabled()) {
                log.trace("validated ticket to get authenticated receipt [{}], now passing request along filter chain.", receipt);
                log.trace("returning from doFilter()");
            }

            return context.filter();
        }

        // 不存在票据，跳转验证
        log.trace("CAS ticket was not present on request.");
        boolean didGateway = Boolean.parseBoolean(session.getAttribute(CAS_FILTER_GATEWAYED));
        if (parameter.casLogin == null) {
            log.error("casLogin was not set, so filter cannot redirect request for authentication.");
            throw new IllegalArgumentException("When CASFilter protects pages that do not receive a 'ticket' parameter, it needs a edu.yale.its.tp.cas.client.filter.loginUrl filter parameter");
        }

        if (!didGateway) {
            log.trace("Did not previously gateway.  Setting session attribute to true.");
            sessionAttributes.put(CAS_FILTER_GATEWAYED, "true");
            return this.redirectToCAS(context);
        }

        log.trace("Previously gatewayed.");
        if (!parameter.casGateway && session.getAttribute(CAS_FILTER_USER) == null) {
            if (session.getAttribute("initFailure") != null) {
                String cause = session.getAttribute("initFailure");
                return this.redirectToInitFailure(context, cause);
            }

            sessionAttributes.put(CAS_FILTER_GATEWAYED, "true");
            return this.redirectToCAS(context);
        }

        log.trace("casGateway was true and CAS_FILTER_USER set: passing request along filter chain.");
        return context.filter();
    }

    /**
     * 二阶段处理，预处理特殊情况，提前中断请求
     *
     * @param context 上下文工具
     * @return 结果
     */
    private Mono<Void> handle(CASContext context) {
        // 优先处理token请求
        if (context.isTokenRequest()) {
            String sessionId = context.getSession().getId();
            log.debug("Storing session identifier for {}", sessionId);

            // 包括ticket，尝试重新替换session
            return sessionMappingStorage.removeBySessionById(sessionId)
                    .onErrorContinue((e, v) -> log.debug("error when remove session"))
                    .then(Mono.defer(() -> sessionMappingStorage.addSessionById(context.getTicket(), context.getSession())
                            .then(translate(context))));
        }
        // post请求需要特殊处理
        if (context.getMethod() == HttpMethod.POST) {
            // 通过form表单获取注销请求，处理注销逻辑
            return context.getFormData("logoutRequest")
                    .doOnNext(payload -> log.trace("Logout request=[{}]", payload))
                    .defaultIfEmpty("")
                    .flatMap(payload -> {
                        if (StringUtils.hasText(payload)) {
                            String token = XmlUtils.getTextForElement(payload, "SessionIndex");
                            if (StringUtils.hasText(token)) {
                                // 满足条件时断路
                                return sessionMappingStorage.removeSessionByMappingId(token)
                                        .doOnNext(session -> log.debug("Invalidating session [{}] for ST [{}]", session.getId(), token))
                                        .flatMap(WebSession::invalidate)
                                        .doOnError(IllegalStateException.class, e -> log.debug(e.getMessage(), e))
                                        .onErrorComplete();
                            }
                        }
                        // 继续执行
                        return translate(context);
                    });
        }
        return translate(context);
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 拦截器需要基于session判定，故提前使用
        return CASContext.create(exchange, chain)
                .flatMap(context -> {
                    WebSession session = context.getSession();
                    if (log.isTraceEnabled()) {
                        log.trace("entering doFilter()");
                    }
                    // 执行跳过策略
                    String pt = context.getQuery("pt");
                    if (StringUtils.hasText(pt)) {
                        if (session.getAttribute(pt) != null) {
                            return context.filter();
                        }
                    }
                    return handle(context);
                });
    }


}
