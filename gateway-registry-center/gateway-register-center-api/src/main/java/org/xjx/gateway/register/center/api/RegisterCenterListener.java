package org.xjx.gateway.register.center.api;

import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInstance;

import java.util.Set;

/**
 *  注册中心监听器
 *  1. 监听注册中心服务信息的变化并及时更新本地状态；
 */
public interface RegisterCenterListener {
    void onChange(ServiceDefinition definition, Set<ServiceInstance>instanceSet);
}
