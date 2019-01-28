package demo.cia;

import org.codehaus.jackson.node.BigIntegerNode;

import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;

public class PrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;

    public PrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!Thread.currentThread().isInterrupted()) {
                queue.put(p = p.nextProbablePrime());
            }
        } catch (InterruptedException e) {
            //设置中断使线程退出
            Thread.currentThread().interrupt();
        }
    }

    void cancel() {
        interrupt();
    }
}
