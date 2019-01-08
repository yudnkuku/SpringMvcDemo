package demo.jvm;

public class StackOverflowTest {

    private int stackLength = 1;

    public void stackLeak() {
        stackLength++;
        stackLeak();
    }

    public int getStackLength() {
        return stackLength;
    }
}
