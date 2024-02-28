package org.xjx.gateway.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import lombok.Getter;
import lombok.Setter;
import org.asynchttpclient.Response;
import org.xjx.common.enums.ResponseCode;
import org.xjx.common.utils.JSONUtil;

/**
 * 自定义封装 GatewayResponse
 */
@Setter
@Getter
public class GatewayResponse implements IGatewayResponse{
    /**
     * 标准响应头
     */
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();
    /**
     * 额外响应头
     */
    private HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();
    /**
     * 响应内容
     */
    private String content;
    /**
     * 返回响应状态码
     */
    private HttpResponseStatus httpResponseStatus;
    /**
     * 异步响应对象
     */
    private Response futureResponse;

    public GatewayResponse() {
    }

    /**
     * 设置响应头数据
     * @param key
     * @param value
     */
    public void addHeader(CharSequence key, CharSequence value) {
        responseHeaders.add(key, value);
    }

    /**
     * 构建异步响应对象
     * asyncHttpClient.Response ——> GatewayResponse
     * @param futureResponse
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Response futureResponse) {
        GatewayResponse response = new GatewayResponse();
        response.setFutureResponse(futureResponse);
        response.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        return response;
    }

    /**
     * 处理失败时 JSON 格式返回
     * @param code
     * @param args
     * @return
     */
    public static GatewayResponse buildGatewayResponse(ResponseCode code, Object...args) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, code.getStatus().code());
        objectNode.put(JSONUtil.CODE, code.getCode());
        objectNode.put(JSONUtil.MESSAGE, code.getMessage() + " - " +args);

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(code.getStatus());
        response.setContent(JSONUtil.toJSONString(objectNode));
        response.addHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        return response;
    }

    /**
     * 处理成功时调用
     * @param data
     * @return
     */
    public static GatewayResponse buildGatewayResponse(Object data) {
        ObjectNode objectNode = JSONUtil.createObjectNode();
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getCode());
        objectNode.put(JSONUtil.MESSAGE, ResponseCode.SUCCESS.getMessage());
        objectNode.putPOJO(JSONUtil.DATA, data);

        GatewayResponse response = new GatewayResponse();
        response.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        response.setContent(JSONUtil.toJSONString(objectNode));
        response.addHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON+";charset=utf-8");
        return response;
    }
}
