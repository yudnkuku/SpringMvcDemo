package offer;

import org.junit.Test;

import java.util.*;

public class Offer {

    /**
     * 给定一个二维数组，每一行都是从左到右递增顺序排列，每一列都是从上到下递增排列，判断该数组中是否包含该元素
     * 思路：二分查找，时间复杂度nO(logN)
     * @param target
     * @param array
     * @return
     */
    public boolean find(int target, int[][] array) {
        if (array == null) {
            return false;
        }
        int len = array.length;
        boolean found = false;
        for (int i = 0; i < len; i++) {
            if (binarySearch(array[i], target)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private boolean binarySearch(int[] arr, int target) {
        int left = 0;
        int right = arr.length-1;
        while (left <= right) {
            int mid = (left+right)/2;
            if (arr[mid] == target) {
                return true;
            } else if (arr[mid] < target) {
                left = mid+1;
            } else {
                right = mid-1;
            }
        }
        return false;
    }

    /**
     * 将str中的空格全部替换为%20
     * @param str
     * @return
     */
    public String replaceSpace(StringBuffer str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        String s = str.toString();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                sb.append(s.charAt(i));
            } else {
                sb.append("%20");
            }
        }
        return sb.toString();
    }

    /**
     * 从尾到头打印链表
     * @param head
     * @return
     */
    public ArrayList<Integer> printListFromTailToHead(ListNode head) {
        ArrayList<Integer> result = new ArrayList<>();
        if (head == null) {
            return result;
        }
        Stack<Integer> stack = new Stack<>();
        ListNode node = head;
        while (node != null) {
            stack.push(node.val);
            node = node.next;
        }
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }

    /**
     * 根据前序和中序遍历结果重建二叉树
     * @param pre
     * @param in
     * @return
     */
    public TreeNode reConstructBinaryTree(int[] pre, int[] in) {
        if (pre == null || pre.length == 0) {
            return null;
        }
        return build(pre, 0, pre.length-1, in, 0, in.length-1);
    }

    //重建二叉树方法
    private TreeNode build(int[] pre, int preStart, int preEnd, int[] in, int inStart, int inEnd) {
        if (preStart > preEnd) {
            return null;
        }
        TreeNode root = new TreeNode(pre[preStart]);
        int middle = inStart;
        for (; middle < inEnd; middle++) {
            if (in[middle] == pre[preStart]) {
                break;
            }
        }
        //instart -> middle-1 middle+1 -> inEnd
        root.left = build(pre, preStart+1, preStart+middle-inStart, in,
                inStart, middle-1);
        root.right = build(pre, preStart+middle-inStart+1, preEnd, in,
                middle+1, inEnd);
        return root;
    }

    //用两个栈实现队列
    Stack<Integer> stack1 = new Stack<>();
    Stack<Integer> stack2 = new Stack<>();

    public void push(int node) {
        stack1.push(node);
    }

    public int pop() {
        int result = 0;
        while (stack1.size() != 1) {
            stack2.push(stack1.pop());
        }
        result = stack1.pop();
        while (!stack2.isEmpty()) {
            stack1.push(stack2.pop());
        }
        return result;
    }

    /**
     * 旋转数组中的最小数字
     * @param array
     * @return
     */
    public int minNumberInRotateArray(int[] array) {
        if (array == null || array.length == 0) {
            return 0;
        }
        int left = 0;
        int right = array.length-1;
        while (left < right) {
            if (array[left] < array[right]) {
                return array[left];
            }
            int mid = (left+right)/2;
            if (array[mid] < array[right]) {    //右半数组是升序数组
                right = mid;
            } else {    //右半数组是旋转数组
                left = mid+1;
            }
        }
        return array[left];
    }

    /**
     * 斐波那契数列
     * @param n
     * @return
     */
    public int Fibonacci(int n) {
        if (n == 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        return Fibonacci(n-1) + Fibonacci(n-2);
    }

    /**
     * 青蛙跳台阶，一次可以跳1级，一次可以跳2级，求出不同的组合结果
     * @param target
     * @return
     */
    public int JumpFloor(int target) {
        if (target == 0 || target == 1) {
            return 1;
        }
        return JumpFloor(target-1) + JumpFloor(target-2);
    }

    /**
     * 可以跳任意级台阶
     * @param target
     * @return
     */
    public int JumpFloorII(int target) {
        if (target == 0 || target == 1) {
            return 1;
        }
        int i = 1;
        int result = 0;
        while (i < target) {
            result += JumpFloorII(target-i);
            i++;
        }
        return result+1;
    }

    /**
     * n & (n-1)可以消除末尾的一个1
     * @param n
     * @return
     */
    public int NumberOf1(int n) {
        int count = 0;
        while (n != 0) {
            n = n & (n-1);
            count++;
        }
        return count;
    }

    /**
     * 求base的exponent次方
     * @param base
     * @param exponent
     * @return
     */
    public double Power(double base, int exponent) {
        double result = 1;
        if (exponent >= 0) {
            int i = 0;
            while (i++ < exponent) {
                result *= base;
            }
        } else {
            //exponent < 0
            int i = 0;
            while (i++ < -exponent) {
                result *= base;
            }
            result = 1/result;
        }
        return result;
    }

    /**
     * 调整数组中元素的顺序，使奇数位于偶数前面，保持奇数、偶数之间的相对位置
     * 思路：要保证奇数、偶数之间的相对位置不变，那么需要使用双指针法，将奇数插入到连续偶数之前
     * @param array
     */
    public void reOrderArray(int[] array) {
        if (array == null || array.length == 0) {
            return;
        }
        int left = -1;
        int cur = 0;
        while (cur < array.length) {
            if (array[cur] % 2 == 0) {
                //cur是偶数
                cur++;
            } else {
                //cur是奇数，将其插入到left+1的位置
                if (cur == 0 || array[cur-1] % 2 == 1) {
                    left++;
                    cur++;
                    continue;
                }
                int temp = array[cur];
                for (int i = cur; i >= left+2; i--) {
                    array[i] = array[i-1];
                }
                array[left+1] = temp;
                left++;
                cur++;
            }
        }
    }

    /**
     * 链表倒数第K个节点
     * @param head
     * @param k
     * @return
     */
    public ListNode FindKthToTail(ListNode head, int k) {
        if (head == null) {
            return null;
        }
        ListNode p = head;
        ListNode q = head;
        int i = 0;
        while (q != null && i < k) {
            q = q.next;
            i++;
        }
        if (q == null && i < k) {
            return null;
        }
        while (q != null) {
            p = p.next;
            q = q.next;
        }
        return p;
    }

    /**
     * 反转链表
     * @param head
     * @return
     */
    public ListNode ReverseList(ListNode head) {
        if (head == null) {
            return null;
        }
        ListNode cur = head;
        ListNode pre = null;
        while (cur != null) {
            ListNode next = cur.next;
            cur.next = pre;
            pre = cur;
            cur = next;
        }
        return pre;
    }

    /**
     * 合并两个升序链表
     * @param list1
     * @param list2
     * @return
     */
    public ListNode Merge(ListNode list1, ListNode list2) {
        if (list1 == null) {
            return list2;
        } else if (list2 == null) {
            return list1;
        }
        ListNode newHead = null;
        if (list1.val <= list2.val) {
            newHead = list1;
            newHead.next = Merge(list1.next, list2);
        } else {
            newHead = list2;
            newHead.next = Merge(list1, list2.next);
        }
        return newHead;
    }

    /**
     * 判断树B是否为A的子结构，空树不为任何树的子结构
     * @param root1
     * @param root2
     * @return
     */
    public boolean HasSubtree(TreeNode root1, TreeNode root2) {
        if (root1 == null || root2 == null) {
            return false;
        }
        return isSubTree(root1, root2) || HasSubtree(root1.left, root2) ||
                HasSubtree(root1.right, root2);
    }

    private boolean isSubTree(TreeNode rootA, TreeNode rootB) {
        if (rootB == null) {
            return true;
        }
        if (rootA == null) {
            return false;
        }
        if (rootA.val == rootB.val) {
            return isSubTree(rootA.left, rootB.left) && isSubTree(rootA.right, rootB.right);
        } else {
            return false;
        }
    }

    /**
     * 转换为镜像树
     * @param root
     */
    public void Mirror(TreeNode root) {
        if (root == null) {
            return;
        }
        TreeNode temp = root.left;
        root.left = root.right;
        root.right = temp;
        Mirror(root.left);
        Mirror(root.right);
    }

    /**
     * 判断序列popA是否是pushA的弹出序列
     * @param pushA
     * @param popA
     * @return
     */
    public boolean isPopOrder(int[] pushA, int[] popA) {
        Stack<Integer> stack = new Stack<>();
        int popIndex = 0;
        for (int i = 0; i < pushA.length; i++) {
            stack.push(pushA[i]);
            while (!stack.isEmpty() && stack.peek() == popA[popIndex]) {
                stack.pop();
                popIndex++;
            }
        }
        return stack.isEmpty();
    }

    /**
     * 层序遍历
     * @param root
     * @return
     */
    public ArrayList<Integer> printFromTopToBottom(TreeNode root) {
        ArrayList<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<TreeNode> queue = new LinkedList<>();
        TreeNode node = root;
        queue.offer(node);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                node = queue.poll();
                result.add(node.val);
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
     * 判断序列是否是BST的后序遍历序列
     * @param sequence
     * @return
     */
    public boolean verifySequenceOfBST(int[] sequence) {
        if (sequence == null || sequence.length == 0) {
            return true;
        }
        return verifyBSTHelper(sequence, 0, sequence.length-1);
    }

    private boolean verifyBSTHelper(int[] seq, int begin, int end) {
        if (begin >= end) {
            return true;
        }
        if (begin < end) {
            int root = seq[end];
            int left = begin;
            for (; left < end; left++) {
                if (seq[left] > root) {
                    break;
                }
            }
            for (int j = left; j < end; j++) {
                if (seq[j] < root) {
                    return false;
                }
            }
            return verifyBSTHelper(seq, begin, left-1)
                    && verifyBSTHelper(seq, left, end-1);
        }
        return false;
    }

    /**
     * 路径长度为target的所有路径
     * @param root
     * @param target
     * @return
     */
    public ArrayList<ArrayList<Integer>> findPath(TreeNode root, int target) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Stack<TreeNode> stack = new Stack<>();
        ArrayList<Integer> list = new ArrayList<>();
        TreeNode node = root;
        TreeNode lastVisit = null;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            node = stack.peek();
            if (node.right != null && node.right != lastVisit) {
                node = node.right;
            } else {
                //node.right == null || node.right == lastVisit
                if (node.right == null) {   //叶子节点
                    int sum = 0;
                    for (TreeNode n : stack) {
                        sum += n.val;
                        list.add(n.val);
                    }
                    if (sum == target) {
                        result.add(list);
                    }
                    list = new ArrayList<>();
                }
                lastVisit = stack.pop();    //将栈顶节点设置为lastVisit
                node = null;    //node置空
            }
        }
        return result;
    }

    /**
     * 复制复杂链表
     * 思路：在每个链表后面插入一个拷贝节点，设置其随机指针：cur.next.random = cur.random.next
     * @param head
     * @return
     */
    public RandomListNode clone(RandomListNode head) {
        if (head == null) {
            return head;
        }
        RandomListNode cur = head;
        while (cur != null) {
            RandomListNode node = new RandomListNode(cur.label);
            node.next = cur.next;
            cur.next = node;
            cur = node.next;
        }
        cur = head;
        //遍历链表，修改random引用
        while (cur != null) {
            if (cur.random != null) {
                cur.next.random = cur.random.next;
            }
            cur = cur.next.next;
        }
        //断开复制节点和原节点的连接
        cur = head;
        RandomListNode newHead = cur.next;
        while (cur != null) {
            RandomListNode next = cur.next;
            if (next != null) {
                cur.next = next.next;
            }
            cur = next;
        }
        return newHead;
    }

    /**
     * 将BST转换为排序的双向链表
     *
     * @param pRootOfTree
     * @return 转换后链表的头节点
     */
    public TreeNode Convert(TreeNode pRootOfTree) {
        if (pRootOfTree == null) {
            return null;
        }
        if (pRootOfTree.left != null) {
            TreeNode left = Convert(pRootOfTree.left);
            while (left.right != null) {
                left = left.right;
            }
            pRootOfTree.left = left;
            left.right = pRootOfTree;
        }
        if (pRootOfTree.right != null) {
            TreeNode right = Convert(pRootOfTree.right);
            pRootOfTree.right = right;
            right.left = pRootOfTree;
        }
        while (pRootOfTree.left != null) {
            pRootOfTree = pRootOfTree.left;
        }
        return pRootOfTree;
    }

    /**
     * 数组中出现次数超过一半的数字
     * @param array
     * @return
     */
    public int moreThanHalfNum(int[] array) {
        if (array == null || array.length == 0) {
            return 0;
        }
        Arrays.sort(array);
        for (int i = 0; i < array.length; i++) {
            int j = i;
            while (j < array.length-1 && array[j] == array[j+1]) {
                j++;
            }
            if ((j-i+1) > array.length/2) {
                return array[i];
            }
        }
        return 0;
    }

    /**
     * 数组中最小的K个数
     * @param num
     * @param k
     * @return
     */
    public ArrayList<Integer> getMinimumKNum(int[] num, int k) {
        ArrayList<Integer> result = new ArrayList<>();
        if (num == null || num.length == 0) {
            return result;
        }
        Arrays.sort(num);
        for (int i = 0; i < k; i++) {
            result.add(num[i]);
        }
        return result;
    }

    private void quickSort(int[] num, int min, int max) {
        if (num == null || num.length == 0) {
            return;
        }
        if (min < max) {
            int index = partition(num, min, max);
            quickSort(num, min, index-1);
            quickSort(num, index+1, max);
        }
    }

    /**
     * 快排分区函数
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
        //交换middle min
        swap(num, min, middle);
        while (left < right) {
            while (left < right && num[left] <= partitionEle) {
                left++;
            }
            //left >= right || num[left] > partitionEle
            while (num[right] > partitionEle) {
                right--;
            }
            if (left < right) {
                swap(num, left, right);
            }
        }
        swap(num, min, right);
        return right;
    }

    private void swap(int[] num, int m, int n) {
        int t = num[m];
        num[m] = num[n];
        num[n] = t;
    }

    /**
     * 连续子数组的最大和
     * 动态规划
     * @param num
     * @return
     */
    public int maxConsecutiveSubArray(int[] num) {
        if (num == null || num.length == 0) {
            return 0;
        }
        //max为包含num[i]的连续数组最大值
        int max = num[0];
        //res为当前所有子数组中最大值
        int res = num[0];
        for (int i = 1; i < num.length; i++) {
            max = Math.max(num[i], num[i]+max);
            res = Math.max(max, res);
        }
        return res;
    }

    /**
     * 打印数组中元素拼接成的最小数字
     * @param numbers
     * @return
     */
    public String printMinNumber(int[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (int i : numbers) {
            list.add(String.valueOf(i));
        }
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String s1 = o1+o2;
                String s2 = o2+o1;
                return s1.compareTo(s2);
            }
        });
        StringBuffer sb = new StringBuffer();
        for (String i : list) {
            sb.append(i);
        }
        return sb.toString();
    }

    /**
     * 求从小到大的第n个丑数
     * @param index
     * @return
     */
    public int getUglyNumber(int index) {
        if (index < 7) {
            return index;
        }
        int[] result = new int[index];
        result[0] = 1;
        int i2 = 0, i3 = 0, i5 = 0;
        for (int i = 1; i < index; i++) {
//            result[i] = min(result[i2]*2, result[i3]*3, result[i5]*5);
            result[i] = Math.min(result[i5]*5, Math.min(result[i2]*2, result[i3]*3));
            if (result[i] == result[i2]*2)
                i2++;
            if (result[i] == result[i3]*3)
                i3++;
            if (result[i] == result[i5]*5)
                i5++;
        }
        return result[index-1];
    }

    private int min(int m, int n, int k) {
        return Math.min(m, Math.min(n,k));
    }

    /**
     * 找出字符串中第一个只出现一次的字符
     * @param str
     * @return
     */
    public int findFirstNoRepeatingChar(String str) {
        if (str == null || str.length() == 0) {
            return -1;
        }
        for (int i = 0; i < str.length(); i++) {
            int index = str.indexOf(str.charAt(i));
            int lastIndex = str.lastIndexOf(str.charAt(i));
            if (lastIndex == index) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 两个链表的第一个公共节点
     * @param pHead1
     * @param pHead2
     * @return
     */
    public ListNode findFirstCommonNode(ListNode pHead1, ListNode pHead2) {
        if (pHead1 == null || pHead2 == null) {
            return null;
        }
        ListNode cur1 = pHead1;
        ListNode cur2 = pHead2;
        HashMap<ListNode, Object> map = new HashMap<>();
        while (cur1 != null) {
            map.put(cur1, null);
            cur1 = cur1.next;
        }
        while (cur2 != null) {
            if (map.containsKey(cur2)) {
                return cur2;
            }
            cur2 = cur2.next;
        }
        return null;
    }

    /**
     * K在排序数组中出现的次数
     * @param array
     * @param k
     * @return
     */
    public int getNumberOfK(int[] array, int k) {
        if (array == null || array.length == 0) {
            return 0;
        }
        if (k < array[0] || k > array[array.length-1]) {
            return 0;
        }
        int left = 0;
        int right = array.length-1;
        int index = 0;
        while (left < right) {
            int middle = (left+right)/2;
            if (array[middle] == k) {
                index = middle;
                break;
            } else if (array[middle] < k) {
                left = middle+1;
            } else {
                right = middle-1;
            }
        }
        if (left == right && array[left] != k) {
            return 0;
        }
        left = right = index;
        while (left >= 0) {
            if (array[left] != k) {
                break;
            }
            left--;
        }
        while (right < array.length) {
            if (array[right] != k) {
                break;
            }
            right++;
        }
        return right-left-1;
    }

    /**
     * 二叉树深度
     * @param root
     * @return
     */
    public int getTreeDepth(TreeNode root) {
        if (root == null) {
            return 0;
        }
        if (root.left == null && root.right == null) {
            return 1;
        }
        int left = getTreeDepth(root.left);
        int right = getTreeDepth(root.right);
        if (Math.abs(left-right) > 1) {
            isBalanced = false;
        }
        return Math.max(left, right) + 1;
    }

    /**
     * 判断二叉树是否平衡二叉树
     * 平衡二叉树：左子树和右子树的深度差不能大于1
     * @param root
     * @return
     */
    public boolean isBalancedTree(TreeNode root) {
        getTreeDepth(root);
        return isBalanced;
    }

    private boolean isBalanced = true;

    /**
     * 数组中除了两个数字之外，其他的数字都出现了两次，找出这两个数字
     * @param array
     * @param num1
     * @param num2
     */
    public void findNumbersAppearOnce(int[] array, int[] num1, int[] num2) {
        if (array == null || array.length == 0) {
            return;
        }
        Arrays.sort(array);
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length-1;) {
            if (array[i] == array[i+1]) {
                i += 2;
            } else {
                list.add(array[i]);
                if (list.size() == 2) {
                    break;
                }
                i++;
            }
        }
        if (list.size() != 2) {
            list.add(array[array.length-1]);
        }
        num1[0] = list.get(0);
        num2[0] = list.get(1);
    }

    /**
     * 找出所有和等于sum的连续正整数序列(至少包含2个数)
     * @param sum
     * @return
     */
    public ArrayList<ArrayList<Integer>> findContinuousSequence(int sum) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        ArrayList<Integer> l1 = new ArrayList<>();
        if (sum <= 2) {
            return result;
        }
        int left = 1;
        int right = 2;
        int temp = 3;
        while (left < right && right <= sum) {
            if (temp == sum) {
                for (int i = left; i <= right; i++) {
                    l1.add(i);
                }
                result.add(l1);
                l1 = new ArrayList<>();
                right++;
                temp += right;
            } else if (temp < sum) {
                right++;
                temp += right;
            } else {
                temp -= left;
                left++;
            }
        }
        return result;
    }

    /**
     * 排序数组中查找两个数使其和等于sum，输出乘积最小的一组
     * @param array
     * @param sum
     * @return
     */
    public ArrayList<Integer> findNumbersWithSum(int[] array, int sum) {
        ArrayList<Integer> result = new ArrayList<>();
        if (array == null || array.length < 2) {
            return result;
        }
        int len = array.length;
        int left = 0;
        int right = len-1;
        int temp = Integer.MAX_VALUE;
        while (left < right && right < len) {
            int t = array[left]+array[right];
            if (t == sum) {
                if (array[left]*array[right] < temp) {
                    temp = array[left]*array[right];
                    result = new ArrayList<>();
                    result.add(array[left]);
                    result.add(array[right]);
                }
                left++;
                right--;
            } else if (t < sum) {
                left++;
            } else {
                right--;
            }
        }
        return result;
    }

    /**
     * 将字符串循环左移n位
     * @param str
     * @param n
     * @return
     */
    public String leftRotate(String str, int n) {
        if (str == null || str.length() == 0) {
            return str;
        }
        int len = str.length();
        n = n%len;
        String s1 = str.substring(0, n);
        String s2 = str.substring(n);
        return s2+s1;
    }

    /**
     * 翻转字符串
     * @param str
     * @return
     */
    public String reverse(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        String[] arr = str.split(" ");
        StringBuffer sb = new StringBuffer();
        for (int i = arr.length-1; i >= 0; i--) {
            sb.append(arr[i]);
            if (i != 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * 能否组成顺子，大小王看成0
     * @param numbers
     * @return
     */
    public boolean isContinous(int[] numbers) {
        List<Integer> list = new ArrayList<>();
        int zeroCnt = 0;
        Arrays.sort(numbers);
        for (int i = 0; i < 5; i++) {
            if (numbers[i] == 0) {
                zeroCnt++;
            } else {
                if (list.contains(numbers[i]))
                    return false;
                list.add(numbers[i]);
            }
        }
        int size = list.size();
        int gap = list.get(size-1)-list.get(0)-size+1;
        return zeroCnt >= gap;
    }

    /**
     * 报数0~m-1  人编号 0~n-1
     * @param n
     * @param m
     * @return
     */
    public int lastRemaining(int n, int m) {
        if (n == 0 || m == 0) {
            return -1;
        }
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            l.add(i);
        }
        int begin = 0;
        while (true) {
            int size = l.size();
            if (size > 1) {
                int index = begin+m-1;
                l.remove(index%size);
                begin = index%size;
                if (begin == size-1) {
                    begin = 0;
                }
            } else {
                return l.get(0);
            }
        }
    }

    /**
     * 1+2+3+...+n
     * @param n
     * @return
     */
    public int Sum_Solution(int n) {
        if (n == 1)
            return 1;
        return Sum_Solution(n-1) + n;
    }

    /**
     * 求两数之和，不使用算术符号
     * @param num1
     * @param num2
     * @return
     */
    public int Add(int num1, int num2) {
        while (num2 != 0) {
            int temp = num1^num2;   //求和
            num2 = (num1&num2) << 1;    //算进位
            num1 = temp;
        }
        return num1;
    }

    /**
     * 将字符串转换为整数
     * @param str
     * @return
     */
    public int StrToInt(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        char[] chars = str.toCharArray();
        boolean positive = true;
        int sum = 0;
        if (chars[0] == '-') {
            positive = false;
        }
        for (int i = 0; i < chars.length; i++) {
            if (i == 0 && (chars[i] == '+' || chars[i] == '-')) {
                continue;
            }
            if (chars[i] < 48 || chars[i] > 57) {
                return 0;
            }
            sum = sum*10+chars[i]-48;
        }
        return positive ? sum : sum*-1;
    }


    /**
     * 构建数组，使数组B中的：B[i]=A[0]*....A[i-1]*A[i+1]...A[n]
     * 不能使用除法
     * @param A
     * @return
     */
    public int[] multiply(int[] A) {
        return null;
    }

    /**
     * 链表环的入口节点，如果没有环则返回null
     * 思路：快慢指针，如果存在环，快指针到入口的距离等于head到入口的距离
     * @param pHead
     * @return
     */
    public ListNode entryNodeOfList(ListNode pHead) {
        if (pHead == null || pHead.next == null) {
            return null;
        }
        ListNode slow = pHead;
        ListNode fast = pHead;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                break;
            }
        }
        if (fast == null || fast.next == null) {
            return null;
        }
        slow = pHead;
        while (slow != fast) {
            slow = slow.next;
            fast = fast.next;
        }
        return slow;
    }

    /**
     * 删除排序链表重复节点
     * @param pHead
     * @return
     */
    public ListNode deleteDuplicate(ListNode pHead) {
        if (pHead == null || pHead.next == null) {
            return pHead;
        }
        ListNode dummy = new ListNode(-1);
        dummy.next = pHead;
        ListNode pre = dummy;
        ListNode cur = pHead;
        while (cur != null) {
            ListNode next = cur.next;
            while (next != null && cur.val == next.val) {
                next = next.next;
            }
            //next == null || cur.val != next.val
            if (cur.next == next) {
                pre = cur;
            } else {
                pre.next = next;
            }
            cur = next;
        }
        return dummy.next;
    }

    /**
     * 找出某节点中序遍历的下一个节点
     * @param pNode
     * @return
     */
    public TreeLinkNode getNext(TreeLinkNode pNode) {
        if (pNode == null) {
            return null;
        }
        if (pNode.next == null) {   //如果父节点为空，即pNode为根节点
            pNode = pNode.right;
            while (pNode != null && pNode.left != null) {
                pNode = pNode.left;
            }
            return pNode;
        }
        if (pNode.next.left == pNode) { //如果该节点是左子节点，直接返回父节点
            return pNode.next;
        }
        //pNode是右子节点，那么向上找到第一个是左子节点的父节点，返回该节点的父节点
        while (pNode.next != null) {
            if (pNode.next.left == pNode) {
                return pNode.next;
            }
            pNode = pNode.next;
        }
        return null;
    }

    public TreeLinkNode getNext1(TreeLinkNode pNode) {
        if (pNode == null) {
            return null;
        }
        if (pNode.right != null) {  //存在右子节点，返回右子树中最左节点
            pNode = pNode.right;
            while (pNode.left != null) {
                pNode = pNode.left;
            }
            return pNode;
        }
        while (pNode.next != null) {    //向上找到第一个是其父节点左节点的节点
            if (pNode.next.left == pNode)
                return pNode.next;
            pNode = pNode.next;
        }
        return null;
    }

    class TreeLinkNode {
        int val;

        TreeLinkNode left = null;

        TreeLinkNode right = null;

        TreeLinkNode next = null;

        TreeLinkNode(int val) {
            this.val = val;
        }
    }

    /**
     * 判断二叉树是否对称
     * @param root
     * @return
     */
    public boolean isSymmetrical(TreeNode root) {
        if (root == null) {
            return true;
        }
        return isSymmetricalHelper(root.left, root.right);
    }

    private boolean isSymmetricalHelper(TreeNode left, TreeNode right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.val == right.val && isSymmetricalHelper(left.left, right.right)
                &&isSymmetricalHelper(left.right, right.left);
    }

    /**
     * 按之字形打印二叉树
     * @param root
     * @return
     */
    public ArrayList<ArrayList<Integer>> print(TreeNode root) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Stack<TreeNode> s1 = new Stack<>();
        Stack<TreeNode> s2 = new Stack<>();
        ArrayList<Integer> l = new ArrayList<>();
        TreeNode node = null;
        s1.push(root);
        while (!s1.isEmpty() || !s2.isEmpty()) {
            while (!s1.isEmpty()) {
                node = s1.pop();
                l.add(node.val);
                if (node.left != null) {
                    s2.push(node.left);
                }
                if (node.right != null) {
                    s2.push(node.right);
                }
            }
            result.add(l);
            l = new ArrayList<>();
            while (!s2.isEmpty()) {
                node = s2.pop();
                l.add(node.val);
                if (node.right != null) {
                    s1.push(node.right);
                }
                if (node.left != null) {
                    s1.push(node.left);
                }
            }
            result.add(l);
            l = new ArrayList<>();
        }
        return result;
    }

    /**
     * 层序遍历二叉树
     * @param root
     * @return
     */
    public ArrayList<ArrayList<Integer>> printInOrder(TreeNode root) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<TreeNode> queue = new LinkedList<>();
        ArrayList<Integer> l = new ArrayList<>();
        TreeNode node = null;
        queue.offer(root);
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                node = queue.poll();
                l.add(node.val);
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
            if (!l.isEmpty()) {
                result.add(l);
                l = new ArrayList<>();
            }
        }
        return result;
    }

    /**
     * 二叉搜索树第K小的节点
     * 思路：中序遍历将二叉搜索树排序
     * @param root
     * @param k
     * @return
     */
    public TreeNode KthNode(TreeNode root, int k) {
        if (root == null) {
            return null;
        }
        ArrayList<TreeNode> inOrderList = inOrderTraversal(root);
        if (inOrderList == null || inOrderList.size() < k || k <= 0) {
            return null;
        }
        return inOrderList.get(k-1);
    }

    private ArrayList<TreeNode> inOrderTraversal(TreeNode root) {
        ArrayList<TreeNode> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        result.addAll(inOrderTraversal(root.left));
        result.add(root);
        result.addAll(inOrderTraversal(root.right));
        return result;
    }

    /**
     * 链表节点定义
     */
    class ListNode {

        int val;

        ListNode next = null;

        ListNode(int val) {
            this.val = val;
        }

    }

    /**
     * 树节点定义
     */
    class TreeNode {

        int val;

        TreeNode left;

        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }

    }

    class RandomListNode {

        int label;

        RandomListNode next = null;

        RandomListNode random = null;

        RandomListNode(int label) {
            this.label = label;
        }
    }

    @Test
    public void test() {
        System.out.println(StrToInt("200"));
        System.out.println(StrToInt("-200"));
    }
}
