package org.xjx.common.exception;

import org.xjx.common.enums.ResponseCode;

/**
 * 限流异常类型
 */
public class LimitedException extends BaseException{
    public LimitedException(ResponseCode code) {
        super(code.getMessage(), code);
    }

    public LimitedException(Throwable cause, ResponseCode code) {
        super(code.getMessage(), cause, code);
    }
}
