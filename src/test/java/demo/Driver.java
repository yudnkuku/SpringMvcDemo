package demo;

import java.util.concurrent.CountDownLatch;

public class Driver {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch stopSignal = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(new Worker(startSignal, stopSignal)).start();
        }
        Thread.sleep(100);
        System.out.println("工作线程开始");
        startSignal.countDown();
        stopSignal.await();
        System.out.println("工作线程全部结束");
    }
}
