package org.xjx.gateway.starter.designpattern.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xjx.gateway.starter.config.ApplicationAutoConfiguration;
import org.xjx.gateway.starter.designpattern.chain.AbstractChainContext;
import org.xjx.gateway.starter.designpattern.strategy.AbstractStrategyChoose;

@ImportAutoConfiguration(ApplicationAutoConfiguration.class)
@Configuration
public class DesignPatternAutoConfiguration {
    /**
     * 策略模式
     */
    @Bean
    public AbstractStrategyChoose abstractExecuteStrategy() {
        return new AbstractStrategyChoose();
    }

    /**
     * 责任链模式
     */
    @Bean
    public AbstractChainContext abstractChainContext() {
        return new AbstractChainContext();
    }
}
