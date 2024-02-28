package org.xjx.gateway.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.xjx.common.utils.RemotingUtil;
import org.xjx.gateway.Config;
import org.xjx.gateway.LifeCycle;
import org.xjx.gateway.netty.processor.NettyProcessor;

import java.net.InetSocketAddress;

/**
 * Netty 自定义服务端
 */
@Slf4j
public class NettyHttpServer implements LifeCycle {
    private final Config config;
    private final NettyProcessor processor;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupBoss;
    private EventLoopGroup eventLoopGroupWorker;

    public NettyHttpServer(Config config, NettyProcessor processor) {
        this.config = config;
        this.processor = processor;
        init();
    }

    public EventLoopGroup getEventLoopGroupWorker() {
        return eventLoopGroupWorker;
    }

    @Override
    public void init() {
        this.serverBootstrap = new ServerBootstrap();
        if (useEpoll()) {
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWorker = new EpollEventLoopGroup(config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio"));
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-boss-nio"));
            this.eventLoopGroupWorker = new NioEventLoopGroup(config.getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("netty-worker-nio"));
        }
    }

    /**
     * 是否选用 epoll 优化IO
     * @return
     */
    public boolean useEpoll() {
        return RemotingUtil.isIsLinuxPlatform() && Epoll.isAvailable();
    }

    @Override
    public void start() {
        this.serverBootstrap
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)     // sync_queue + accept_queue = backlog
                .option(ChannelOption.SO_REUSEADDR, true)   // tcp port rebind
                .option(ChannelOption.SO_KEEPALIVE, false)  // heartBeat
                .childOption(ChannelOption.TCP_NODELAY, true) // 非Nagle算法，小数据合并传输
                .childOption(ChannelOption.SO_SNDBUF, 65535)
                .childOption(ChannelOption.SO_RCVBUF, 65535)
                .localAddress(new InetSocketAddress(config.getPort()))
                .childHandler(new ChannelInitializer<Channel>() {
                   @Override
                   protected void initChannel(Channel channel) throws Exception {
                       channel.pipeline().addLast(
                               new HttpServerCodec(),  //http解编码
                               new HttpObjectAggregator(config.getMaxContentLength()), //聚合器，请求报文聚合为 FullHttpRequest
                               new HttpServerExpectContinueHandler(),
                               new NettyHttpServerHandler(processor),
                               new NettyServerConnectManagerHandler()   //http连接管理器
                       );
                   }
                });
        try {
            this.serverBootstrap.bind().sync();
            log.info("server startup on port {}", config.getPort());
        } catch (InterruptedException e) {
            log.error("NettyHttpServer start failed", e);
            throw new RuntimeException();
        }
    }

    @Override
    public void shutdown() {
        if (eventLoopGroupBoss != null) {
            eventLoopGroupBoss.shutdownGracefully();
        }
        if (eventLoopGroupWorker != null) {
            eventLoopGroupWorker.shutdownGracefully();
        }
    }
}
