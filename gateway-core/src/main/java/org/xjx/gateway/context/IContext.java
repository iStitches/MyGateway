package org.xjx.gateway.context;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.function.Consumer;

/**
 *  base interface about context
 *  context need containing status,protocol,rule,request,response,throwable,attributes,nettyContext
 */
public interface IContext {
    // 设置上下文状态
    void setContextStatus(ContextStatus status);
    // 判断上下文状态
    boolean judgeContextStatus(ContextStatus status);
    // 获取请求协议名
    String getProtocol();
    // 获取请求转换规则
    //Rule getRule();
    // 获取请求对象
    Object getRequest();
    // 获取响应对象
    Object getResponse();
    // 获取异常信息
    Throwable getThrowable();
    // 获取上下文参数
    Object getAttribute(Map<String, Object> key);
    // 获取 Netty 上下文
    ChannelHandlerContext getNettyContext();

    // 设置响应
    void setResponse();
    // 设置异常信息
    void setThrowable(Throwable throwable);
    // 设置上下文参数
    void setAttribute(String key, Object value);
    // 是否保持长连接
    boolean isKeepAlive();
    // 资源释放
    void releaseRequest();
    // 设置回调函数
    void setCompletedCallBack(Consumer<IContext> consumer);
    // 执行回调函数
    void invokeCompletedCallBacks();
}
