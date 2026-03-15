package org.xjx.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.xjx.common.config.ServiceRuleDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RouteTrieBenchmarkTest {
    
    private static final int THREAD_COUNT = 32;
    private static final int OPERATIONS_PER_THREAD = 1000;
    private static final int ROUTE_COUNT = 5000;
    
    @Test
    public void benchmarkOriginalRouteTrie() throws InterruptedException {
        testRouteTrie(new RouteTrie(), "Original RouteTrie");
    }
    
    @Test
    public void benchmarkOptimizedRouteTrie() throws InterruptedException {
        testRouteTrie(new OptimizedRouteTrie(), "Optimized RouteTrie");
    }
    
    @Test
    public void benchmarkConcurrentRouteTrie() throws InterruptedException {
        testRouteTrie(new ConcurrentRouteTrie(), "Concurrent RouteTrie");
    }
    
    private void testRouteTrie(RouteTrie trie, String name) throws InterruptedException {
        log.info("开始基准测试: {}", name);
        
        // 生成测试数据
        Map<String, ServiceRuleDTO> routes = generateRoutes(ROUTE_COUNT);
        AtomicLong insertTime = new AtomicLong();
        AtomicLong queryTime = new AtomicLong();
        AtomicLong deleteTime = new AtomicLong();
        
        // 预热
        warmup(trie, routes);
        
        // 并发插入测试
        long startInsert = System.currentTimeMillis();
        testConcurrentInsert(trie, routes, insertTime);
        long endInsert = System.currentTimeMillis();
        
        // 并发查询测试  
        long startQuery = System.currentTimeMillis();
        testConcurrentQuery(trie, queryTime);
        long endQuery = System.currentTimeMillis();
        
        // 并发删除测试
        long startDelete = System.currentTimeMillis();
        testConcurrentDelete(trie, deleteTime);
        long endDelete = System.currentTimeMillis();
        
        // 输出结果
        log.info("{} 性能测试结果:", name);
        log.info("  插入 {} 条路由耗时: {} ms", ROUTE_COUNT, endInsert - startInsert);
        log.info("  并发插入平均耗时: {} ms", insertTime.get() / (THREAD_COUNT * OPERATIONS_PER_THREAD));
        log.info("  查询操作平均耗时: {} ns", queryTime.get() / (THREAD_COUNT * OPERATIONS_PER_THREAD));
        log.info("  删除操作平均耗时: {} ns", deleteTime.get() / (THREAD_COUNT * OPERATIONS_PER_THREAD));
        
        // 内存使用统计
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        log.info("  内存使用: {} MB", memoryUsed);
    }
    
    private Map<String, ServiceRuleDTO> generateRoutes(int count) {
        Map<String, ServiceRuleDTO> routes = new HashMap<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            String path = String.format("/api/service%d/%s/resource%d", 
                random.nextInt(10), 
                random.nextBoolean() ? "user" : "order",
                random.nextInt(100));
            
            routes.put(path, ServiceRuleDTO.builder()
                .serviceName("service-" + random.nextInt(10))
                .routeName(path)
                .build());
        }
        
        return routes;
    }
    
    private void warmup(RouteTrie trie, Map<String, ServiceRuleDTO> routes) {
        routes.forEach((k, v) -> trie.insertRoute(k, v));
        routes.keySet().forEach(trie::removeRouteRule);
    }
    
    private void testConcurrentInsert(RouteTrie trie, Map<String, ServiceRuleDTO> routes, AtomicLong time) 
        throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    long startTime = System.nanoTime();
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        String key = routes.keySet().toArray()[random.nextInt(routes.size())].toString();
                        trie.insertRoute(key, routes.get(key));
                        
                        if (i % 100 == 0) {
                            Thread.yield(); // 避免线程饥饿
                        }
                    }
                    
                    time.addAndGet(System.nanoTime() - startTime);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
    }
    
    private void testConcurrentQuery(RouteTrie trie, AtomicLong time) 
        throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    long startTime = System.nanoTime();
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        String path = String.format("/api/test%d/user%d", 
                            random.nextInt(100), random.nextInt(1000));
                        trie.getServiceRule(path);
                        
                        if (i % 100 == 0) {
                            Thread.yield();
                        }
                    }
                    
                    time.addAndGet(System.nanoTime() - startTime);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
    }
    
    private void testConcurrentDelete(RouteTrie trie, AtomicLong time) 
        throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    long startTime = System.nanoTime();
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        String path = String.format("/api/test%d/resource%d", 
                            random.nextInt(50), random.nextInt(500));
                        trie.removeRouteRule(path);
                        
                        if (i % 100 == 0) {
                            Thread.yield();
                        }
                    }
                    
                    time.addAndGet(System.nanoTime() - startTime);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
    }
}