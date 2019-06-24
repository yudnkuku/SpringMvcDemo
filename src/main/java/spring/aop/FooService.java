package spring.aop;

import java.util.Date;

public class FooService {

    public String printCurrentTime() {
        Date date = new Date();
        System.out.println(date.toString());
        return date.toString();
    }

    public String print(String arg) {
        System.out.println("arg:" + arg);
        return arg;
    }
}
