package leetcode;

import org.junit.Test;

public class SortArray {

    /**
     * 插入排序
     * @param arr
     * @return
     */
    public int[] insertionSort(int[] arr) {
        for (int scan = 1; scan < arr.length; scan++) {
            int tmp = arr[scan];
            int position = scan - 1;
            while (position >= 0 && arr[position] > tmp) {
                arr[position+1] = arr[position];
                position--;
            }
            arr[position+1] = tmp;
        }
        return arr;
    }

    @Test
    public void test() {
        int[] arr = new int[] {1,5,3,7,6,2};
        for (int i : insertionSort(arr)) {
            System.out.println(i);
        }
    }
}
