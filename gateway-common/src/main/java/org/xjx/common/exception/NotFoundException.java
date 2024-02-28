package org.xjx.common.exception;

import org.xjx.common.enums.ResponseCode;

public class NotFoundException extends BaseException{
    public NotFoundException(ResponseCode code) {
        super(code);
    }

    public NotFoundException(String message, ResponseCode code) {
        super(message, code);
    }
}
