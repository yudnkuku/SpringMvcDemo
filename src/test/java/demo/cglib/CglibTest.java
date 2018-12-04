package demo.cglib;

import org.junit.Test;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.NoOp;

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
}
