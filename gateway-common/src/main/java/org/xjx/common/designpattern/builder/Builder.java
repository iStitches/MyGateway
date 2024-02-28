package org.xjx.common.designpattern.builder;

import java.io.Serializable;

public interface Builder<T> extends Serializable {
    /**
     * 构建方法
     * @return
     */
    T build();
}
