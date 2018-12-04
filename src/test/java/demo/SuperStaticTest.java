package demo;

public class SuperStaticTest {

    static
    {
        System.out.println("Super Static block");
    }

    public SuperStaticTest() {
        System.out.println("Super Constructor");
    }
}
