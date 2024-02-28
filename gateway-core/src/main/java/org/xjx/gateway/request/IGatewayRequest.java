package org.xjx.gateway.request;

import org.asynchttpclient.Request;
import org.asynchttpclient.cookie.Cookie;

/**
 *  网关请求接口，供用户自定义操作
 */
public interface IGatewayRequest {
    /**
     * 修改目标服务地址
     * @param host
     */
    void setModifyHost(String host);

    /**
     * 获取目标服务地址
     * @return
     */
    String getModifyHost();

    /**
     * 修改目标服务路径
     * @param path
     */
    void setModifyPath(String path);

    /**
     * 获取目标服务路径
     * @return
     */
    String getModifyPath();

    /**
     * 添加请求头信息
     * @param name
     * @param value
     */
    void addHeader(CharSequence name, String value);

    /**
     * 设置请求头信息
     * @param name
     * @param value
     */
    void setHeader(CharSequence name, String value);

    /**
     * 添加Get请求参数
     * @param name
     * @param value
     */
    void addQueryParams(String name, String value);

    /**
     * 添加表单请求参数
     * @param name
     * @param value
     */
    void addFormParam(String name, String value);

    /**
     * 添加或替换Cookie
     * @param cookie
     */
    void addOrReplaceCookie(Cookie cookie);

    /**
     * 设置请求超时时间
     * @param timeout
     */
    void setRequestTimeOut(int timeout);

    /**
     * 获取最终请求URL路径，例如：  Http://localhost?8081/api/admin?name=xxx
     * @return
     */
    String getFinalUrl();

    Request build();
}
