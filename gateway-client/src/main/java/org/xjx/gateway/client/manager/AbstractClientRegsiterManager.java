package org.xjx.gateway.client.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInstance;
import org.xjx.gateway.client.api.ApiProperties;
import org.xjx.gateway.register.center.api.RegisterCenter;

import java.util.ServiceLoader;

/**
 * 抽象客户端注册管理器，支持多种协议
 */
@Slf4j
public abstract class AbstractClientRegsiterManager {
    @Getter
    private ApiProperties apiProperties;

    private RegisterCenter registerCenter;

    public AbstractClientRegsiterManager(ApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        // spi加载并初始化注册中心对象
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        registerCenter = serviceLoader.findFirst().orElseThrow(()-> {
            log.error("don't find RegisterCenter impl");
            return new RuntimeException("don't find RegisterCenter impl");
        });
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getEnv());
    }

    /**
     * 服务注册————子类可重新实现
     * @param definition
     * @param instance
     */
    protected void registerService(ServiceDefinition definition, ServiceInstance instance) {
        registerCenter.register(definition, instance);
    }
}
