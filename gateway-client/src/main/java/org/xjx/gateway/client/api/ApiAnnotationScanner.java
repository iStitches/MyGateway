package org.xjx.gateway.client.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.spring.ServiceBean;
import org.xjx.common.config.DubboServiceInvoker;
import org.xjx.common.config.HttpServiceInvoker;
import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInvoker;
import org.xjx.common.constants.BasicConst;
import org.xjx.common.constants.DubboConstants;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解扫描类，扫描 ApiService、ApiInvoker 注解
 */
public class ApiAnnotationScanner {
    private static class SingletonHolder {
        static final ApiAnnotationScanner INSTANCE = new ApiAnnotationScanner();
    }

    public ApiAnnotationScanner() {
    }

    public static ApiAnnotationScanner getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 扫描服务注解，解析服务定义对象
     * @param bean
     * @param args
     * @return
     */
    public ServiceDefinition scanner(Object bean, Object... args) {
        Class<?> clazz = bean.getClass();
        if (!clazz.isAnnotationPresent(ApiService.class)) {
            return null;
        }
        ApiService apiService = clazz.getAnnotation(ApiService.class);
        String version = apiService.version();

        ServiceDefinition definition = new ServiceDefinition();
        Map<String, ServiceInvoker> invokerMap = new HashMap<>();
        Method[] methods = clazz.getMethods();
        if (methods != null && methods.length > 0) {
            for (Method method : methods) {
                ApiInvoker invoker = method.getAnnotation(ApiInvoker.class);
                if (invoker == null) {
                    continue;
                }
                String path = invoker.path();
                // 根据服务协议判断
                switch (apiService.protocol()) {
                    case HTTP:
                        HttpServiceInvoker httpServiceInvoker = createHttpServiceInvoker(path);
                        invokerMap.put(path, httpServiceInvoker);
                        break;
                    case DUBBO:
                        ServiceBean<?> serviceBean = (ServiceBean<?>) args[0];
                        DubboServiceInvoker dubboServiceInvoker = createDubboServiceInvoker(path, serviceBean, method);
                        String dubboVersion = dubboServiceInvoker.getVersion();
                        if (!StringUtils.isEmpty(dubboVersion)) {
                            version = dubboVersion;
                        }
                        invokerMap.put(path, dubboServiceInvoker);
                        break;
                    default:
                        break;
                }
            }
        }
        definition.setUniqueId(apiService.serviceId() + BasicConst.COLON_SEPARATOR + version);
        definition.setServiceId(apiService.serviceId());
        definition.setVersion(version);
        definition.setProtocol(apiService.protocol().getDesc());
        definition.setPatternPath(apiService.pattternPath());
        definition.setAvailable(true);
        definition.setInvokerMap(invokerMap);
        return definition;
    }


    /**
     * 构造 HttpServiceInvoker 执行对象
     * @param path
     * @return
     */
    private HttpServiceInvoker createHttpServiceInvoker(String path) {
        HttpServiceInvoker httpServiceInvoker = new HttpServiceInvoker();
        httpServiceInvoker.setInvokerPath(path);
        return httpServiceInvoker;
    }

    /**
     * 其它协议请求转化为网关内部Dubbo服务请求
     * @param path
     * @return
     */
    private DubboServiceInvoker createDubboServiceInvoker(String path, ServiceBean<?> serviceBean, Method method) {
        DubboServiceInvoker dubboServiceInvoker = new DubboServiceInvoker();
        dubboServiceInvoker.setInvokerPath(path);

        String methodName = method.getName();
        // 获取注册中心信息
        String registerAddress = serviceBean.getRegistry().getAddress();
        String interfaceClass = serviceBean.getInterface();
        // 配置执行器信息
        dubboServiceInvoker.setRegisterAddress(registerAddress);
        dubboServiceInvoker.setMethodName(methodName);
        dubboServiceInvoker.setInterfaceClass(interfaceClass);

        // 参数类型数组
        String[] parameterTypes = new String[method.getParameterCount()];
        Class<?>[] classes = method.getParameterTypes();
        for (int i = 0; i < classes.length; i++) {
            parameterTypes[i] = classes[i].getName();
        }
        dubboServiceInvoker.setParameterTypes(parameterTypes);

        // 设置请求超时时间
        Integer timeout = serviceBean.getTimeout();
        if (timeout == null && timeout.intValue() == 0) {
            ProviderConfig config = serviceBean.getProvider();
            if (config != null) {
                Integer providerTimeout = config.getTimeout();
                if (providerTimeout == null || providerTimeout.intValue() == 0) {
                    timeout = DubboConstants.DUBBO_TIMEOUT;
                } else {
                    timeout = providerTimeout;
                }
            }
        }
        dubboServiceInvoker.setTimeOut(timeout);
        dubboServiceInvoker.setVersion(serviceBean.getVersion());
        return dubboServiceInvoker;
    }
}
