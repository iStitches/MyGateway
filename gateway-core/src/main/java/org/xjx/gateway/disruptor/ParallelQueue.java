package org.xjx.gateway.disruptor;

public interface ParallelQueue<E> {
    void add(E event);
    void add(E... event);

    boolean tryAdd(E event);
    boolean tryAdd(E... event);

    void start();
    void shutdown();
    boolean isShutDown();
}
