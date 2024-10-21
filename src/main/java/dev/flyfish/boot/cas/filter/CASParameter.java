package dev.flyfish.boot.cas.filter;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

@Data
public class CASParameter {

    // 排除的过滤地址
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

    public void setAuthorizedProxies(String casAuthorizedProxy) {
        if (casAuthorizedProxy != null) {
            StringTokenizer casProxies = new StringTokenizer(casAuthorizedProxy);

            while (casProxies.hasMoreTokens()) {
                String anAuthorizedProxy = casProxies.nextToken();
                this.authorizedProxies.add(anAuthorizedProxy);
            }
        }
    }

    public String getFullServerUrl() {
        if (StringUtils.hasText(casServerName)) {
            return casServerProtocol + "://" + casServerName;
        }
        return null;
    }

    /**
     * 检查配置参数是否有误
     */
    public CASParameter checked() {
        if (this.casGateway && this.casRenew) {
            throw new IllegalArgumentException("gateway and renew cannot both be true in filter configuration");
        } else if (this.casServerName != null && this.casServiceUrl != null) {
            throw new IllegalArgumentException("serverName and serviceUrl cannot both be set: choose one.");
        } else if (this.casServerName == null && this.casServiceUrl == null) {
            throw new IllegalArgumentException("one of serverName or serviceUrl must be set.");
        } else if (this.casValidate == null) {
            throw new IllegalArgumentException("validateUrl parameter must be set.");
        }
        return this;
    }
}
