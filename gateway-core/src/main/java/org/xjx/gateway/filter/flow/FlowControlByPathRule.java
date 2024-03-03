package org.xjx.gateway.filter.flow;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.LimitedException;
import org.xjx.common.utils.redis.JedisUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  根据请求路径获取具体的流控规则过滤器
 */
public class FlowControlByPathRule implements FlowControlRule {
    // 服务ID
    private String serviceId;
    // 服务路径
    private String path;
    // Redis限流器
    private RedisCountLimiter redisCountLimiter;

    private static final String LIMIT_MESSAGE = "too many requests, try again later";

    private static ConcurrentHashMap<String, FlowControlByPathRule> pathRuleMap = new ConcurrentHashMap<>();

    /**
     * 根据服务ID、请求路径获取限流规则
     * @param serviceId
     * @param path
     * @return
     */
    public static FlowControlByPathRule getInstance(String serviceId, String path) {
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(path).toString();
        FlowControlByPathRule flowByPathRule = pathRuleMap.get(key);
        // 当前服务不在限流规则中，则保存
        if (flowByPathRule == null) {
            flowByPathRule = new FlowControlByPathRule(serviceId, path, new RedisCountLimiter(new JedisUtil()));
            pathRuleMap.put(key, flowByPathRule);
        }
        return flowByPathRule;
    }

    public FlowControlByPathRule(String serviceId, String path, RedisCountLimiter redisCountLimiter) {
        this.serviceId = serviceId;
        this.path = path;
        this.redisCountLimiter = redisCountLimiter;
    }

    /**
     * 限流操作
     * @param flowControlConfig
     * @param serviceId
     */
    @Override
    public void doFlowControlFilter(Rule.FlowControlConfig flowControlConfig, String serviceId) {
        if (flowControlConfig == null || StringUtils.isEmpty(serviceId) || StringUtils.isEmpty(flowControlConfig.getConfig())) {
            return;
        }
        // 获取流控规则的次数限制和时间限制
        Map<String, Integer> configMap = JSON.parseObject(flowControlConfig.getConfig(), Map.class);
        if (!configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_PERMITS)) {
            return;
        }
        double duration = configMap.get(FilterConst.FLOW_CTL_LIMIT_DURATION);
        double permits = configMap.get(FilterConst.FLOW_CTL_LIMIT_PERMITS);
        StringBuffer buffer = new StringBuffer();

        // 区分流控粒度（单机/分布式），判断是否需要流控
        boolean flag = false;
        String key = buffer.append(serviceId).append(".").append(path).toString();
        if (FilterConst.FLOW_CTL_MODE_DISTRIBUTED.equalsIgnoreCase(flowControlConfig.getMode())) {
            flag = redisCountLimiter.doFlowCtl(key, (int)permits, (int)duration);
        } else {
            GuavaCountLimiter guavaCountLimiter = GuavaCountLimiter.getInstance(serviceId, flowControlConfig);
            if (guavaCountLimiter == null) {
                throw new LimitedException(ResponseCode.FLOW_CONTROL_SINGLE_ERROR);
            }
//            double count = Math.ceil(permits / duration);
            flag = guavaCountLimiter.acquire(1);
        }
        if (!flag) {
            throw new LimitedException(ResponseCode.FLOW_CONTROL_ERROR);
        }
    }
}
