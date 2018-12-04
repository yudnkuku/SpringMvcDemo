package demo;

import java.util.concurrent.locks.ReentrantLock;

public class LockTest {

    public static void main(String[] args) {
        SyncThread syncThread = new SyncThread();
        Thread thread1 = new Thread(syncThread,"thread1");
        Thread thread2 = new Thread(syncThread,"thread2");
        thread1.start();
        thread2.start();
    }

    static class SyncThread implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            ReentrantLock lock = new ReentrantLock();
            lock.lock();
            try{
                for (int i = 0; i < 5; i++) {
                    System.out.println(Thread.currentThread().getName() + ":" + count++);
                }
            }finally {
                lock.unlock();
            }
        }
    }
}
