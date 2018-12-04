package demo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolTest {

    public static void main(String[] args) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println("heartbeat:........");
            }
        };
        executor.scheduleAtFixedRate(task,5,3, TimeUnit.SECONDS);
    }
}
