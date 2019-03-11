package demo.jmh;

import org.apache.commons.collections.map.HashedMap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@Threads(5)
@State(Scope.Benchmark)
public class JmhTest {

    private static Map<String, String> hashMap = new HashedMap();

    private static Map<String, String> syncHashMap = Collections.synchronizedMap(new HashMap<>());

    private static Map<String, String> conHashMap = new ConcurrentHashMap<>();

    @Setup
    public void setup() {
        for (int i = 0; i < 10000; i++) {
            hashMap.put(String.valueOf(i), String.valueOf(i));
            syncHashMap.put(String.valueOf(i), String.valueOf(i));
            conHashMap.put(String.valueOf(i), String.valueOf(i));
        }
    }

    @Benchmark
    public void hashMapGet() {
        hashMap.get("4");
    }

    @Benchmark
    public void syncMapGet() {
        syncHashMap.get("4");
    }

    @Benchmark
    public void conHashMapGet() {
        conHashMap.get("4");
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(JmhTest.class.getSimpleName())
                .forks(1)
                .jvmArgs("-ea")
                .build();
        new Runner(opts).run();
    }
}
