package demo.dynamicProxy;

public class SubjectImpl implements Subject {
    @Override
    public void add() {
        System.out.println("invoke method add()");
    }
}
