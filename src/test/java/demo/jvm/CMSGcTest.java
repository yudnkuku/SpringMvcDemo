package demo.jvm;

import java.util.concurrent.TimeUnit;

/**
 * 测试ygc/cmsgc
 * 虚拟机参数：-Xmx20m -Xms20m -Xmn10m -XX:PretenureSizeThreshold=2M -XX:+UseConcMarkSweepGC
 *           -XX:+UseParNewGC -XX:CMSInitiatingOccupancyFration=60 -XX:+UseCMSInitiatingOccupancyOnly
 */
public class CMSGcTest {

    private static final int _1M = 1 * 1024 * 1024;

    private static final int _2M = 2 * 1024 * 1024;

    public static void main(String[] args) {
        ygc(1);
        cmsgc(1);
    }

    private static void ygc(int n) {
        for (int i = 0; i < n; i++) {
            //Eden区设置的8M，因此每分配8M内存就触发依次ygc
            for (int j = 0; j < 8; j++) {
                byte[] tmp = new byte[_1M];
            }
        }
    }

    private static void cmsgc(int n) {
        for (int i = 0; i < n; i++) {
            //设置了CMSInitiatingOccupancyThreshold=60，老年代使用率达到60%时会触发CMS gc
            for (int j = 0; j < 3; j++) {
                //设置了PretenureSizeThreshold=2M，对象大于2M会被视为大对象直接进入老年代
                byte[] tmp= new byte[_2M];
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
