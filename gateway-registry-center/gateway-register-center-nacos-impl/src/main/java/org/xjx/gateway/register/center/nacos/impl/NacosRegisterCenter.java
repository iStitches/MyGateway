package org.xjx.gateway.register.center.nacos.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInstance;
import org.xjx.common.constants.GatewayConst;
import org.xjx.gateway.register.center.api.RegisterCenter;
import org.xjx.gateway.register.center.api.RegisterCenterListener;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *  Nacos 注册中心接口实现
 */
@Slf4j
public class NacosRegisterCenter implements RegisterCenter {
    /**
     * 注册中心地址
     */
    private String registerAddress;

    /**
     * 使用环境
     */
    private String env;

    /**
     *  服务实例信息
     */
    private NamingService namingService;

    /**
     * 服务信息维护实例
     */
    private NamingMaintainService namingMaintainService;

    /**
     * 监听器列表，多个服务多个监听器，同时修改存在并发问题
     */
    private List<RegisterCenterListener> registerCenterListenerList = new CopyOnWriteArrayList<>();

    @Override
    public void init(String registerAddress, String env) {
        this.registerAddress = registerAddress;
        this.env = env;
        try {
            this.namingMaintainService = NamingMaintainFactory.createMaintainService(registerAddress);
            this.namingService = NamingFactory.createNamingService(registerAddress);
        } catch (NacosException e) {
            log.warn("can't init nacosRegisterCenter");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceDefinition definition, ServiceInstance serviceInstance) {
        try {
            // build nacos service
            Instance instance = new Instance();
            instance.setInstanceId(serviceInstance.getServiceInstanceId());
            instance.setIp(serviceInstance.getIp());
            instance.setPort(serviceInstance.getPort());
            instance.setMetadata(Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceInstance)));
            // register
            namingService.registerInstance(definition.getServiceId(), env, instance);
            // update service definition
            namingMaintainService.updateService(definition.getServiceId(), env, 0,
                    Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(definition)));
            log.info("register {} {}", definition, serviceInstance);
        } catch (NacosException e) {
            log.error("register {} failed", definition, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deregister(ServiceDefinition definition, ServiceInstance serviceInstance) {
        try {
            namingService.deregisterInstance(definition.getServiceId(), env, serviceInstance.getIp(), serviceInstance.getPort());
        } catch (NacosException e) {
            log.error("deregister {} failed", definition, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        // register registerCenterListener
        registerCenterListenerList.add(registerCenterListener);
        // register service
        doSubscribeServices();
        // 定时任务检查是否有新服务加入
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(1, new NameThreadFactory("doSubscribeAllServices"));
        scheduledPool.scheduleWithFixedDelay(()-> doSubscribeServices(), 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 订阅服务
     * 当服务注册中心发起 NamingEvent 事件时更新本地服务列表信息
     */
    private void doSubscribeServices() {
        try {
            // 获取已订阅的服务列表
            Set<String> subscribeServiceSet = namingService.getSubscribeServices().stream().map(ServiceInfo::getName).collect(Collectors.toSet());
            int pageNo = 1;
            int pageSize = 100;

            // 分页获取所有服务列表，缓存未订阅的服务信息
            List<String> serviceList = namingService.getServicesOfServer(pageNo, pageSize, env).getData();
            while(CollectionUtils.isNotEmpty(serviceList)) {
                log.info("service list size {}", serviceList.size());
                for (String service : serviceList) {
                    if (subscribeServiceSet.contains(service)) {
                        continue;
                    }
                    // 具体的 Nacos 事件订阅类，执行订阅时的操作
                    EventListener eventListener = new NacosRegisterListener();
                    eventListener.onEvent(new NamingEvent(service, null));
                    // 订阅指定运行环境下对应的服务名，注册中心服务发生变动时调用 onEvent() 方法更新本地缓存的服务信息
                    namingService.subscribe(service, env, eventListener);
                    log.info("subscribe a service, serviceName {} env{}", service, env);
                }
                serviceList = namingService.getServicesOfServer(++pageNo, pageSize, env).getData();
            }
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Nacos 事件监听器，在Nacos注册中心发现服务信息变化时向本地发送事件信息，触发本地回调
     */
    private class NacosRegisterListener implements EventListener {
        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                log.info("Nacos event is {}", JSON.toJSON(event));
                // 获取变更服务名
                NamingEvent namingEvent = (NamingEvent) event;
                String serviceName = namingEvent.getServiceName();

                try {
                    // 获取最新的服务信息
                    Service service = namingMaintainService.queryService(serviceName, env);
                    ServiceDefinition definition = JSON.parseObject(service.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceDefinition.class);
                    // 获取服务实例信息
                    List<Instance> instances = namingService.getAllInstances(service.getName(), env);
                    Set<ServiceInstance> instanceSet = new HashSet<>();
                    for (Instance instance : instances) {
                        ServiceInstance serviceInstance = JSON.parseObject(instance.getMetadata().get(GatewayConst.META_DATA_KEY), ServiceInstance.class);
                        instanceSet.add(serviceInstance);
                    }
                    // 调用订阅监听器接口
                    registerCenterListenerList.forEach(listener-> {
                        listener.onChange(definition, instanceSet);
                    });
                } catch (NacosException e) {
                    log.error("Nacos update ServiceInfo failed", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
