package org.xjx.gateway.starter.core.dynamic;

import com.alibaba.nacos.api.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.xjx.gateway.starter.config.DefaultThreadPoolProperty;
import org.xjx.gateway.starter.core.ThreadPool;
import org.xjx.gateway.starter.core.enums.ThreadPoolEnum;
import org.xjx.gateway.starter.listener.NacosListener;

import java.time.LocalTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 动态线程池核心类
 */
@Slf4j
public class DynamicThreadPool implements ThreadPool {
    private static final long AWAIT_TIMEOUT = 600;

    private static DefaultThreadPoolProperty THREADPOOL_PROPERTY;

    private volatile static DynamicThreadPool THREADPOOL_INSTANCE;

    private ThreadPoolExecutor executor;

    private static String NACOS_ADDR = "192.168.220.1:8848";

    private NacosListener nacosListener;

    /**
     * 单例模式，只有一个线程池
     */
    public static DynamicThreadPool getInstance() {
        if (THREADPOOL_INSTANCE == null) {
            synchronized (DynamicThreadPool.class) {
                if (THREADPOOL_INSTANCE == null) {
                    THREADPOOL_INSTANCE = new DynamicThreadPool();
                }
            }
        }
        return THREADPOOL_INSTANCE;
    }

    @Override
    public String Name() {
        return ThreadPoolEnum.DYNAMIC_THREAD_POOL.getType();
    }

    public void execute(Runnable command) {
        THREADPOOL_INSTANCE.executor.execute(command);
    }

    @Override
    public Object executeResp(Runnable task) {
        return null;
    }

    @Override
    public void shutdown() {
        destory(executor);
    }

    @Override
    public void shutdownNow() {
        destory(executor);
    }

    private DynamicThreadPool() {
        this.executor = commonThreadPool();
        this.nacosListener = new NacosListener(NACOS_ADDR, this.executor);
        registerListener();
    }

    /**
     * 注册 Nacos 监听器
     */
    private void registerListener() {
        try {
            nacosListener.listenerNacosConfig();
        } catch (Exception e) {
            log.error("NacosListener failed, err : {}", e.getMessage());
        }
    }


    public static void init() {
        init(null);
    }
    public static void init(String nacosAddr) {
        THREADPOOL_PROPERTY = new DefaultThreadPoolProperty();
        if (!StringUtils.isEmpty(nacosAddr)) {
            NACOS_ADDR = nacosAddr;
        }
    }

    private ThreadPoolExecutor commonThreadPool() {
        ThreadPoolExecutor executor = dynamicThreadPoolExecutor();
        threadStatus(executor, "dynamic-thread-pool");
        return executor;
    }

    /**
     * 初始化基础线程池
     * @return
     */
    private ThreadPoolExecutor baseThreadPool() {
        return new ThreadPoolExecutor(
                THREADPOOL_PROPERTY.getCorePoolSize(),
                THREADPOOL_PROPERTY.getMaximumPoolSize(),
                60,
                TimeUnit.SECONDS,
                new ResizableCapacityLinkedBlockIngQueue<>(),
                ThreadFactoryBuilder.builder()
                        .prefix(Name())
                        .daemon(false)
                        .build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    private ThreadPoolExecutor dynamicThreadPoolExecutor() {
        ThreadPoolExecutor executor = baseThreadPool();
        return executor;
    }

    /**
     * 线程池优雅停机
     * 1.shutdown 开始关闭,不再接受任何新任务，但是已存在的任务还会继续执行
     * 2.awaitTermination：设定线程池关闭的超时时间，到时间能够关闭返回true，否则返回false
     * 3.shutdownNow：队列中所有尚未被执行的任务都会被抛弃，同时设置每个线程的中断标志位
     * @param executor
     */
    public void destory(ThreadPoolExecutor executor) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(AWAIT_TIMEOUT, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void threadStatus(ThreadPoolExecutor executor, String name) {
        BlockingQueue<Runnable> queue = executor.getQueue();
        System.out.println(LocalTime.now().toString() + "  " +
                Thread.currentThread().getName() + name +
                "coreThreadSize:" + executor.getCorePoolSize() +
                "activeThreadNums:" + executor.getActiveCount() +
                "maxThreadSize:" + executor.getMaximumPoolSize() +
                "threadActive:" + divide(executor.getActiveCount(), executor.getMaximumPoolSize()) +
                "completedTaskNums:" + executor.getCompletedTaskCount() +
                "queueSize:" + (queue.size() + queue.remainingCapacity()) +
                "waitingThreads:" + queue.size() + " queueCapicity:" + queue.remainingCapacity() +
                "queueUseAvailable:" + divide(queue.size(), queue.size() + queue.remainingCapacity()));
    }

    private static String divide(int num1, int num2) {
        return String.format("%1.2f%%", Double.parseDouble(num1 + "") / Double.parseDouble(num2 + "") * 100);
    }
}
