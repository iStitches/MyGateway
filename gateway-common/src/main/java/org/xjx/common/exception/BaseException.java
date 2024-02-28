package org.xjx.common.exception;

import org.xjx.common.enums.ResponseCode;

public class BaseException extends RuntimeException{
    protected ResponseCode code;

    public BaseException() {
    }

    public BaseException(ResponseCode code) {
        this.code = code;
    }

    public BaseException(String message, ResponseCode code) {
        super(message);
        this.code = code;
    }

    public BaseException(String message, Throwable cause, ResponseCode code) {
        super(message, cause);
        this.code = code;
    }

    public ResponseCode getCode() {
        return code;
    }
}
