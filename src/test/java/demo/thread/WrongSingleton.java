package demo.thread;

/**
 * 错误的单例模式
 */
public class WrongSingleton {

    private static WrongSingleton instance = null;

    private WrongSingleton () {}

    public static WrongSingleton getInstance() {
        if (instance == null) {
            instance = new WrongSingleton();
        }
        return instance;
    }
}
