package demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.MediaType;
import spring.bean.ClientService;
import spring.bean.LookupMethod;
import spring.entity.User;
import spring.entity.UserWrapper;
import spring.service.HelloService;
import spring.service.StudentService;
import spring.service.TeacherService;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class Test {

    @org.junit.Test
    public void print() {
//        System.out.println("test方法返回" + test());
        System.out.println("test()方法执行");
    }

    public String test() {
        try {
            System.out.println("执行try");
            oneMethod();
            System.out.println("try返回");
            return "正常返回";
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("condition1");
            return "return condition1";
        } catch (Exception e) {
            System.out.println("condition2");
//            return "return condition2";
        } finally {
            System.out.println("finally");
//            return "finally";
        }
        return "抛出异常返回";
    }

    private void oneMethod() throws Exception {
        throw new Exception("exception");
    }

    @org.junit.Test
    public void lazyInit() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/applicationContext.xml");
        for (int i = 0;i < 5;i++) {
            LookupMethod bean = (LookupMethod) ctx.getBean("lookupBean");
            bean.print();
            System.out.println(bean.getValue());
        }
    }

    @org.junit.Test
    public void postProcessorTest() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/applicationContext.xml");
    }


    @org.junit.Test
    public void testRequires() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/applicationContext.xml");
        StudentService studentService = ctx.getBean(StudentService.class);
        TeacherService teacherService = ctx.getBean(TeacherService.class);
        studentService.addStudent();
        teacherService.addTeacher();
    }

    @org.junit.Test
    public void testProxy() {
        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{FactoryBean.class}, new InvocationHandler() {
                    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                        return null;
                    }
                });
        System.out.println(proxy instanceof FactoryBean);
    }

    @org.junit.Test
    public void testFactoryBean() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/applicationContext.xml");
        HelloService helloService = ctx.getBean("myFactoryBean", HelloService.class);
        helloService.sayHello();
        helloService.getName();
    }

    @org.junit.Test
    public void testFactory() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/applicationContext.xml");
        ClientService bean1 = ctx.getBean("factoryMethod", ClientService.class);
        ClientService bean2 = ctx.getBean("beanFactoryMethod", ClientService.class);
    }

    @org.junit.Test
    public void testSystemProperties() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("spring/applicationContext.xml");
        Map<String,String> systemPropertiesBean = (Map<String, String>) ctx.getBean("systemProperties");
        for (Map.Entry<String,String> entry : systemPropertiesBean.entrySet()) {
            System.out.println(entry.getKey() + "->" + entry.getValue());
        }
        System.out.println("============================");
        Map<String,String> systemEnvBean = (Map<String, String>) ctx.getBean("systemEnvironment");
        for (Map.Entry<String,String> entry : systemEnvBean.entrySet()) {
            System.out.println(entry.getKey() + "->" + entry.getValue());
        }
    }

    @org.junit.Test
    public void testList() {
        List<String> list = new ArrayList<>(2);
        list.add("1");
        list.add("2");

        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(),"2")) {
                iterator.remove();
            }
        }

        for (String item :list) {
            System.out.println(item);
        }
    }

    @org.junit.Test
    public void testMapper() {
        User user = new User("yuding", "", "", null);
        UserWrapper userWrapper = new UserWrapper(user);
        User tmp = userWrapper.getUser();
        System.out.println(tmp.getUsername());
        userWrapper.getUser().setUsername("java");
        System.out.println(tmp.getUsername());
    }

    @org.junit.Test
    public void test1() {
        System.out.println(5 >> 1);
    }

    @org.junit.Test
    public void testMediaType() {
        MediaType mt = MediaType.parseMediaType("application/json");
        System.out.println(mt.getType() + "/" + mt.getSubtype());
    }

    @org.junit.Test
    public void testBit() {
        int i = 5;
        System.out.println(i >>> 1);
        System.out.println(i >> 1);
    }

    @org.junit.Test
    public void testHashMapResize() {
        HashMap<Object, Object> map = new HashMap<>();
        map.put(1, 1);
    }


    @org.junit.Test
    public void testLogger() {
        log.debug("this is debug");
        log.info("this is info");
        log.warn("this is warn");
        log.trace("this is trace");
        log.error("this is error");
    }

    @org.junit.Test
    public void testBitCal() {
        System.out.println((-1 << 2));
        System.out.println((-1 << 29));
        System.out.println((0 << 29));
        System.out.println((1 << 29));
        System.out.println((2 << 29));
        System.out.println((3 << 29));
    }

    @org.junit.Test
    public void testBytes2Str() {
        String hexStr = "31440000000000000000000000000000000000000000000000000000000000000000000000000000";
        String tmp = "";
        for (int i = 0; i < hexStr.length() / 2; i++) {
            tmp = tmp + (char) Integer.valueOf(hexStr.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        System.out.println(tmp);
    }

    @org.junit.Test
    public void testHex() {
        System.out.println(Integer.toHexString(2019));
        String hex = Integer.toHexString(2019);
        StringBuilder sb = new StringBuilder();
        if (hex.length() < 4) {
            int space = 4 - hex.length();
            for (int i = 0; i < space; i++) {
                sb.append("0");
            }
            sb.append(hex);
        }
        System.out.println(sb.toString());
    }
    
}
