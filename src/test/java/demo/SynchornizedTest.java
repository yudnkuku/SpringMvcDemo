package demo;

import org.junit.Test;

public class SynchornizedTest {

    public static void main(String[] args) {
        SyncThread syncThread = new SyncThread();
//        Thread thread1 = new Thread(syncThread,"SyncThread1");
//        Thread thread2 = new Thread(syncThread,"SyncThread2");
        Thread thread1 = new Thread(new SyncThread(),"SyncThread1");
        Thread thread2 = new Thread(new SyncThread(),"SyncThread2");
        thread1.start();
        thread2.start();
    }

    static class SyncThread implements Runnable {

        private int count;

        public SyncThread() {
            count = 0;
        }

        @Override
        public void run() {
            //每个对象对应一把锁
            ///一个线程访问synchronized(this)代码块后，其他线程试图访问该同步代码块都会被阻塞
            synchronized (this) {
                for (int i = 0; i < 5; i++) {
                    System.out.println(Thread.currentThread().getName() + ":" + (count++));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public int getCount() {
            return count;
        }
    }
}
