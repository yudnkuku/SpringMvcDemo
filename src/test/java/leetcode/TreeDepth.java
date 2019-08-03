package leetcode;

import java.util.Stack;
import java.util.TreeSet;

public class TreeDepth {

    /**
     * 二叉树最短路径(递归)
     * @param root
     * @return
     */
    public int shortestDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        if (root.left == null && root.right == null) {
            return 1;
        }
        if (root.left == null) {
            return shortestDepth(root.right);
        }
        if (root.right == null) {
            return shortestDepth(root.left);
        }
        return Math.min(shortestDepth(root.left), shortestDepth(root.right));
    }

    /**
     * 二叉树最短路径（迭代）
     * 思路：利用后序遍历统计所有路径，再求最短路径
     * @param root
     * @return
     */
    public int shortestDepthIterably(TreeNode root) {
        if (root == null) {
            return 0;
        }
        Stack<TreeNode> stack = new Stack<>();
        TreeNode lastVisited = null;
        TreeNode node = root;
        TreeSet<Integer> treeSet = new TreeSet<>();
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            //查看栈顶元素是否有右子节点，如果右节点为空(叶子节点)或者右节点已经遍历到，弹出栈顶节点，并将该节点设为最近访问节点
            node = stack.peek();
            if (node.right != null && node.right != lastVisited) {
                node = node.right;
            } else {
                //如果左右子节点都为空，那么堆栈元素个数即为路径长度
                if (node.left == null && node.right == null) {
                    treeSet.add(stack.size());
                }
                //弹出栈顶元素
                lastVisited = stack.pop();
                node = null;
            }
        }
        return treeSet.first();
    }
}
