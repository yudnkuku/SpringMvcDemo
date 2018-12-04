package demo.cglib;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class DaoProxy implements MethodInterceptor {

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

        System.out.println("Before Method Invoke");
        methodProxy.invokeSuper(o,objects);
        System.out.println("After Method Invoke");
        return o;
    }
}
