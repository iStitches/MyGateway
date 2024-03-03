package org.xjx.gateway.filter.router;

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory;
import io.netty.handler.timeout.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.ConnectException;
import org.xjx.common.exception.ResponseException;
import org.xjx.gateway.ConfigLoader;
import org.xjx.gateway.context.ContextStatus;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.filter.Filter;
import org.xjx.gateway.filter.FilterAspect;
import org.xjx.gateway.helper.AsyncHttpHelper;
import org.xjx.gateway.helper.ResponseHelper;
import org.xjx.gateway.response.GatewayResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由过滤器
 * 1.最终的过滤器组件，用于向下游服务转发请求；
 * 2.请求异常重试；
 * 3.服务熔断降级；
 * 4.以上配置均支持配置中心动态更新。
 */
@FilterAspect(id = FilterConst.ROUTER_FILTER_ID, name = FilterConst.ROUTER_FILTER_NAME, order = FilterConst.ROUTER_FILTER_ORDER)
public class RouterFilter implements Filter {
    private static Logger accessLog = LoggerFactory.getLogger("accessLog");

    private ConcurrentHashMap<String, RouterHystrixCommand> commandMap = new ConcurrentHashMap<>();

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 获取熔断降级的配置
        Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(ctx);
        // 判断是否走熔断降级配置
        if (hystrixConfig.isPresent()) {
            routeWithHystrix(ctx, hystrixConfig);
        } else {
            route(ctx, hystrixConfig);
        }
    }

    /**
     * 获取 HystrixConfig 配置
     * 1.对比请求路径和注册中心注册的路径参数；
     * 2.判断当前请求是否需要走熔断策略分支；
     * @param gatewayContext
     * @return
     */
    private static Optional<Rule.HystrixConfig> getHystrixConfig(GatewayContext gatewayContext) {
        Rule rule = gatewayContext.getRules();
        Optional<Rule.HystrixConfig> hystrixConfig = rule.getHystrixConfigs().stream().filter(c -> StringUtils.equals(c.getPath(), gatewayContext.getRequest().getPath())).findFirst();
        return hystrixConfig;
    }

    /**
     *  默认路由逻辑：
     *  1. 根据 whenComplete 判断执行回调的线程是否阻塞执行；
     *  1.1 whenComplete 是非异步完成方法，提供了一个回调；
     *  1.2 whenCompleteAsync 是异步完成方法，回调会在不同的线程中执行。
     * @param gatewayContext
     * @param hystrixConfig
     * @return
     */
    private CompletableFuture<Response> route(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        // 异步请求发送
        Request request = gatewayContext.getRequest().build();
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        // 单异步/双异步模型
        if (whenComplete) {
            future.whenComplete(((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            }));
        } else {
            future.whenCompleteAsync(((response, throwable) -> {
                complete(request, response, throwable, gatewayContext);
            }));
        }
        return future;
    }

    /**
     * 熔断降级请求策略：
     * 1.命令执行超过配置超时时间；
     * 2.命令执行出现异常或错误；
     * 3.连续失败率达到配置的阈值；
     * @param gatewayContext
     * @param hystrixConfig
     */
    private void routeWithHystrix(GatewayContext gatewayContext, Optional<Rule.HystrixConfig> hystrixConfig) {
        //accessLog.info("rule.HystrixTimeoutInMilliseconds = {}", hystrixConfig.get().getTimeoutInMilliseconds());
        String key = gatewayContext.getUniqueId() + "." + gatewayContext.getRequest().getPath();
        RouterHystrixCommand proxyCommand = null;
        if (commandMap.containsKey(key)) {
            proxyCommand = commandMap.get(key);
            if (!hystrixConfig.get().equals(commandMap.get(key))) {
                //accessLog.info("previous HystrixCommand instance hashCode: {}", proxyCommand.hashCode());
                proxyCommand.updateHystrixCommandProperties(proxyCommand.getCommandKey().name());
                proxyCommand = new RouterHystrixCommand(gatewayContext, hystrixConfig);
                //accessLog.info("after HystrixCommand instance hashCode: {}", proxyCommand.hashCode());
                commandMap.put(key, proxyCommand);
            }
        } else {
            proxyCommand = new RouterHystrixCommand(gatewayContext, hystrixConfig);
            commandMap.put(key, proxyCommand);
        }
        proxyCommand.execute();
    }

    /**
     * Hystrix命令集合
     */
    private class RouterHystrixCommand extends HystrixCommand<Object> {
        private GatewayContext context;
        private Optional<Rule.HystrixConfig> config;

        public RouterHystrixCommand(GatewayContext context, Optional<Rule.HystrixConfig> config) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(context.getUniqueId()))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(context.getRequest().getPath()))
                    .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                            // 核心线程数
                            .withCoreSize(config.get().getCoreThreadSize()))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            // 线程隔离类型
                            .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                            // 命令执行超时
                            .withExecutionTimeoutInMilliseconds(config.get().getTimeoutInMilliseconds())
                            // 超时中断
                            .withExecutionIsolationThreadInterruptOnTimeout(true)
                            .withExecutionTimeoutEnabled(true)));
            //accessLog.info("Construct RouterHystrixCommand Object");
            this.config = config;
            this.context = context;
        }

        @Override
        protected Object run() throws Exception {
            // 实际路由操作
            //accessLog.info("hystrix current timeOut is {}", this.getProperties().executionTimeoutInMilliseconds().get());
            route(context, config).get();
            return null;
        }

        // 熔断降级操作
        @Override
        protected Object getFallback() {
            // 是否是超时引发的熔断
            if (isFailedExecution() || getExecutionException() instanceof HystrixTimeoutException) {
                // 针对超时的异常处理
                context.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.GATEWAY_FALLBACK_TIMEOUT));
            } else {
                // 其它类型异常熔断处理
                context.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.GATEWAY_FALLBACK_ERROR, config.get().getFallbackResponse()));
            }
            context.setContextStatus(ContextStatus.Written);
            return null;
        }

        /**
         * 动态更新 CommandProperteis 配置
         * 1.因为 Hystrix 内部使用了缓存，如果仅仅修改 HystrixCommand.Setter 是没有用的；
         * 2.利用反射获取 HystrixPropertiesFactory 的 commandProperties 字段，并更新
         */
        protected void updateHystrixCommandProperties(String commandKey) {
            try {
                Field field = HystrixPropertiesFactory.class.getDeclaredField("commandProperties");
                field.setAccessible(true);
                ConcurrentHashMap<String, HystrixCommandProperties> commandProperties = (ConcurrentHashMap<String, HystrixCommandProperties>) field.get(null);
                System.out.println(commandProperties.toString());
                commandProperties.remove(commandKey);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                //accessLog.error("Remove cache in HystrixCommandFactory failed, commandKey: {}", commandKey, e);
            }
        }
    }

    /**
     * 响应回调处理
     * @param request
     * @param response
     * @param throwable
     * @param gatewayContext
     */
    private void complete(Request request, Response response, Throwable throwable, GatewayContext gatewayContext) {
        // 请求已经处理完毕 释放请求资源
        gatewayContext.releaseRequest();
        // 获取上下文请求配置规则
        Rule rule = gatewayContext.getRules();
        // 获取重试次数
        int currentRetryTimes = gatewayContext.getCurrentRetryTimes();
        int confRetryTimes = rule.getRetryConfig().getTimes();
        // 异常重试
        if ((throwable instanceof TimeoutException || throwable instanceof IOException) &&
        currentRetryTimes <= confRetryTimes) {
            doRetry(gatewayContext, currentRetryTimes);
        }
        String url = request.getUrl();
        try {
            if (Objects.nonNull(throwable)) {
                if (throwable instanceof TimeoutException) {
                    //accessLog.warn("complete timeout {}", url);
                    gatewayContext.setThrowable(throwable);
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    gatewayContext.setThrowable(new ConnectException(throwable, gatewayContext.getUniqueId(), url, ResponseCode.HTTP_RESPONSE_ERROR));
                    gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            gatewayContext.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            gatewayContext.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            //accessLog.error("complete process failed", t);
        } finally {
            gatewayContext.setContextStatus(ContextStatus.Written);
            ResponseHelper.writeResponse(gatewayContext);
            accessLog.info("{} {} {} {} {} {} {}",
                    System.currentTimeMillis() - gatewayContext.getRequest().getBeginTime(),
                    gatewayContext.getRequest().getClientIp(),
                    gatewayContext.getRequest().getUniqueId(),
                    gatewayContext.getRequest().getMethod(),
                    gatewayContext.getRequest().getPath(),
                    gatewayContext.getResponse().getHttpResponseStatus().code(),
                    gatewayContext.getResponse().getFutureResponse().getResponseBodyAsBytes().length);
        }
    }

    /**
     *  重试策略
     * @param gatewayContext
     * @param retryTimes
     */
    private void doRetry(GatewayContext gatewayContext, int retryTimes) {
        //accessLog.warn("requestId={}, retryTimes={}", gatewayContext.getUniqueId(), retryTimes);
        gatewayContext.setCurrentRetryTimes(retryTimes + 1);
        try {
            // 重新执行过滤器逻辑
            doFilter(gatewayContext);
        } catch (Exception e) {
            //accessLog.warn("重试请求失败, requestId={}", gatewayContext.getUniqueId());
            throw new RuntimeException(e);
        }
    }
}
