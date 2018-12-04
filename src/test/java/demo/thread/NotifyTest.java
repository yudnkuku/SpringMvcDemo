package demo.thread;

import org.junit.Test;

public class NotifyTest {

    private static class WaitClass extends Thread{

        private Object lock;

        public WaitClass (Object lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            synchronized (lock) {
                System.out.println(Thread.currentThread().getName() +
                                "获取lock对象锁，进入wait()方法");
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("wait()方法结束，重新获取lock对象锁");
            }
        }
    }

    private static class NotifyClass extends Thread {

        private Object lock;

        public NotifyClass (Object lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            synchronized (lock) {
                System.out.println(Thread.currentThread().getName() +
                                "获取lock对象锁，进入notify()方法");
                lock.notify();
                System.out.println("notify()方法结束");
            }
        }
    }

    @Test
    public void test() throws InterruptedException {
        Object lock = new Object();
        Thread thread0 = new WaitClass(lock);
        Thread thread1 = new NotifyClass(lock);
        thread0.start();
        Thread.sleep(100);
        thread1.start();
    }
}
