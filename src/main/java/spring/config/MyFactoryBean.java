package spring.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MyFactoryBean implements FactoryBean<Object>, InitializingBean, DisposableBean {

    //被proxyObj实现的接口名
    private String interfaceName;

    //代理对象
    private Object proxyObj;

    //被代理的对象
    private Object target;

    //销毁方法回调
    public void destroy() throws Exception {
        System.out.println("destroy...");
    }

    //返回bean实例
    public Object getObject() throws Exception {
        return proxyObj;
    }

    //返回工厂bean生产的bean类型
    public Class<?> getObjectType() {
        return proxyObj == null ? Object.class : proxyObj.getClass();
    }

    public boolean isSingleton() {
        return true;
    }

    //初始化回调方法
    public void afterPropertiesSet() throws Exception {
        proxyObj = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{Class.forName(interfaceName)}, new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("method:" + method.getName());
                        System.out.println("Method before...");
                        Object result = method.invoke(target, args);
                        System.out.println("Method after...");
                        return result;
                    }
                });
//        System.out.println("AfterPropertiesSet...");
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }
}
