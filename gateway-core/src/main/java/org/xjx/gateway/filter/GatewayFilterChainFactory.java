package org.xjx.gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;
import org.xjx.gateway.context.GatewayContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 过滤器工厂具体实现类
 * 1.根据SPI 动态加载驱动实现的过滤器类对象，并存储到本地内存；
 * 2.根据注册中心配置的规则策略，加载实时可用的过滤器，组装为网关过滤器链。
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterChainFactory{
    // 本地实现的过滤器对象缓存
    private Map<String, Filter> processorFilterIdMap = new ConcurrentHashMap<>();

    private Map<String, String> processFilterIdName = new ConcurrentHashMap<>();

    private static class SingleInstanceHolder {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    public static GatewayFilterChainFactory getInstance() {
        return SingleInstanceHolder.INSTANCE;
    }

    /**
     * 过滤器链缓存（服务ID——>过滤器链）
     * ruleId——GatewayFilterChain
     */
    private Cache<String, GatewayFilterChain> chainCache = Caffeine.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.SECONDS).build();

    /**
     * SPI加载本地过滤器实现类对象
     */
    public GatewayFilterChainFactory() {
        ServiceLoader<Filter> filters = ServiceLoader.load(Filter.class);
        filters.stream().forEach(filterProvider -> {
            Filter filter = filterProvider.get();
            FilterAspect aspect = filter.getClass().getAnnotation(FilterAspect.class);
            log.info("load filters by spi : {},{},{},{}", filter.getClass(), aspect.id(), aspect.name(), filter.getOrder());
            if (aspect != null) {
                String filterId = aspect.id();
                if (StringUtils.isEmpty(filterId)) {
                    filterId = filter.getClass().getName();
                }
                processorFilterIdMap.put(filterId, filter);
                processFilterIdName.put(filterId, aspect.name());
            }
        });
    }

    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        return chainCache.get(ctx.getRules().getId(), k->doBuildFilterChain(ctx.getRules()));
    }

    /**
     * 参考注册中心配置构建过滤器链
     * @param rule
     * @return
     */
    public GatewayFilterChain doBuildFilterChain(Rule rule) {
        GatewayFilterChain chain = new GatewayFilterChain();
        List<Filter> contextFilters = new ArrayList<>();
        if (rule != null) {
            Set<Rule.FilterConfig> configFilters = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = configFilters.iterator();
            while (iterator.hasNext()) {
                Rule.FilterConfig config = iterator.next();
                if (config == null) {
                    continue;
                }
                String filterConfigId = config.getId();
                if (StringUtils.isNotEmpty(filterConfigId) && processorFilterIdMap.containsKey(filterConfigId)) {
                    Filter filter = processorFilterIdMap.get(filterConfigId);
                    log.info("set filter into filterChain, {} {}", filterConfigId, processFilterIdName.get(filterConfigId));
                    contextFilters.add(filter);
                }
            }
        }
        // 每个服务请求最终最后需要添加路由过滤器
        contextFilters.add(processorFilterIdMap.get(FilterConst.ROUTER_FILTER_ID));
        // 过滤器排序
        contextFilters.sort(Comparator.comparingInt(Filter::getOrder));
        chain.addFilterList(contextFilters);
        return chain;
    }

    @Override
    public Filter getFilterInfo(String filterId) throws Exception {
        return processorFilterIdMap.get(filterId);
    }
}
