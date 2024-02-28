package org.xjx.gateway;

/**
 * 组件生命周期接口
 */
public interface LifeCycle {
    void init();

    void start();

    void shutdown();
}
