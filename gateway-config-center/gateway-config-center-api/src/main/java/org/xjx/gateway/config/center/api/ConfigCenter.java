package org.xjx.gateway.config.center.api;

/**
 * 配置中心接口
 */
public interface ConfigCenter {
    /**
     * 初始化
     * @param serverAddr
     * @param env
     */
    void init(String serverAddr, String env);

    /**
     * 订阅配置中心配置变更
     * @param listener
     */
    void subscribeRulesChange(RulesChangeListener listener);
}
