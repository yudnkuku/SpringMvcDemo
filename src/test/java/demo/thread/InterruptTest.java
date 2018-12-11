package demo.thread;


import org.junit.Test;

public class InterruptTest {

    public static void main(String[] args) throws InterruptedException {

        Object lock = new Object();

        Thread t = new Thread(() -> {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        Thread.sleep(3000);
        t.interrupt();
    }

    @Test
    public void testSleepInterrupt() throws InterruptedException {
        Thread t = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                System.out.println("Thread is running...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Thread is interrupted...");
                    System.out.println("Thread interrupt flag is : " +
                            Thread.currentThread().isInterrupted());
                }
            }
        });
        System.out.println("Starting thread...");
        t.start();
        Thread.sleep(3000);
        System.out.println("Asking thread to stop...");
        t.interrupt();
        Thread.sleep(3000);
        System.out.println("Stop application...");
    }

}
