package demo.conpro;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Storage {

    //仓库最大存储量
    private static final int MAX_SIZE = 100;

    //仓库存储的载体
    private LinkedList<Object> list = new LinkedList<>();

    //锁
    private final Lock lock = new ReentrantLock();

    //仓库满condition
    private final Condition full = lock.newCondition();

    //仓库空condition
    private final Condition empty = lock.newCondition();

    //生产n个产品
//    public void produce(int n) {
//        synchronized (list) {
//            String tName = Thread.currentThread().getName();
//            System.out.println(tName + "======拿到对象锁");
//            //如果仓库容量不足，线程挂起
//            //这里用while判断条件，不能用if，因为线程被挂起恢复执行后需要重新判断条件
//            while (list.size() + n > MAX_SIZE) {
//                System.out.println(tName + "======要生产的产品数量：" + n + "\t目前库存量：" +
//                                    list.size() + "\t超过最大库存限制无法生产操作");
//                try {
//                    list.wait();
//                    System.out.println(tName + "======阻塞后执行代码");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            for (int i = 0; i < n; i++) {
//                list.add(new Object());
//            }
//            System.out.println(tName + "======已经生产产品数：" + n + "\t目前库存量：" + list.size());
//            //恢复其他线程
//            list.notifyAll();
//            System.out.println(tName + "======释放锁");
//        }
//    }

//    生产n个产品
    public void produce(int n) {
        String tName = Thread.currentThread().getName();
        //获取锁
        lock.lock();
        System.out.println(tName + "======获取对象锁");
        try {
            while(list.size() + n > MAX_SIZE) {
                System.out.println(tName + "======要生产的产品数量：" + n + "\t目前库存量：" +
                        list.size() + "\t超过最大库存限制无法生产操作");
                full.await();
                System.out.println(tName + "======阻塞后执行代码");
            }
            for (int i = 0; i < n; i++) {
                list.add(new Object());
            }
            System.out.println(tName + "======已经生产产品数：" + n + "\t目前库存量：" + list.size());
            //恢复其他线程
            full.signalAll();
            empty.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            System.out.println(tName + "======释放锁");
        }
    }
    //消费n个产品
//    public void consume(int n) {
//        synchronized(list) {
//            String tName = Thread.currentThread().getName();
//            System.out.println(tName + "======拿到对象锁");
//            while (list.size() < n) {
//                System.out.println(tName + "======要消费的产品数量：" + n + "\t目前库存数量：" +
//                                    list.size() + "\t消耗大于库存无法完成消费操作");
//                try {
//                    list.wait();
//                    System.out.println(tName + "======阻塞后执行代码");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            for (int i = 0; i < n; i++) {
//                list.remove();
//            }
//            System.out.println(tName + "======已经消费产品数：" + n + "\t目前库存量：" + list.size());
//            //恢复其它线程
//            list.notifyAll();
//            System.out.println(tName + "======释放锁");
//        }
//    }

//    消费n个产品
    public void consume(int n) {
        String tName = Thread.currentThread().getName();
        //获取锁
        lock.lock();
        System.out.println(tName + "======获取对象锁");
        try {
            while(list.size() < n) {
                System.out.println(tName + "======要消费的产品数量：" + n + "\t目前库存数量：" +
                                    list.size() + "\t消耗大于库存无法完成消费操作");
                empty.await();
                System.out.println(tName + "======阻塞后执行代码");
            }
            for (int i = 0; i < n; i++) {
                list.remove();
            }
            System.out.println(tName + "======已经消费产品数：" + n + "\t目前库存量：" + list.size());
            //恢复其他线程
            full.signalAll();
            empty.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //在finally块中释放锁
            lock.unlock();
            System.out.println(tName + "======释放锁");
        }
    }

    public LinkedList<Object> getList() {
        return list;
    }

    public void setList(LinkedList<Object> list) {
        this.list = list;
    }

}
