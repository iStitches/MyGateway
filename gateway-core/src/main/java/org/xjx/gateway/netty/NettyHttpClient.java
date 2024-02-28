package org.xjx.gateway.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.xjx.gateway.Config;
import org.xjx.gateway.LifeCycle;
import org.xjx.gateway.helper.AsyncHttpHelper;

import java.io.IOException;

/**
 * Netty客户端，做下游服务请求转发
 * 1. 依赖AsyncHttpClient实现
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {
    private final Config config;

    private final EventLoopGroup eventLoopGroupWorker;

    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWorker) {
        this.config = config;
        this.eventLoopGroupWorker = eventLoopGroupWorker;
        init();
    }

    @Override
    public void init() {
        DefaultAsyncHttpClientConfig.Builder httpClientBuilder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroupWorker)            // 工作线程组
                .setConnectTimeout(config.getHttpConnectTimeout())  // 连接超时
                .setRequestTimeout(config.getHttpRequestTimeout())  // 请求超时
                .setMaxRequestRetry(config.getHttpMaxRetryTimes())  // 最大重试请求次数
                .setAllocator(PooledByteBufAllocator.DEFAULT)       // 池化ByteBuf分配器
                .setCompressionEnforced(true)
                .setMaxConnections(config.getHttpMaxConnections())  // 最大连接数
                .setMaxConnectionsPerHost(config.getHttpMaxConnectionsPerHost())
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());
        this.asyncHttpClient = new DefaultAsyncHttpClient(httpClientBuilder.build());
    }

    @Override
    public void start() {
        AsyncHttpHelper.getInstance().inilitized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        if (asyncHttpClient != null) {
            try {
                asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttpClient shutdown failed", e);
            }
        }
    }
}
