package demo.dynamicProxy;

import org.junit.Test;
import sun.misc.ProxyGenerator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DynamicProxtTest {

    @Test
    public void test() {
        Subject subject = new SubjectImpl();
        MyInvocationHandler myInvocationHandler = new MyInvocationHandler(subject);
        Subject proxy = (Subject) myInvocationHandler.getProxy();
        proxy.add();
    }

    class MyInvocationHandler implements InvocationHandler {

        private Subject target;

        public MyInvocationHandler(Subject target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("---before---");
            Object result = method.invoke(target, args);
            System.out.println("---after---");
            return result;
        }

        public Object getProxy() {
            //获取ClassLoader
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            //获取被代理类实现的接口
            Class[] interfaces = target.getClass().getInterfaces();
            Object proxy = Proxy.newProxyInstance(cl, interfaces, this);
            return proxy;
        }
    }

    @Test
    public void testProxy() {
        String path = "D:/$Proxy0.class";
        byte[] proxyClassFile = ProxyGenerator.generateProxyClass("$Proxy0", SubjectImpl.class.getInterfaces());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(proxyClassFile);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
