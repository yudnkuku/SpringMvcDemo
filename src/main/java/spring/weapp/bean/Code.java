package spring.weapp.bean;

public enum Code {

    SUCCESS(0), FAIL(1);

    private int state;

    Code(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
