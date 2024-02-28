package org.xjx.gateway.config.center.nacos.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.xjx.common.config.Rule;
import org.xjx.gateway.config.center.api.ConfigCenter;
import org.xjx.gateway.config.center.api.RulesChangeListener;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Nacos 配置中心实现
 */
@Slf4j
public class NacosConfigCenter implements ConfigCenter {
    /**
     * 服务器地址
     */
    private String serverAddr;

    /**
     * 服务环境
     */
    private String env;

    /**
     * 需要拉取的服务配置的 DATA_ID
     */
    private static final String DATA_ID = "api-gateway";

    /**
     * 获取配置服务
     */
    private ConfigService configService;

    @Override
    public void init(String serverAddr, String env) {
        this.serverAddr = serverAddr;
        this.env = env;
        try {
            this.configService = NacosFactory.createConfigService(serverAddr);
        } catch (NacosException e) {
            log.error("NacosConfigCenter init failed {}", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        try {
            // 获取注册中心保存的配置信息
            String msg = configService.getConfig(DATA_ID, env, 5000);
            log.info("Rules-Config From Nacos: {}", msg);
            List<Rule> rules = JSON.parseObject(msg).getJSONArray("rules").toJavaList(Rule.class);
            // 保存配置信息到本地
            listener.onRulesChange(rules);

            // 配置远程注册中心配置变更的回调函数
            configService.addListener(DATA_ID, env, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("new Config from Nacos: {}", configInfo);
                    List<Rule> newRules = JSON.parseObject(configInfo).getJSONArray("rules").toJavaList(Rule.class);
                    listener.onRulesChange(newRules);
                }
            });
        } catch (NacosException e) {
            log.error("subscribeRulesChange failed", e);
            throw new RuntimeException(e);
        }
    }
}
