package org.xjx.gateway.starter.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用容器初始化类
 */
@RequiredArgsConstructor
public class ApplicationContentPostProcessor implements ApplicationListener<ApplicationReadyEvent> {
    private final ApplicationContext applicationContext;

    private final AtomicBoolean initOnce = new AtomicBoolean(false);

    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!initOnce.compareAndSet(false, true)) {
            return;
        }
        applicationContext.publishEvent(new ApplicationInitlizingEvent(event));
    }
}
