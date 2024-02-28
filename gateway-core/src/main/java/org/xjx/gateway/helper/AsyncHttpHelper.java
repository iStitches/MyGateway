package org.xjx.gateway.helper;

import org.asynchttpclient.*;

import java.util.concurrent.CompletableFuture;

/**
 *  异步 HttpClient 处理辅助类
 *  1.整合 AsyncHttpClient、NettyEventLoopGroup 对象；
 */
public class AsyncHttpHelper {
    private static final class SingleHolder {
        private static final AsyncHttpHelper instance = new AsyncHttpHelper();
    }

    private AsyncHttpHelper() {
    }

    public static AsyncHttpHelper getInstance() {return SingleHolder.instance;}

    private AsyncHttpClient asyncHttpClient;

    public void inilitized(AsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }

    public CompletableFuture<Response> executeRequest(Request request) {
        ListenableFuture<Response> future = asyncHttpClient.executeRequest(request);
        return future.toCompletableFuture();
    }

    public <T> CompletableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
        ListenableFuture<T> future = asyncHttpClient.executeRequest(request, handler);
        return future.toCompletableFuture();
    }
}
