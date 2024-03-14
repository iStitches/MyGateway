package org.xjx.gateway.starter.core.enums;

/**
 * 支持多种后台处理线程池
 */
public enum ThreadPoolEnum {
    DYNAMIC_THREAD_POOL("dynamic-thread-pool"),
    EAGER_THREAD_POOL("eager-thread-pool");

    private String type;

    ThreadPoolEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
