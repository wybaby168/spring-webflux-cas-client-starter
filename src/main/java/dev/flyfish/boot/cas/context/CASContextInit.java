package dev.flyfish.boot.cas.context;

/**
 * 上下文初始化逻辑
 *
 * @author wangyu
 */
public interface CASContextInit {

    String getTranslatorUser(String username);

    void initContext(CASContext casContext, String username);
}
