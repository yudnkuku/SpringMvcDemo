package leetcode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class TreeTraversal {

    /**
     * 层序遍历
     * @param root
     */
    public ArrayList<ArrayList<Integer>> TraversalInLevelOrder(TreeNode root) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (null == root) {
            return result;
        }
        ArrayList<Integer> temp = new ArrayList<>();
        ArrayList<TreeNode> current = new ArrayList<>();
        ArrayList<TreeNode> next = new ArrayList<>();
        current.add(root);
        while (!current.isEmpty()) {
            for (TreeNode node : current) {
                temp.add(node.val);
                if (node.left != null) {
                    next.add(node.left);
                }
                if (node.right != null) {
                    next.add(node.right);
                }
            }
            result.add(temp);
            current = next;
            next = new ArrayList<>();
        }
        return result;
    }

    /**
     * 层序遍历（迭代）
     * @param root
     * @return
     */
    public ArrayList<ArrayList<Integer>> levelOrderTraversal(TreeNode root) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        LinkedList<TreeNode> queue = new LinkedList<>();
        ArrayList<Integer> temp = new ArrayList<>();
        TreeNode node = null;
        queue.offer(root);
        int size = 0;
        while (!queue.isEmpty()) {
            size = queue.size();
            for (int i = 0; i < size; i++) {
                node = queue.poll();
                temp.add(node.val);
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
            result.add(temp);
            temp = new ArrayList<>();
        }
        return result;
    }

    /**
     * 层序遍历，从底部往上
     * @param root
     * @return
     */
    public ArrayList<ArrayList<Integer>> levelOrderTraversal1(TreeNode root) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<TreeNode> queue = new LinkedList<>();
        ArrayList<Integer> temp = new ArrayList<>();
        Stack<ArrayList<Integer>> stack = new Stack<>();
        TreeNode node = root;
        queue.add(node);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                node = queue.poll();
                temp.add(node.val);
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
            stack.push(temp);
            temp = new ArrayList<>();
        }
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }
    /**
     * 前序遍历(递归)
     * @param root
     * @return
     */
    public ArrayList<Integer> preorderTraversal(TreeNode root) {
        ArrayList<Integer> result = new ArrayList<>();
        if (null == root) {
            return result;
        }
        result.add(root.val);
        if (root.left != null) {
            result.addAll(preorderTraversal(root.left));
        }
        if (root.right != null) {
            result.addAll(preorderTraversal(root.right));
        }
        return result;
    }

    /**
     * 前序遍历（迭代）
     * 思路：使用栈，先入右节点，再入左节点
     * @param root
     * @return
     */
    public ArrayList<Integer> preorderTraversal1(TreeNode root) {
        ArrayList<Integer> result = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = null;
        if (null == root) {
            return result;
        }
        stack.push(root);
        while (!stack.isEmpty()) {
            node = stack.pop();
            result.add(node.val);
            if (node.right != null) {
                stack.push(node.right);
            }
            if (node.left != null) {
                stack.push(node.left);
            }
        }
        return result;
    }

    /**
     * 后序遍历（递归）
     * @param root
     * @return
     */
    public ArrayList<Integer> postorderTraversal(TreeNode root) {
        ArrayList<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        if (root.left != null) {
            result.addAll(postorderTraversal(root.left));
        }
        if (root.right == null) {
            result.addAll(postorderTraversal(root));
        }
        result.add(root.val);
        return result;
    }

    /**
     * 后序遍历（迭代），这个方法很重要
     * 思路：采用堆栈和一个lastVisit指针指向上一次被访问的节点，首先循环将左子节点压入堆栈，查看栈顶元素，
     *      如果栈顶元素右子节点不为空且lastVisit!=peek.right，那么遍历右子树，继续下一轮循环压入左子节点；
     *      如果栈顶元素右子节点为空或者lastVisit==peek.right，那么表明可以访问当前栈顶节点，即可以弹出栈顶节点，
     *      将lastVisit更新为弹出节点
     * @param root
     * @return
     */
    public ArrayList<Integer> postorderTraversal1(TreeNode root) {
        ArrayList<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Stack<TreeNode> stack = new Stack<>();
        TreeNode node = root;
        TreeNode lastVisit = root;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            //查看栈顶节点
            node = stack.peek();
            //如果栈顶元素右节点为空或者右节点被访问，那么弹出该栈顶元素
            if (node.right == null || node.right == lastVisit) {
                result.add(stack.pop().val);
                lastVisit = node;
                node = null;
            } else {
                node = node.right;
            }
        }
        return result;
    }

    /**
     * 判断树是否是对称树，例如：
     *       1
     *      / \
     *     2   2
     *    /\  /\
     *   3 4 4 3
     * @param root
     * @return
     */
    public boolean isSymmetric(TreeNode root) {
        if (null == root) {
            return true;
        }
        return isSym(root.left, root.right);
    }

    private boolean isSym(TreeNode left, TreeNode right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.val == right.val && isSym(left.left, right.right)
                && isSym(left.right, right.left);
    }

    /**
     * 判断数是否是平衡二叉树，平衡二叉树指的是每个节点的左右子树深度相差不超过1
     * 思路：后序遍历树的每一个节点，在遍历每个节点时记录它的深度
     * @param root
     * @return
     */
    public boolean isBalanced(TreeNode root) {
        getDepth(root);
        return isBalanced;
    }

    /**
     * 私有方法，获取树的深度
     * @param root
     * @return
     */
    private boolean isBalanced = true;

    private int getDepth(TreeNode root) {
        if (null == root) {
            return 0;
        }
        int left = getDepth(root.left);
        int right = getDepth(root.right);
        if (Math.abs(left-right) > 1) {
            isBalanced = false;
        }
        return left > right ? left+1 : right+1;
    }

    /**
     * 返回树的右视图，即树的最右侧节点
     * 思路：层序遍历，遍历每一层时将每一层的最后一个节点返回
     * @param root
     * @return
     */
    public ArrayList<Integer> rightSideView(TreeNode root) {
        ArrayList<Integer> result = new ArrayList<>();
        TreeNode node = root;
        LinkedList<TreeNode> queue = new LinkedList<>();
        queue.offer(node);
        while (!queue.isEmpty()) {
            result.add(queue.getLast().val);
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                node = queue.poll();
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }
        return result;
    }

    /**
     * 判断两颗树是否相同
     * @param p
     * @param q
     * @return
     */
    public boolean isSameTree(TreeNode p, TreeNode q) {
        if (p == null && q == null) {
            return true;
        }
        if (p == null || q == null) {
            return false;
        }
        return p.val == q.val && isSameTree(p.left, q.left)
                && isSameTree(p.right, q.right);
    }

    /**
     * 由中序遍历和后序遍历重建树,树中没有重复节点
     * 中序遍历：先左子节点，再父节点，最后右子节点
     * 后序遍历：先左右子节点，再父节点
     * 特点：后序遍历的最后一个节点是根节点，在中序遍历数组中查找等于根节点值得节点，左半数组是左子树，右半数组是右子树，
     *      再在后序数组相同范围中确定左右子节点，依次递归
     * @param inOrder
     * @param postOrder
     * @return
     */
    public TreeNode buildTree(int[] inOrder, int[] postOrder) {
        if (inOrder == null || inOrder.length == 0) {
            return null;
        }
        int last = inOrder.length-1;
        return build(inOrder, 0, last, postOrder, 0, last);
    }

    private TreeNode build(int[] inOrder, int inStart, int inEnd, int[] postOrder, int postStart, int postEnd) {
        if (inStart > inEnd) {
            return null;
        }
        TreeNode root = new TreeNode(postOrder[postEnd]);
        int middle = inStart;
        for (;middle <= inEnd; middle++) {
            if (inOrder[middle] == postOrder[postEnd]) {
                break;
            }
        }
        root.left = build(inOrder, inStart, middle-1, postOrder,
                postStart, postStart+middle-inStart-1);
        root.right = build(inOrder, middle+1, inEnd, postOrder,
                postEnd-inEnd+middle, postEnd-1);
        return root;
    }

    /**
     * 根据前序遍历和中序遍历重建树
     * 前序遍历：先根节点，再左右节点
     * 思路：只需要修改上述的build方法即可
     * @param preOrder
     * @param inOrder
     * @return
     */
    public TreeNode buildTree2(int[] preOrder, int[] inOrder) {
        if (preOrder == null || preOrder.length == 0) {
            return null;
        }
        int last = preOrder.length-1;
        return build2(preOrder, 0, last, inOrder, 0, last);
    }

    private TreeNode build2(int[] preOrder, int preStart, int preEnd, int[] inOrder, int inStart, int inEnd) {
        if (preStart > preEnd) {
            return null;
        }
        TreeNode root = new TreeNode(preOrder[preStart]);
        int middle = inStart;
        for (; middle <= inEnd; middle++) {
            if (inOrder[middle] == preOrder[preStart])
                break;
        }
        //middle-1-inStart == x-(preStart+1)
        root.left = build2(preOrder, preStart+1, middle-inStart+preStart, inOrder,
                inStart, middle-1);
        root.right = build2(preOrder, middle-inStart+preStart+1, preEnd, inOrder,
                middle+1, inEnd);
        return root;
    }

    /**
     * 给定数字n，求出1-n中的数字能组成多少个不同的二叉查找树(BST)，左子节点<root<右子节点
     * @param n
     * @return
     */
    public int numTrees(int n) {
        if (n <= 1) {
            return 1;
        }
        int sum = 0;
        int i = 1;
        while (i <= n) {
            sum += numTrees(i-1) * numTrees(n-i);
        }
        return sum;
    }


}
