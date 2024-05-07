package org.xjx.gateway.designPattern.strategy;

/**
 * 策略模式统一接口
 */
public interface AbstractExecuteStrategy<REQUEST, RESPONSE> {
    /**
     * 执行策略标识
     * @return
     */
    default String mark() {
        return null;
    }

    /**
     * 执行策略范围匹配标识
     * @return
     */
    default String patternMatchMark() {
        return null;
    }

    default void execute(REQUEST requestParam) {

    }

    default RESPONSE executeResp(REQUEST requestParam) {
        return null;
    }
}
