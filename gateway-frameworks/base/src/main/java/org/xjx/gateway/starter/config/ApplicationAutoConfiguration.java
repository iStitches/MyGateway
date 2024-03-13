package org.xjx.gateway.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.xjx.gateway.starter.ApplicationContextHolder;
import org.xjx.gateway.starter.init.ApplicationContentPostProcessor;

public class ApplicationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContentPostProcessor congoApplicationContentPostProcessor(ApplicationContext applicationContext) {
        return new ApplicationContentPostProcessor(applicationContext);
    }

    /**
     * Spring 容器初始化完毕后触发，扫描所有 Bean对象，执行操作
     * @param context
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationContentPostProcessor contentPostProcessor(ApplicationContext context) {
        return new ApplicationContentPostProcessor(context);
    }
}
