package org.xjx.gateway.request;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.common.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.xjx.common.constants.BasicConst;
import org.xjx.common.utils.TimeUtil;

import java.nio.charset.Charset;
import java.util.*;

/**
 *  网关请求对象，处理包装客户端请求
 */
@Slf4j
public class GatewayRequest implements IGatewayRequest{
    /**
     * 服务ID
     */
    @Getter
    private final String uniqueId;

    /**
     * 进入网关时间
     */
    @Getter
    private final long beginTime;

    /**
     * 结束请求时间
     */
    @Getter
    @Setter
    private long endTime;

    /**
     * 字符集
     */
    @Getter
    private final Charset charset;

    /**
     *  客户端IP，做黑白名单，流控
     */
    @Getter
    private final String clientIp;

    /**
     *  请求地址
     */
    @Getter
    private final String host;

    /**
     * 请求路径
     */
    @Getter
    private final String path;

    /**
     * 统一资源标识符
     */
    @Getter
    private final String uri;
    /**
     * 请求方式
     */
    @Getter
    private final HttpMethod method;

    /**
     * 请求格式
     */
    @Getter
    private final String contentType;

    /**
     * 请求头信息
     */
    @Getter
    private final HttpHeaders headers;

    /**
     * 请求参数解析器
     */
    @Getter
    private final QueryStringDecoder queryStringDecoder;
    /**
     * 完整Http请求
     */
    @Getter
    private final FullHttpRequest fullHttpRequest;

    /**
     * 请求体
     */
    @Getter
    private String body;
    /**
     * 用户ID，JWT认证后使用
     */
    @Getter
    @Setter
    private Long userId;
    /**
     * Cookie集合
     */
    @Getter
    private Map<String, Cookie> cookieMap;
    /**
     * post请求定义参数集合
     */
    @Getter
    private Map<String, List<String>> postParameters;

    /**
     * 可变请求协议名 Http——>Https
     */
    private String modifyScheme;

    private String modifyHost;

    private String modifyPath;

    /**
     *  下游请求的 Http 请求构建器
     */
    private final RequestBuilder requestBuilder;

    public GatewayRequest(String uniqueId,
                          Charset charset,
                          String clientIp,
                          String host,
                          String uri,
                          HttpMethod method,
                          String contentType,
                          HttpHeaders headers,
                          FullHttpRequest fullHttpRequest) {
        this.uniqueId = uniqueId;
        this.beginTime = TimeUtil.getCurrentTimeMillis();
        this.charset = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.uri = uri;
        this.method = method;
        this.contentType = contentType;
        this.headers = headers;
        this.queryStringDecoder = new QueryStringDecoder(uri, charset);
        this.path = this.queryStringDecoder.path();
        this.fullHttpRequest = fullHttpRequest;

        this.modifyHost = host;
        this.modifyPath = path;
        this.modifyScheme = BasicConst.HTTP_PREFIX_SEPARATOR;
        this.requestBuilder = new RequestBuilder();
        this.requestBuilder.setHeaders(getHeaders());
        this.requestBuilder.setMethod(getMethod().name());
        this.requestBuilder.setQueryParams(queryStringDecoder.parameters());
        // 检查请求参数是否为空
        ByteBuf contentBuf = fullHttpRequest.content();
        if (Objects.nonNull(contentBuf)) {
            this.requestBuilder.setBody(contentBuf.nioBuffer());
        }
    }

    /**
     * 获取请求体
     * @return
     */
    public String getBody() {
        if (StringUtils.isEmpty(body)) {
            body = fullHttpRequest.content().toString(charset);
        }
        return body;
    }

    /**
     * 获取指定 cookie
     * @param name
     * @return
     */
    public Cookie getCookie(String name) {
        if (cookieMap == null) {
            cookieMap = new HashMap<>();
            String cookie = getHeaders().get(HttpHeaderNames.COOKIE);
            if (cookie == null) {
                log.warn("header cookies is null");
                return null;
            }
            Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookie);
            for (Cookie c : cookies) {
                cookieMap.put(name, c);
            }
        }
        return cookieMap.get(name);
    }

    /**
     * 获取请求参数值——GET
     * @param name
     * @return
     */
    public List<String> getQueryParameterValue(String name) {
        return queryStringDecoder.parameters().get(name);
    }

    /**
     *  获取请求参数值——POST
     * @param name
     * @return
     */
    public List<String> getPostParameterValue(String name) {
        String body = getBody();
        if (isFormPost()) {
            if (postParameters == null) {
                QueryStringDecoder decoder = new QueryStringDecoder(body, charset);
                postParameters = decoder.parameters();
            }
            if (postParameters == null || postParameters.isEmpty()) {
                return null;
            } else {
                return postParameters.get(name);
            }
        } else if (isJsonPost()) {
            try {
                return Lists.newArrayList(JsonPath.read(body, name).toString());
            } catch (Exception e) {
                log.error("JsonPath parse failed, jsonPath:{}, body:{}", name, body, e);
            }
        }
        return null;
    }

    /**
     * 是否表单提交/文件上传
     * @return
     */
    public boolean isFormPost() {
        return HttpMethod.POST.equals(method) &&
                (contentType.startsWith(HttpHeaderValues.FORM_DATA.toString()) || contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()));
    }

    /**
     * 是否json格式请求
     * @return
     */
    public boolean isJsonPost() {
        return HttpMethod.POST.equals(method) &&
                contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString());
    }

    @Override
    public void setModifyHost(String host) {
        this.modifyHost = host;
    }

    @Override
    public String getModifyHost() {
        return modifyHost;
    }

    @Override
    public void setModifyPath(String path) {
        this.modifyPath = path;
    }

    @Override
    public String getModifyPath() {
        return modifyPath;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        requestBuilder.addHeader(name, value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        requestBuilder.setHeader(name, value);
    }

    @Override
    public void addQueryParams(String name, String value) {
        requestBuilder.addQueryParam(name, value);
    }

    @Override
    public void addFormParam(String name, String value) {
        requestBuilder.addFormParam(name, value);
    }

    @Override
    public void addOrReplaceCookie(org.asynchttpclient.cookie.Cookie cookie) {
        requestBuilder.addOrReplaceCookie(cookie);
    }

    @Override
    public void setRequestTimeOut(int timeout) {
        requestBuilder.setRequestTimeout(timeout);
    }

    @Override
    public String getFinalUrl() {
        return modifyScheme + modifyHost + modifyPath;
    }

    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalUrl());
        return requestBuilder.build();
    }
}
