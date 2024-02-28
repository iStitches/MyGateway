package org.xjx.gateway.client.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInstance;
import org.xjx.common.constants.BasicConst;
import org.xjx.common.constants.GatewayConst;
import org.xjx.common.utils.NetUtil;
import org.xjx.common.utils.TimeUtil;
import org.xjx.gateway.client.api.ApiAnnotationScanner;
import org.xjx.gateway.client.api.ApiProperties;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SpringMVC 客户端注册管理器
 * 1. SpringBean 启动后
 */
@Slf4j
public class SpringMVCClientRegisterManager extends AbstractClientRegsiterManager implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private Set<Object> set = new HashSet<>();

    @Autowired
    private ServerProperties serverProperties;

    public SpringMVCClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // 监听 spring 启动事件，等待Bean加载完毕后开始注册下游服务
        if (applicationEvent instanceof ApplicationStartedEvent) {
            try {
                doRegisterSpringMvc();
            } catch (Exception e) {
                log.error("doRegisterSpringMvc error", e);
                throw new RuntimeException(e);
            }
            log.info("springmvc api started");
        }
    }

    /**
     * 具体服务注册操作
     * 1.解析获取所有 RequestMappingHandlerMapping bean对象（controller对象）；
     * 2.解析 controller 内部方法请求参数、路径等约束，包装为服务定义对象；
     */
    private void doRegisterSpringMvc() {
        // 获取所有 RequestMappingHandlerMapping bean实例，即所有 controller 对象
        Map<String, RequestMappingHandlerMapping> allRequestMappings = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RequestMappingHandlerMapping.class, true, false);

        // 循环处理每一个处理器映射器——controller类
        for (RequestMappingHandlerMapping handlerMapping : allRequestMappings.values()) {
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
            for (Map.Entry<RequestMappingInfo, HandlerMethod> me : handlerMethods.entrySet()) {
                HandlerMethod handlerMethod = me.getValue();
                Class<?> clazz = handlerMethod.getBeanType();
                // 服务Bean对象
                Object bean = applicationContext.getBean(clazz);
                if (set.contains(bean)) {
                    continue;
                }
                // 解析Bean对象类上注解 ApiService、类方法注解 ApiInvoker
                ServiceDefinition definition = ApiAnnotationScanner.getInstance().scanner(bean);
                if (definition == null) {
                    continue;
                }
                // 设定开发环境
                definition.setEnvType(getApiProperties().getEnv());
                // 配置服务实例
                ServiceInstance instance = new ServiceInstance();
                String serviceId = NetUtil.getLocalIp() + BasicConst.COLON_SEPARATOR + serverProperties.getPort();
                instance.setUniqueId(definition.getUniqueId());
                instance.setServiceInstanceId(serviceId);
                instance.setIp(NetUtil.getLocalIp());
                instance.setPort(serverProperties.getPort());
                instance.setRegisterTime(TimeUtil.getCurrentTimeMillis());
                instance.setVersion(definition.getVersion());
                instance.setWeight(GatewayConst.DEFAULT_WEIGHT);
                // 灰度发布属性
                if (getApiProperties().isGray()) {
                    instance.setGray(true);
                }
                // 注册服务实例定义、服务实例
                registerService(definition, instance);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
