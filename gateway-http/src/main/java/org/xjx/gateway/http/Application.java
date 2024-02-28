package org.xjx.gateway.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.xjx")
public class Application {
    public static void main(String[] args) {
       SpringApplication.run(Application.class, args);
    }
}
