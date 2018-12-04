package demo.thread;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicTest {

    private static class AtomClass {

        public static AtomicInteger num = new AtomicInteger(0);

        public synchronized void addNum() { //如果没有加synchronized，那么无法保证输出结果从100递增到500
            //但是如果加了synchronized的话，那么就可以不需要AtomicInteger
            //虽然AtomicInteger保证了递增操作的原子性和可见性(volatile修饰)，但是无法保证操作的顺序性
            System.out.println(Thread.currentThread().getName() + "加了100之后的结果：" +
                            num.addAndGet(100));
        }
    }

    private static class AtomicThread extends Thread{

        private AtomClass atomClass;

        public AtomicThread(AtomClass atomClass) {
            this.atomClass = atomClass;
        }

        @Override
        public void run() {
            atomClass.addNum();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AtomClass atomClass = new AtomClass();
        for (int i = 0; i < 5; i++) {
            Thread thread = new AtomicThread(atomClass);
            thread.start();
        }
        Thread.sleep(1000);

    }
}
