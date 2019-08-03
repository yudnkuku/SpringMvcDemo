package leetcode;

import org.junit.Test;

import java.util.*;

public class Array {

    /**
     * 最长连续数列
     * 思路：利用HashSet，遍历HashSet，删除某元素前后元素
     * @param num
     * @return
     */
    public int longestConsecutive(int[] num) {
        if (null == num || num.length == 0) {
            return 0;
        }
        //建立HashSet
        HashSet<Integer> hashSet = new HashSet<>();
        for (int i : num) {
            hashSet.add(i);
        }
        int result = 1;
        //遍历HashSet，删除连续元素，求连续数组最大长度
        while (!hashSet.isEmpty()) {
            Iterator iterator = hashSet.iterator();
            int item = (int) iterator.next();
            hashSet.remove(item);
            int len = 1;
            int i = item - 1;
            while (hashSet.contains(i)) {
                hashSet.remove(i--);
                len++;
            }
            i = item + 1;
            while (hashSet.contains(i)) {
                hashSet.remove(i++);
                len++;
            }
            if (len > result) {
                result = len;
            }
        }
        return result;
    }

    /**
     * 给定一个排序数组，在原地删除重复出现的元素，使得每个元素只出现一次，返回删除后数组的长度
     * 思路：遍历数组，使用两个指针，pre/cur，当两者相同时，cur++，不同时使num[++pre] = num[cur++]
     * @param nums
     * @return
     */
    public int removeDuplicate(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }
        int cur = 0, pre = 0;
        while (cur < nums.length) {
            if (nums[cur] == nums[pre]) {
                cur++;
            } else {
                nums[++pre] = nums[cur++];
            }
        }
        return pre+1;
    }

    /**
     * 数组组成的整数加1
     * @param digits
     * @return
     */
    public int[] plusOne(int[] digits) {
        int carried = 1;
        int i = digits.length-1;
        while (i >= 0) {
            if (digits[i] + carried > 9) {
                digits[i] = 0;
                carried = 1;
            } else {
                digits[i] = digits[i] + carried;
                carried = 0;
                break;
            }
            i--;
        }
        if (carried == 1) {
            int[] result = new int[digits.length+1];
            result[0] = 1;
            for (int j = 0; j < digits.length; j++) {
                result[j+1] = digits[j];
            }
            return result;
        } else {
            return digits;
        }
    }

    /**
     * 对颜色数组进行排序，假设数组A中指包含0,1,2三个数分别代表红、白、蓝三色，现在将其排序为红、白、蓝
     * 思路：设定两个索引，zero和two，假设[0,zero]全部是0，[two,n-1]之间全是2，(zero,two)之间全是1，那么遍历数组，如果A[i]==2，
     *      交换A[i]和A[--two]，如果A[i]==1，i++，如果A[i]==0，交换A[i++]和A[++zero]，这里有个默认的设定：0->zero=0
     *      zero+1->i-1=1 two->n-1=2
     * @param A
     * @return
     */
    public int[] sortColors(int[] A) {
        int zero = -1;
        int two = A.length;
        for (int i = 0; i < two; ) {
            if (A[i] == 1) {
                i++;
            } else if (A[i] == 2) {
                two--;
                swap(A, i, two);
            } else if (A[i] == 0) {
                zero++;
                swap(A, i, zero);
                i++;
            }
        }
        return A;
    }

    private void swap(int[] A, int m, int n) {
        int tmp = A[m];
        A[m] = A[n];
        A[n] = tmp;
    }

    /**
     * 给定整数s，求数组中连续元素和>=s的最小数组长度
     * 思路：使用左右l/r指针，右移r指针直到数组元素和>=s，记录数组长度，然后左移l指针，sum-num[l]，重复上述步骤
     * @param s
     * @param num
     * @return
     */
    public int minSubArrayLen(int s, int[] num) {
        int l = 0;
        int r = 0;
        int sum = num[0];
        int result = num.length+1;
        while (l < num.length) {
            if (r < num.length-1 && sum < s) {
                r++;
                sum += num[r];
            } else {    //r == num.length-1 || sum >= s
                sum -= num[l];
                l++;
            }
            if (sum >= s) {
                result = (r-l+1) < result ? r-l+1 : result;
            }
        }
        if (result == num.length+1) {
            return 0;
        }
        return result;
    }

    /**
     * 合并两个有序数组
     * @param a
     * @param m
     * @param b
     * @param n
     * @return
     */
    public int[] mergeTwoSortedArray(int[] a, int m, int[] b, int n) {
        int[] c = new int[m+n];
        int j = 0;
        int k = 0;
        for (int i = 0; i < m + n; i++) {
            if (j == m) {
                c[i] = b[k++];
            } else if (k == n) {
                c[i] = a[j++];
            }
            if (j < m && k < n) {
                if (a[j] <= b[k]) {
                    c[i] = a[j];
                    j++;
                } else {
                    c[i] = b[k];
                    k++;
                }
            }
        }
        return c;
    }

    /**
     * 合并两个有序数组，将所有数存储在A中
     * @param A
     * @param m
     * @param B
     * @param n
     */
    public void merge(int[] A, int m, int[] B, int n) {
        int[] temp = new int[m+n];
        int i = 0;
        int j = 0;
        int k = 0;
        while (i < m && j < n) {
            if (A[i] <= B[j]) {
                temp[k++] = A[i++];
            } else {
                temp[k++] = B[j++];
            }
        }
        while (i < m) {
            temp[k++] = A[i++];
        }
        while (j < n) {
            temp[k++] = B[j++];
        }
        i = k = 0;
        while (k < m+n) {
            A[i++] = temp[k++];
        }
    }

    /**
     * 给定排序数组，删除重复元素，重复元素最多允许出现2次，返回删除后数组的长度，例如：[1,2,2,2,3,3] 返回 [1,2,2,3,3] 5
     * @param A
     * @return
     */
    public int removeDuplicates(int[] A) {
        if (A == null) {
            return -1;
        }
        int i = 0;
        int result = A.length;
        while (i < A.length-1) {
            if (A[i] != A[i+1]) {
                i++;
            } else {
                int t = A[i];
                int begin = i;
                while (i < A.length && A[i] == t) {
                    i++;
                }
                //A[i] != t
                int end = i-1;
                result -= (end-begin-1);
            }
        }
        return result;
    }

    public class Interval {

        int start;

        int end;

        public Interval() {
            start = 0;
            end = 0;
        }

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }

    }

    /**
     * 将区间列表合并，例如给定区间列表：[1,3],[2,6],[7,9]，合并之后返回:[1,6],[7,9]
     * @param intervals
     * @return
     */
    public ArrayList<Interval> merge(ArrayList<Interval> intervals) {
        if (intervals == null || intervals.isEmpty() || intervals.size() == 1) {
            return intervals;
        }
        ArrayList<Interval> result = new ArrayList<>();
        Interval temp = intervals.get(0);
        Iterator<Interval> iterator = intervals.iterator();
        while (iterator.hasNext()) {
            Interval i = iterator.next();
            if (i.start <= temp.end && i.end >= temp.start) {
                temp = new Interval(Math.min(i.start, temp.start), Math.max(i.end, temp.end));
            } else {
                if (temp.end < i.end) {
                    result.add(temp);
                    temp = i;
                } else {
                    result.add(i);
                }
            }
        }
        result.add(temp);
        return result;
    }

    /**
     * 从数组中找出三个数之和最接近sum，例如[-1,2,1,4] sum=1，最接近：[-1,1,2]=2
     * @param num
     * @param sum
     * @return
     */
    public int threeSumClosest(int[] num, int sum) {
        if (num == null) {
            return 0;
        }
        int result = 0;
        if (num.length <= 3) {
            for (int i : num) {
                result += i;
            }
            return result;
        }
        Arrays.sort(num);
        result = num[0] + num[1] + num[2];
        for (int i = 0; i < num.length-2; i++) {
            int left = i+1;
            int right = num.length-1;
            while (left < right) {
                int curSum = num[i] + num[left] + num[right];
                if (curSum == sum) {
                    return sum;
                } else if (curSum < sum) {
                    left++;
                } else {
                    right--;
                }
                result = Math.abs(curSum-sum) < Math.abs(result-sum) ? curSum : result;
            }
        }
        return result;
    }

    @Test
    public void test() {
        int[] num = {-1,1,2,3,-2};
        System.out.println(threeSumClosest(num, 1));
    }

}
