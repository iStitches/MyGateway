package org.xjx.gateway.client.manager;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.event.ServiceBeanExportedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInstance;
import org.xjx.common.constants.BasicConst;
import org.xjx.common.constants.GatewayConst;
import org.xjx.common.utils.NetUtil;
import org.xjx.common.utils.TimeUtil;
import org.xjx.gateway.client.api.ApiAnnotationScanner;
import org.xjx.gateway.client.api.ApiProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * Dubbo 服务注册中心
 */
@Slf4j
public class DubboClientRegisterManager extends AbstractClientRegsiterManager implements ApplicationListener<ApplicationEvent> {
    /**
     * 缓存注册过的服务对象
     */
    private Set<Object> set = new HashSet<>();

    public DubboClientRegisterManager(ApiProperties apiProperties) {
        super(apiProperties);
    }

    /**
     * dubbo 的导出事件会在 spring 容器初始化之后执行，可以通过 onApplicationEvent 来监听
     * @param applicationEvent
     */
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ServiceBeanExportedEvent) {
            try {
                ServiceBean bean = ((ServiceBeanExportedEvent) applicationEvent).getServiceBean();
                doRegisterDubbo(bean);
            } catch (Exception e) {
                log.error("doRegisterDubbo failed", e);
                throw new RuntimeException(e);
            }
        } else {
            log.info("dubbo api started");
        }
    }

    /**
     * 注册 Dubbo ServiceBean 服务对象
     * @param serviceBean
     */
    private void doRegisterDubbo(ServiceBean serviceBean) {
        // 获取真正的服务实例对象
        Object bean = serviceBean.getRef();

        if (set.contains(bean)) {
            return;
        }
        ServiceDefinition definition = ApiAnnotationScanner.getInstance().scanner(bean, serviceBean);
        ServiceInstance instance = new ServiceInstance();
        int port = serviceBean.getProtocol().getPort();
        String serviceId = NetUtil.getLocalIp() + BasicConst.COLON_SEPARATOR + port;
        instance.setServiceInstanceId(serviceId);
        instance.setUniqueId(definition.getUniqueId());
        instance.setIp(NetUtil.getLocalIp());
        instance.setPort(port);
        instance.setRegisterTime(TimeUtil.getCurrentTimeMillis());
        instance.setVersion(definition.getVersion());
        instance.setWeight(GatewayConst.DEFAULT_WEIGHT);

        registerService(definition, instance);
    }
}
