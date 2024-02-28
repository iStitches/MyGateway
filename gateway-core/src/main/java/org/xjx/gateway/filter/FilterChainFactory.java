package org.xjx.gateway.filter;

import org.xjx.gateway.context.GatewayContext;

/**
 *  过滤器链工厂 用于生成过滤器链
 */
public interface FilterChainFactory {
    /**
     * 构建过滤器链
     * @param ctx
     * @return
     * @throws Exception
     */
    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;

    /**
     * 根据过滤器ID获取过滤器信息
     * @param filterId
     * @param <T>
     * @return
     * @throws Exception
     */
    <T> T getFilterInfo(String filterId) throws Exception;
}
