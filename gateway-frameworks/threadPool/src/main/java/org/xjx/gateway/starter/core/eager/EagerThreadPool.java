package org.xjx.gateway.starter.core.eager;

import org.xjx.gateway.starter.core.ThreadPool;
import org.xjx.gateway.starter.core.enums.ThreadPoolEnum;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 快速消费线程池
 */
public class EagerThreadPool implements ThreadPool {
    private ThreadPoolExecutor executor;

    public EagerThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    private final AtomicInteger taskCount = new AtomicInteger();

    public int getSubmittedTaskCount() {
        return taskCount.get();
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    @Override
    public String Name() {
        return ThreadPoolEnum.EAGER_THREAD_POOL.getType();
    }

    @Override
    public void execute(Runnable command) {
        taskCount.incrementAndGet();
        try {
            executor.execute(command);
        } catch (RejectedExecutionException e) {
            EagerTaskQueue queue = (EagerTaskQueue) executor.getQueue();
            try {
                if (!queue.retryOffer(command, 0, TimeUnit.MILLISECONDS)) {
                    taskCount.decrementAndGet();
                    throw new RejectedExecutionException("Queue capacity is full.", e);
                }
            } catch (Exception ex) {
                taskCount.decrementAndGet();
                throw new RejectedExecutionException(ex);
            }
        } catch (Exception ex) {
            taskCount.decrementAndGet();
            throw ex;
        }
    }

    @Override
    public Object executeResp(Runnable task) {
        return null;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void shutdownNow() {
        executor.shutdownNow();
    }
}
