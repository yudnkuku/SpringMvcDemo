package demo.cglib;

import org.junit.Test;
import org.springframework.cglib.proxy.*;
import java.lang.reflect.Method;

public class CglibTest {

    @Test
    public void test() {
        DaoProxy daoProxy = new DaoProxy();
        DaoAnotherProxy daoAnotherProxy = new DaoAnotherProxy();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Dao.class);
//        enhancer.setCallback(daoProxy);

        enhancer.setCallbacks(new Callback[] {daoProxy,daoAnotherProxy,NoOp.INSTANCE});
        enhancer.setCallbackFilter(new DaoFilter());
        Dao dao = (Dao) enhancer.create();
        dao.update();
        dao.select();
    }

    class Panda {
        public Panda() {
        }

        public void eat() {
            System.out.println("The panda is eating");
        }
    }

    class CglibProxy implements MethodInterceptor {

        private Object target;

        public Object getInstance(Object target) {
            this.target = target;
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(target.getClass());
            enhancer.setCallback(this);
            return enhancer.create();
        }

        @Override
        public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy)
                throws Throwable {
            System.out.println("方法调用前");
            Object result = methodProxy.invoke(o, objects);
            System.out.println("方法调用后");
            return result;
        }
    }

    @Test
    public void test1() {
        CglibProxy cglibProxy = new CglibProxy();
        Panda proxy = (Panda) cglibProxy.getInstance(new Panda());
        proxy.eat();
    }


}
