package leetcode;

import org.junit.Test;

public class SingleNum {

    /**
     * 输入数组中全是成对数，只有一个是不同的，找出不同的数
     * 思路：异或，0^n=n, n^n=0
     * @param arr
     * @return
     */
    public int findSingle(int[] arr) {
        int result = 0;
        for (int i : arr) {
            result ^= i;
        }
        return result;
    }

    /**
     * 输入数组中相同的数三个出现，只有一个不同的，找出不同的数
     * 思路：将数组中所有元素转换成二进制求和，对3求余
     * @param arr
     * @return
     */
    public int findSingle1(int[] arr) {
        //bit存储数组所有数字加和的二进制
        int[] bit = new int[32];
        int result = 0;
        for (int i = 0; i < bit.length; i++) {
            for (int j = 0; j < arr.length; j++) {
                bit[i] += ((arr[j] >> i) & 1);
            }
            result |= ((bit[i] % 3) << i);
        }
        return result;
    }

    @Test
    public void test() {
        int[] arr = new int[] {1,3,1,3,2};
        System.out.println(findSingle(arr));
    }
}
