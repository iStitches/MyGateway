package org.xjx.gateway.http.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.netty.handler.codec.spdy.SpdyHttpCodec;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xjx.common.constants.FilterConst;
import org.xjx.gateway.client.api.ApiInvoker;
import org.xjx.gateway.client.api.ApiProperties;
import org.xjx.gateway.client.api.ApiProtocol;
import org.xjx.gateway.client.api.ApiService;
import org.xjx.gateway.http.entity.UserInfo;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, pattternPath = "/http-server/**")
public class HttpController {
    @Autowired
    private ApiProperties properties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() {
//        try {
//            Thread.sleep(6000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return "pong";
    }

    /**
     * 模拟登录接口
     * @param phoneNumber
     * @param code
     * @param response
     * @return
     */
    @ApiInvoker(path = "/http-server/login")
    @GetMapping("/http-server/login")
    public UserInfo login(@RequestParam("phoneNumber")String phoneNumber,
                          @RequestParam("code")String code,
                          HttpServletResponse response) {
        String token = Jwts.builder().setIssuedAt(new Date())
                .setSubject(String.valueOf(phoneNumber + code))
                .signWith(SignatureAlgorithm.HS256, FilterConst.TOKEN_SECRET).compact();
        response.addCookie(new Cookie(FilterConst.COOKIE_KEY, token));
        return UserInfo.builder()
                .id(Integer.parseInt(phoneNumber + code))
                .name("testUser")
                .phoneNumber(phoneNumber).build();
    }

    /**
     * 模拟灰度发布的服务对象
     * @return
     */
    @ApiInvoker(path = "/http-server/gray")
    @GetMapping("/http-server/gray")
    public String grayRelease() {
        log.info("start exec gray release service");
        return "gray exec success";
    }

    @ApiInvoker(path = "/http-server/normal")
    @GetMapping("/http-server/normal")
    public String normalService() {
        log.info("normal service");
        return "normal";
    }
}
