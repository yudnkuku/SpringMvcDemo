package spring.bean;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

public class CustomMethodReplacer implements MethodReplacer {

    @Override
    public Object reimplement(Object obj, Method method, Object[] args)
            throws Throwable {
        System.out.println("replacer method");
        return "Custom Replacer Implement";
    }
}
