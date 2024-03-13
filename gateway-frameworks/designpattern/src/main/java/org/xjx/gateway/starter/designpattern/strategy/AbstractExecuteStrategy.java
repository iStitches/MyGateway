package org.xjx.gateway.starter.designpattern.strategy;

/**
 * 策略模式选择器
 */
public interface AbstractExecuteStrategy<REQUEST, RESPONSE> {
    /**
     * 执行策略的标识
     * @return
     */
    default String mark() {return null;}

    /**
     * 执行策略范围匹配标识
     * @return
     */
    default String patternMatchMark() {
        return null;
    }

    /**
     * 执行策略
     * @param requestParam
     */
    default void execute(REQUEST requestParam) {}

    /**
     * 执行策略带返回值
     * @param requestParam
     * @return
     */
    default RESPONSE executeResp(REQUEST requestParam) {return null;}
}
