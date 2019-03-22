package demo;

/**
 * 执行结果：
 *  2
 *  3
 *  a=110,b=0
 *  1
 *  4
 *
 *  调用main方法会触发StaticTest2的类初始化，类初始化流程：加载、验证、准备、解析、初始化
 *  类初始化过程会收集类定义中所有的静态代码块和类变量赋值语句并按顺序合成类构造器，因此先执行 static StaticTest2 st = new StaticTest2()；
 *  这会触发对象实例的初始化，这里很有意思，对象的实例化被嵌入到了类的初始化过程，对象的实例化会执行代码块、赋值和构造方法
 */
public class StaticTest2 {

    public static void main(String[] args) {
        staticFuntion();
    }

    //初始化实例对象，依次调用(代码块、赋值)和构造方法
    //可以改变这条语句的位置得到不同的结果
    static StaticTest2 st = new StaticTest2();

    static {
        System.out.println("1");
    }

    {
        System.out.println("2");
    }

    public StaticTest2() {
        System.out.println("3");
        System.out.println("a=" + a + ",b=" + b);
    }

    public static void staticFuntion() {
        System.out.println("4");
    }

    int a = 110;

    static int b = 112;

}
