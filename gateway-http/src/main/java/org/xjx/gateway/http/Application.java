package org.xjx.gateway.http;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;


@SpringBootApplication(scanBasePackages = {"org.xjx.gateway.http", "org.xjx.gateway.starter.designpattern", "org.xjx.gateway.starter.cache", "org.xjx.gateway.starter"})
@MapperScan("org.xjx.gateway.http")
public class Application {
    @Autowired
    ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
