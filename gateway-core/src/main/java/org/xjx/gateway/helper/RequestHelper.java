package org.xjx.gateway.helper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.xjx.common.config.*;
import org.xjx.common.constants.BasicConst;
import org.xjx.common.constants.GatewayConst;
import org.xjx.common.constants.GatewayProtocol;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.exception.ResponseException;
import org.xjx.gateway.context.GatewayContext;
import org.xjx.gateway.request.GatewayRequest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 *  解析 FullHttpRequest对象，进一步封装为网关上下文、网关内部请求体
 */
public class RequestHelper {
    public static GatewayContext doContext(FullHttpRequest request, ChannelHandlerContext context) {
        // 封装网关内部请求对象
        GatewayRequest gatewayRequest = doRequest(request, context);

        // 路由匹配，先查找 Map，未找到再查找前缀树
        ServiceDefinition definition = null;
        definition = DynamicConfigManager.getInstance().getServiceDefinition(gatewayRequest.getUniqueId());
        if (definition == null) {
            ServiceRuleDTO serviceRule = DynamicConfigManager.getInstance().getServiceRule(gatewayRequest.getPath());
            if (serviceRule != null) {
                definition = DynamicConfigManager.getInstance().getServiceDefinition(serviceRule.getServiceName());
            }
        }
        if(definition == null) {
            throw new RuntimeException("未发现目标服务");
        }

        // 服务调用对象初始化
        ServiceInvoker serviceInvoker = new HttpServiceInvoker();
        serviceInvoker.setInvokerPath(gatewayRequest.getPath());
        serviceInvoker.setTimeOut(500);

        // 获取具体服务对象访问规则
        Rule rule = getRule(gatewayRequest, definition.getServiceId());
        // 动态服务发现
        GatewayContext gatewayContext = GatewayContext.newBuilder().setProtocol(definition.getProtocol()).setKeepAlive(HttpUtil.isKeepAlive(request))
                .setNettyCtx(context).setRequest(gatewayRequest).setRule(rule).build();
        // 这里改为动态服务发现并请求，默认转发到 8080 端口
        return gatewayContext;
    }

    /**
     * 封装 GatewayRequest
     * @param request
     * @param context
     * @return
     */
    private static GatewayRequest doRequest(FullHttpRequest request, ChannelHandlerContext context) {
        HttpHeaders headers = request.headers();
        String uniqueId = headers.get(GatewayConst.UNIQUE_ID);
        String host = headers.get(HttpHeaderNames.HOST);
        HttpMethod method = request.method();
        String uri = request.uri();
        String clientIp = getClientIp(context, request);
        Charset charset = HttpUtil.getCharset(request, StandardCharsets.UTF_8);
        String contentType = HttpUtil.getMimeType(request) == null ? null : HttpUtil.getMimeType(request).toString();
        GatewayRequest gatewayRequest = new GatewayRequest(uniqueId, charset, clientIp, host, uri, method, contentType, headers, request);
        return gatewayRequest;
    }

    /**
     * 获取客户端IP地址
     * @param ctx
     * @param request
     * @return
     */
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
        String clientIp = null;
        // first parse X-Forwarded-For header to get clientIp
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(","));
            if (values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }
        // else parse clientIp from client Channel
        if (clientIp == null) {
            InetSocketAddress address = (InetSocketAddress)ctx.channel().remoteAddress();
            clientIp = address.getAddress().getHostAddress();
        }
        return clientIp;
    }

    /**
     * 解析请求url对应的规则匹配
     * @param request
     * @param serviceId
     * @return
     */
    private static Rule getRule(GatewayRequest request, String serviceId) {
        // 拼接服务ID+请求路径
        String encryptRequestPath = request.getPath();
        if (encryptRequestPath.indexOf("/encrypt") != -1) {
            encryptRequestPath = encryptRequestPath.substring(0, encryptRequestPath.indexOf("/encrypt"));
        }
        String key = serviceId + "." + encryptRequestPath;
        Rule rule = DynamicConfigManager.getInstance().getRulePath(key);
        if (rule != null) {
            return rule;
        }
        return DynamicConfigManager.getInstance().getRuleByServiceId(serviceId).stream()
                .filter(r -> request.getPath().startsWith(r.getPrefix())).findAny()
                .orElseThrow(() -> new ResponseException(ResponseCode.PATH_NO_MATCHED));
    }
}
