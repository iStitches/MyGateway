package org.xjx.gateway.starter.cache.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存穿透布隆过滤器
 */
@Data
@ConfigurationProperties(prefix = BloomFilterPenetrateProperties.PREFIX)
public class BloomFilterPenetrateProperties {
    public static final String PREFIX = "framework.cache.redis.bloom-filter.default";

    /**
     * 布隆过滤器默认实例
     */
    private String name = "cache_penetration_bloom_filter";

    /**
     * 预期插入量
     */
    private Long expectedInsertions = 64000L;

    /**
     * 预期错误率
     */
    private Double falseProbability = 0.03D;
}
