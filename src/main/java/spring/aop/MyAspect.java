package spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

//@Aspect
public class MyAspect {

//    @Pointcut("execution(* FooService.*(..))")
    public void pointcut() {}

//    @Before("pointcut()")
    public void beforeAdvice() {
        System.out.println("pointcut()调用之前");
    }

//    @AfterReturning(pointcut = "pointcut()", returning = "retVal")
    public void afterRetureAdvice(Object retVal) {
        System.out.println("pointcut()返回后调用");
        System.out.println("返回值为：" + retVal);
    }

//    @Around("pointcut()")
    public void aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("环绕增强前...");
        Object retVal = pjp.proceed(new Object[]{"Deacon"});
        System.out.println("切点方法调用返回值：" + retVal.toString());
        System.out.println("环绕增强后...");
    }
}
