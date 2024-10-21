package dev.flyfish.boot.cas.filter;

import dev.flyfish.boot.cas.context.CASContext;
import dev.flyfish.boot.cas.context.CASContextInit;

/**
 * 登录过滤器，旨在缓存用户名
 *
 * @author wangyu
 */
public class CASLoginFilter implements CASContextInit {
    public static String CONST_CAS_USERNAME = "const_cas_username";

    @Override
    public String getTranslatorUser(String username) {
        return username;
    }

    @Override
    public void initContext(CASContext casContext, String username) {
        casContext.setSessionAttribute(CONST_CAS_USERNAME, username);
    }
}
