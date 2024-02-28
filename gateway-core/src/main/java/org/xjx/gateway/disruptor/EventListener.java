package org.xjx.gateway.disruptor;

/**
 * 事件监听处理器
 * @param <E>
 */
public interface EventListener<E> {
    void onEvent(E event);

    void onException(Throwable ex, long sequence, E event);
}
