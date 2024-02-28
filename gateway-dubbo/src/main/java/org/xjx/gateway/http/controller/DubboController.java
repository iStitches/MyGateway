package org.xjx.gateway.http.controller;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DubboController {
    @GetMapping("/http-server/ping")
    public String ping() {
        return "pong";
    }
}
