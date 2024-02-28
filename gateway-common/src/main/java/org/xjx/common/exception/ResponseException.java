package org.xjx.common.exception;

import org.asynchttpclient.Response;
import org.xjx.common.enums.ResponseCode;

public class ResponseException extends BaseException{
    public ResponseException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public ResponseException() {
        this(ResponseCode.INTERNAL_ERROR);
    }

    public ResponseException(Throwable throwable, ResponseCode code) {
        super(code.getMessage(), throwable, code);
        this.code = code;
    }
}
