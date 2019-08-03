package leetcode;

import org.junit.Test;

public class SortCode {

    /**
     * 快排
     * @param num
     * @param min
     * @param max
     */
    public void quickSort(int[] num, int min, int max) {
        if (null == num || num.length == 0 || num.length == 1) {
            return;
        }
        if (min > max) {
            return;
        }
        int indexOfPartition = partition(num, min, max);
        quickSort(num, min, indexOfPartition-1);
        quickSort(num, indexOfPartition+1, max);
    }

    /**
     * 快排的分区函数，返回分区索引，小于分区索引元素在左边，大于分区索引的在右边
     * @param num
     * @param min
     * @param max
     * @return
     */
    private int partition(int[] num, int min, int max) {
        int left = min;
        int right = max;
        int middle = (left+right)/2;
        int partitionEle = num[middle];
        //暂时交换min middle
        swap(num, min, middle);
        //使用双指针遍历数组，将小于middle的放在数组左边，大于middle的放在数组右边
        while (left < right) {
            while (left < right && num[left] <= partitionEle) {
                left++;
            }
            while (num[right] > partitionEle) {
                right--;
            }
            if (left < right) {
                swap(num, left, right);
            }
        }
        //right < middle && right+1 > middle
        swap(num, min, right);
        return right;
    }

    private void swap(int[] num, int m, int n) {
        int temp = num[m];
        num[m] = num[n];
        num[n] = temp;
    }

    public void mergeSort(int[] arr) {
        int[] temp = new int[arr.length];
        mergeSort(arr, 0, arr.length-1, temp);
    }

    /**
     * 归并排序
     * @param arr
     * @param left
     * @param right
     * @param temp
     */
    private void mergeSort(int[] arr, int left, int right, int[] temp) {
        if (left < right) {
            int mid = (left+right)/2;
            mergeSort(arr, left, mid, temp);
            mergeSort(arr, mid+1, right, temp);
            merge(arr, left, mid, right, temp);
        }
    }

    /**
     * 合并两个有序数组，left->mid mid+1->right
     * @param arr
     * @param left
     * @param mid
     * @param right
     * @param temp
     */
    private void merge(int[] arr, int left, int mid, int right, int[] temp) {
        int i = left;
        int j = mid+1;
        int k = 0;
        while (i <= mid && j <= right) {
            if (arr[i] <= arr[j]) {
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }
        while (i <= mid) {
            temp[k++] = arr[i++];
        }
        while (j <= right) {
            temp[k++] = arr[j++];
        }
        //将temp中的数拷贝到arr中
        k = 0;
        while (left <= right) {
            arr[left++] = temp[k++];
        }
    }

    @Test
    public void test() {
        int[] num = {2,3,6,5,4};
        mergeSort(num);
        for (int i : num) {
            System.out.println(i);
        }
    }
}
