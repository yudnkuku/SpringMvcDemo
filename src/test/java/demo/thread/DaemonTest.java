package demo.thread;

import org.junit.Test;

public class DaemonTest {

    @Test
    public void testDeamon() throws InterruptedException {
        Thread daemon = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().sleep(10000);
                    System.out.println("Daemon线程休眠10s结束");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        daemon.setDaemon(true);
        daemon.start();
        Thread.currentThread().sleep(5000);
        System.out.println("主线程休眠5S结束");
    }
}
