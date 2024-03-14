package org.xjx.gateway.starter.core;

/**
 * 线程池基础接口
 */
public interface ThreadPool<RESPONSE> {
    default String Name() {return "";}
    void execute(Runnable task);
    RESPONSE executeResp(Runnable task);
    void shutdown();
    void shutdownNow();
}
