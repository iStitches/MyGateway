package org.xjx.gateway.starter.cache.core;

@FunctionalInterface
public interface CacheGetFilter<T> {
    /**
     * 缓存过滤
     */
    boolean filter(T param);
}
