package spring.bean;

public class SingletonBean {

    private PrototypeBean prototypeBean;

    public void setPrototypeBean(PrototypeBean prototypeBean) {
        this.prototypeBean = prototypeBean;
    }

    public void printTime() {
        prototypeBean.printTime();
    }
}
