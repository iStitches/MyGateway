package org.xjx.gateway.filter.flow;

import org.xjx.common.config.Rule;

/**
 *  根据请求路径执行限流操作
 */
public class FlowControlByServiceRule implements FlowControlRule{
    @Override
    public void doFlowControlFilter(Rule.FlowControlConfig flowControlConfig, String serviceId) {

    }
}
