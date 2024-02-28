package org.xjx.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.xjx.gateway.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关过滤器链
 */
@Slf4j
public class GatewayFilterChain {
    private List<Filter> filters = new ArrayList<>();

    public GatewayFilterChain addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public GatewayFilterChain addFilterList(List<Filter> list) {
        filters.addAll(list);
        return this;
    }

    /**
     * 执行过滤器链
     * @param ctx
     * @throws Exception
     */
    public GatewayContext doFilter(GatewayContext ctx) {
        if (filters.isEmpty()) {
            return ctx;
        }
        try {
            for (Filter filter : filters) {
                filter.doFilter(ctx);
            }
        } catch (Exception e) {
            log.error("执行过滤器发生异常: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return ctx;
    }
}
