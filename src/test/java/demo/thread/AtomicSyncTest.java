package demo.thread;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicSyncTest {

    private static final long VAL = 10000000L;

    private static void calc() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        long val = 0L;
        while (val < VAL) {
            val++;
        }
        stopWatch.stop();
        System.out.println("calc() elapsed(ms)" + stopWatch.getElapsedTime());
    }

    private static void calcAtomic() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        AtomicLong val = new AtomicLong(0);
        while (val.getAndIncrement() < VAL) {

        }
        stopWatch.stop();
        System.out.println("calcAtomic() elapsed(ms)" + stopWatch.getElapsedTime());
    }

    private static void calcSync() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        long val = 0L;
        while (val < VAL) {
            synchronized (AtomicSyncTest.class) {
                val++;
            }
        }
        stopWatch.stop();
        System.out.println("calcSync() elapsed(ms)" + stopWatch.getElapsedTime());
    }

    private static void testSyncThreads() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Thread thread1 = new Thread(new LoopSync());
        thread1.start();

        Thread thread2 = new Thread(new LoopSync());
        thread2.start();

        while(thread1.isAlive() || thread2.isAlive()) {

        }
        stopWatch.stop();
        System.out.println("testSyncThreads() elapsed(ms)" + stopWatch.getElapsedTime());
    }

    private static void testAtomicThreads() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Thread thread1 = new Thread(new LoopAtomic());
        thread1.start();

        Thread thread2 = new Thread(new LoopAtomic());
        thread2.start();

        while(thread1.isAlive() || thread2.isAlive()) {

        }
        stopWatch.stop();
        System.out.println("testAtomicThreads() elapsed(ms)" + stopWatch.getElapsedTime());
    }

    private static class LoopAtomic implements Runnable {

        @Override
        public void run() {
            AtomicLong val = new AtomicLong(0);
            while (val.getAndIncrement() < VAL) {

            }
        }
    }

    private static class LoopSync implements Runnable {

        @Override
        public void run() {
            long val = 0L;
            while (val < VAL) {
                synchronized (AtomicSyncTest.class) {
                    val++;
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("test starts");

        calc();
        calcAtomic();
        calcSync();

        testAtomicThreads();
        testSyncThreads();

        System.out.println("test ends");
    }
}
