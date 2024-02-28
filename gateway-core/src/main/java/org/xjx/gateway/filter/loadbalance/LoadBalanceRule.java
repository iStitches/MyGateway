package org.xjx.gateway.filter.loadbalance;

import org.xjx.common.config.ServiceInstance;
import org.xjx.gateway.context.GatewayContext;

/**
 * 负载均衡策略接口
 */
public interface LoadBalanceRule {
    ServiceInstance choose(GatewayContext ctx, boolean gray);

    ServiceInstance chooseByServiceId(String serviceId, boolean gray);
}