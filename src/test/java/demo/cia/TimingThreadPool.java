package demo.cia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimingThreadPool extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger("TimingThreadPool");
    private final AtomicLong numTasks = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();

    public TimingThreadPool(int corePoolSize, int maxPoolSize, long keepTimeAlive,
                            TimeUnit unit, BlockingQueue queue) {
        super(corePoolSize, maxPoolSize, keepTimeAlive, unit, queue);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        logger.info("Thread %s start %s", t, r);
        startTime.set(System.nanoTime());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            long endTime = System.nanoTime();
            long taskTime = endTime - startTime.get();
            numTasks.incrementAndGet();
            totalTime.addAndGet(taskTime);
            logger.info("Thread %s : end %s, time=%s", t, r, taskTime);
        } finally {
            super.afterExecute(r, t);
        }
    }

    @Override
    protected void terminated() {
        try {
            logger.info(String.format("Terminated: avg time=%dns",
                        totalTime.get() / numTasks.get()));
        } finally {
            super.terminated();
        }
    }
}
