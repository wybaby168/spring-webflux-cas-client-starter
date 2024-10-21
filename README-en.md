# **Ready-to-use WebFlux CAS Client**

[中文版](README.md) | For English

This project provides a CAS client implementation for Spring WebFlux to fill the gap in the official implementation.

**Supported Features:**
* ✅ Login status verification
* ✅ Authentication redirect and callback handling
* ✅ Authentication callback processing
* ✅ Login session context
* ✅ Logged-in username
* ✅ Convenient controller parameter injection
* ✅ YAML-based configuration

**Features to be added:**
* ☑️ Custom user conversion
* ☑️ Custom session handling

## **Quick Start**

> This component requires Spring WebFlux and **does not support Spring MVC**.

Using Maven:

```xml
<dependency>
    <groupId>dev.flyfish.boot</groupId>
    <artifactId>spring-webflux-cas-client-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

Add configuration:

```yaml
cas:
  filter:
    cas-login: https://xxxx/authserver/login               # login web url
    cas-validate: https://xxxx/authserver/serviceValidate  # token validate url
    cas-server-protocol: https                             # local server protocol
    cas-server-name: magnetic-first-yak.ngrok-free.app     # local servername
    cas-init-context-class: dev.flyfish.boot.cas.filter.CASLoginFilter  # class of login interceptor
```

For other configurations, refer to:

```java
// ... configuration properties ...
```

## **Getting and Injecting Username**

```java
import dev.flyfish.boot.cas.config.annotation.CASUser;
import org.springframework.web.bind.annotation.GetMapping;

// Using webSession to get
@GetMapping("username")
public Mono<String> getUsername(ServerWebExchange exchange) {
    return exchange.getSession().mapNotNull(session -> session.getAttribute(CASLoginFilter.CONST_CAS_USERNAME));
}

// Using parameter injection
@GetMapping("username-inject")
public Mono<String> getUsername(@CASUser String username) {
    return exchange.getSession().mapNotNull(session -> session.getAttribute(CASLoginFilter.CONST_CAS_USERNAME));
}
```
