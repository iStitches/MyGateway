package org.xjx.gateway.starter.core.eager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 快速消费线程池任务队列
 */
public class EagerTaskQueue<R extends Runnable> extends LinkedBlockingQueue<Runnable> {
    private EagerThreadPool executor;

    public EagerTaskQueue(int capicity) {
        super(capicity);
    }

    @Override
    public boolean offer(Runnable runnable) {
        /**
         * 获取当前运行的核心线程
         */
        int currentPoolSize = executor.getExecutor().getPoolSize();
        if (executor.getSubmittedTaskCount() < currentPoolSize) {
            return super.offer(runnable);
        }
        /**
         * 当前线程数量小于最大线程数，返回 False，不添加到队列中，创建新线程运行任务
         */
        if (currentPoolSize < executor.getExecutor().getMaximumPoolSize()) {
            return false;
        }
        return super.offer(runnable);
    }

    public boolean retryOffer(Runnable o, long timeout, TimeUnit timeUnit) throws Exception{
        if (executor.getExecutor().isShutdown()) {
            throw new RejectedExecutionException("Executor is shutdown");
        }
        return super.offer(o, timeout, timeUnit);
    }
}
