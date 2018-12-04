package demo.dynamicProxy;

import org.junit.Test;
import sun.misc.ClassLoaderUtil;

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
}
