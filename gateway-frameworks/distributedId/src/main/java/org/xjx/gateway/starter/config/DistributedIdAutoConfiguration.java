package org.xjx.gateway.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.xjx.gateway.starter.ApplicationContextHolder;
import org.xjx.gateway.starter.core.snowflake.LocalRedisWorkIdChoose;
import org.xjx.gateway.starter.core.snowflake.RandomWorkIdChoose;

@Import(ApplicationContextHolder.class)
public class DistributedIdAutoConfiguration {
    @Bean
    @ConditionalOnProperty("spring.data.redis.host")
    public LocalRedisWorkIdChoose redisWorkIdChoose() {
        return new LocalRedisWorkIdChoose();
    }

    @Bean
    @ConditionalOnMissingBean(LocalRedisWorkIdChoose.class)
    public RandomWorkIdChoose randomWorkIdChoose() {
        return new RandomWorkIdChoose();
    }
}
