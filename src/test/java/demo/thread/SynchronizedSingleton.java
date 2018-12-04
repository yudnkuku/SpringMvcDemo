package demo.thread;

/**
 * 通过synchronized关键字实现多线程环境下的单例
 * 如果getInstance()方法被频繁调用会造成严重的开销问题
 */
public class SynchronizedSingleton {

    private static SynchronizedSingleton instance = null;

    public SynchronizedSingleton() {
    }

    public synchronized static SynchronizedSingleton getInstance() {
        if (instance == null) {
            instance = new SynchronizedSingleton();
        }
        return instance;
    }
}
