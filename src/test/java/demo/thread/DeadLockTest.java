package demo.thread;

public class DeadLockTest {

    private static class DeadLock {

        private final Object right = new Object();

        private final Object left = new Object();

        public void leftRight() throws Exception{
            synchronized (right) {
                Thread.sleep(200);
                synchronized (left) {
                    System.out.println("leftRight() ends");
                }
            }
        }

        public void rightLeft() throws Exception{
            synchronized (left) {
                Thread.sleep(200);
                synchronized (right) {
                    System.out.println("rightLeft() ends");
                }
            }
        }
    }

    private static class Thread0 extends Thread {

        protected DeadLock deadLock;

        public Thread0 (DeadLock deadLock) {
            this.deadLock = deadLock;
        }

        @Override
        public void run() {
            try {
                deadLock.leftRight();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class Thread1 extends Thread0 {

        public Thread1 (DeadLock deadLock) {
            super(deadLock);
        }

        @Override
        public void run() {
            try {
                deadLock.rightLeft();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        DeadLock deadLock = new DeadLock();
        Thread0 thread0 = new Thread0(deadLock);
        thread0.start();
        Thread1 thread1 = new Thread1(deadLock);
        thread1.start();
    }
}
