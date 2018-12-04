package demo.thread;

/**
 *完整的双重检查单例模式
 */
public class DoubleCheckedSingleton {

    //必须使用volatile关键字
    private volatile static DoubleCheckedSingleton instance = null;

    public DoubleCheckedSingleton() {
    }

    public static DoubleCheckedSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedSingleton();
                }
            }
        }
        return instance;
    }
}
