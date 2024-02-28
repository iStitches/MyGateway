package org.xjx.gateway;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.xjx.common.config.DynamicConfigManager;
import org.xjx.common.config.ServiceDefinition;
import org.xjx.common.config.ServiceInstance;
import org.xjx.common.constants.BasicConst;
import org.xjx.common.utils.NetUtil;
import org.xjx.common.utils.TimeUtil;
import org.xjx.gateway.config.center.api.ConfigCenter;
import org.xjx.gateway.register.center.api.RegisterCenter;
import org.xjx.gateway.register.center.api.RegisterCenterListener;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 *  网关启动类
 */
@Slf4j
public class Bootstrap {
    public static void main(String[] args) {
        // 加载网关静态核心配置
        Config config = ConfigLoader.getInstance().load(args);

        // 网关配置、服务信息初始化
        // 连接配置中心、监听配置中心的增删改查操作
        ServiceLoader<ConfigCenter> serviceLoader = ServiceLoader.load(ConfigCenter.class);
        final ConfigCenter configCenter = serviceLoader.findFirst().orElseThrow(() -> {
            log.error("can't found ConfigCenter impl");
            return new RuntimeException("can't found ConfigCenter impl");
        });

        // 监听配置中心数据变化、修改
        configCenter.init(config.getRegistryAddress(), config.getEnv());
        configCenter.subscribeRulesChange(rules -> {
            DynamicConfigManager.getInstance().putAllRule(rules);
        });

        // Netty组件创建及启动
        Container container = new Container(config);
        container.start();

        // 连接注册中心，加载注册中心实例到本地
        final RegisterCenter registerCenter = registerAndSubscribe(config);

        // 收到 kill 信号时触发，服务停机
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                registerCenter.deregister(buildGatewayServiceDefinition(config),
                        buildGatewayServiceInstance(config));
                container.shutdown();
            }
        });
    }

    /**
     * 服务注册和订阅服务变更信息通知, spi 方式实现服务注册
     */
    private static RegisterCenter registerAndSubscribe(Config config) {
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        final RegisterCenter registerCenter = serviceLoader.findFirst().orElseThrow(()-> {
            log.error("don't find RegisterCenter serviceImpl");
            return new RuntimeException("not find RegisterCenter impl");
        });
        // 注册中心初始化
        registerCenter.init(config.getRegistryAddress(), config.getEnv());

        // 构造网关服务定义、服务实例并注册到注册中心
        ServiceDefinition gatewayDefinition = buildGatewayServiceDefinition(config);
        ServiceInstance gatewayInstance = buildGatewayServiceInstance(config);

        // 注册
        registerCenter.register(gatewayDefinition, gatewayInstance);

        // 订阅
        registerCenter.subscribeAllServices(new RegisterCenterListener() {
            @Override
            public void onChange(ServiceDefinition definition, Set<ServiceInstance> instanceSet) {
                log.info("update serviceDefinition and serviceInstance: {} {}", definition.getUniqueId(), JSON.toJSON(instanceSet));
                DynamicConfigManager manager = DynamicConfigManager.getInstance();
                manager.putServiceInstance(definition.getUniqueId(), instanceSet);
                manager.putServiceDefinition(definition.getUniqueId(), definition);
            }
        });
        return registerCenter;
    }

    /**
     * 获取服务定义信息
     * @param config
     * @return
     */
    private static ServiceDefinition buildGatewayServiceDefinition(Config config) {
        ServiceDefinition definition = new ServiceDefinition();
        definition.setServiceId(config.getApplicationName());
        definition.setEnvType(config.getEnv());
        definition.setUniqueId(config.getApplicationName());
        definition.setInvokerMap(Map.of());
        return definition;
    }

    /**
     * 获取服务实例信息
     * @param config
     * @return
     */
    private static ServiceInstance buildGatewayServiceInstance(Config config) {
        String localIp = NetUtil.getLocalIp();
        int port = config.getPort();
        ServiceInstance instance = new ServiceInstance();
        instance.setPort(port);
        instance.setIp(localIp);
        instance.setServiceInstanceId(localIp + BasicConst.COLON_SEPARATOR + port);
        instance.setRegisterTime(TimeUtil.getCurrentTimeMillis());
        return instance;
    }
}
