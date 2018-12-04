package demo.conpro;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 具有阻塞功能的缓存区，当缓存区为空时，阻塞读线程，当缓存区满时，阻塞写线程
 */
public class BounderBuffer {

    //锁对象
    private final Lock lock = new ReentrantLock();

    //可写标志
    private final Condition canWrite = lock.newCondition();

    //可读标志
    private final Condition canRead = lock.newCondition();

    //对象数组
    Object[] items = new Object[100];

    private int readIndex,writeIndex,count;

    public void write(Object obj) {
        lock.lock();
        try {
            while(count == items.length) {
                canWrite.await();
            }
            items[writeIndex] = obj;
            if (++writeIndex == items.length) {
                writeIndex = 0;
            }
            ++count;
            canRead.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


    public Object read() {
        Object result = null;
        lock.lock();
        try {
            while (count == 0) {
                canRead.await();
            }
            result = items[readIndex];
            if (++readIndex == items.length) {
                readIndex = 0;
            }
            --count;
            canWrite.signal();
            return result;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return result;
    }
}
