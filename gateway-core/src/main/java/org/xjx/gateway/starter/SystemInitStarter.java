package org.xjx.gateway.starter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.generator.config.IConfigBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.xjx.gateway.Config;
import org.xjx.gateway.Container;
import org.xjx.gateway.pojo.params.InitParam;
import org.xjx.gateway.pojo.params.QRegisterCenterInfo;
import org.xjx.gateway.service.CoreService;
import org.xjx.gateway.service.QNacosInfoService;

import javax.annotation.Resource;

@Slf4j
@Component
public class SystemInitStarter {
    private final QNacosInfoService nacosInfoService;

    private final CoreService coreService;

    private final Config config;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    public SystemInitStarter(QNacosInfoService nacosInfoService, CoreService coreService, Config config) {
        this.nacosInfoService = nacosInfoService;
        this.coreService = coreService;
        this.config = config;
    }

    /**
     * 系统初始化
     * @throws Exception
     */
    public void run() throws Exception {
        log.info("开始初始化系统...");
        // 加载配置中心、并监听配置信息变更
        InitParam initParam = new InitParam();
        QRegisterCenterInfo info = nacosInfoService.getOne(new LambdaQueryWrapper<>());
        if (info != null) {
            log.info("检测到配置中心配置，开始初始化...");
            initParam.setServerAddr(info.getAddress());
            initParam.setCenterType(info.getType());
            coreService.init(initParam);
        }
        // 初始化上下文容器（存储解析 HttpServletRequest 后的结果）
        Container container = new Container(config);
        container.start();
    }
}
