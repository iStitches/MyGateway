package org.xjx.gateway.netty.processor;

import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.xjx.common.enums.ResponseCode;
import org.xjx.gateway.Config;
import org.xjx.gateway.WaitStrategyFactory;
import org.xjx.gateway.context.HttpRequestWrapper;
import org.xjx.gateway.disruptor.EventListener;
import org.xjx.gateway.disruptor.ParallelQueueHandler;
import org.xjx.gateway.helper.ResponseHelper;

/**
 *  Disruptor 提升 Netty 性能
 *  通过 Disruptor 异步处理 HTTP 请求提升性能
 *  并行处理器
 */
@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor{
    private static final String THREAD_NAME_PREFIX = "gateway-queue-";

    private Config config;

    private NettyCoreProcessor nettyCoreProcessor;

    private ParallelQueueHandler<HttpRequestWrapper> parallelQueueHandler;

    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor) {
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;
        ParallelQueueHandler.Builder<HttpRequestWrapper> builder = new ParallelQueueHandler.Builder<HttpRequestWrapper>()
                .setBufferSize(config.getBufferSize())
                .setNamePrefix(THREAD_NAME_PREFIX)
                .setThreads(config.getProcessThread())
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(WaitStrategyFactory.getWaitStrategy(config.getWaitStrategy()));
        // 监听事件并处理
        BatchEventListenerProcessor processor = new BatchEventListenerProcessor();
        builder.setEventListener(processor);
        this.parallelQueueHandler = builder.build();
    }

    @Override
    public void process(HttpRequestWrapper wrapper) {
        this.parallelQueueHandler.add(wrapper);
    }

    @Override
    public void start() {
        parallelQueueHandler.start();
    }

    @Override
    public void shutDown() {
        parallelQueueHandler.shutdown();
    }

    /**
     * 监听处理从 disruptor 队列中取出的事件
     */
    public class BatchEventListenerProcessor implements EventListener<HttpRequestWrapper> {
        @Override
        public void onEvent(HttpRequestWrapper event) {
            // 调用 Netty 处理事件
            nettyCoreProcessor.process(event);
        }

        @Override
        public void onException(Throwable ex, long sequence, HttpRequestWrapper event) {
            FullHttpRequest request = event.getRequest();
            ChannelHandlerContext context = event.getCtx();
            try {
                log.error("BatchEventListenerProcessor onException failed, request:{}, errMsg:{}", request, ex.getMessage(), ex);
                // 构建响应对象
                FullHttpResponse response = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                // 非长连接直接关闭
                if (!HttpUtil.isKeepAlive(request)) {
                    context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    context.writeAndFlush(response);
                }
            } catch (Exception e) {
                log.error("BatchEventListenerProcessor onException, request:{}, errMsg:{}", request, e.getMessage(), e);
            }
        }
    }
}
