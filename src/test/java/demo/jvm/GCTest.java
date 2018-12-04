package demo.jvm;

import org.junit.Test;

public class GCTest {

    private static final int _1MB = 1024 * 1024;

    @Test
    public void testAllocation() {
        byte[] allocation1, allocation2, allocation3, allocation4;
        allocation1 = new byte[2 * _1MB];
        allocation2 = new byte[2 * _1MB];
        allocation3 = new byte[2 * _1MB];
        allocation4 = new byte[4 * _1MB];
    }

    @Test
    public void testPretenureThreshold() {
        byte[] allocation;
        allocation = new byte[5 * _1MB];
    }

    @Test
    public void testTenuringThreshold() {
        byte[] allocation1, allocation2, allocation3;
        allocation1 = new byte[_1MB / 4];
        allocation2 = new byte[4 * _1MB];
        allocation3 = new byte[4 * _1MB];   //第一次Minor GC
        allocation3 = null;    //第二次Minor GC
        allocation3 = new byte[4 * _1MB];

    }
}
