package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CallableTest {

    public static void main(String[] args) {
        //使用Executors工具类创建缓存线程池
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<String>> resultList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            //提交Callable()任务，call()方法会自动在线程上执行
            Future<String> result = executorService.submit(new CallableTask(i));
            resultList.add(result);
        }

        //遍历结果列表
        for (Future<String> future : resultList) {
            //如果任务没有完成，等待完成
            while(!future.isDone());
            try {
                System.out.println(future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    static class CallableTask implements Callable<String> {

        private int id;

        public CallableTask(int id) {
            this.id = id;
        }

        @Override
        public String call() throws Exception {
            System.out.println("call()方法被调用！！！--->" + Thread.currentThread().getName());
            return "任务返回结果是：" + id + "--->" + Thread.currentThread().getName();
        }
    }
}
