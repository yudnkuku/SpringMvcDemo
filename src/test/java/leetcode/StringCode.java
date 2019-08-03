package leetcode;

import org.junit.Test;

import java.util.HashSet;
import java.util.Stack;

public class StringCode {

    /**
     * 字符串中最长连续不含相同字符的子串
     * 思路：使用滑动窗口和一个HashSet，滑动窗口有左右两个索引l/r，如果HashSet不包含r元素，添加r元素，r++，否则从HashSet中移除l元素，l++
     * @param str
     * @return
     */
    public int longestDiffSubString(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        int result = 0;
        int l = 0;
        int r = 0;
        HashSet<Character> hashSet = new HashSet<>();
        while (l <= r && r < str.length()) {
            if (!hashSet.contains(str.charAt(r))) {
                hashSet.add(str.charAt(r));
                result = Math.max(result, r-l+1);
                r++;
            } else {
                hashSet.remove(str.charAt(l));
                l++;
            }
        }
        return result;
    }

    /**
     * 给定字符串只包括：{、[、(、)、]、}，判断字符串是否有效，左括号和右括号一一对应
     * 思路：使用栈，如果是左边括号将其压入堆栈，否则判断栈顶元素和当前元素是否成对，如果成对弹出栈顶继续下一轮循环，否则返回false
     * @param str
     * @return
     */
    public boolean verifyString(String str) {
        Stack<Character> stack = new Stack<>();
        char c = ' ';
        for (int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            if (c == '(' || c == '[' || c== '{') {
                stack.push(c);
            } else {
                if (stack.isEmpty()) {
                    return false;
                }
                char top = stack.peek();
                if (top == '(') {
                    if (c != ')') {
                        return false;
                    }
                } else if (top == '[') {
                    if (c != ']') {
                        return false;
                    }
                } else if (top == '{') {
                    if (c != '}') {
                        return false;
                    }
                }
                stack.pop();
            }
        }
        return stack.isEmpty();
    }

    /**
     * 判断字符串是否是回文字符串
     * @param s
     * @return
     */
    public boolean palindromeString(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int l = 0;
        int r = s.length()-1;
        while (l < r) {
            if (!Character.isLetterOrDigit(s.charAt(l))) {
                l++;
            }
            if (!Character.isLetterOrDigit(s.charAt(r))) {
                r--;
            }
            if (s.charAt(l) != s.charAt(r)) {
                return false;
            } else {
                l++;
                r--;
            }
        }
        return true;
    }

    /**
     * 判断一个整数是否是回文整数
     * @param n
     * @return
     */
    public boolean isPalindrome(int n) {
        if (n < 0) {
            return false;
        }
        String s = String.valueOf(n);
        int l = 0;
        int r = s.length()-1;
        while (l < r) {
            if (s.charAt(l) != s.charAt(r)) {
                return false;
            }
            l++;
            r--;
        }
        return true;
    }

    /**
     * 罗马字符串转换为整数
     * @param roman
     * @return
     */
    public int romanToInt(String roman) {
        if (roman == null || roman.length() == 0) {
            return 0;
        }
        int len = roman.length();
        int sum = value(roman.charAt(len-1));
        for (int i = len-1, j = i-1; j >= 0; i--, j--) {
            int number = value(roman.charAt(i));
            int preNumber = value(roman.charAt(j));
            if (number <= preNumber) {
                sum += preNumber;
            } else {
                sum -= preNumber;
            }
        }
        return sum;
    }

    private int value(char a) {
        if (a == 'I') return 1;
        else if (a == 'V') return 5;
        else if (a == 'X') return 10;
        else if (a == 'L') return 50;
        else if (a == 'C') return 100;
        else if (a == 'D') return 500;
        else if (a == 'M') return 1000;
        else return 0;
    }

    @Test
    public void test() {
        System.out.println(romanToInt("IV"));
    }
}
