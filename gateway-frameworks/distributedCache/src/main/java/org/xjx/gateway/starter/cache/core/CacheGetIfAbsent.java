package org.xjx.gateway.starter.cache.core;

@FunctionalInterface
public interface CacheGetIfAbsent<T> {
    /**
     * 缓存查询为空的处理逻辑
     * @param param
     */
    void execute(T param);
}
