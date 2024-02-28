package org.xjx.gateway.netty.processor;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.BaseException;
import org.xjx.common.exception.ConnectException;
import org.xjx.common.exception.LimitedException;
import org.xjx.common.exception.ResponseException;
import org.xjx.gateway.ConfigLoader;
import org.xjx.gateway.context.ContextStatus;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.context.HttpRequestWrapper;
import org.xjx.gateway.filter.FilterChainFactory;
import org.xjx.gateway.filter.GatewayFilterChainFactory;
import org.xjx.gateway.helper.AsyncHttpHelper;
import org.xjx.gateway.helper.RequestHelper;
import org.xjx.gateway.helper.ResponseHelper;
import org.xjx.gateway.response.GatewayResponse;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * 网关请求核心处理类
 * 串行处理器
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor{
    /**
     * 过滤器链工厂
     */
    private FilterChainFactory chainFactory = GatewayFilterChainFactory.getInstance();

    @Override
    public void process(HttpRequestWrapper wrapper) {
        // 解析Request请求对象
        ChannelHandlerContext context = wrapper.getCtx();
        FullHttpRequest request = wrapper.getRequest();
        try {
            // 解析并封装GatewayContext网关上下文对象（内部包含request、response、rule....）
            GatewayContext gatewayContext = RequestHelper.doContext(request, context);
            // 组装过滤器并执行过滤操作
            chainFactory.buildFilterChain(gatewayContext).doFilter(gatewayContext);
        } catch (HystrixRuntimeException e) {
            log.error("gateway runTimeout error {}", e);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.GATEWAY_FALLBACK_ERROR);
            doWriteAndRelease(context, request, httpResponse);
        } catch (LimitedException e) {
            log.error("request overlimited {}", e);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.FLOW_CONTROL_ERROR);
            doWriteAndRelease(context, request, httpResponse);
        } catch (BaseException e) {
            log.error("process error {} {}", e.getCode().getCode(), e.getCode().getMessage());
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(e.getCode());
            doWriteAndRelease(context, request, httpResponse);
        } catch (Throwable t) {
            log.error("process unknown error", t);
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            doWriteAndRelease(context, request, httpResponse);
        }
    }

    /**
     * 回写结果，释放资源
     * @param ctx
     * @param request
     * @param response
     */
    public void doWriteAndRelease(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
        ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(request);
    }

    /**
     * 发起异步/双异步请求
     * @param context
     */
    public void route(GatewayContext context) {
        Request request = context.getRequest().build();
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        if (whenComplete) {
            future.whenComplete((response, throwable)->{
                complete(request, response, throwable, context);
            });
        } else {
            // 双异步，线程A发起请求，由另外线程B接收请求
            future.whenCompleteAsync((response, throwable) -> {
               complete(request, response, throwable, context);
            });
        }
    }

    /**
     * 完成请求触发回调
     * @param request
     * @param response
     * @param throwable
     * @param context
     */
    public void complete(Request request, Response response, Throwable throwable, GatewayContext context) {
        try {
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                // judge exception kind
                if (throwable instanceof TimeoutException) {
                    log.warn("complete time out {}", url);
                    context.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    context.setThrowable(new ConnectException(throwable, context.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                context.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            context.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            log.error("complete error", t);
        } finally {
            context.setContextStatus(ContextStatus.Written);
            // write response back
            ResponseHelper.writeResponse(context);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void shutDown() {

    }
}
