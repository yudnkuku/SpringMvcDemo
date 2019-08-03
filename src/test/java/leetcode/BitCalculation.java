package leetcode;

import org.junit.Test;

public class BitCalculation {

    /**
     * 判断整数中二进制中1的个数
     * 思路：n&(n-1)可以消除末尾的1
     * @param num
     * @return
     */
    public int howManyOne(int num) {
        int count = 0;
        while (num > 0) {
            count++;
            num = num & (num-1);
        }
        return count;
    }

    /**
     * 给定边界[m,n]，求区间内所有数相与的结果
     * 思路：找到每个数字二进制左边公共部分，可以构造一个32位全1的掩码，如果m&mask != n&mask，mask左移一位直到相等，返回m&mask
     * @param m
     * @param n
     * @return
     */
    public int rangeAndCalc(int m, int n) {
        int mask = Integer.MAX_VALUE;
        while ((m & mask) != (n & mask)) {
            mask <<= 1;
        }
        return m & mask;
    }

    @Test
    public void test() {
        System.out.println(rangeAndCalc(26, 30));
    }
}
