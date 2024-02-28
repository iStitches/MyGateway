package org.xjx.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Dubbo服务调用执行器
 */
@Getter
@Setter
public class DubboServiceInvoker extends AbstractServiceInvoker{
    // 注册中心地址
    private String registerAddress;

    // 接口全限定类名
    private String interfaceClass;

    // 方法名称
    private String methodName;

    // 参数名字集合
    private String[] parameterTypes;

    // dubbo服务版本号
    private String version;
}
