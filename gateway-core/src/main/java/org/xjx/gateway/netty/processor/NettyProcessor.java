package org.xjx.gateway.netty.processor;

import org.xjx.gateway.context.HttpRequestWrapper;

/**
 *  Netty请求处理器
 */
public interface NettyProcessor {
    void process(HttpRequestWrapper wrapper);
    void start();
    void shutDown();
}
