# Spring 

标签（空格分隔）： Spring

---

## 1 DI(依赖注入) ##
## 1.1 延迟初始化Bean ##
延迟初始化也叫惰性初始化，指不提前初始化`Bean`，而是在只有真正使用时才会创建及初始化`Bean`，配置方式也很简单，在`<bean>`标签上指定`lazy-init`属性值为`true`即可延迟初始化`Bean`

    <bean id="helloApi" class="someClass" lazy-init="true"/>

## 1.2 使用depends-on ##
`depends-on`指定`Bean`初始化和销毁时的顺序，使用`depends-on`属性指定的`Bean`要先初始化完毕后才会初始化当前`Bean`，由于只有`singleton Bean`能被`Spring`管理销毁，所以当指定的`Bean`都是`singleton`时，使用`depends-on`指定的`Bean`才会在指定的`Bean`之后销毁，例如配置方式如下：

    <bean id="helloApi" class="class1"/>
    <bean id="decorator" class="class2" depends-on="helloApi">
        <property name="helloApi"><ref bean="helloApi"/></property>
    </bean>
`decorator`指定了`depends-on`属性为`helloApi`，所以`decorator`在初始化之前需要先初始化`helloApi`，而在销毁`helloApi`之前先要销毁`decorator`

## 1.3 自动装配 ##
**byName**
通过设定`Bean`定义属性`autowire="byName"`，根据名称自动装配，只能用于`setter`注入，比如我们有方法`setHelloApi`，则`byName`方式将会查找名字为`helloApi`的`Bean`并注入。

    <bean id="helloApi" class="class1"/>
    <bean id="bean" class="class2" autowire="byName">
注意，在根据名字注入时，将当前`Bean`自己排除在外，比如在`hello Bean`类中定义了方法`setHello`，则不会注入`hello Bean`。

**byType**
定义`autowire="byType"`，用于`setter`注入方式，如果有多个候选`bean`，则可以使用`primary`属性指定首选`bean`。可以使用`autowire-candidate="false"`让指定`bean`放弃作为自动装配的候选，

    <bean class="class1" primary="true"/>   //设置首选bean
    <bean class="class1" autowire-candidate="false"/>   //取消候选资格

**construc**
设定`autowire="constructor"`用于类型注入构造器参数

不是所有的类型都能自动装配：

 - 不能装配的数据类型：Object、基本数据类型(Date、CharSequence、Number、URI、Class、int)等
 - 通过`<beans>`标签`default-autowire-candidates`属性指定的匹配模式，不匹配的将不能作为自动装配的候选者，例如指定`*Service`，`*Dao`，将只能匹配这些模式的`Bean`作为候选者
 - 通过设置`auwowire-candidate="false"`，设置该`bean`不作为依赖注入的候选者

数组、集合、字典类型的根据类型自动装配和普通类型的自动装配室友区别的：

 - 数组类型、集合接口类型：将根据泛型获取匹配的所有候选者并注入到数组或集合中，如`List<HelloApi> list`将选择所有的`HelloApi`类型`bean`并注入到`list`中，而对于集合的具体实现类型将只有一个候选者，如`ArrayList<HelloApi> list`将会选择一个类型为`ArrayList`的`bean`注入，而不是选择所有的`HelloApi`类型的`bean`注入

## 1.4 方法注入 ##
所谓方法注入其实就是通过配置方式覆盖或者拦截指定的方法，通常通过代理模式实现。`Spring`提供两种方法注入：查找方法注入和方法替换注入。
## 1.4.1 查找方法注入 ##
配置方式：`<lookup-method name="方法名" bean="bean名称"/>`，其中`name`属性指定方法名，`bean`属性指定方法需返回的`bean`，使用改配置实际上使`Spring`拦截了该方法并使用注入的`Bean`替换了方法返回结果。方法注入主要用于处理`singleton`作用域的`bean`需要其他作用域的`bean`时，采用`Spring`查找方法注入方式无需修改任何代码即能获得需要的其他作用域的`bean`

    <bean id="prototypePrinter"
        class="cn.javass.spring.chapter3.bean.Printer" scope="prototype"/> //作用域为prototype的Printer
    <bean id="singletonPrinter"
        class="cn.javass.spring.chapter3.bean.Printer" scope="singleton"/> //作用域为singleton的Printer
    <bean id="helloApi1"                                           class="cn.javass.spring.chapter3.HelloImpl5" scope="singleton">  //作用域为singleton的HelloImpl5
        <property name="printer" ref="prototypePrinter"/>
        <lookup-method name="createPrototypePrinter" bean="prototypePrinter"/>   //查找方法注入prototypePrinter
        <lookup-method name="createSingletonPrinter" bean="singletonPrinter"/>   //查找方法注入singletonPrinter
    </bean>
    <bean id="helloApi2"                                           class="cn.javass.spring.chapter3.HelloImpl5" scope="prototype">  //作用域为prototype的HelloImpl5
        <property name="printer" ref="prototypePrinter"/>
        <lookup-method name="createPrototypePrinter" bean="prototypePrinter"/>   //查找方法注入prototypePrinter
        <lookup-method name="createSingletonPrinter" bean="singletonPrinter"/>   //查找方法注入singletonPrinter
    </bean>

 测试代码：
 

    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("*.xml")；
    System.out.println("=======singleton sayHello======");
    HelloApi helloApi1 = ctx.getBean("helloApi1",HelloApi.class);
    helloApi1.sayHello();
    helloApi1 = ctx.getBean("helloApi1",HelloApi.class);
    helloApi1.sayHello();
    System.out.println("=======prototype sayHello======");
    HelloApi helloApi2 = ctx.getBean("helloApi2",HelloApi.class);
    helloApi2.sayHello();
    helloApi2 = ctx.getBean("helloApi2",HelloApi.class);
    helloApi2.sayHello();

`HelloImpl5`代码：

    public abstract class HelloImpl5 implements HelloApi {
        private Printer printer;
        public void sayHello() {
            printer.print("setter");
            createPrototypePrinter().print("prototype");
            createSingletonPrinter().print("singleton");
        }
        public abstract Printer createPrototypePrinter();
        public Printer createSingletonPrinter() {
            System.out.println("该方法不会被执行")；
            return new Printer();
        }
        public void setPrinter(Printer printer) {
            this.printer = printer;
        }
    }

测试结果：

    ======singleton sayHello======
    setter printer:0
    prototype printer:0
    setter printer:1
    prototype printer:0
    singleton printer:1
    ======prototype sayHello======
    setter printer:0
    prototype printer:0
    setter printer:2
    prototype printer:0
    singleton printer:3
可见：由于`helloApi1`是`singleton`，通过`setter`注入的`printer`是`prototypePrinter`，所以他应该输出`setter printer:0`和`setter printer:1`，而通过`lookup-method`方法注入的`bean`和`helloApi1`的作用域似乎无关系，只和实际注入的`bean`的作用域有关，如例中尽管`helloApi1`的作用域为`singleton`，通过`lookup-method`可以注入`prototype`的`bean`

## 1.4.2 替换方法注入 ##
也叫`MethodReplacer`注入，和查找注入方法不一样的是，他主要用来替换方法体，通过定义一个`MethodReplacer`接口实现，然后如下配置来实现：

    <replaced-method name="方法名" replacer="MethodReplacer实现">
        <arg-type>参数类型</arg-type>
    </replaced-method>
    
首先定义`MethodReplacer`实现，完全替换掉被替换方法的方法体和返回值，再写配置文件。

    <bean id="replacer" class="MethodReplacerImpl"/>
    <bean id="printer" class="Printer">
        <replaced-method name="print" replacer="replacer">
            <arg-type>java.lang.String</arg-type>
        </replaced-method>
    </bean>


## 2 AOP ##
`AOP`能干什么：

 - 用于横切关注点的分离和织入横切关注点到系统
 - 完善OOP
 - 降低组件和模块间的耦合性
 - 使系统容易扩展
 - 由于关注点的分离从而可以获得组件更好的复用

`AOP`基本概念：

 - **连接点**(Jointpoint)：表示需要在程序中插入横切关注点的拓展点，连接点可能是类初始化、方法执行、方法调用、字段调用或处理异常等，`Spring`只支持方法执行连接点
 - **切入点**(Pointcut)：选择一组相关连接点的模式，即可认为是连接点的集合
 - **通知**(Advice)：在连接点上的行为，包括：前置通知(before advice)、后置通知(after advice)、环绕通知(around advice)
 - **方法/切面**(Aspect)：横切关注点的模块化，可以认为是通知、引入和切入点的集合
 - **引入**(inter-type declaration)：也成为内部类型声明，为已有的类添加额外的字段或方法
 - **目标对象**(Target object)：需要被织入横切关注点的对象，即该对象是切入点选择的对象，需要被通知的对象
 - **AOP代理**(AOP Proxy)：AOP框架使用代理模式创建的对象，从而实现在连接点处插入通知(即应用切面)，就是通过代理来对目标对象应用切面，在`Spring`中，`AOP`代理可以用`JDK`动态代理或者`CGLIB`代理实现，而通过拦截器模型应用切面
 - **织入**(Weaving)：织入是一个过程，是将切面应用到目标对象从而创建出`AOP`代理对象的过程，织入可以在编译期、装载期、运行期进行
 
`Spring`通知类型：
 - **前置通知(Before Advice)**：在切入点选择的连接点出的方法之前执行的通知，该
通知不影响正常程序执行流程(除非该通知抛出异常，该异常将中断当前方法链的执行而返回)
 - **后置通知(After Advice)**：在切入点选择的连接点处的方法之后执行的通知，包括如下类型的后置通知：
 - **后置返回通知(After returning advice)**：在连接点处的方法正常执行完毕时执行的通知，必须是连接点处的方法没有抛出任何异常正常返回时才调用后置通知
 - **后置异常通知(After throwing advice)**：在连接点处的方法抛出异常返回时执行的通知，必须是连接点处的方法抛出任何异常返回时才调用异常通知
 - **后置最终通知(After finally advice)**：在连接点处的方法返回时执行的通知，不管有没有抛出异常，类似于finally块
 - **环绕通知(Around advice)**：环绕着连接点处的方法所执行的通知，环绕通知可以再方法调用之前和之后自定义任何行为，并且可以决定是否执行连接点出的方法，替换返回值，抛出异常等

`AOP`代理
`Spring`使用`JDK`动态代理或者`CGLIB`代理来实现`AOP`代理，缺省使用`JDK`动态代理，`AOP`代理的目的就是讲切面织入目标对象

## 2.1 基于Schema的AOP ##
在`Spring`配置文件中，所有`AOP`相关定义必须放在`<aop:config>`标签下，该标签下可以有`<aop:pointcut>`、`<aop:advisor>`、`<aop:aspect>`标签，配置顺序不可变

 - `<aop:pointcut>`：用来定义切入点，该切入点可以重用
 - `<aop:advisor>`：用来定义只有一个切入点和一个通知的切面
 - `<aop:aspect>`：用来定义切面，该切面包含多个切入点和通知，而且标签内部的通知和切入点定义是无序的，和`advisor`的区别在于，`advisor`只能定义一个切入点和通知

## 2.1.1 声明切入点 ##
三种方式：
(1)在`<aop:config>`下声明`<aop:pointcut>`声明一个切入点`bean`，该切入点可以被多个切面使用，该切入点使用`id`属性指定`bean`名字，在通知定义时使用`pointcut-ref`属性通过该`id`引入切入点，`expression`属性指定切入点表达式

    <aop:config>
        <aop:pointcut id="pointcut" expression="execution(* cn.javass..*.*(..))"/>
        <aop:aspect ref="aspectSupportBean">
            <aop:before pointcut-ref="pointcut" method="beafore"/>
        </aop:aspect>
    </aop:config>

(2)在`<aop:aspect>`标签下使用`<aop:pointcut>`声明切入点`bean`

    <aop:config>
        <aop:aspect ref="aspectSupportBean">
            <aop:pointcut id="pointcut" expression="execution(* cn.javass..*.*(..))"/>
            <aop:before pointcut-ref="pointcut" method="before"/>
        </aop:aspect>
    </aop:config>

 (3)匿名切入点，在声明通知时通过指定切入点表达式，该切入点是匿名切入点，只能被该通知使用
 

    <aop:config>
        <aop:aspect ref="aspectSupportBean">
            <aop:before pointcut="execution(* cn.javass..*.*(..))" method="before"/>
        </aop:aspect>
    </aop:config>

## 2.1.2 声明通知 ##
基于`Schema`方式支持前边介绍的五种通知类型：
1、前置通知：在切入点选择的方法之前执行，通过`<aop:aspect>`标签下的`<aop:before>`标签声明：

    <aop:before pointcut="切入点表达式" pointcut-ref="切入点bean引用" 
    method="前置通知方法实现名" arg-names="前置通知实现方法参数列表参数名字"/>

`pointcut`和`pointcut-ref`二者选其一，指定切入点
`method`指定前置通知实现方法名，如果是多态需要加上参数类型，多个用`,`隔开，如`beforeAdvice(java.lang.String)`
`arg-names`指定通知实现方法参数名字，通常绑定到切入点表达式中声明的参数名称，如:

    <aop:before pointcut="execution(* cn.javass..*.*(..)) and args(param)" method="beforeAdvice(java.lang.String)" arg-names="param"/>

2、后置返回通知：在切入点选择的方法正常返回时执行，通过`<aop:aspect>`标签下的`<aop:after-returning>`标签声明

    <aop:after-returning pointcut="切入点表达式" pointcut-ref="切入点bean引用" method="后置返回通知实现方法名" arg-names="后置返回通知实现方法参数列表参数名字" returning="返回值对应的后置返回通知实现方法参数名"/>

`returning`声明的返回值会绑定到`arg-names`同名后置方法参数中

3、后置异常通知：在切入点选择的方法抛出异常是执行，通过`<aop:aspect>`标签下的`<aop:after-throwing>`标签声明

    <aop:after-throwing pointcut="切入点表达式" pointcut-ref="切入点bean引用" method="后置异常通知实现方法名" arg-names="后置异常通知实现方法参数列表参数名字" throwing="将抛出的异常赋值给通知实现方法参数名"/>

4、后置最终通知：在切入点选择的方法返回时执行，不管是正常返回还是抛出异常都执行，通过`<aop:aspect>`标签下的`<aop:after>`标签声明

    <aop:after pointcut="切入点表达式" pointcut-ref="切入点bean引用" method="后置最终通知实现方法名" arg-names="后置最终通知实现方法参数名"/>
    
5、环绕通知：环绕着在切入点选择的连接点处的方法所执行的通知，环绕通知非常强大，可以**决定目标方法是否执行，什么时候执行，执行是否需要替换方法参数，执行完毕是否需要替换返回值**，可通过`<aop:aspect>`标签下的`<aop:around>`标签声明

    <aop:around pointcut="切入点表达式" pointcut-ref="切入点bean引用" method="环绕通知实现方法名" arg-names="环绕通知实现方法名参数名"/>

环绕通知的第一个参数必须是`org.aspectj.lang.ProceedingJoinPoint`类型，在通知实现方法内部使用`ProceedingJoinPoint`的`proceed()`方法使目标方法执行，`proceed()`方法可以传入可选的`Object[]`数组，该数组的值将被作为目标方法执行时的参数
    
## 2.1.3 引入 ##
`Spring`引入允许为目标对象引入新的接口，通过在`<aop:aspect>`标签内使用`<aop:declare-parents>`标签进行引入，定义方式如下：

    <aop:declare-parents types-matching="AspectJ语法类型表达式" implement-interface="引入的接口" default-impl="引入接口的默认实现" default-ref="引入接口的默认实现bean引用"/>

## 2.1.4 Advisor ##
`Advisor`表示只有一个通知和一个切入点的切面，由于`Spring AOP`都是基于`AOP`联盟的拦截器模型的环绕通知的，所以引入`Advisor`来支持各种通知类型(如前置通知等5种)
`Advisor`可以使用`<aop:config>`标签下的`<aop:advisor>`标签来定义

    <aop:advisor pointcut="切入点表达式" pointcut-ref="切入点bean引用" advice-ref="通知API实现"/>
`advice-ref`引用通知实现`bean`，需实现通知接口，如前置通知接口为`MethodBeforeAdvice`

## 2.2 AspectJ切入点语法详解 ##
## 2.2.1 Spring AOP支持的AspectJ切入点指示符 ##
切入点指示符用来指示切入点目的表达式，支持的切入点指示符如下：

 - `execution`：用于匹配方法执行的连接点
 - `within`：用于匹配指定类型内的方法执行
 - `this`：用于匹配当前`AOP`代理对象类型的执行方法，注意是`AOP`代理对象的类型匹配，这样就可能包括引入接口
 - `target`：用于匹配当前目标对象类型的执行方法，不包括引入接口
 - `args`：用于匹配当前传入的参数为指定类型的执行方法
 - `@within`：用于匹配所有持有指定注解类型的方法
 - `@target`：用于匹配当前目标对象类型的执行方法，其中目标对象持有指定的注解
 - `@args`：用于匹配传入的参数持有指定注解的执行方法
 - `@annotation`：用于匹配持有指定注解的执行方法
 - `bean`：匹配特定名称bean对象的执行方法
 - `reference pointcut`：表示引入其他命名切入点
 
## 2.2.3 命名及匿名切入点 ##
命名切入点可以被其他切入点引用，而匿名切入点不可以
只有`@AspectJ`支持命名切入点，而`Schema`风格不支持命名切入点

    @Pointcut(..)
    public void beforePointcut(String param) {}
    @Before(value="beforePointcut(param)", argNames="param")
    public void beforeAdvice(String param) {
        ...
    }

## 2.2.4 类型匹配语法 ##
通配符：

 - `*`：匹配任何数量字符
 - `..`：匹配任何数量字符的重复，如在类型模式中匹配任何数量子包，在方法参数模式中匹配任何数量参数
 - `+`：匹配指定类型的子类型，仅能作为后缀放在类型模式后面
 
几个例子：
 - `java.lang.String`：匹配`String`类型
 - `java.*.String`：匹配java包下任何一级子包下的String类型
 - `java..*`：匹配java包及子包下的任何类型
 - `java.lang.Number+`：匹配Number类的子类
 
匹配表达式类型：
(1)匹配类型：`注解？类名`
(2)匹配方法执行：`注解？修饰符？返回值类型 类型声明?方法名(参数列表)异常列表`(返回值类型和方法名是必须的，如果不限定可以用*代替)
(3)匹配bean名称

## 2.2.5 组合切入表达式 ##
使用且(&&)、或(||)、非(!)来组合切入点表达式，在`Schema`风格下，由于在`xml`中需要使用转义字符来代替，很不方便，`Spring ASP`提供了and/or/not来代替&&/||/！


## 2.3 AOP通知参数 ##
如果想获取被通知方法参数并传递给通知方法，可以有两种实现方法
(1)使用`JoinPoint`获取：`Spring AOP`提供使用`JoinPoint`类型来获取连接点数据，任何通知方法的第一个参数都可以是`JoinPoint`(环绕通知是`ProceedingJoinPoint`,`JoinPoint`的子类)，当然第一个参数位置也可以是`JoinPoint.StaticPart`类型，这个只返回连接点的静态部分

(2)自动获取：通过切入点表达式将对象的参数自动传递给通知方法，在`Spring AOP`中，除了`execution`和`bean`指示符不能传递参数给通知方法，其他指示符都可以将匹配的相应参数或对象自动传递给通知方法

    @Before(value="execution(* test(*)) && args(param)"， argNames="param")
    public void before(String param) {
        ...
    }

 - 首先切入点表达式匹配任何方法名为test，且有一个任何类型的参数
 - args(param)将首先查找通知方法上同名的参数，并在方法执行时匹配传入的参数是使用同名参数类型，即java.lang.String；如果匹配将把该被通知参数传递给通知方法上同名参数

## 4 依赖注入(DI) ##
依赖注入的三种方式：

 - 构造器注入：通过在`bean`定义中指定构造器参数进行依赖注入
 - `setter`注入：通过`setter`方法进行依赖注入
 - 方法注入：通过配置方法替换掉`bean`方法，包括查找方法(`lookup-method`)和替换方法(`replaced-method`)


## 4.1 构造器注入 ##
构造器注入可以通过参数索引注入、参数类型注入或者参数名注入
1、参数索引注入

    <constructor-arg index=0 value="value"/>

2、参数类型注入

    <constructor-arg type="java.lang.String" value="value"/>

3、参数名称注入

    <constructor-arg name="message" value="value"/>
**工厂方式注入**
(1)静态工厂类

    public class StaticFactory {
        public static HelloApi newInstance(String message, int index) {
            return new HelloImpl(message, index);
        }
    }
    
    //bean定义
    <bean id="bean" class="../StaticFactory" factory-method="newInstance"> //定义factory-method属性，工厂方法
        <constructor-arg index="0" value="message"/>
        <constructor-arg index="1" value="1"/>
    </bean>
    //测试直接 HelloApi bean = (HelloApi) ctx.getBean("bean");
    
(2)实例工厂类

    public class StaticFactory {
        public static HelloApi newInstance(String message, int index) {
            return new HelloImpl(message, index);
        }
    }
    
    //bean定义
    <bean id="instanceFactory" class="../StaticFactory">
    <bean id="bean" class="../StaticFactory" factory-bean="intanceFactory" factory-method="newInstance"> //声明factory-bean和factory-method两个属性，工厂bean和工厂方法，工厂bean之前定义过，这里作个引用
        <constructor-arg index="0" value="message"/>
        <constructor-arg index="1" value="1"/>
    </bean>
    //测试方法：HelloApi bean = (HelloApi) ctx.getBean("bean");

## 4.2 setter注入 ##
`setter`注入是在构造器注入实例好`bean`之后，通过调用`bean`类的`setter`方法进行注入依赖，此种方式必须要在目标`bean`类中包含指定属性的`setter`方法

    <bean id="" class="">
        <property name="" value=""/> //setter方式注入属性值
    </bean>

## 4.3 注入集合、数组和字典 ##
(1)`list`&`set` &`collection`

    <bean id="listBean" class="">
        <property name="">
            <list>
                <value>1</value>
                <value>2</value>
            </list>
        </property>
    </bean>
    
## 4.4 引用其他bean ##
使用`ref="beanName"`属性或者`<ref bean="beanName">`子标签
**其他变种`ref`**
`<ref local="">`：引用本容器内部的`bean`
`<ref parent="">`：引用父容器的`bean`，会现在子容器配置文件中找对应`bean`，如果找到就使用，否则才会去父容器配置文件中找对应`bean`。

## 4.5 内部定义bean ##
在`<property>`或者`<constructor-arg>`标签内部通过`<bean>`标签定义的`bean`，该`bean`会有唯一匿名标识符，而且不能指定别名，该内部`bean`对其他外部`bean`不可见,即使是通过`ctx.getBean("beanName")`也不能访问到

    <bean id="" class="">
        <property name="">
            <bean id="" class=""/>  //这个bean只能在此property内部有效
        </property>
    </bean>

## 4.6 配置简写 ##
|功能|全写|简写|
|:-:|:-:|:-:|
|注入常量值|`<constructor-arg index=""><value></value></constructor-arg>`|`<constructor-arg index="" value=""/>`|
|`bean`引用|`<constructor-arg index=""><ref bean=""></ref></constructor-arg>`|`<constructor-arg index="" ref="">`|
|`map`简写|`<map><entry><key><value></value></key><value></value></entry></map>`|`<map><entry key="" value=""/></map>`|

## 4.7 bean定义属性详解 ##
## 4.7.1 depends-on ##
`depends-on`属性指定`bean`初始化和销毁时的顺序，使用`depends-on`属性指定的`bean`要先初始化完毕后才会初始化当前`bean`，由于只有`singleton bean`才能被`Spring`管理销毁，所以当指定的`bean`是`singleton`时，使用`depends-on`属性指定的`bean`要在指定的`bean`之后销毁，总结一下就是`depens-on`属性表明被依赖者在依赖者之前初始化，之后销毁，通常与`init-method`和`destroy-method`两个属性一起使用。
该属性的好处在于可以在`bean`的声明周期内进行资源的准备和释放

## 4.7.2 autowire ##
这个属性具有4个取值，分别是:`default(默认装配)`、`no(不支持自动装配，需要明确指定依赖)`、`byName(根据名称自动装配)`、`byType(通过类型自动装配)`

其中`byType`方式是根据类型来自动装配，当有多个匹配者时可以通过`primary="true"`(确定首选)或者`autowire-candidate="false"`(从候选者中去除)来进一步筛选

通过类型自动注入如果有多个候选者，会将所有的`bean`全部注入到集合中，而对于`Map<String,BeanType>`类型的属性会注入所有`BeanType`类型的`bean`到`map`集合中，键类型必须为`String`，而对于`HashMap<String,BeanType>`类型的属性只会注入`HashMap`的`bean`

## 4.7.3 方法注入 ##
所谓方法注入就是通过配置方式覆盖或者拦截指定方法，通常通过代理模式(`CGLIB`代理)实现，`Spring`提供两种方法注入：查找方法注入和方法替换注入
**1、查找方法注入**
用于注入方法返回结果，也就是说能通过配置方式替换方法方法返回结果，使用`<lookup-method name="方法名" bean="bean名称"/>`配置。
对方法定义有一定格式要求：访问级别必须是`public`或者`protected`，保证子类能被重载，可以使抽象方法，必须有返回值，必须是无参方法

    <public|protected> [absctract] <return-type> theMethodName(no-arguments);
    //注入方式
    <bean id="" class="">
        <lookup-method name="" bean=""/>
    </bean>
查找方法注入主要解决在`singleton`作用域`bean`中注入`prototype`作用域的`bean`

**2、替换方法注入**
也叫`MethodReplacer`注入，通过替换方法体来实现注入

    <bean id="" class=""> 
        <replaced-method name="" replacer="MethodReplacer实现"/>
    </bean>

`replacer`属性指定的内容必须实现`MethodReplacer`接口，必将其声明为`bean`

    <bean id="replacer" class="MethodReplacerImpl"/>
    <bean id="" class="">
        <replaced-method name="" replacer="replacer"/>
    </bean>
    
## 4.8 bean作用域 ##
1、`singleton`：在容器中只会存在一个实例
`Spring`不仅会缓存单例对象(单例缓存池)，`bean`定义也是会缓存的

2、`prototype`：每次都会获取一个全新的`bean`

`Web`中的作用域：
1、`request`：每次请求容器创建一个全新的`bean`，比如每次叫表单数据必须是对每次请求新建一个`bean`来保持这些表单数据，请求结束释放这些数据
2、`session`：表示每个会话需要容器创建一个全新`bean`
3、`globalSession`：类似于`session`作用域，只是其用于`portlet`环境中的`web`应用

## 5、表达式语言(SpEL) ##
表达式语言给`java`静态语言增加了动态功能，`SpEL`支持如下表达式：

 - 基本表达式：字面量、关系、逻辑和算数、三目运算、`Elivis`表达式、正则等
 - 类相关表达式
 - 集合相关表达式：集合、字典访问、列表、集合投影、集合选择
 - 其他表达式：模板表达式

## 5.1 SpEL原理和接口 ##
1、表达式：`Expression`接口
2、解析器：`ExpressionParser`接口
3、上下文：`EvaluationContext`接口
4、根对象及活动上下文对象
`SpEL`表达式默认前缀时`#{`，后缀是`}`，可以参考`org.springframework.contxt.expression.StandardBeanExpressionResolver`，该类实现了`BeanExpressionResolver`接口
**拓展**：可以自定义`SpEL`表达式前后缀，自定义一个类实现`BeanFactoryPostProcessor`接口，该结构提供的回调方法会在任何`bean`进行初始化前就被调用

    //自定义实现BeanFactoryPostProcessor接口
    public class SpELBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException{
            StandardBeanExpressionResolver resolver = (StandardBeanExpressionResolver)beanFactory.getBeanExpressionResolver();
            resolver.setPrefix("*{");
            resolver.setSuffix("}");
        }
    }
    //再在bean定义中将其注册为bean
    <bean class="SpELBeanFactoryPostProcessor"/>
    
**类相关表达式**：使用`T(type)`来表示某个类实例，`type`必须指定类全限定名，`java.lang`包除外

**变量定义及引用**：变量定义通过`EvaluationContext`接口的`setVariable(variableName,value)`方法，在表达式中使用`#variableName`引用，除了引用自定义变量，`SpEL`还允许引用跟对象和当前上下文对象，使用`#root`引用跟对象，使用`#this`引用当前上下文对象。

**集合、字典元素访问**
`SpEL`支持所有集合类型和字典类型的元素访问，使用`集合[索引]`访问集合元素，使用`map[key]`访问字典元素

    int result = parser.parseExpression("{1,2,3}[0]").getValue(int.class);
    int result = parser.parseExpression("#map['key']").getValue(context, int.class);
    
**集合投影**
使用`(list|map).![投影表达式]`进行投影运算，将集合中的值进行`map`操作

    Collection<Integer> result = parser.parseExpression("#collection.![#this+1]").getValue(context,Collection.class);

上述代码将`#collction`指定的集合中的值加1组成新的集合，`#this`代表遍历集合中的每一个元素，可以使用比如`#this.property`来获取集合元素的属性，其中`#this`可以省略

`SpEL`还支持`Map`投影，但是`Map`最终只能得到`List`结果，`#this`指代`Map.Entry`，直接用`value`取代`#this.value`来获取`value`

    List<Integer> result = parser.parseExpression("#map.![value+1]").getValue(context,List.class);
    
**集合选择**
`SpEL`使用选择表达式来选择合适的元素组成新的集合，表达式语法为

    (list|map).?[选择表达式]
选择表达式结果必须是`boolean`类型，`true`添加到新集合中，`false`不添加到新集合中

    Collection<Integer> result = parser.parseExpression("#list.?[#this>4]").getValue(context, Collection.class);
对于字典选择，最终结果还是`Map`，这和投影不同；集合选择可以和集合投影一起使用，如`#map.?[key !='a'].![value+1]`，将首先选出键值不为`a`的，然后再选出的`Map`中进行`value+1`操作

