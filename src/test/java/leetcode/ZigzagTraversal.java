package leetcode;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Stack;

public class ZigzagTraversal {

    /**
     * zigzag遍历，即奇数行从左往右，偶数行从右往左
     * 思路：使用两个堆栈
     * @param root
     * @return
     */
    public ArrayList<ArrayList<Integer>> zigzagTraversal(TreeNode root) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (null == root) {
            return result;
        }
        Stack<TreeNode> left2RightStack = new Stack<>();
        Stack<TreeNode> right2LeftStack = new Stack<>();
        ArrayList<Integer> temp = new ArrayList<>();
        TreeNode top = null;
        left2RightStack.push(root);
        while (!left2RightStack.isEmpty() || !right2LeftStack.isEmpty()) {
            while (!left2RightStack.isEmpty()) {
                top = left2RightStack.pop();
                temp.add(top.val);
                if (top.left != null) {
                    right2LeftStack.push(top.left);
                }
                if (top.right != null) {
                    right2LeftStack.push(top.right);
                }
            }
            if (!temp.isEmpty()) {
                result.add(temp);
                temp = new ArrayList<>();
            }
            while (!right2LeftStack.isEmpty()) {
                top = right2LeftStack.pop();
                temp.add(top.val);
                if (top.right != null) {
                    left2RightStack.push(top.right);
                }
                if (top.left != null) {
                    left2RightStack.push(top.left);
                }
            }
            if (!temp.isEmpty()) {
                result.add(temp);
                temp = new ArrayList<>();
            }
        }
        return result;
    }

    @Test
    public void test() {
        TreeNode root = new TreeNode(1);
        TreeNode left1 = new TreeNode(2);
        TreeNode right1 = new TreeNode(3);
        TreeNode left2 = new TreeNode(4);
        TreeNode right2 = new TreeNode(5);
        root.left = left1;
        root.right = right1;
        right1.left = left2;
        right1.right = right2;
        ArrayList<ArrayList<Integer>> result = zigzagTraversal(root);

    }
}
