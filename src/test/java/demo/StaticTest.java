package demo;

public class StaticTest extends SuperStaticTest{

//    private static int a = method();
    static int i = 2;

    static {
        System.out.println("Static block");
        System.out.println("i=" + i);
        i = 3;
        j = 4;
    }

    static int j = 3;

    private static int method() {
        System.out.println("Static Method");
        return 1;
    }

    public StaticTest() {
        System.out.println("Static Constructor");
    }

    public static void main(String[] args) {
        new StaticTest();
        new StaticTest();
    }

}
