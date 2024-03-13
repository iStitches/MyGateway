package org.xjx.gateway.starter.init;

import org.springframework.context.ApplicationEvent;

/**
 * 应用初始化完毕事件
 */
public class ApplicationInitlizingEvent extends ApplicationEvent {

    public ApplicationInitlizingEvent(Object source) {
        super(source);
    }
}
