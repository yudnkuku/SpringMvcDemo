package leetcode;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Stack;

public class LinkedList {

    class ListNode {

        int val;

        ListNode next;

        public ListNode(int val) {
            this.val = val;
            this.next = null;
        }

        public ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }

    }

    /**
     * 判断单项链表是否有环
     * 思路：使用快慢指针的方式，如果有环则快慢指针必然相遇
     * @param head
     * @return
     */
    public boolean hasCycle(ListNode head) {
        if (head == null || head.next == null) {
            return false;
        }
        //定义两个指针：slow & fast , 分别走1步和两步
        ListNode slow = head;
        ListNode fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断单向链表环入口，如果没有环返回null
     * 思路：还是使用快慢指针方式，有如下定理：碰撞点到环入口的距离=头指针到环入口的距离
     * @param head
     * @return
     */
    public ListNode detectCollisionPoint(ListNode head) {
        if (head == null || head.next == null) {
            return null;
        }
        ListNode slow = head;
        ListNode fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                break;
            }
        }
        //如果不存在环，返回null
        if (fast == null || fast.next == null) {
            return null;
        }
        slow = head;
        while (slow != fast) {
            slow = slow.next;
            fast = fast.next;
        }
        return slow;
    }

    /**
     * 对单向链表进行重构，例如L0,L1,L2...Ln  => L0,Ln,L1,Ln-1...
     * 思路：单向链表只能从前往后遍历，但是要先遍历到最末节点，那么考虑使用栈，把后一半链表节点压入堆栈，再同时遍历前一半链表和弹出堆栈
     *      组成新的链表
     * @param head
     */
    public void reorderList(ListNode head) {
        if (head == null || head.next == null) {
            return;
        }
        //遍历链表，得到链表长度n
        ListNode node = head;
        int length = 0;
        int i = 0;
        while (node != null) {
            length++;
            node = node.next;
        }
        Stack<ListNode> stack = new Stack<>();
        ListNode next = node = head;
        while (node != null) {
            next = node.next;
            int p = length/2;
            if (length % 2 == 0) {
                p--;
            }
            if (i == p) {
                node.next = null;
            } else if (i > p) {
                stack.push(node);
            }
            i++;
            node = next;
        }
        node = head;
        ListNode top = null;
        while (node != null) {
            if (stack.isEmpty()) {
                break;
            }
            next = node.next;
            top = stack.pop();
            node.next = top;
            top.next = next;
            node = next;
        }
    }

    /**
     * 链表插入排序
     * @param head
     * @return
     */
    public ListNode insertionSort2(ListNode head) {
        for (ListNode scan = head.next; scan != null; scan = scan.next) {
            ListNode lastScan = lastNode(head, scan);
            ListNode scanNext = scan.next;
            int tmp = scan.val;
            ListNode position = head;
            while (position != scan && position.val < tmp) {
                position = position.next;
            }
            //position.val > tmp && lastPosition.val < tmp, position is the place to insert
            ListNode next = position.next;
            if (position != head) {
                ListNode lastPosition = lastNode(head, position);
                lastScan.next = scanNext;
                lastPosition.next = position;
                position.next = next;
            } else {
                lastScan.next = scanNext;
                position.next = head.next;
                head = position;
            }
        }
        return head;
    }

    /**
     * 链表中指定节点上一个节点
     * @param head
     * @param node
     * @return
     */
    private ListNode lastNode(ListNode head, ListNode node) {
        if (node == head) {
            return null;
        }
        ListNode last = head;
        while (last.next != node) {
            last = last.next;
        }
        return last;
    }

    public ListNode insertionSort1(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode cur = head.next;
        ListNode pre = head;
        while (cur != null) {
            ListNode scan = head;
            ListNode prescan = null;
            ListNode next = cur.next;
            while (scan != cur && scan.val <= cur.val) {
                prescan = scan;
                scan = scan.next;
            }
            //scan == cur || scan.val > cur.val && prescan.val <= cur.val
            if (prescan == null) {
                pre.next = cur.next;
                cur.next = head;
                head = cur;
            } else {
                pre.next = cur.next;
                cur.next = scan;
                prescan.next = cur;
            }
            if (cur.next == next) {
                pre = cur;
            }
            cur = next;
        }
        return head;
    }


    /**
     * 链表插入排序（升序）
     * 思路：构造一个虚拟节点，作为新链表的头结点，从head节点开始遍历链表，找到下一节点小于当前节点的节点，
     *      将其插入至虚拟节点和当前节点之间
     * @param head
     * @return
     */
    public ListNode insertionSort(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode root = new ListNode(0);
        root.next = head;
        ListNode cur = head;
        ListNode scan = root;
        ListNode temp = null;
        while (cur != null && cur.next != null) {
            if (cur.val <= cur.next.val) {
                cur = cur.next;
            } else {
                //将cur.next插入到root和cur之间的某个位置
                temp = cur.next;
                cur.next = temp.next;
                scan = root;
                while (scan.next.val <= temp.val) { //使用next节点遍历，而不是使用当前节点
                    scan = scan.next;
                }
                temp.next = scan.next;
                scan.next = temp;
            }
        }
        return root.next;
    }

    /**
     * 链表插入排序（降序）
     * @param head
     * @return
     */
    public ListNode insertionSortDesc(ListNode head) {
        if (head == null || head.next == null) {
            return null;
        }
        ListNode root = new ListNode(0);
        root.next = head;
        ListNode cur = head;
        ListNode scan = null;
        ListNode temp = null;
        while (cur != null && cur.next != null) {
            if (cur.val >= cur.next.val) {
                cur = cur.next;
            } else {
                scan = root;
                temp = cur.next;
                cur.next = temp.next;
                while (scan.next.val >= cur.val) {
                    scan = scan.next;
                }
                temp.next = scan.next;
                scan.next = temp;
            }
        }
        return root.next;
    }

    /**
     * 两个链表组成的整数求和，组成新的链表
     * 思路：使用哑节点，遍历两个链表，求每位的和，注意进位处理，遍历完链表后，最后还要考虑最后的进位是否为1，
     *      如果是1末尾加上进位节点，否则设为null
     * @param head1
     * @param head2
     * @return
     */
    public ListNode sum(ListNode head1, ListNode head2) {
        if (head1 == null && head2 == null) {
            return null;
        }
        ListNode dummy = new ListNode(-1);
        ListNode p1 = head1;
        ListNode p2 = head2;
        ListNode cur = dummy;
        int carried = 0;
        while (p1 != null || p2 != null) {
            int n1 = p1 == null ? 0 : p1.val;
            int n2 = p2 == null ? 0 : p2.val;
            cur.next = new ListNode((n1 + n2 + carried) % 10);
            carried = (n1 + n2 + carried) / 10;
            cur = cur.next;
            p1 = (p1 == null) ? null : p1.next;
            p2 = (p2 == null) ? null : p2.next;
        }
        cur.next = (carried == 1) ? new ListNode(1) : null;
        return dummy.next;
    }

    /**
     * 删除链表倒数第n个节点，只遍历一次链表
     * 思路：采用双指针p/q和一个虚拟头结点，初始化时p/q均指向虚拟头结点，对 q 取 n 次 next，
     *      然后对p/q同时next，直到q.next=null，p.next就是要删除的倒数第n个节点
     * @param head
     * @param n
     * @return
     */
    public ListNode deleteNode(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head);
        ListNode p = dummy;
        ListNode q = dummy;
        while (n > 0) {
            q = q.next;
            n--;
        }
        while (q.next != null) {
            p = p.next;
            q = q.next;
        }
        ListNode temp = p.next.next;
        p.next = temp;
        return dummy.next;
    }

    /**
     * 删除链表中所有等于给定val的节点
     * 思路：遍历链表，如果节点值等于val，那么删除该节点
     * @param head
     * @param val
     * @return
     */
    public ListNode removeElements(ListNode head, int val) {
        if (head == null) {
            return null;
        }
        ListNode dummy = new ListNode(0, head);
        ListNode node = dummy;
        while (node.next != null) {
            if (node.next.val == val) {
                node.next = node.next.next;
            }
            node = node.next;
        }
        return dummy.next;
    }

    /**
     * 将两个有序链表组成一个新的有序链表（递归）
     * 思路：递归，比较两个列表的头结点
     * @param head1
     * @param head2
     * @return
     */
    public ListNode mergeTwoSortedList(ListNode head1, ListNode head2) {
        ListNode newHead = null;
        if (head1 == null) {
            return head2;
        } else if (head2 == null) {
            return head1;
        } else {
            if (head1.val <= head2.val) {
                newHead = head1;
                newHead.next = mergeTwoSortedList(head1.next, head2);
            } else {
                newHead = head2;
                newHead.next = mergeTwoSortedList(head1, head2.next);
            }
        }
        return newHead;
    }


    public ListNode mergeTwoSortedList1(ListNode head1, ListNode head2) {
        if (head1 == null) {
            return head2;
        }
        if (head2 == null) {
            return head1;
        }
        ListNode dummy = new ListNode(0);
        ListNode l1 = head1;
        ListNode l2 = head2;
        ListNode cur = dummy;
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                cur.next = l1;
                l1 = l1.next;
            } else {
                cur.next = l2;
                l2 = l2.next;
            }
            cur = cur.next;
        }
        if (l1 == null) {
            cur.next = l2;
        } else {
            cur.next = l1;
        }
        return dummy.next;
    }

    /**
     * 合并K个有序链表
     * 思路：递归，现在已知合并两个有序链表的方法，如何合并K个有序链表，借鉴归并思想，将K个有序链表拆分为单独的有序链表，再将每个有序链表合并
     * @param lists
     * @return
     */
    public ListNode mergeKLists(ArrayList<ListNode> lists) {
        if (lists == null || lists.isEmpty()) {
            return null;
        }
        if (lists.size() == 1) {
            return lists.get(0);
        }
        if (lists.size() == 2) {
            return mergeTwoSortedList1(lists.get(0), lists.get(1));
        }
        int mid = lists.size()/2;
        ArrayList<ListNode> l1 = new ArrayList<>();
        ArrayList<ListNode> l2 = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            if (i < mid) {
                l1.add(lists.get(i));
            } else {
                l2.add(lists.get(i));
            }
        }
        return mergeTwoSortedList1(mergeKLists(l1), mergeKLists(l2));

    }

    /**
     * 交换链表中相邻的两个节点顺序
     * 思路：堆栈，每次压入相邻两个节点，再弹出节点添加到新的链表末端
     * @param head
     * @return
     */
    public ListNode swapPairs(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode dummy = new ListNode(-1);
        ListNode cur = dummy;
        ListNode node = head;
        Stack<ListNode> stack = new Stack<>();
        while (node != null) {
            stack.push(node);
            node = node.next;
            if (node != null) {
                stack.push(node);
                node = node.next;
            }
            cur.next = stack.pop();
            cur = cur.next;
            if (!stack.isEmpty()) {
                cur.next = stack.pop();
                cur = cur.next;
            }

        }
        cur.next = null;
        return dummy.next;
    }

    /**
     * 交换相邻两个节点的位置
     * 思路：
     * @param head
     * @return
     */
    public ListNode swapPairs1(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode p = dummy;
        while (p.next != null && p.next.next != null) {
            ListNode n1 = p.next;
            ListNode n2 = n1.next;
            ListNode next = n2.next;
            p.next = n2;
            n2.next = n1;
            n1.next = next;
            p = n1;
        }

        return dummy.next;
    }

    /**
     * 分隔链表， 使所有小于x的节点都在大于等于x的节点之前
     * 思路：构造两个虚拟链表，遍历原始链表，小于x链接到第一个链表，大于等于x链接到第二个链表
     * @param head
     * @param x
     * @return
     */
    public ListNode partitionList(ListNode head, int x) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode dummy1 = new ListNode(0);
        ListNode n1 = dummy1;
        ListNode dummy2 = new ListNode(0);
        ListNode n2 = dummy2;
        ListNode node = head;
        while (node != null) {
            if (node.val < x) {
                n1.next = node;
                n1 = node;
            } else {
                n2.next = node;
                n2 = node;
            }
            node = node.next;
        }
        if (n1 != null) {
            n1.next = dummy2.next;
        }
        return dummy1.next;
    }

    /**
     * 反转链表m-n的节点(1<=m<=n<=链表长度)  例如对于链表1->2->3->4  m=2,n=4  得到  1->4->3->2
     * 思路：遍历链表，遍历链表，保存 m-1 和 n+1 节点的引用，将m-n节点压入堆栈，最后弹出堆栈重新组成新的链表
     * @param head
     * @param m
     * @param n
     * @return
     */
    public ListNode rotateList1(ListNode head, int m, int n) {
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode front = dummy;
        ListNode rear = null;
        ListNode node = dummy;
        Stack<ListNode> stack = new Stack<>();
        int i = 0;
        while (node != null) {
            node = node.next;
            i++;
            if (i == m-1) {
                front = node;
            }
            if (i == n+1) {
                rear = node;
            }
            if (i >= m && i <= n) {
                stack.push(node);
            }
        }
        ListNode cur = front;
        if (front != null) {
            if (stack.isEmpty()) {
                front.next = null;
            } else {
                while (!stack.isEmpty()) {
                    cur.next = stack.pop();
                    cur = cur.next;
                }
            }
        }
        cur.next = rear;
        return dummy.next;
    }

    /**
     * 反转[m,n]区间内的节点
     * @param head
     * @param m
     * @param n
     * @return
     */
    public ListNode rotateList2(ListNode head, int m, int n) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode dummy = new ListNode(0, head);
        ListNode cur = dummy;
        for (int i = 1; i < m; i++) {
            cur = cur.next;
        }
        ListNode pre = cur;
        //保留链表最后一个节点引用
        ListNode last = cur.next;
        ListNode front = null;
        ListNode next = cur.next;
        for (int i = m; i <= n; i++) {
//            cur = pre.next;
//            pre.next = cur.next;
//            cur.next = front;
//            front = cur;
            cur = next;
            next = cur.next;
            cur.next = front;
            front = cur;
        }
//        cur = pre.next;
        cur = next;
        pre.next = front;
        last.next = cur;
        return dummy.next;
    }

    /**
     * 反转链表
     * 思路：使用两个指针，pre/cur，遍历链表，使得cur.next = pre，更新pre和cur
     * @param head
     * @return
     */
    public ListNode rotateList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode pre = null;
        ListNode cur = head;
        ListNode next = null;
        while (cur != null) {
            next = cur.next;
            cur.next = pre;
            pre = cur;
            cur = next;
        }
        return pre;
    }

    /**
     * 将链表后K个节点翻转到前面，例如 1->2->3->4->5  n=2  => 4->5->1->2->3
     * 思路：使用两个指针p/q，通过这两个指针找到后K个节点
     * @param head
     * @param n
     * @return
     */
    public ListNode rotateRight(ListNode head, int n) {
        if (null == head || null == head.next || n == 0) {
            return head;
        }
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode p = dummy;
        ListNode q = dummy;
        for (int i = 0; i < n && q != null; i++) {
            q = q.next;
        }
        //如果q == null，反转链表
        if (q == null) {
            ListNode pre = null;
            ListNode cur = head;
            ListNode next = null;
            while (cur != null) {
                next = cur.next;
                cur.next = pre;
                pre = cur;
                cur = next;
            }
            return pre;
        }
        while (q.next != null) {
            p = p.next;
            q = q.next;
        }
        //head -> p  p.next -> q
        if (p == dummy) {
            return head;
        }
        dummy.next = p.next;
        p.next = null;
        q.next = head;
        return dummy.next;
    }

    class RandomListNode {

        int label;

        RandomListNode random, next;

        RandomListNode(int x) {
            this.label = x;
        }

    }

    /**
     * 给定一个带随机指针的链表，返回其深拷贝
     * 思路：在每个链表节点之后都插入一个相同的节点，那么该拷贝节点的随机指针应该这样设置：cur.next.random = cur.random.next
     *      最后再断开拷贝节点，组成新的链表
     * @param head
     * @return
     */
    public RandomListNode copyRandomList(RandomListNode head) {
        if (null == head) {
            return null;
        }
        //遍历链表，在每个节点之后插入新的节点
        RandomListNode cur = head;
        while (null != cur) {
            RandomListNode node = new RandomListNode(cur.label);
            node.next = cur.next;
            cur.next = node;
            cur = node.next;
        }
        //遍历链表，更改插入节点的random
        cur = head;
        while (null != cur) {
            if (cur.random != null) {
                cur.next.random = cur.random.next;
            }
            cur = cur.next.next;
        }
        //遍历链表，断开原节点和复制节点
        cur = head;
        RandomListNode newHead = cur.next;
        while (null != cur) {
            RandomListNode next = cur.next;
            if (next != null) {
                cur.next = next.next;
            }
            cur = next;
        }
        return newHead;
    }

    /**
     * 对链表进行快速排序
     * @param head
     */
    public void quickSortList(ListNode head, ListNode tail) {
        if (head == tail) {
            return;
        }
        ListNode partitionNode = partition(head, tail);
        quickSortList(head, partitionNode);
        quickSortList(partitionNode.next, tail);
    }

    /**
     * 链表快排分区函数
     * @param head
     * @param tail
     * @return
     */
    private ListNode partition(ListNode head, ListNode tail) {
        ListNode cur = head;
        ListNode next = cur.next;
        while (next != tail) {
            if (next.val <= head.val) { //cur及之前都是小于等于head的节点，cur-next都是大于head的节点
                cur = cur.next;
                swap(cur, next);
            }
            next = next.next;
        }
        swap(head, cur);
        return cur;
    }

    /**
     * 交换节点值
     * @param n1
     * @param n2
     */
    private void swap(ListNode n1, ListNode n2) {
        int temp = n1.val;
        n1.val = n2.val;
        n2.val = temp;
    }

    /**
     * 删除重复节点，例如 1->2->3->3->4  =>  1->2->3->4
     * @param head
     * @return
     */
    public ListNode deleteDuplicates(ListNode head) {
        if (null == head) {
            return head;
        }
        ListNode cur = head;
        ListNode next = null;
        while (cur != null) {
            next = cur.next;
            while (next != null && next.val == cur.val) {
                next = next.next;
            }
            cur.next = next;
            cur = next;
        }
        return head;
    }

    /**
     * 删除重复节点，例如 1->2->2->3->3->4  =>  1->4
     * @param head
     * @return
     */
    public ListNode deleteDuplicate1(ListNode head) {
        if (head == null) {
            return head;
        }
        ListNode dummy = new ListNode(0, head);
        ListNode pre = dummy;
        ListNode cur = pre.next;
        ListNode scan = cur.next;
        while (cur != null) {
            while (scan != null && cur.val == scan.val) {
                scan = scan.next;
            }
            //scan == null || scan.val != cur.val
            if (cur.next == scan) {
                pre = cur;
            } else {
                pre.next = scan;
            }
            cur = scan;
            if (scan != null) {
                scan = scan.next;
            }
        }
        return dummy.next;
    }

    /**
     * 链表分区函数，小于x的在左边，大于x的排在右边
     * @param head
     * @param x
     * @return
     */
    public ListNode partition(ListNode head, int x) {
        ListNode newHead = new ListNode(x);
        newHead.next = head;
        ListNode cur = newHead;
        ListNode pre = cur;
        ListNode next = cur.next;
        while (next != null) {
            if (next.val < x) {
                if (cur.next == next) {
                    pre = cur;
                    cur = next;
                    next = next.next;
                } else {
                    //将next节点插入到cur后面
                    ListNode curNext = cur.next;
                    ListNode nextNext = next.next;
                    pre.next = nextNext;
                    cur.next = next;
                    next.next = curNext;
                    cur = cur.next;
                    next = nextNext;
                }
            } else {
                pre = next;
                next = next.next;
            }
        }
        return newHead.next;
    }

    @Test
    public void test() {
        ListNode head = new ListNode(1, null);
        ListNode n1 = new ListNode(2, null);
        ListNode n2 = new ListNode(3, null);
        ListNode n3 = new ListNode(4, null);
        head.next = n1;
        n1.next = n2;
        n2.next = n3;

//        ListNode node = removeElements(head,5);
//        quickSortList(head, null);
//        deleteDuplicate1(head);
        rotateList2(head, 1, 3);
//        rotateRight(head, 5);
//        removeElements(head, 2);
    }

}
