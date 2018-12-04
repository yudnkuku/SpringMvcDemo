package demo.thread;

public class ThreadLocalTest {

    ThreadLocal<Long> longLocal = new ThreadLocal<>();
    ThreadLocal<String> stringLocal = new ThreadLocal<>();

    public void set() {
        longLocal.set(Thread.currentThread().getId());
        stringLocal.set(Thread.currentThread().getName());
    }

    public Object getLong() {
        return longLocal.get();
    }

    public Object getString() {
        return stringLocal.get();
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadLocalTest test = new ThreadLocalTest();

        System.out.println(test.getLong());
        System.out.println(test.getString());

        test.set();
        System.out.println(test.getLong());
        System.out.println(test.getString());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                test.set();
                System.out.println(test.getLong());
                System.out.println(test.getString());
            }
        });

        thread.start();
        thread.join();

        System.out.println(test.getLong());
        System.out.println(test.getString());
    }

}
