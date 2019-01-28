package demo.cia;

import java.math.BigInteger;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BrokenPrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;
    private volatile boolean cancelled = false;

    public BrokenPrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!cancelled) {
                queue.put(p = p.nextProbablePrime());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        cancelled = true;
    }

//    void consumePrimes() throws InterruptedException {
//        BlockingQueue<BigInteger> primes = new ArrayBlockingQueue<>(16);
//        BrokenPrimeProducer producer = new BrokenPrimeProducer(queue);
//        producer.start();
//        try {
//            while (needMorePrimes()) {
//                consume(primes.take());
//            }
//        } finally {
//            producer.cancel();
//        }
//    }
}
