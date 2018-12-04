package demo;

public class VolatileTest {

    private volatile int inc = 0;

    public void increase() {
        inc++;
    }

    public static void main(String[] args) {
        final VolatileTest test = new VolatileTest();
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 10000; j++) {
                        test.increase();
                    }
                }
            }).start();
        }

        while(Thread.activeCount() > 1) {
            Thread.yield();
        }
        System.out.println(test.inc);
    }
}
