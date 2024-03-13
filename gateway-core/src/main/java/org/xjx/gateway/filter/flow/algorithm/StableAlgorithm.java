package org.xjx.gateway.filter.flow.algorithm;

import com.alibaba.fastjson.JSON;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.utils.redis.JedisUtil;
import org.xjx.gateway.filter.flow.FlowAlgorithmEnum;
import org.xjx.gateway.starter.designpattern.strategy.AbstractExecuteStrategy;

import java.util.Map;

/**
 * 固定窗口限流算法
 */
public class StableAlgorithm implements AbstractExecuteStrategy<Rule.FlowControlConfig, Boolean> {
    private static final String PREFIX = "fixedWindowRateLimiter";
    protected JedisUtil jedisUtil;

    private static final Long SUCCESS_FLAG = 1L;

    @Override
    public String mark() {
        return FlowAlgorithmEnum.FIXED_WINDOWS_ALGORITHM.getAlg();
    }

    @Override
    public String patternMatchMark() {
        return AbstractExecuteStrategy.super.patternMatchMark();
    }

    /**
     * 限流具体操作
     * @param requestParam
     * @return
     */
    @Override
    public Boolean executeResp(Rule.FlowControlConfig requestParam) {
        Map<String, Integer> configMap = JSON.parseObject(requestParam.getConfig(), Map.class);
        if (!configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_DURATION) || !configMap.containsKey(FilterConst.FLOW_CTL_LIMIT_PERMITS)) {
            return false;
        }
        double duration = configMap.get(FilterConst.FLOW_CTL_LIMIT_DURATION);
        double permits = configMap.get(FilterConst.FLOW_CTL_LIMIT_PERMITS);
        return null;
    }

    /**
     * @param id
     * @param limit       请求限制数量
     * @param windowSize  窗口大小
     * @return
     */
    public boolean isAllowed(String id, int limit, int windowSize, int timeout) {
        String lockKey = PREFIX + ":" + "LOCK" + ":" + id;
        // 窗口初始化
        try {
            boolean isLock = jedisUtil.getDistributeLock(lockKey, id, timeout);
            if (isLock) {
                String window_key = PREFIX + ":" + id;
                long current = jedisUtil.atomicIncrBy(window_key, 1);
                if (current == 1) {
                    jedisUtil.setExpire(window_key, timeout);
                }
                if (current > limit) {
                    return false;
                }
                return false;
            }
        } finally {
            jedisUtil.releaseDistributeLock(lockKey, id);
        }
        return false;
    }
}
