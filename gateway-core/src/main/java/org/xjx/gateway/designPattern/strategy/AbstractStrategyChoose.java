package org.xjx.gateway.designPattern.strategy;

import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;
import org.xjx.gateway.starter.ApplicationContextHolder;
import org.xjx.gateway.starter.init.ApplicationInitlizingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 策略选择器
 */
public class AbstractStrategyChoose implements ApplicationListener<ApplicationInitlizingEvent> {
    private final Map<String, AbstractExecuteStrategy> executeStrategyMap = new HashMap<>();

    /**
     * 根据 mark 查询具体策略
     * @param mark
     * @param predicateFlag
     * @return
     */
    public AbstractExecuteStrategy choose(String mark, Boolean predicateFlag) {
        // 正则表达式匹配策略
        if (predicateFlag != null && predicateFlag) {
            return executeStrategyMap.values().stream()
                    .filter(each-> StringUtils.hasText(each.patternMatchMark()))
                    .filter(each-> Pattern.compile(each.patternMatchMark()).matcher(mark).matches())
                    .findFirst()
                    .orElseThrow(()->new RuntimeException("策略未定义"));
        }
        // 普通模式匹配策略
        return Optional.ofNullable(executeStrategyMap.get(mark))
                .orElseThrow(()->new RuntimeException(String.format("[%s] 策略未定义", mark)));
    }

    /**
     * 查询策略并执行
     * @param mark
     * @param requestParam
     * @param <REQUEST>
     */
    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        executeStrategy.execute(requestParam);
    }

    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam, Boolean predictFlag) {
        AbstractExecuteStrategy executeStrategy = choose(mark, predictFlag);
        executeStrategy.execute(requestParam);
    }

    /**
     * 有返回值的执行策略
     * @param mark
     * @param requestParam
     * @param <REQUEST>
     * @param <RESPONSE>
     * @return
     */
    public <REQUEST,RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    @Override
    public void onApplicationEvent(ApplicationInitlizingEvent event) {
        Map<String, AbstractExecuteStrategy> beanMap = ApplicationContextHolder.getBeansOfType(AbstractExecuteStrategy.class);
        beanMap.forEach((beanName, bean)-> {
            AbstractExecuteStrategy strategy = executeStrategyMap.get(bean.mark());
            if (strategy != null) {
                throw new RuntimeException("Duplicate execution strategy for " + bean.mark());
            }
            executeStrategyMap.put(bean.mark(), bean);
        });
    }
}
