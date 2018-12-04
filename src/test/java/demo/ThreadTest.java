package demo;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ThreadTest {

    public static void main(String[] args) {
        FutureTask<Integer> futureTask = new FutureTask<Integer>(() -> {
            int i = 0;
            int sum = 0;
            for (; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + " " + i);
                sum += i;
            }
            return sum;
        });

        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " " + i);
            if (i == 3) {
                Thread thread = new Thread(futureTask);
                thread.start();
            }
        }

        System.out.println("主线程执行完毕！！！");

        try {
            int sum = futureTask.get();
            System.out.println("子线程计算结果为:" + sum);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testJoin() {
        Thread thread = new Thread(new MyRunnable(), "thread_join");
        Thread thread1 = new Thread(new MyRunnable(), "thread_max");
        thread1.setPriority(Thread.MAX_PRIORITY);
        for(int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " " + i);
            if (i == 3) {
                thread.start();
                thread1.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testSleep() {
        Thread thread_main = Thread.currentThread();
        Thread thread = new Thread(new SleepRunnable(thread_main));
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " " + i);
            //thread 进入就绪状态
            if (i == 3) {
                thread.start();
                //睡眠当前线程,必然会启动线程thread
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class SleepRunnable implements Runnable {
        private Thread outThread;

        public SleepRunnable(Thread thread) {
            this.outThread = thread;
        }

        @Override
        public void run() {
            System.out.println("睡眠线程状态：" + outThread.getState().name());
            System.out.println("当前线程状态：" + Thread.currentThread().getState().name());
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + " " + i);
            }
        }
    }

    @Test
    public void testPriority() {
        Thread thread = new Thread(new MyRunnable(),"thread_max");
        Thread thread1 = new Thread(new MyRunnable(),"thread_normal");
        Thread currentThread = Thread.currentThread();
        System.out.println(currentThread.getName() + "优先级为：" + currentThread.getPriority());
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " " + i);
            if (i == 3) {
                thread.setPriority(Thread.MAX_PRIORITY);
                thread1.setPriority(Thread.NORM_PRIORITY);
                thread1.start();
                thread.start();
            }
        }
    }

    class MyRunnable implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + " " + i);
            }
        }
    }

    class MyRunnable1 implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + " " + i);
                if (i == 5) {
                    //暂停当前线程执行，将其放入就绪线程队列，从队列中选取优先级不小于该线程的线程执行
                    System.out.println("执行yield()方法");
                    Thread.yield();
                }
            }
        }
    }

    @Test
    public void testYield() {
        Thread thread1  =new Thread(new MyRunnable1(), "thread_min");
        thread1.setPriority(Thread.MIN_PRIORITY);
        Thread thread2 = new Thread(new MyRunnable(), "thread_max");
        thread2.setPriority(Thread.MAX_PRIORITY);
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " " + i);
            if (i == 3) {
                try {
                    thread1.start();
                    thread2.start();
                    //睡眠当前线程，此时线程thread2应该执行，其优先级更高
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testSleep1() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "执行并即将sleep");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "恢复执行");
            }
        });
        thread.start();
        System.out.println(Thread.currentThread().getName() + "执行并即将sleep");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + "恢复执行");

    }

    @Test
    public void testInterrupt() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while(!Thread.interrupted()) {
                    Thread thread1 = Thread.currentThread();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.out.println(thread1.getName() + "  (" +
                                thread1.getState() + ")  catch InterruptedException");
                    }
                    i++;
                    System.out.println(thread1.getName() + "  (" +
                                thread1.getState() + ")  loop " + i);
                }
            }
        });
        thread.start();
        Thread.sleep(300);
        thread.interrupt();
        System.out.println(thread.getName() + "  (" + thread.getState() + ")  is interrupted");
        Thread.sleep(100);
        System.out.println(thread.getName() + "  (" + thread.getState() + ")  is interrupted now");
    }
}
