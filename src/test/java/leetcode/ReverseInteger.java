package leetcode;

import org.junit.Test;

public class ReverseInteger {

    public int reverse(int i) {
        String temp = "";
        int result = 0;
        String str = String.valueOf(i);
        StringBuilder sb = new StringBuilder();
        for (int k = str.length()-1; k >= 0; k--) {
            if (str.charAt(k) != '-') {
                sb.append(str.charAt(k));
            }
        }
        temp = sb.toString();
        result = Integer.valueOf(temp);
        if (temp.length() != str.length()) {
            result = Integer.valueOf("-" + temp);
        }
        return result;
    }

    @Test
    public void test() {
        System.out.println(reverse(100));
        System.out.println(reverse(-100));
    }
}
