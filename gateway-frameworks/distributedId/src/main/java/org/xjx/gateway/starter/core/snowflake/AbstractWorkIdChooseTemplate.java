package org.xjx.gateway.starter.core.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.xjx.gateway.starter.core.Snowflake;
import org.xjx.gateway.starter.tookit.SnowflakeIdUtil;

/**
 * ID生成器模板类
 * 生成ID的方法延时到子类实现
 */
@Slf4j
public abstract class AbstractWorkIdChooseTemplate {
    @Value("${framework.distributed.id.snowflake.is-use-system-clock:false}")
    private boolean isUseSystemClock;

    protected abstract WorkIdWrapper chooseWorkId();

    public void chooseAndInit() {
        WorkIdWrapper workIdWrapper = chooseWorkId();
        long workId = workIdWrapper.getWorkId();
        long dataCenterId = workIdWrapper.getDataCenterId();
        Snowflake snowflake = new Snowflake(workId, dataCenterId, isUseSystemClock);
        log.info("Snowflake type: {}, workId: {}, dataCenterId: {}",this.getClass().getSimpleName(), workId, dataCenterId);
        SnowflakeIdUtil.initSnowflake(snowflake);
    }
}
