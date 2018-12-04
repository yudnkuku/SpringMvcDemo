package demo.thread;

public class SyncThread implements Runnable {

    private int count;

    public SyncThread() {
        count = 0;
    }

    @Override
    public void run() {
        synchronized (this) {
            for (int i = 0; i < 5; i++) {
                System.out.println(Thread.currentThread().getName() + "======" + count++);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
