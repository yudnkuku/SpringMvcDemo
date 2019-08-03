package leetcode;

import org.junit.Test;

public class SearchCode {

    /**
     * 假如某个排序数组分区了（快速排序中的分区函数），在分区数组中查找是否存在n，返回n的索引
     * 思路：
     * @param num
     * @param target
     * @return
     */
    public int searchInRotateArray(int[] num, int target) {
        if (num == null | num.length == 0) {
            return -1;
        }
        int left = 0;
        int right = num.length-1;
        while (left <= right) {
            int mid = (left+right)/2;
            if (num[mid] == target) {
                return mid;
            } else if (num[left] <= num[mid]) {  //左半数组升序，右半数组rotate
                if (num[left] <= target && target < num[mid]) {  //在左半数组查找
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {    //右半数组升序，左半数组rotate
                if (num[mid] < target && target <= num[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return -1;
    }

    /**
     * 找到rotate数组的拐点，然后使用二分法查找target
     * @param num
     * @param target
     * @return
     */
    public int searchInRotateArray1(int[] num, int target) {
        if (num == null || num.length == 0) {
            return -1;
        }
        int left = 0;
        int right = num.length-1;
        while (left < right) {
            if (num[left] < num[right]) {   //升序数组，left即为最小
                break;
            }
            int mid = (left+right)/2;
            if (num[mid] > num[right]) {    //右半为rotate数组，且mid不是最小
                left = mid + 1;
            } else {    //左半为rotate数组
                right = mid;
            }
        }
        int offset = left;  //拐点
        left = 0;
        right = num.length-1;
        while (left <= right) {
            int mid = (left+right)/2;
            int realMid = (mid+offset) % num.length;
            if (num[realMid] == target) {
                return realMid;
            } else if (num[realMid] < target) {
                left = realMid+1;
            } else {
                right = realMid-1;
            }
        }
        return -1;
    }

    /**
     * 在rotate数组中找出最小元素
     * 思路：还是使用二分查找，如果num[left] < num[right]，那么数组肯定是严格升序排序的；如果num[middle] > num[right]，
     *      那么右半数组是rotate数组，且最小元素肯定在右半数组中(mid+1 -> right)；否则说明左半数组是rotate数组，右半数组是升序数组，
     *      最小元素在左半数组中
     * @param num
     * @return
     */
    public int findMin(int[] num) {
        if (num == null || num.length == 0) {
            return -1;
        }
        int left = 0;
        int right = num.length-1;
        while (left < right) {
            if (num[left] < num[right]) {
                return left;
            }
            int mid = (left+right)/2;
            if (num[mid] > num[right]) {    //右半rotate，左半升序，且mid不可能是最小元素，所以left = mid+1，不包含mid
                left = mid + 1;
            } else {    //右半升序，左半rotate，mid有可能是最小元素，所以right = mid，包含mid
                right = mid;
            }
        }
        return left;
    }

    /**
     * 给定一个排序数组和数值target，如果数组中同样的值，返回该值的索引，否则返回该值的插入位置
     * @param num
     * @param target
     * @return
     */
    public int searchInsert(int[] num, int target) {
        if (num == null) {
            return -1;
        }
        int left = 0;
        int right = num.length-1;
        while (left < right) {
            int mid = (left+right)/2;
            if (num[mid] == target) {
                return mid;
            } else if (num[mid] < target) {
                left = mid+1;
            } else {
                right = mid-1;
            }
        }
        //not found left == right
        if (num[left] == target) {
            return left;
        } else if (num[left] < target) {
            return left+1;
        } else {
            return left;
        }
    }

    /**
     * 给定一个数组和目标值target，找出数组中所有target的索引区间，要求时间复杂度是O(logN)
     * 例如：[1,2,3,4,4,4,5,5,6]  target=4 => [3,5]
     * @param num
     * @param target
     * @return
     */
    public int[] searchRange(int[] num, int target) {
        if (num == null) {
            return NOT_FOUND;
        }
        return searchInRange(num, target, 0, num.length-1);
    }

    public static final int[] NOT_FOUND = {-1, -1};

    private int[] searchInRange(int[] num, int target, int min, int max) {
        if (min > max) {
            return NOT_FOUND;
        }
        if (min == max) {
            if (num[min] == target) {
                return new int[]{min, max};
            } else {
                return NOT_FOUND;
            }
        }
        int mid = (min+max)/2;
        if (num[mid] == target) {
            int[] left = searchInRange(num, target, min, mid-1);
            int[] right = searchInRange(num, target, mid+1, max);
            int l = found(left) ? left[0] : mid;
            int r = found(right) ? right[1] : mid;
            return new int[]{l, r};
        } else if (num[mid] < target) {
            return searchInRange(num, target, mid+1, max);
        } else {
            return searchInRange(num, target, min, mid-1);
        }
    }

    private boolean found(int[] range) {
        return range[0] != -1 && range[1] != -1;
    }

    @Test
    public void test() {
        int[] num = {1,2,3,4,5,7};
//        System.out.println(findMin(num));
//        System.out.println(searchInRotateArray(num, 1));
        System.out.println(searchInsert(num, 6));
    }
}
