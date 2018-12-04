package demo.cglib;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class DaoAnotherProxy implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        System.out.println("Start time :" + System.currentTimeMillis());
        methodProxy.invokeSuper(o,objects);
        System.out.println("End time :" + System.currentTimeMillis());
        return o;
    }
}
