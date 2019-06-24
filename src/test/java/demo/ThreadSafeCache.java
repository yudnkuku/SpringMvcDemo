package demo;

public class ThreadSafeCache {

    private volatile int result;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public static void main(String[] args) {
        ThreadSafeCache cache = new ThreadSafeCache();

        for (int i = 0; i < 8; i++) {
            new Thread(() -> {
                int x = 0;
                while (cache.getResult() < 100) {
                    x++;
                }
                System.out.println(x);
            }).start();
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cache.setResult(200);
    }
}
