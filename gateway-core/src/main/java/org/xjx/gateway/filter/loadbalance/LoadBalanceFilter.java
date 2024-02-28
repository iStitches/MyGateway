package org.xjx.gateway.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.Rule;
import org.xjx.common.config.ServiceInstance;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.NotFoundException;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.filter.Filter;
import org.xjx.gateway.filter.FilterAspect;
import org.xjx.gateway.request.GatewayRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 负载均衡过滤器
 */
@Slf4j
@FilterAspect(id = FilterConst.LOAD_BALANCE_FILTER_ID, name = FilterConst.LOAD_BALANCE_FILTER_NAME, order = FilterConst.LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 获取上下文服务ID
        String serviceId = ctx.getUniqueId();
        // 加载负载均衡策略
        LoadBalanceRule gatewayRule = getLoadBalanceRule(ctx);
        // 选取服务实例，重新构造 Request 请求头
        ServiceInstance instance = gatewayRule.chooseByServiceId(serviceId, ctx.isGray());
        log.info("ServiceInstance ip:{}, port:{}", instance.getIp(), instance.getPort());
        GatewayRequest gatewayRequest = ctx.getRequest();
        if (instance != null && gatewayRequest != null) {
            String modifyHost = instance.getIp() + ":" + instance.getPort();
            gatewayRequest.setModifyHost(modifyHost);
        } else {
            log.error("No instance avaliable for {}", instance);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
    }

    /**
     * 获取负载均衡策略
     * @param context
     * @return
     */
    public LoadBalanceRule getLoadBalanceRule(GatewayContext context) {
        LoadBalanceRule balanceRule = null;
        Rule rule = context.getRules();
        if (rule != null) {
            Set<Rule.FilterConfig> configFilters = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = configFilters.iterator();
            Rule.FilterConfig filterConfig = null;
            // 逐个解析上下文规则中过滤器配置,动态加载过滤器
            while (iterator.hasNext()) {
                filterConfig = iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterId = filterConfig.getId();
                // 解析Rule配置的过滤器属性，获取过滤器类型描述
                if (filterId.equals(FilterConst.LOAD_BALANCE_FILTER_ID)) {
                    String config = filterConfig.getConfig();
                    String strategy = FilterConst.LOAD_BALANCE_STRATEGY_RANDOM;
                    if (StringUtils.isNotEmpty(config)) {
                        Map<String, String> map = JSON.parseObject(config, Map.class);
                        strategy = map.getOrDefault(FilterConst.LOAD_BALANCE_KEY, strategy);
                    }
                    switch (strategy) {
                        case FilterConst.LOAD_BALANCE_STRATEGY_RANDOM:
                            balanceRule = RandomLoadBalanceRule.getInstance(rule.getServiceId());break;
                        case FilterConst.LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            balanceRule = RoundRobinLoadBalanceRule.getInstance(rule.getServiceId());break;
                        default:
                            log.warn("no loadBalanceRule can be load for service:{}", strategy);
                            balanceRule = RandomLoadBalanceRule.getInstance(rule.getServiceId());break;
                    }
                }
            }
        }
        return balanceRule;
    }
}
