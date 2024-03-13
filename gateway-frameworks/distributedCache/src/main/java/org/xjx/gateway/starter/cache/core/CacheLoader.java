package org.xjx.gateway.starter.cache.core;

/**
 * 缓存加载器
 * @param <T>
 */
@FunctionalInterface
public interface CacheLoader<T> {
    T load();
}
