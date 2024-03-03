package org.xjx.gateway.filter.flow;

import lombok.extern.slf4j.Slf4j;
import org.xjx.common.utils.redis.JedisUtil;

/**
 *  Redis 分布式限流工具类
 *  1.保存请求路径；
 *  2.保存限流的限制次数；
 */
@Slf4j
public class RedisCountLimiter {
    protected JedisUtil jedisUtil;

    public RedisCountLimiter(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }

    private static final int SCRIPT_SUCCESS = 1;
    private static final int SCRIPT_FAILURE = 0;

    /**
     * Redis限流操作：
     * 1.增加 服务/路径 对应键的值，如果返回值为1，则设定超时时间；
     * 2.判断返回值是否超过最大限流阈值；
     * 3.超过则返回0，否则为1；
     * @param key
     * @param limit
     * @param expire
     * @return
     */
    public boolean doFlowCtl(String key, int limit, int expire) {
        try {
            Object object = jedisUtil.executeScript(key, limit, expire);
            if (object == null) {
                return true;
            }
            Long result = Long.valueOf(object.toString());
            if (SCRIPT_FAILURE == result) {
                log.debug("request overflow");
                return false;
            }
        } catch (Exception e) {
            log.error("distribute flowControl throws:{}", e.getMessage());
            throw e;
        }
        return true;
    }
}
