package demo.thread;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTest {

    @Test
    public void testThreadPool() {
        long startTime = System.currentTimeMillis();
        final List<Integer> list = new LinkedList<>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 60, TimeUnit.SECONDS,
                                        new LinkedBlockingQueue<>(20000));
        final Random random = new Random();
        for (int i = 0; i < 20000; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    list.add(random.nextInt());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("线程池执行20000个任务消耗时间 ：" + elapsedTime + "ms");
    }

    @Test
    public void testThread() {
        long startTime = System.currentTimeMillis();
        final List<Integer> list = new LinkedList<>();
        final Random random = new Random();
        for (int i = 0; i < 20000; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    list.add(random.nextInt());
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("线程执行20000个任务消耗时间 ：" + elapsedTime + "ms");
    }
}
