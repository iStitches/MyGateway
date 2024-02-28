package org.xjx.gateway.filter;

import org.xjx.gateway.context.GatewayContext;

/**
 * 过滤器顶层接口
 * 1.Filter 作为顶层接口，具体子类实现过滤器功能；
 * 2.FilterAspect 作为AOP切面提供增强功能；
 * 3.GatewayFilterChain 定义为过滤器链；
 * 4.GatewayFilterChainFactory 定义为过滤器链工厂，用来构建过滤器链
 */
public interface Filter {
    void doFilter(GatewayContext ctx) throws Exception;

    default int getOrder() {
        FilterAspect aspect = this.getClass().getAnnotation(FilterAspect.class);
        if (aspect != null) {
            return aspect.order();
        }
        return Integer.MAX_VALUE;
    }
}
