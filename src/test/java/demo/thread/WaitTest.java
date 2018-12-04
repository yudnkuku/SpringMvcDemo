package demo.thread;

public class WaitTest {

    private static class WaitClass extends Thread {

        private Object lock;

        public WaitClass(Object lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            synchronized (lock) {
                System.out.println(Thread.currentThread().getName() + " wait begin");
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Object lock = new Object();
        Thread thread0 = new WaitClass(lock);
        Thread thread1 = new WaitClass(lock);
        thread0.start();
        thread1.start();
    }
}
