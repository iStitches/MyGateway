package org.xjx.gateway.starter.designpattern.strategy;

import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;
import org.xjx.gateway.starter.ApplicationContextHolder;
import org.xjx.gateway.starter.init.ApplicationInitlizingEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 策略选择器上下文
 */
public class AbstractStrategyChoose implements ApplicationListener<ApplicationInitlizingEvent> {
    /**
     * 可执行的策略集合
     */
    private final Map<String, AbstractExecuteStrategy> executeStrategyMap = new HashMap<>();

    /**
     * 获取 mark 对应执行策略
     * @param mark
     * @param predicateFlag
     * @return
     */
    public AbstractExecuteStrategy choose(String mark, Boolean predicateFlag) {
        if (predicateFlag != null && predicateFlag) {
            return executeStrategyMap.values().stream()
                    .filter(each -> StringUtils.hasText(each.patternMatchMark()))
                    .filter(each -> Pattern.compile(each.patternMatchMark()).matcher(mark).matches())
                    .findFirst()
                    .orElseThrow(()->new RuntimeException("策略未定义"));
        }
        return Optional.ofNullable(executeStrategyMap.get(mark)).orElseThrow(()->new RuntimeException("策略未定义："+mark));
    }

    /**
     * 根据 mark 执行具体策略
     * @param mark
     * @param requestParam
     * @param <REQUEST>
     */
    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        executeStrategy.execute(requestParam);
    }

    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam, Boolean predicateFlag) {
        AbstractExecuteStrategy executeStrategy = choose(mark, predicateFlag);
        executeStrategy.execute(requestParam);
    }

    public <REQUEST, RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = choose(mark, null);
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    /**
     * Spring Bean注册完毕后，扫描并执行相关操作
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationInitlizingEvent event) {
        Map<String, AbstractExecuteStrategy> actual = ApplicationContextHolder.getBeansOfType(AbstractExecuteStrategy.class);
        executeStrategyMap.forEach((beanName, bean)-> {
            AbstractExecuteStrategy beanExist = executeStrategyMap.get(bean.mark());
            if (beanExist != null) {
                throw new RuntimeException("Duplicate execution policy: " + beanName);
            }
            executeStrategyMap.put(bean.mark(), bean);
        });
    }
}
