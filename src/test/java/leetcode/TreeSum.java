package leetcode;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Stack;

public class TreeSum {

    /**
     * 根节点到叶子结点组成的整数求和
     * 例如：
     *       1
     *      / \
     *     2  3
     * 结果：12+13=25
     * @param root
     * @return
     */
    public int sumToLeaf(TreeNode root) {
        return sumHelper(root, 0);
    }

    /**
     * 递归实现，传递给左右子节点递归的条件是：10*parent+root.val
     * @param root
     * @param parent  父节点返回的值
     * @return
     */
    private int sumHelper(TreeNode root, int parent) {
        if (root == null) {
            return 0;
        }
        if (root.left == null && root.right == null) {
            return parent * 10 + root.val;
        }
        return sumHelper(root.left, parent*10 + root.val) +
                sumHelper(root.right, parent*10 + root.val);
    }

    /**
     * 判断数是否有指定sum的路径
     * 思路：后序遍历找出等于sum的路径
     * @param root
     * @param sum
     * @return
     */
    public boolean hasPathSum(TreeNode root, int sum) {
        if (root == null) {
            return sum == 0;
        }
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = root;
        TreeNode lastVisit = null;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            //查看栈顶元素
            node = stack.peek();
            if (node.right != null && node.right != lastVisit) {
                //压入右子树
                node = node.right;
            } else {
                //node.right == null || node.right == lastVisit
                if (node.left == null && node.right == null) {
                    //叶子结点
                    int tmp = 0;
                    for (TreeNode n : stack) {
                        tmp += n.val;
                    }
                    if (tmp == sum) {
                        return true;
                    }
                }
                lastVisit = stack.pop();
                node = null;
            }
        }
        return false;
    }

    /**
     * 返回二叉树中所有路径和等于sum的路径
     * 思路：后序遍历，找出所有等于sum的路径
     * @param root
     * @param sum
     * @return
     */
    public ArrayList<ArrayList<Integer>> findAllPathEqualSum(TreeNode root, int sum) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = root;
        TreeNode lastVisit = null;
        ArrayList<Integer> list = new ArrayList<>();
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            //查看栈顶元素
            node = stack.peek();
            if (node.right != null && node.right != lastVisit) {
                //压入右子树
                node = node.right;
            } else {
                //node.right == null || node.right == lastVisit
                if (node.left == null && node.right == null) {
                    //叶子结点
                    int tmp = 0;
                    for (TreeNode n : stack) {
                        tmp += n.val;
                        list.add(n.val);
                    }
                    if (tmp == sum) {
                        result.add(list);
                    }
                    list = new ArrayList<>();
                }
                lastVisit = stack.pop();
                node = null;
            }
        }
        return result;
    }

    @Test
    public void test() {
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        stack.push(2);
        for (int i : stack) {
            System.out.println(i);
        }
    }
}
