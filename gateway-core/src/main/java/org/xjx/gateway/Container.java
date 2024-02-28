package org.xjx.gateway;

import lombok.extern.slf4j.Slf4j;
import org.xjx.common.constants.GatewayConst;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.netty.NettyHttpClient;
import org.xjx.gateway.netty.NettyHttpServer;
import org.xjx.gateway.netty.processor.DisruptorNettyCoreProcessor;
import org.xjx.gateway.netty.processor.NettyCoreProcessor;
import org.xjx.gateway.netty.processor.NettyProcessor;

/**
 * Netty组件集合封装
 */
@Slf4j
public class Container implements LifeCycle{
    private final Config config;
    private NettyHttpServer nettyHttpServer;
    private NettyHttpClient nettyHttpClient;
    private NettyProcessor nettyProcessor;

    public Container(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void init() {
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor();
        // 多生产者多消费者模式
        if (GatewayConst.BUFFER_TYPE_PARALLEL.equals(config.getBufferType())) {
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        } else {
            this.nettyProcessor = nettyCoreProcessor;
        }
        this.nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
        // nettyClient、nettyServer 公用相同 work_threadGroup
        this.nettyHttpClient = new NettyHttpClient(config, nettyHttpServer.getEventLoopGroupWorker());
    }

    @Override
    public void start() {
        nettyProcessor.start();
        nettyHttpServer.start();
        nettyHttpClient.start();
        log.info("api gateway starting!");
    }

    @Override
    public void shutdown() {
        nettyProcessor.shutDown();
        nettyHttpClient.shutdown();
        nettyHttpServer.shutdown();
    }
}
