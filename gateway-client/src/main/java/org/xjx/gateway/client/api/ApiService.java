package org.xjx.gateway.client.api;

import java.lang.annotation.*;

/**
 * 服务信息注解，用在服务提供类上
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiService {
    String serviceId();
    String version() default "1.0.0";
    ApiProtocol protocol();
    String pattternPath();
}
