package demo.thread;

import java.util.concurrent.Semaphore;

/**
 * jdk官方给的Semaphore示例
 */
public class Pool {

    private static final int MAX_AVAILABLE = 100;

    private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

    protected Object[] items = new Object[MAX_AVAILABLE];

    protected boolean[] used = new boolean[MAX_AVAILABLE];

    /**
     * 从Pool中获取
     * @return
     * @throws InterruptedException
     */
    public Object getItem() throws InterruptedException {
        //拿到许可
        available.acquire();
        return getNextAvaliableItem();
    }

    /**
     * 加同步处理，获取下一个可用对象
     * @return
     */
    protected synchronized Object getNextAvaliableItem() {
        for (int i = 0; i < MAX_AVAILABLE; i++) {
            if (!used[i]) {
                used[i] = true;
                return items[i];
            }
        }
        return null;
    }

    /**
     * 将对象归还Pool
     * @param item
     */
    public void putItem(Object item) {
        if (markAsUnused(item)) {
            available.release();
        }
    }

    /**
     * 是否成功归还对象，加同步处理
     * @param item
     * @return
     */
    protected synchronized boolean markAsUnused(Object item) {
        for (int i = 0; i < MAX_AVAILABLE; i++) {
            if (items[i] == item) {
                if (used[i]) {
                    used[i] = false;
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
