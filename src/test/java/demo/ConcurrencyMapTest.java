package demo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrencyMapTest {

    private static final int THREAD_POOL_SIZE = 5;

    private static Map<String,Integer> hashTable = null;

    private static Map<String,Integer> synchronizedMap = null;

    private static Map<String,Integer> concurrentHashMap = null;

    public static void main(String[] args) throws InterruptedException {
        hashTable = new Hashtable<>();
        concurrencyTest(hashTable);

        synchronizedMap = Collections.synchronizedMap(new HashMap<>());
        concurrencyTest(synchronizedMap);

        concurrentHashMap = new ConcurrentHashMap<>();
        concurrencyTest(concurrentHashMap);
    }

    public static void concurrencyTest(Map<String,Integer> map) throws InterruptedException {
        System.out.println("Test started for :" + map.getClass());
        long averageTime = 0;
        for (int i = 0; i < 5; i++) {
            long startTime = System.nanoTime();
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            for (int j = 0; j < THREAD_POOL_SIZE; j++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 500000; i++) {
                            Integer number = (int) Math.ceil(Math.random() * 550000);
                            Integer value = map.get(String.valueOf(number));
                            map.put(String.valueOf(number),number);
                        }
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE,TimeUnit.DAYS);
            long endTime = System.nanoTime();
            long totalTime = (endTime - startTime) / 1000000;
            averageTime += totalTime;
            System.out.println("50000 add/retrieve in " + totalTime + " ms");
        }
        System.out.println("For" + map.getClass() + ",the average time is " + averageTime/5 + "ms\n");
    }
}
