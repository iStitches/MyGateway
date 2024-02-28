package org.example;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.junit.Test;
import org.xjx.gateway.disruptor.EventListener;
import org.xjx.gateway.disruptor.ParallelQueueHandler;

import java.util.concurrent.ThreadFactory;

public class DisruptorTest {
    @Test
    public void Test1() throws InterruptedException {
        // 元素
        class Element {
            private int value;
            public int get() {
                return value;
            }
            public void set(int value) {
                this.value = value;
            }
        }
        // 生产者线程工厂
        ThreadFactory producerFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "simpleThread");
            }
        };
        // RingBuffer生产工厂
        EventFactory<Element> ringBufferFactory = new EventFactory<Element>() {
            @Override
            public Element newInstance() {
                return new Element();
            }
        };
        // 事件处理器
        EventHandler<Element> handler = new EventHandler<Element>() {
            @Override
            public void onEvent(Element event, long sequence, boolean endOfBatch) throws Exception {
                System.out.println("ThreadName:"+ Thread.currentThread().getName()+ ", Element: " + event.get());
            }
        };
        WaitStrategy strategy = new BlockingWaitStrategy();
        int bufferSize = 16;
        Disruptor<Element> disruptor = new Disruptor<Element>(ringBufferFactory, bufferSize, producerFactory, ProducerType.SINGLE, strategy);
        disruptor.handleEventsWith(handler);

        // 启动 disruptor
        disruptor.start();

        // 生产者放入数据
        RingBuffer<Element> ringBuffer = disruptor.getRingBuffer();
        for (int i = 0; true; i++) {
            long sequence = ringBuffer.next();
            try {
                Element element = ringBuffer.get(sequence);
                element.set(i);
            } finally {
                ringBuffer.publish(sequence);
            }
            Thread.sleep(10);
        }
    }

    @Test
    public void Test2() throws InterruptedException {
        class Element {
            private int value;
            public int get() {
                return value;
            }
            public void set(int value) {
                this.value = value;
            }
        }
        EventListener<Element> listener = new EventListener<Element>() {
            @Override
            public void onEvent(Element event) {
                System.out.println("ThreadName:"+Thread.currentThread().getName()+", Element:" + event.get());
            }

            @Override
            public void onException(Throwable ex, long sequence, Element event) {
                System.out.println("Exception:" + ex.getMessage());
            }
        };
        ParallelQueueHandler<Element> queue = new ParallelQueueHandler.Builder<Element>()
                .setBufferSize(16)
                .setEventListener(listener)
                .setNamePrefix("test-element")
                .setProducerType(ProducerType.MULTI)
                .setWaitStrategy(new BlockingWaitStrategy())
                .setThreads(Runtime.getRuntime().availableProcessors()).build();
        queue.start();
        for (int i = 0; true; i++) {
            Element element = new Element();
            element.set(i);
            queue.add(element);
            Thread.sleep(10);
        }
    }
}
