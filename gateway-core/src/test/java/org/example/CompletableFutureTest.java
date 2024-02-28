package org.example;

import org.junit.Test;
import org.xjx.common.utils.NetUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureTest {
    @Test
    public void test1() throws InterruptedException, ExecutionException {
        CompletableFuture<String> base = new CompletableFuture<>();
        CompletableFuture<String> future = base.thenApply(s -> s + " 2").thenApply(s -> s + " 3");
        base.complete("1");
        System.out.println(future.get());
    }


    @Test
    public void test2() throws ExecutionException, InterruptedException {
        CompletableFuture<String> base = new CompletableFuture<>();
        CompletableFuture<String> future = base.thenApply(s -> s + " 2").thenApply(s -> s + " 3");
        future.complete("1");
        System.out.println(future.get());
    }

    @Test
    public void test3() {
        System.out.println(NetUtil.getLocalIp());
    }
}
