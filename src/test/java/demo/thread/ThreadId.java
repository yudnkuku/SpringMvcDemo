package demo.thread;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadId {

    private static final AtomicInteger nextId = new AtomicInteger(0);

    private static final ThreadLocal<Integer> threadLocal =
            new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return nextId.getAndIncrement();
                }
            };

    public static int getId() {
        return threadLocal.get();
    }

    @Test
    public void test() {
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(new MyThread(),"thread-" + i);
            threads[i].start();
        }
    }

    class MyThread implements Runnable {

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() +
                        "======" + ThreadId.getId());
        }
    }
}
