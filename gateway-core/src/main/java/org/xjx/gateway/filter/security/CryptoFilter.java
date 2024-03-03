package org.xjx.gateway.filter.security;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.xjx.common.config.Rule;
import org.xjx.common.config.UserContext;
import org.xjx.common.constants.FilterConst;
import org.xjx.common.utils.cipher.CryptoHelper;
import org.xjx.common.utils.jwt.JWTUtil;
import org.xjx.common.utils.redis.JedisUtil;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.filter.Filter;
import org.xjx.gateway.filter.FilterAspect;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@FilterAspect(id = FilterConst.CRYPTO_FILTER_ID, name = FilterConst.CRYPTO_FILTER_NAME, order = FilterConst.CRYPTO_FILTER_ORDER)
public class CryptoFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Iterator<Rule.FilterConfig> iterator = ctx.getRules().getFilterConfigs().iterator();
        while (iterator.hasNext()) {
            Rule.FilterConfig config = iterator.next();
            if (!config.getId().equalsIgnoreCase(FilterConst.CRYPTO_FILTER_ID)) {
                continue;
            }
            Map<String, List<String>> configMap = JSON.parseObject(config.getConfig(), Map.class);
            List<String> white_lists = configMap.get(FilterConst.WHITE_LIST_KEY);
            if (white_lists.contains(ctx.getRequest().getPath())) {
                return;
            }
        }
        // get symmetricKey from redis
        JedisUtil jedis = new JedisUtil();
        String userId = (String) JWTUtil.getClaimByToken(ctx.getRequest().getCookie(FilterConst.COOKIE_KEY).value(), FilterConst.TOKEN_SECRET).get(FilterConst.TOKEN_USERID_KEY);
        String securityKey = FilterConst.SECURITYKEY_PREFIX + ":" + userId;
        String symmetricPublicKey = null;
        Set<String> securityKeyLists = jedis.listScoreSetString(securityKey, 0, -1, true);
        for (String key : securityKeyLists) {
            if (key.startsWith(FilterConst.SYMMETRICKEY_PREFIX)) {
                symmetricPublicKey = key;
                break;
            }
        }
        if (symmetricPublicKey == null) {
            log.error("symmetricPublicKey load from redis is null");
            return;
        }
        // decode url and verify completability of url
        String encryptUrl = ctx.getRequest().getUri();
        String path = ctx.getRequest().getPath();
        String encryptPathParams = path.substring(path.indexOf("/encrypt/") + 9);
        String decryptedPathParam = CryptoHelper.decryptUrl(encryptPathParams, symmetricPublicKey);
        String decryptUri = encryptUrl.substring(0, encryptUrl.indexOf("/encrypt/")).concat("?").concat(decryptedPathParam);
        // change uri and queryParamsDecoder
        ctx.getRequest().setUri(decryptUri);
        ctx.getRequest().setQueryStringDecoder(decryptUri);
        // verify requestParams
        Map<String, List<String>> decryptedPathParams = ctx.getRequest().getQueryStringDecoder().parameters();
        if (decryptedPathParams.size() < 2) {
            log.error("decryptedPathParams size must bigger than 2");
            throw new RuntimeException("decryptedPathParams size is too small");
        }
        String signature = decryptedPathParams.get("signature").get(0);
        if (!CryptoHelper.verifySignature(new LinkedMultiValueMap<>(decryptedPathParams), signature, symmetricPublicKey)) {
            log. error("the param has something wrong");
            throw new RuntimeException("the param has something wrong");
        }
        String decryptQueryParams = CryptoHelper.decryptUrl(signature.replace(" ", "+"), symmetricPublicKey);
        ctx.getRequest().setModifyPath(ctx.getRequest().getQueryStringDecoder().path()+ "?" + decryptQueryParams);
        return;
    }
}
