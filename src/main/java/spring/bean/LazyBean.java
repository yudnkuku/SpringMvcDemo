package spring.bean;

public class LazyBean {

    public void init() {
        System.out.println("spring.bean.LazyBean initial,after all properties set");
    }
}
