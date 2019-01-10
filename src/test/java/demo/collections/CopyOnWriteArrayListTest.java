package demo.collections;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListTest {

    @Test
    public void testCowIteratror() {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(new Integer[]{1,10,30,40});
        Iterator<Integer> iterator = list.iterator();
        list.add(100);
        List<Integer> result = new ArrayList<>();
        iterator.forEachRemaining((Integer item) -> {list.add(item);});

    }
}
