package spring.bean;

import org.springframework.beans.factory.InitializingBean;

public class PreInstantiationBean implements InitializingBean {

    private int id;

    private String name;

    public PreInstantiationBean() {
        System.out.println("First Step======PreInstantiationBean Constructor");
    }

    public void init() {
        System.out.println("Third Step======spring.bean.PreInstantiationBean initial");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void afterPropertiesSet() throws Exception {
        System.out.println("Initializing Callback======afterPropertiesSet()");
    }
}
