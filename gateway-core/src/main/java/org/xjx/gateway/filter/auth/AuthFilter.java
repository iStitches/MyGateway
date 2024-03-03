package org.xjx.gateway.filter.auth;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.Rule;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.ResponseException;
import org.xjx.common.utils.jwt.JWTUtil;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.filter.Filter;
import org.xjx.gateway.filter.FilterAspect;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Slf4j
@FilterAspect(id= FilterConst.AUTH_FILTER_ID, name = FilterConst.AUTH_FILTER_NAME, order = FilterConst.AUTH_FILTER_ORDER)
public class AuthFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 判断是否需要执行认证过滤
        Iterator<Rule.FilterConfig> iterator = ctx.getRules().getFilterConfigs().iterator();
        while (iterator.hasNext()) {
            Rule.FilterConfig config = iterator.next();
            if (!config.getId().equalsIgnoreCase(FilterConst.AUTH_FILTER_ID)) {
                continue;
            }
            Map<String, String> configMap = JSON.parseObject(config.getConfig(), Map.class);
            String authPath = configMap.get(FilterConst.AUTH_FILTER_KEY);
            String curRequestKey = ctx.getRequest().getPath();
            if (!authPath.equals(curRequestKey)) {
                return;
            }
            // 解析负载
            Cookie cookie = ctx.getRequest().getCookie(FilterConst.COOKIE_KEY);
            String token = Optional.ofNullable(cookie).map(c->c.value()).orElse(null);
            if (StringUtils.isEmpty(token)) {
                throw new ResponseException(ResponseCode.UNAUTHORIZED);
            }
            // jwt认证并解析载荷——UserId
            try {
                long userId = (Long) JWTUtil.getClaimByToken(token, FilterConst.TOKEN_SECRET).get(FilterConst.TOKEN_USERID_KEY);
                ctx.getRequest().setUserId(userId);
                log.info("AuthFilter parse token successful, userId {}", userId);
            } catch (Exception e) {
                log.info("AuthFilter parse token failed, requestPath {}", ctx.getRequest().getPath());
                throw new ResponseException(ResponseCode.UNAUTHORIZED);
            }
        }
        return;
    }

    /**
     * 解析token中的载荷——用户ID
     * @param token
     * @return
     */
    private long parseUserId(String token) {
        Jwt jwt = Jwts.parser().setSigningKey(FilterConst.TOKEN_SECRET).parse(token);
        return Long.parseLong(((DefaultClaims)jwt.getBody()).getSubject());
    }
}
