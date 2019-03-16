package demo.thread;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {

    @Test
    public void testCountDownLatch() {

        CountDownLatch count = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count.countDown();
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count.countDown();
        });

        t1.start();
        t2.start();

        Thread t3 = new Thread(() -> {
            try {
                count.await();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + "被中断了");
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + "从await阻塞中返回");
        });

        Thread t4 = new Thread(() -> {
            try {
                count.await();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + "被中断了");
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + "从await阻塞中返回");
        });

        t3.start();
        t4.start();
    }

    public static void main(String[] args) {
        CountDownLatch count = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count.countDown();
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count.countDown();
        });

        t1.start();
        t2.start();

        Thread t3 = new Thread(() -> {
            try {
                count.await();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + "被中断了");
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + "从await阻塞中返回");
        });

        Thread t4 = new Thread(() -> {
            try {
                count.await();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + "被中断了");
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + "从await阻塞中返回");
        });

        t3.start();
        t4.start();
    }
}
