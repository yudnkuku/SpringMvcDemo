package demo.thread;

public class ThreadTest {

    public static void main(String[] args) {
        Thread syncTh1 = new Thread(new SyncThread(),"syncTh1");
        Thread syncTh2 = new Thread(new SyncThread(),"syncTh2");
        syncTh1.start();
        syncTh2.start();
    }
}
