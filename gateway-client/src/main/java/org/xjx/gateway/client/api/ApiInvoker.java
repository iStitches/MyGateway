package org.xjx.gateway.client.api;

import java.lang.annotation.*;

/**
 * 服务调用注解，用在服务提供类的方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiInvoker {
    String path();
}
