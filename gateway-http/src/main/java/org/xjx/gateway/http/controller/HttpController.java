package org.xjx.gateway.http.controller;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xjx.common.config.UserContext;
import org.xjx.common.config.UserInfoDTO;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.utils.JSONUtil;
import org.xjx.common.utils.cipher.RSAUtil;
import org.xjx.common.utils.jwt.JWTUtil;
import org.xjx.common.utils.redis.JedisUtil;
import org.xjx.gateway.client.api.ApiInvoker;
import org.xjx.gateway.client.api.ApiProperties;
import org.xjx.gateway.client.api.ApiProtocol;
import org.xjx.gateway.client.api.ApiService;
import org.xjx.gateway.http.entity.UserInfo;

import javax.servlet.Filter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Stream;

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
        Map<String, Object> params = new HashMap<>();
        params.put(FilterConst.TOKEN_USERID_KEY, String.valueOf(phoneNumber + code));  // 雪花算法生成唯一 userId
        String token = JWTUtil.generateToken(params, FilterConst.TOKEN_SECRET);

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

    /**
     * 提供给前端的RSA公钥
     * @return
     */
    @ApiInvoker(path = "/http-server/public-key")
    @GetMapping("/http-server/public-key")
    public String getRSASecretKey(HttpServletRequest request) {
        // generate rsa-key
        RSAUtil.KeyPairInfo keyPair = RSAUtil.getKeyPair();
        // parse jwt from token
        Cookie token = Stream.of(request.getCookies()).filter(cookie -> cookie.getName().equals(FilterConst.COOKIE_KEY)).findFirst().orElse(null);
        if (ObjectUtils.isEmpty(token)) {
            log.error("token is empty");
            throw new RuntimeException("token is empty");
        }
        String userId = String.valueOf(JWTUtil.getClaimByToken(token.getValue(), FilterConst.TOKEN_SECRET).get(FilterConst.TOKEN_USERID_KEY));
        if (StringUtils.isEmpty(userId)) {
            log.error("parse token failed");
            throw new RuntimeException("parse token failed");
        }
        // save rsa-privateKey into redis（zset）
        // 这里使用 zset 数据结构来记录后端为不同用户生成的 RSA私钥，因为不同用户创建私钥的时间不同导致过期时间不同，hash无法为内部元素设置过期时间
        JedisUtil jedis = new JedisUtil();
        // Zset:     security:key:{userId}    rsa:key:{rsa-privateKey}     {rsa-expireTime}
        if (!jedis.addScoreSet(FilterConst.SECURITYKEY_PREFIX+":"+userId, FilterConst.RSA_PRIVATEKEY_PREFIX+":"+keyPair.getPrivateKey(), (int) new Date(new Date().getTime()+FilterConst.RSA_PRIVATEKEY_EXPIRE_TIME).getTime())) {
            log.error("save rsa-privatekey into redis failed");
            throw new RuntimeException("save rsa-privatekey into redis failed");
        }
        log.info("save rsa-privateKey into redis success, key: {}, expire: {}", FilterConst.RSA_PRIVATEKEY_PREFIX, FilterConst.RSA_PRIVATEKEY_EXPIRE_TIME);
        jedis.setExpire(FilterConst.RSA_PRIVATEKEY_PREFIX, FilterConst.RSA_PRIVATEKEY_EXPIRE_TIME);
        log.info("create rsa-publicKey: {}, rsa-privateKey: {}", keyPair.getPublicKey(), keyPair.getPrivateKey());
        return keyPair.getPublicKey();
    }

    /**
     * 测试 URL 加密
     * @return
     */
    @ApiInvoker(path = "/http-server/v1/product")
    @GetMapping("/http-server/v1/product")
    public String testURLEncode(@RequestParam("productId") String productId) {
        log.info("productId: {}", productId);
        return "url encode test success";
    }
}
