package org.xjx.gateway.filter.flow;

import org.xjx.common.config.Rule;

/**
 *  限流规则接口
 *  1.注册中心限流配置；
 *  2.服务ID；
 */
public interface FlowControlRule {
    void doFlowControlFilter(Rule.FlowControlConfig flowControlConfig, String serviceId);
}
