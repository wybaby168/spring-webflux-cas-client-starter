# 开箱即用的webflux cas客户端

中文版 | [For English](README-en.md)

本项目提供了spring webflux的cas客户端支持，用于填补官方空缺的实现。

能够完成以下功能：
- ✅登录状态验证
- ✅鉴权跳转
- ✅鉴权回调处理
- ✅登录会话上下文
- ✅登录用户名
- ✅便捷的controller参数注入
- ✅基于yaml的快捷配置

待添加功能
- ☑️自定义用户转换
- ☑️自定义session处理

## 快速开始

> 本组件必须搭配spring webflux，**不支持spring mvc**

使用maven

```xml
<dependency>
    <groupId>dev.flyfish.boot</groupId>
    <artifactId>spring-webflux-cas-client-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

添加配置

```yaml
cas:
  filter:
    cas-login: https://xxxx/authserver/login               # login web url
    cas-validate: https://xxxx/authserver/serviceValidate  # token validate url
    cas-server-protocol: https                             # local server protocol
    cas-server-name: magnetic-first-yak.ngrok-free.app     # local servername
    cas-init-context-class: dev.flyfish.boot.cas.filter.CASLoginFilter  # class of login interceptor
```

其他配置请参考：
```java

    @JsonAlias(CASFilter.CAS_FILTER_EXCLUSION)
    Set<String> exclusions;

    @JsonAlias(CASFilter.LOGIN_INIT_PARAM)
    String casLogin;

    @JsonAlias(CASFilter.VALIDATE_INIT_PARAM)
    String casValidate;

    @JsonAlias(CASFilter.SERVICE_INIT_PARAM)
    String casServiceUrl;

    @JsonAlias(CASFilter.SERVERNAME_INIT_PARAM)
    String casServerName;

    String casServerProtocol = "http";

    @JsonAlias(CASFilter.PROXY_CALLBACK_INIT_PARAM)
    String casProxyCallbackUrl;

    @JsonAlias(CASFilter.CAS_FILTER_INITCONTEXTCLASS)
    Class<?> casInitContextClass;

    @JsonAlias(CASFilter.RENEW_INIT_PARAM)
    boolean casRenew;

    @JsonAlias(CASFilter.WRAP_REQUESTS_INIT_PARAM)
    boolean wrapRequest;

    @JsonAlias(CASFilter.GATEWAY_INIT_PARAM)
    boolean casGateway = false;

    @JsonAlias(CASFilter.CAS_FILTER_USERLOGINMARK)
    String userLoginMark = null;

    // 已授权的代理地址列表
    @JsonAlias(CASFilter.AUTHORIZED_PROXY_INIT_PARAM)
    List<String> authorizedProxies = new ArrayList<>();
```

## 获取和注入用户名

```java
import dev.flyfish.boot.cas.config.annotation.CASUser;
import org.springframework.web.bind.annotation.GetMapping;

// 使用webSession获取
@GetMapping("username")
public Mono<String> getUsername(ServerWebExchange exchange) {
    exchange.getSession().mapNotNull(session -> session.getAttribute(CASLoginFilter.CONST_CAS_USERNAME));
}

// 使用参数注入
@GetMapping("username-inject")
public Mono<String> getUsername(@CASUser String username) {
    exchange.getSession().mapNotNull(session -> session.getAttribute(CASLoginFilter.CONST_CAS_USERNAME));
}
```
