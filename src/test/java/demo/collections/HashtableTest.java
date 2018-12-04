package demo.collections;

import org.junit.Test;

import java.util.Hashtable;

public class HashtableTest {

    @Test
    public void test() {
        Hashtable<Object,Object> hashtable = new Hashtable<>(16);
        hashtable.put("name", "yuding");
    }
}
