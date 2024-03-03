package org.xjx.gateway.filter.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.iterators.FilterIterator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.xjx.common.config.UserContext;
import org.xjx.common.config.UserInfoDTO;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.utils.cipher.RSAUtil;
import org.xjx.common.utils.jwt.JWTUtil;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.filter.Filter;
import org.xjx.gateway.filter.FilterAspect;
import org.xjx.common.utils.redis.JedisUtil;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.Set;

/**
 * 保存 AES 密钥过滤器
 */
@Slf4j
@FilterAspect(id = FilterConst.SYMMETRICKEY_FILTER_ID, name = FilterConst.SYMMETRICKEY_FILTER_NAME, order = FilterConst.SYMMETRICKEY_FILTER_ORDER)
public class SymmetricKeyFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // get symmetricKey from RequestHeader and save into redis
        String encryptSymmetricKey = ctx.getRequest().getHeaders().get(FilterConst.SYMMETRICKEY_PUBLICKEY);
        if (encryptSymmetricKey != null) {
            try {
                JedisUtil jedis = new JedisUtil();
                String userId = getUserId(ctx);
                String securityKey = FilterConst.SECURITYKEY_PREFIX + ":" + userId;
                Set<String> keys = jedis.listScoreSetString(securityKey, 0, -1, true);
                String privateKey = null;
                for (String key : keys) {
                    if (key.startsWith(FilterConst.RSA_PRIVATEKEY_PREFIX)) {
                        privateKey = key.substring(FilterConst.RSA_PRIVATEKEY_PREFIX.length()+1);
                    }
                }
                log.info("Get rsa-privateKey from redis {}", privateKey);
                if (StringUtils.isEmpty(privateKey)) {
                    log.error("PrivateKey is empty in SymmetricKeyFilter");
                    throw new RuntimeException("PrivateKey is empty in SymmetricKeyFilter");
                }

                // decrypt RSA and get AES key
                String symmetricPublicKey = RSAUtil.decryptPrivateKey(encryptSymmetricKey, privateKey);
                log.info("After decryptSymmetric, symmetric: {}", symmetricPublicKey);
                // save AES-key into redis
                // Zset:     security:key:{userId}    symmetric:key:{symmetricKey}     {symmetric-expireTime}
                String symmetricKey = FilterConst.SYMMETRICKEY_PREFIX + ":" + symmetricPublicKey;

                // check AES-key expiration and create newOne if needed
                if (jedis.isExistScoreSet(securityKey, symmetricKey)) {
                    int expireTime = (int)jedis.getScore(securityKey, symmetricKey).doubleValue();
                    if (expireTime > new Date().getTime()) {
                        return;
                    }
                }
                if (!jedis.addScoreSet(securityKey, symmetricKey, (int) new Date(new Date().getTime() + FilterConst.SYMMETRICKEY_EXPIRE_TIME).getTime())) {
                    log.error("save symmetricKey into redis failed");
                    throw new RuntimeException("save symmetricKey into redis failed");
                }
            } catch (Exception e) {
                log.error("SymmetricFilter decrypt symmetric failed, err : {}", e.getMessage());
                throw new RuntimeException("SymmetricFilter decrypt symmetric failed, throws: " + e.getMessage());
            }
        }
    }

    public String getUserId(GatewayContext ctx) {
        String token = ctx.getRequest().getCookie(FilterConst.COOKIE_KEY).value();
        if (StringUtils.isEmpty(token)) {
            log.error("SymmetricKeyFilter token is null");
            throw new RuntimeException("SymmetricKeyFilter token is null");
        }
        String userId = (String) JWTUtil.getClaimByToken(token, FilterConst.TOKEN_SECRET).get(FilterConst.TOKEN_USERID_KEY);
        if (ObjectUtils.isEmpty(token)) {
            log.error("token is empty");
            throw new RuntimeException("token is empty");
        }
        return userId;
    }
}
