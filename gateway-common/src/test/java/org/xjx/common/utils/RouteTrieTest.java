package org.xjx.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xjx.common.config.ServiceRuleDTO;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RouteTrieTest {
    private static RouteTrie routeTrie = new RouteTrie();

    @Test
    public void testExtractRoute() {
        routeTrie.insertRoute("/api/test/hello", ServiceRuleDTO.builder().serviceName("medical").routeName("/api/test/hello").build());
        ServiceRuleDTO rule = routeTrie.getServiceRule("/api/test/hello");
        System.out.println(rule.toString());
        log.info("insert rule {}", rule);
        Assertions.assertNotNull(rule);
        routeTrie.removeRouteRule("/api/test/hello");
    }

    @Test
    public void testWildRoute() {
        routeTrie.insertRoute("/api/test/*", ServiceRuleDTO.builder().serviceName("medical").routeName("/api/test/*").build());
        ServiceRuleDTO rule = routeTrie.getServiceRule("/api/test/hi");
        Assertions.assertNotNull(rule);
        routeTrie.insertRoute("/api/medical/*", ServiceRuleDTO.builder().serviceName("medical").routeName("/api/medical/*").build());
        ServiceRuleDTO rule2 = routeTrie.getServiceRule("/api/medical/hi");
        log.info("insert rule2: {}", rule2);
        Assertions.assertNotNull(rule2);
        routeTrie.removeRouteRule("/api/test/*");
        routeTrie.removeRouteRule("/api/medical/*");
    }

    @Test
    public void testVariableRoute() {
        routeTrie.insertRoute("/api/test/:id", ServiceRuleDTO.builder().serviceName("medical").routeName("/api/test/:id").build());
        ServiceRuleDTO rule = routeTrie.getServiceRule("/api/test/8888");
        log.info("insert rule: {}", rule);
        Assertions.assertNotNull(rule);
        routeTrie.removeRouteRule("/api/test/:id");
    }

    @Test
    public void testRemoveRoute() {
        routeTrie.insertRoute("/api/test/*", ServiceRuleDTO.builder().serviceName("medical").routeName("/api/test/*").build());
        ServiceRuleDTO rule = routeTrie.getServiceRule("/api/test/hi");
        log.info("insert rule: {}", rule);
        Assertions.assertNotNull(rule);
        routeTrie.insertRoute("/api/medical/*", ServiceRuleDTO.builder().serviceName("medical").routeName("/api/medical/*").build());
        routeTrie.removeRouteRule("/api/test/*");
        ServiceRuleDTO oldRule = routeTrie.getServiceRule("/api/test/hi");
        Assertions.assertNull(oldRule);
        routeTrie.removeRouteRule("/api/medical/*");
    }

    @Test
    public void testMultiThreadRoute() throws InterruptedException {
        int count = 1000;
        CountDownLatch latch = new CountDownLatch(count);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(32, 32, 2,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(count));
        for (int i = 0; i < count; i++) {
            executor.execute(new Run(latch));
        }
        latch.await();
    }

    class Run implements Runnable {
        private final CountDownLatch latch;

        public Run(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                long insertStartTime = System.currentTimeMillis();
                String path = String.format("/api/test%d/*", new Random().nextInt());
                routeTrie.insertRoute(path, ServiceRuleDTO.builder().serviceName("medical").routeName("/api/test/*").build());
                log.info("insert cost {} ms", System.currentTimeMillis() - insertStartTime);

                long getStartTime = System.currentTimeMillis();
                ServiceRuleDTO serviceRule = routeTrie.getServiceRule(path);
                log.info("get cost {} ms", System.currentTimeMillis() - getStartTime);
                Assertions.assertNotNull(serviceRule);

                routeTrie.removeRouteRule(path);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }
    }
}
