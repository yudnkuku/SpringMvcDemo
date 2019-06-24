package demo;

import java.util.concurrent.CountDownLatch;

public class Worker implements Runnable{

    private final CountDownLatch startSignal;

    private final CountDownLatch stopSignal;

    public Worker(CountDownLatch startSignal, CountDownLatch stopSignal) {
        this.startSignal = startSignal;
        this.stopSignal = stopSignal;
    }

    void doWork() {
        System.out.println("i'm working");
    }


    @Override
    public void run() {
        try {
            startSignal.await();
            doWork();
            stopSignal.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
