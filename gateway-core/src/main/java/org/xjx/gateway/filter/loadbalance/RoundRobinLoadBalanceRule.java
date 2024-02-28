package org.xjx.gateway.filter.loadbalance;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.xjx.common.config.DynamicConfigManager;
import org.xjx.common.config.Rule;
import org.xjx.common.config.ServiceInstance;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.ResponseException;
import org.xjx.gateway.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  轮询——负载均衡策略
 */
@Slf4j
public class RoundRobinLoadBalanceRule implements LoadBalanceRule{
    private static Map<String, RoundRobinLoadBalanceRule> loadBalanceRuleMap = new ConcurrentHashMap<>();

    private AtomicInteger position = new AtomicInteger(0);

    private String serviceId;

    public RoundRobinLoadBalanceRule(String serviceId) {
        this.serviceId = serviceId;
    }

    public static RoundRobinLoadBalanceRule getInstance(String serviceId) {
        RoundRobinLoadBalanceRule rule = loadBalanceRuleMap.get(serviceId);
        if (rule == null) {
            rule = new RoundRobinLoadBalanceRule(serviceId);
            loadBalanceRuleMap.put(serviceId, rule);
        }
        return rule;
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx, boolean gray) {
        // 获取上下文 Rule 对象
        Rule rule = ctx.getRules();
        return chooseByServiceId(rule.getServiceId(), gray);
    }

    @Override
    public ServiceInstance chooseByServiceId(String serviceId, boolean gray) {
        // 根据服务ID获取服务实例集合
        Set<ServiceInstance> serviceSets = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
        if (CollectionUtils.isEmpty(serviceSets)) {
            log.warn("serviceId {} don't match any serviceInstance", serviceId);
            throw new ResponseException(ResponseCode.SERVICE_INVOKER_NOT_FOUND);
        }
        List<ServiceInstance> serviceLists = new ArrayList<>(serviceSets);
        int pos = Math.abs(position.incrementAndGet());
        return serviceLists.get(pos % serviceLists.size());
    }
}
