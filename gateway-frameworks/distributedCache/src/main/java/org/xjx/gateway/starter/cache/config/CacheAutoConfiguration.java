package org.xjx.gateway.starter.cache.config;

import lombok.AllArgsConstructor;
import org.redisson.RedissonBloomFilter;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.xjx.gateway.starter.cache.RedisKeySerializer;
import org.xjx.gateway.starter.cache.StringRedisTemplateProxy;

/**
 * 缓存自动装配
 */
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({RedisDistributedProperties.class, BloomFilterPenetrateProperties.class})
public class CacheAutoConfiguration {
    private final RedisDistributedProperties redisDistributedProperties;

    /**
     * 注册Redis序列化器
     */
    @Bean
    public RedisKeySerializer redisKeySerializer() {
        String prefix = redisDistributedProperties.getPrefix();
        String prefixCharset = redisDistributedProperties.getPrefixCharset();
        return new RedisKeySerializer(prefix, prefixCharset);
    }

    /**
     * 注册布隆过滤器防止缓存穿透
     */
    @Bean
    public RBloomFilter<String> cachePenetrationBloomFilter(RedissonClient redissonClient, BloomFilterPenetrateProperties properties) {
        RBloomFilter<String> cacheFilter = redissonClient.getBloomFilter(properties.getName());
        cacheFilter.tryInit(properties.getExpectedInsertions(), properties.getFalseProbability());
        return cacheFilter;
    }

    /**
     * 静态代理，代理增强 Redis 客户端
     */
    @Bean
    public StringRedisTemplateProxy stringRedisTemplateProxy(RedisKeySerializer redisKeySerializer,
                                                             StringRedisTemplate stringRedisTemplate,
                                                             RedissonClient redissonClient) {
        stringRedisTemplate.setKeySerializer(redisKeySerializer);
        return new StringRedisTemplateProxy(stringRedisTemplate, redisDistributedProperties, redissonClient);
    }
}
