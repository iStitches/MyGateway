package org.xjx.gateway.filter.flow;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 单机限流——Guava缓存
 */
public class GuavaCountLimiter {
    /**
     * Guava 限流依据
     */
    private RateLimiter rateLimiter;
    /**
     * 最大允许请求量
     */
    private double maxPermits;

    public GuavaCountLimiter(double maxPermits) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits);
    }

    public GuavaCountLimiter(double maxPermits, long warmUpPeriodAsSeconds) {
        this.maxPermits = maxPermits;
        rateLimiter = RateLimiter.create(maxPermits, warmUpPeriodAsSeconds, TimeUnit.SECONDS);
    }

    public double getMaxPermits() {
        return maxPermits;
    }

    public void setMaxPermits(double maxPermits) {
        this.maxPermits = maxPermits;
    }

    /**
     * 请求路径.服务名 —— 限流器
     */
    public static ConcurrentHashMap<String, GuavaCountLimiter> resourceRateLimiterMap = new ConcurrentHashMap<>();

    public static GuavaCountLimiter getInstance(String serviceId, Rule.FlowControlConfig flowControlConfig) {
        if (StringUtils.isEmpty(serviceId) || flowControlConfig == null || StringUtils.isEmpty(flowControlConfig.getValue())
        || StringUtils.isEmpty(flowControlConfig.getConfig()) || StringUtils.isEmpty(flowControlConfig.getType())) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(flowControlConfig.getValue()).toString();
        GuavaCountLimiter countLimiter = resourceRateLimiterMap.get(key);
        // 计算当前流控阈值
        Map<String, Integer> configMap = JSON.parseObject(flowControlConfig.getConfig(), Map.class);
        if (!configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_PERMITS)) {
            return null;
        }
        // 得到流控次数和时间
        double permits = configMap.get(FilterConst.FLOW_CTL_LIMIT_PERMITS);
        double duration = configMap.get(FilterConst.FLOW_CTL_LIMIT_DURATION);
        double perSecondRate = permits / duration;

        // 缓存当前请求流控规则
        if (countLimiter == null) {
            countLimiter = new GuavaCountLimiter(perSecondRate);
            resourceRateLimiterMap.putIfAbsent(key, countLimiter);
        } else if (countLimiter.getMaxPermits() != perSecondRate) {
            countLimiter = new GuavaCountLimiter(perSecondRate);
            resourceRateLimiterMap.put(key, countLimiter);
        }
        return countLimiter;
    }

    public boolean acquire(int permits) {
        boolean success = rateLimiter.tryAcquire(permits);
        if (success) {
            return true;
        } else {
            return false;
        }
    }
}
