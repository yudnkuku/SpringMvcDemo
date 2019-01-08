package spring.web;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import spring.bean.SingletonBean;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:spring/applicationContext.xml"})
public class ScopedProxyTest {

    @Autowired
    private SingletonBean singletonBean;

    @Test
    public void testScopedProxy() {
        singletonBean.printTime();
        System.out.println("========");
        singletonBean.printTime();
    }
}
