package demo.conpro;

public class Test {

    public static void main(String[] args) {
        Storage storage = new Storage();

        //生产者对象
        Producer p1 = new Producer(storage,"thread_p1");
        Producer p2 = new Producer(storage,"thread_p2");
        Producer p3 = new Producer(storage,"thread_p3");
        Producer p4 = new Producer(storage,"thread_p4");
        Producer p5 = new Producer(storage,"thread_p5");
        Producer p6 = new Producer(storage,"thread_p6");
        Producer p7 = new Producer(storage,"thread_p7");

        p1.setNum(10);
        p2.setNum(10);
        p3.setNum(10);
        p4.setNum(10);
        p5.setNum(10);
        p6.setNum(10);
        p7.setNum(80);

        //消费者对象
        Consumer c1 = new Consumer(storage,"thread_c1");
        Consumer c2 = new Consumer(storage,"thread_c2");
        Consumer c3 = new Consumer(storage,"thread_c3");

        c1.setNum(50);
        c2.setNum(20);
        c3.setNum(30);

        c1.start();
        c2.start();
        c3.start();
        p1.start();
        p2.start();
        p3.start();
        p4.start();
        p5.start();
        p6.start();
        p7.start();
    }
}
