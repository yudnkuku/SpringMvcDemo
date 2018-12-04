package demo.collections;

import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 测试ArrayList和LinkedList通过for和foreach循环的效率
 * 两者通过foreach循环时间消耗差别不大，但是LinkedList通过for循环遍历明显慢于ArrayList
 * 所以建议LinkedList遍历是不要使用for循环
 */
public class ListTest {

    private static final int SIZE = 100000;

    private void loop(List<Integer> list) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < SIZE; i++) {
            list.get(i);
        }
        System.out.println(list.getClass().getSimpleName() + "用for循环遍历耗时：" +
                (System.currentTimeMillis() - startTime) + "ms");
        startTime = System.currentTimeMillis();
        for (Integer i : list) {

        }
        System.out.println(list.getClass().getSimpleName() + "用foreach循环遍历耗时：" +
                (System.currentTimeMillis() - startTime) + "ms");
    }

    @Test
    public void test() {
        ArrayList<Integer> arrList = new ArrayList<>();
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < SIZE; i++) {
            arrList.add(i);
            linkedList.add(i);
        }
        loop(arrList);
        loop(linkedList);
    }
}
