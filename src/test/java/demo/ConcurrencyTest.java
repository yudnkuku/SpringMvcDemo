package demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrencyTest {

    public static void main(String[] args) {
        ExecutorService executorService  = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            executorService.execute(new TestRunnable());
            System.out.println("======a" + i + "======");
        }
    }

    static class TestRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + "线程被调用了！");
        }
    }
}
