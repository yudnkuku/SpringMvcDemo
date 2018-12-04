package demo.collections;

import org.junit.Test;

import java.util.HashMap;

public class HashMapTest {

    @Test
    public void test() {
        HashMap<Integer, String> map = new HashMap<>(2);
        map.put(1, "a");
        map.put(3, "b");
        map.put(5, "c");
        System.out.println(map.size());
    }
}
