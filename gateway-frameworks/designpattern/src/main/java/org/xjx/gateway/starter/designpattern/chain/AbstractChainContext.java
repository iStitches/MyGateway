package org.xjx.gateway.starter.designpattern.chain;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.xjx.gateway.starter.ApplicationContextHolder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 责任链上下文
 * @param <T>
 */
public class AbstractChainContext<T> implements CommandLineRunner {
    private final Map<String, List<AbstractChainHandler>> chainHandlerMap = new HashMap<>();

    public void handler(String mark, T requestParam) {
        List<AbstractChainHandler> chainHandlers = chainHandlerMap.get(mark);
        if (CollectionUtils.isEmpty(chainHandlers)) {
            throw new RuntimeException("ChainHandlerMap is empty, mark:"+mark);
        }
        chainHandlers.forEach(each->each.handler(requestParam));
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, AbstractChainHandler> chainFilterMap = ApplicationContextHolder
                .getBeansOfType(AbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            List<AbstractChainHandler> abstractChainHandlers = chainHandlerMap.get(bean.mark());
            if (CollectionUtils.isEmpty(abstractChainHandlers)) {
                abstractChainHandlers = new ArrayList();
            }
            abstractChainHandlers.add(bean);
            List<AbstractChainHandler> actualAbstractChainHandlers = abstractChainHandlers.stream()
                    .sorted(Comparator.comparing(Ordered::getOrder))
                    .collect(Collectors.toList());
            chainHandlerMap.put(bean.mark(), actualAbstractChainHandlers);
        });
    }
}
