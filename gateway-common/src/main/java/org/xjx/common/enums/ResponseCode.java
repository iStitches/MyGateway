package org.xjx.common.enums;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS(HttpResponseStatus.OK, 0, "success"),
    UNAUTHORIZED(HttpResponseStatus.UNAUTHORIZED, 401, "user unauthorized"),
    INTERNAL_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 1000, "gateway inner error"),
    SERVICE_UNAVALIABLE(HttpResponseStatus.SERVICE_UNAVAILABLE, 2000, "unavailable service, try later"),

    REQUEST_PARSE_ERROR(HttpResponseStatus.BAD_REQUEST, 10000, "parse request failed"),
    REQUEST_PARSE_ERROR_NO_UNIQUEID(HttpResponseStatus.BAD_REQUEST, 10001, "parse request failed, uniqueId must exists in header"),
    PATH_NO_MATCHED(HttpResponseStatus.NOT_FOUND,10002, "can't find matched requestPath"),
    SERVICE_DEFINITION_NOT_FOUND(HttpResponseStatus.NOT_FOUND,10003, "can't find declartion of service"),
    SERVICE_INVOKER_NOT_FOUND(HttpResponseStatus.NOT_FOUND,10004, "can't find applyInstance of service"),
    SERVICE_INSTANCE_NOT_FOUND(HttpResponseStatus.NOT_FOUND,10005, "can't find instance of service"),
    FILTER_CONFIG_PARSE_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR,10006, "filter parse error"),
    GATEWAY_FALLBACK_TIMEOUT(HttpResponseStatus.GATEWAY_TIMEOUT, 10055, "requestTime, cause hystrix fallback"),
    GATEWAY_FALLBACK_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10066, "internal server error, cause hystrix fallback"),

    REQUEST_TIMEOUT(HttpResponseStatus.GATEWAY_TIMEOUT, 10007, "connect downStream services timeout"),

    HTTP_RESPONSE_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10030, "service response failed"),
    FLOW_CONTROL_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10040, "request overlimited error"),
    FLOW_CONTROL_SINGLE_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10050, "signleFlowControlStrategy is empty"),

    DUBBO_DISPATCH_CONFIG_EMPTY(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10016, "router can't be empty"),
    DUBBO_PARAMETER_TYPE_EMPTY(HttpResponseStatus.BAD_REQUEST, 10017, "requestParam type can't be empty"),
    DUBBO_PARAMETER_VALUE_ERROR(HttpResponseStatus.BAD_REQUEST, 10018, "parse requestParam failed"),
    DUBBO_METHOD_NOT_FOUNT(HttpResponseStatus.NOT_FOUND, 10021, "method doesn't exist"),
    DUBBO_CONNECT_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10022, "downStream services failed, try later"),
    DUBBO_REQUEST_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10028, "request service failed"),
    DUBBO_RESPONSE_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR, 10029, "service response error"),
    VERIFICATION_FAILED(HttpResponseStatus.BAD_REQUEST,10030, "requestParam check failed"),
    BLACKLIST(HttpResponseStatus.FORBIDDEN,10004, "refuse request because ip exists in blacklist"),
    WHITELIST(HttpResponseStatus.FORBIDDEN,10005, "accept request because ip exists in whitelist");

    ResponseCode(HttpResponseStatus status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private HttpResponseStatus status;
    private int code;
    private String message;
}
