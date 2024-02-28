package org.xjx.gateway.filter.gray;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xjx.common.constants.FilterConst;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.filter.Filter;
import org.xjx.gateway.filter.FilterAspect;

/**
 * 灰度发布过滤器
 */
@FilterAspect(id = FilterConst.GRAY_FILTER_ID, name = FilterConst.GRAY_FILTER_NAME, order = FilterConst.GRAY_FILTER_ORDER)
public class GrayFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(GrayFilter.class);
    private static String GRAY = "true";
    private static final int HASH_LENGTH = 1024;

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        String gray = ctx.getRequest().getHeaders().get(FilterConst.GRAY_FILTER_KEY);
        if (StringUtils.isNotEmpty(gray) && gray.equalsIgnoreCase(GRAY)) {
            logger.info("current user {} is set for grayService", ctx.getRequest().getClientIp());
            ctx.setGray(true);
            return;
        }
        // 选取部分灰度发布用户处理灰度流量
        String clientIp = ctx.getRequest().getClientIp();
        int res = clientIp.hashCode() & (HASH_LENGTH);
        if (res == 1) {
            logger.info("current client {} is selected for grayService", clientIp);
            ctx.setGray(true);
        }
    }
}
