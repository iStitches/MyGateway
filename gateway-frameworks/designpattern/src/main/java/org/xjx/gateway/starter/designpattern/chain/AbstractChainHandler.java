package org.xjx.gateway.starter.designpattern.chain;


import org.springframework.core.Ordered;

/**
 * 抽象责任链组件
 */
public interface AbstractChainHandler<T> extends Ordered {
    /**
     * 执行逻辑
     * @param requestParam
     */
    void handler(T requestParam);

    /**
     * 组件标识
     * @return
     */
    String mark();
}
