package org.xjx.common.config;

/**
 * 服务调用的接口模型
 */
public interface ServiceInvoker {
    /**
     * 获取服务调用全路径
     * @return
     */
    String getInvokerPath();
    void setInvokerPath(String path);

    /**
     * 获取服务方法调用超时时间
     * @return
     */
    long getTimeOut();
    void setTimeOut(long timeOut);
}
