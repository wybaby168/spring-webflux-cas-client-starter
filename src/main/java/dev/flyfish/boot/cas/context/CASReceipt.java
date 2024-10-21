package dev.flyfish.boot.cas.context;

import dev.flyfish.boot.cas.exception.CASAuthenticationException;
import dev.flyfish.boot.cas.validator.ProxyTicketValidator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Setter
@ToString
public class CASReceipt implements Serializable {

    private static final Log log = LogFactory.getLog(CASReceipt.class);
    @Getter
    private String casValidateUrl;
    @Getter
    private String pgtIou;
    @Getter
    private boolean primaryAuthentication = false;
    @Getter
    private String proxyCallbackUrl;

    private List<?> proxyList = new ArrayList<>();
    @Getter
    private String userName;

    public static CASReceipt getReceipt(ProxyTicketValidator ptv) throws CASAuthenticationException {
        if (log.isTraceEnabled()) {
            log.trace("entering getReceipt(ProxyTicketValidator=[" + ptv + "])");
        }

        if (!ptv.isAuthenticationSuccessful()) {
            try {
                ptv.validate();
            } catch (Exception e) {
                CASAuthenticationException casException = new CASAuthenticationException("Unable to validate ProxyTicketValidator [" + ptv + "]", e);
                log.error(casException);
                throw casException;
            }
        }

        if (!ptv.isAuthenticationSuccessful()) {
            log.error("validation of [" + ptv + "] was not successful.");
            throw new CASAuthenticationException("Unable to validate ProxyTicketValidator [" + ptv + "]");
        } else {
            CASReceipt receipt = new CASReceipt();
            receipt.casValidateUrl = ptv.getCasValidateUrl();
            receipt.pgtIou = ptv.getPgtIou();
            receipt.userName = ptv.getUser();
            receipt.proxyCallbackUrl = ptv.getProxyCallbackUrl();
            receipt.proxyList = ptv.getProxyList();
            receipt.primaryAuthentication = ptv.isRenew();
            if (!receipt.validate()) {
                throw new CASAuthenticationException("Validation of [" + ptv + "] did not result in an internally consistent CASReceipt.");
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("returning from getReceipt() with return value [" + receipt + "]");
                }

                return receipt;
            }
        }
    }

    public CASReceipt() {
    }

    public List<?> getProxyList() {
        return Collections.unmodifiableList(this.proxyList);
    }

    public boolean isProxied() {
        return !this.proxyList.isEmpty();
    }

    public String getProxyingService() {
        return this.proxyList.isEmpty() ? null : (String) this.proxyList.getFirst();
    }

    private boolean validate() {
        boolean valid = true;
        if (this.userName == null) {
            log.error("Receipt was invalid because userName was null. Receipt:[" + this + "]");
            valid = false;
        }

        if (this.casValidateUrl == null) {
            log.error("Receipt was invalid because casValidateUrl was null.  Receipt:[" + this + "]");
            valid = false;
        }

        if (this.proxyList == null) {
            log.error("receipt was invalid because proxyList was null.  Receipt:[" + this + "]");
            valid = false;
        }

        if (this.primaryAuthentication && !this.proxyList.isEmpty()) {
            log.error("If authentication was by primary credentials then it could not have been proxied. Yet, primaryAuthentication is true where proxyList is not empty.  Receipt:[" + this + "]");
            valid = false;
        }

        return valid;
    }
}
