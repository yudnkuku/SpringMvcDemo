package demo;

import org.junit.Test;

public class ClassLoaderTest {

    public static void main(String[] args) {
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
        System.out.println(System.getProperty("java.ext.dirs"));
        System.out.println(sysClassLoader);
        System.out.println(sysClassLoader.getParent());
        System.out.println(sysClassLoader.getParent().getParent());
    }

}
