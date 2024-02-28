package org.xjx.gateway.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.xjx.gateway.context.HttpRequestWrapper;
import org.xjx.gateway.netty.processor.NettyProcessor;

/**
 * NettyServer request processor
 */
@Slf4j
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
    private NettyProcessor processor;

    public NettyHttpServerHandler(NettyProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        HttpRequestWrapper wrapper = new HttpRequestWrapper();
        wrapper.setCtx(ctx);
        wrapper.setRequest(request);
        processor.process(wrapper);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
        log.error("Netty occur exception", cause.getMessage());
    }
}
