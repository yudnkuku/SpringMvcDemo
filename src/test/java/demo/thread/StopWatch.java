package demo.thread;

public class StopWatch {

    private long startTime;

    private long stopTime;

    private boolean running;

    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    public void stop() {
        stopTime = System.currentTimeMillis();
        running = false;
    }

    public long getElapsedTime() {
        long elapsedTime = 0L;
        if (running) {
            elapsedTime = System.currentTimeMillis() - startTime;
        } else {
            elapsedTime = stopTime - startTime;
        }
        return elapsedTime;
    }

    public long getElapsedTimeSec() {
        long elapasedSec = 0L;
        if (running) {
            elapasedSec = (System.currentTimeMillis() - startTime) / 1000;
        } else {
            elapasedSec = (startTime - stopTime) / 1000;
        }
        return elapasedSec;
    }

}
