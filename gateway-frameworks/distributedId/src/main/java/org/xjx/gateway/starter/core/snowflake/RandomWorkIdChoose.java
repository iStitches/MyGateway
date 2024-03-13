package org.xjx.gateway.starter.core.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class RandomWorkIdChoose extends AbstractWorkIdChooseTemplate implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        chooseAndInit();
    }

    @Override
    protected WorkIdWrapper chooseWorkId() {
        int start = 0, end = 31;
        return new WorkIdWrapper(getRandom(start, end), getRandom(start, end));
    }

    private static long getRandom(int start, int end) {
        long random = (long) (Math.random() * (end - start + 1) + start);
        return random;
    }
}
