package leetcode;

import org.junit.Test;

public class TwoPointerCode {

    /**
     * 给定一个升序数组，找出数组中两个数相加等于给定数的组合索引
     * 思路：从数组左右同时遍历，由于数组已经排序，那么当左右之和小于sum时，r--，当左右之和大于sum时，l++
     * @param num
     * @return
     */
    public int[] twoSum(int[] num, int sum) {
        if (num == null || num.length < 2) {
            return null;
        }
        int[] result = new int[2];
        int l = 0;
        int r = num.length-1;
        while (l < r) {
            int temp = num[l] + num[r];
            if (sum == temp) {
                result[0] = l;
                result[1] = r;
                break;
            } else if (sum > temp) {
                r--;
            } else {
                l++;
            }
        }
        return result;
    }

    @Test
    public void test() {
        int[] result = twoSum(new int[]{1,2,3,4}, 5);
        for (int i : result) {
            System.out.println(i);
        }
    }
}
