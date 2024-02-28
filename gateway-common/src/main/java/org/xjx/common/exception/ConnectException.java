package org.xjx.common.exception;

import lombok.Getter;
import org.xjx.common.enums.ResponseCode;

public class ConnectException extends BaseException{
    @Getter
    private final String uniqueId;

    @Getter
    private final String requestUrl;

    public ConnectException(String uniqueId, String requestUrl) {
        this.uniqueId = uniqueId;
        this.requestUrl = requestUrl;
    }

    public ConnectException(Throwable throwable, String uniqueId, String requestUrl, ResponseCode code) {
        super(code.getMessage(), throwable, code);
        this.uniqueId = uniqueId;
        this.requestUrl = requestUrl;
    }
}
