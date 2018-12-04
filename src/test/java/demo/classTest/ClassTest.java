package demo.classTest;

import org.junit.Test;

import java.io.InputStream;

public class ClassTest {

    @Test
    public void test() {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("demo/test.properties");
        System.out.println(is.toString());
        InputStream inputStream = ClassTest.class.getResourceAsStream("/demo/1.properties");
        System.out.println(inputStream.toString());
    }
}
