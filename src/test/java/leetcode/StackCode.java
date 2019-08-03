package leetcode;

import org.junit.Test;

import java.util.Stack;

public class StackCode {

    /**
     * 逆波兰表达式求值
     * 思路：遍历数组，如果是非操作符，将其压入栈中，如果是操作符，那么取栈顶前两位元素进行运算，将结果压入栈中，
     *      遍历完整个数组，栈顶元素即为最终答案
     * @param s
     * @return
     */
    public int polandExpression(String[] s) {
        if (s == null || s.length == 0) {
            return -1;
        }
        int result = 0;
        Stack<String> stack = new Stack<>();
        for (int i = 0; i < s.length; i++) {
            String t = s[i];
            if (!t.matches("[=*/-]")) {
                stack.push(t);
            } else {
                int m = Integer.valueOf(stack.pop());
                int n = Integer.valueOf(stack.pop());
                int temp = 0;
                if (t == "+") {
                    temp = m + n;
                } else if (t == "-") {
                    temp = n - m;
                } else if (t == "*") {
                    temp = m * n;
                } else {
                    temp = n / m;
                }
                stack.push(String.valueOf(temp));
            }
        }
        result = Integer.valueOf(stack.peek());
        return result;
    }

    @Test
    public void test() {
    }
}
