package demo.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class JenkovJmhDemo {

    @State(Scope.Thread)
    public static class MyState {

        public MyState() {
        }

        private int a;

        @Setup(Level.Trial)
        public void doSetup() {
            a = 1;
            System.out.println("Do Setup");
        }

        @TearDown(Level.Trial)
        public void doTearDown() {
            System.out.println("Do Teardown");
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Threads(5)
    public void testHook(MyState myState) {
        System.out.println(myState.a);
    }


    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(JenkovJmhDemo.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
