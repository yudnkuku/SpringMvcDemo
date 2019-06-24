package spring.aop;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//@Configuration
//@EnableAspectJAutoProxy
public class AopConfig {

    @Bean
    public FooService fooService() {
        return new FooService();
    }

    @Bean
    public MyAspect myAspect() {
        return new MyAspect();
    }
}
