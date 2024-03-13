package org.xjx.gateway.filter.flow.algorithm;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.utils.redis.JedisUtil;
import org.xjx.gateway.filter.flow.FlowAlgorithmEnum;
import org.xjx.gateway.starter.designpattern.strategy.AbstractExecuteStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 令牌桶限流算法
 * 1.思想为以固定速率向桶中放入令牌，每消费一次移除一个令牌；
 * 2.整个过程lua脚本实现确保原子性
 *   2.1 Redis存储结构：String(令牌key-value)、String(最近一次消费令牌key-value)
 *   2.2 限流思路：每次请求以 服务ID+请求路径 作为令牌Key，以服务ID+请求路径+timeStamp 作为时间戳Key；
 *               预先加载这些变量，查询最近一次请求剩余的令牌数量，如果为空说明需要初始化，初始化令牌为最大容量，过期时间设置为填满时间的两倍；
 *               获取最近一次获取令牌的时间戳，当前时间戳减去最近一次时间戳求出应该放入多少令牌；
 *               放入的令牌+上次剩余令牌 > 本次需要的令牌：成功获取
 *               最终重置两个Key的存活时间
 */
public class VoteBucketAlgorithm implements AbstractExecuteStrategy<Rule.FlowControlConfig, Boolean> {
    private static final String PREFIX = "voteBucketRateLimiter";
    private static final Integer MAX_BUCKET_VOLUME = 10;
    private static final Integer FILL_SPEED_PERSECOND = 1;

    protected JedisUtil jedisUtil;

    private static final Long SUCCESS_FLAG = 1L;

    @Override
    public String mark() {
        return FlowAlgorithmEnum.VOTE_BUCKET_ALGORITHM.getAlg();
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
        return isAllowed(requestParam.getValue(), (int)(permits/duration), (int)permits, 1);
    }

    /**
     * 判断当前桶是否还有剩余令牌
     * @param id        服务ID+请求路径
     * @param rate      填充速率
     * @param capicity  容量
     * @param tokens    需要令牌数
     * @return
     */
    public boolean isAllowed(String id, int rate, int capicity, int tokens) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("vote_bucket_flow.lua")));
        StringBuilder builder = new StringBuilder();
        String line = null;
        try {
            while (StringUtils.isNotEmpty(line = reader.readLine())) {
                builder.append(line);
            }
            Object ans = jedisUtil.executeLuaScript(builder.toString(), getKey(id), String.valueOf(rate), String.valueOf(capicity),
                    String.valueOf(Instant.now().getEpochSecond()), String.valueOf(tokens));
            return SUCCESS_FLAG.equals(ans);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<String> getKey(String id) {
        String prefix = PREFIX + ":" + id;
        String tokenKey = prefix + ":tokens";
        String timestampKey = prefix + ":timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }

}
