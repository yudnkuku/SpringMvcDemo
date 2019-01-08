package spring.bean;

import java.util.Date;

public class PrototypeBean {

    private long time;

    public PrototypeBean() {
        this.time = new Date().getTime();
    }

    public void printTime() {
        System.out.println("now time is : " + time);
    }
}
