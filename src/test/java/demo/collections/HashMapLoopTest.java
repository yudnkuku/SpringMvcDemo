package demo.collections;

import java.util.HashMap;

public class HashMapLoopTest {

    public static void main(String[] args) {
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(1, 0.75f);
        map.put(1, 11);
        new Thread(new Runnable() {
            @Override
            public void run() {
                map.put(2, 22);
                System.out.println(map.toString());
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                map.put(3, 33);
                System.out.println(map.toString());
            }
        }).start();
    }
}
