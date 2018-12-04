package demo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AspectExample {

    @Around("demo.Test.print()")
    public void aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("around advice:test()方法执行前");
        Object obj = pjp.proceed();
        System.out.println("around advice:test()方法执行后");
    }
}
