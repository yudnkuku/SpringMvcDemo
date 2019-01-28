package demo.cia;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;

public class LogService {
    private final BlockingQueue<String> queue;
    private final LoggerThread loggerThread;
    private final PrintWriter writer;
    private boolean isShutdown;
    private int reservations;

    public LogService(BlockingQueue<String> queue, LoggerThread loggerThread, PrintWriter writer) {
        this.queue = queue;
        this.loggerThread = loggerThread;
        this.writer = writer;
    }

    public void start() {
        loggerThread.start();
    }

    public void stop() {
        synchronized (this) {
            isShutdown = true;
        }
        loggerThread.interrupt();
    }

    public void log(String msg) throws InterruptedException {
        synchronized (this) {
            if (isShutdown) {
                throw new IllegalStateException();
            }
            ++reservations;
        }
        queue.put(msg);
    }

    private class LoggerThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        synchronized (LogService.this) {
                            if (isShutdown && reservations == 0) {  //如果收到stop请求且reservations=0，那么退出消费者线程
                                break;
                            }
                        }
                        String msg = queue.take();  //当队列为空时会阻塞，从而响应中断，在中断中重试
                        synchronized (LogService.this) {
                            --reservations;
                        }
                        writer.println(msg);
                    } catch (InterruptedException e) {
                        //重试
                        e.printStackTrace();
                    }
                }
            } finally {
                //退出消费者线程前关闭writer
                writer.close();
            }
        }
    }
}
