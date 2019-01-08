# Spring MVC学习笔记

标签（空格分隔）： SpringMVC

---

1、Container容器简介
---------------

    

`org.springframework.context.ApplicationContext`代表`Spring IoC`容器，负责初始化、配置和组装定义的`bean`，配置元数据为上述操作提供了依据（`configuration metadata`）,配置元数据通常包括`xml`、`java`注释或者`java`代码

1.1、初始化容器
---------

```
    //从CLASSPATH中加载services.xml和daos.xml两个配置文件
    ApplicationContext context =
        new ClassPathXmlApplicationContext(new String[] {"services.xml", "daos.xml"});   
```

`services.xml`:
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- services -->

    <bean id="petStore" class="org.springframework.samples.jpetstore.services.PetStoreServiceImpl">
        <property name="accountDao" ref="accountDao"/>
        <property name="itemDao" ref="itemDao"/>
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>

        <!-- more bean definitions for services go here -->
    
</beans>
```
`daos.xml`:
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="accountDao"
        class="org.springframework.samples.jpetstore.dao.jpa.JpaAccountDao">
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>

    <bean id="itemDao" class="org.springframework.samples.jpetstore.dao.jpa.JpaItemDao">
        <!-- additional collaborators and configuration for this bean go here -->
    </bean>

    <!-- more bean definitions for data access objects go here -->

</beans>
```

1.2 使用容器
--------

 `ApplicationContext`维持了不同的bean和对应依赖的注册信息，使用方法`T getBean(String name, Class<T> requiredType)`能够获取注册的bean

   



```
    // create and configure beans
    ApplicationContext context = new ClassPathXmlApplicationContext("services.xml", "daos.xml");
    
    // retrieve configured instance
    PetStoreService service = context.getBean("petStore", PetStoreService.class);
    
    // use configured instance
    List<String> userList = service.getUsernameList();
```   

## 1.3 环境抽象(Environment Abstraction)##
## 1.3.1 PropertySource抽象##
`Spring`的环境抽象提供了对可配置的层级属性源的搜索操作，一个简单的例子判断环境中是否存在`foo`属性

    ApplicationContext ctx = new GenericApplicationContext();
    Environment env = ctx.getEnvironment();
    boolean containsFoo = env.containsProperty("foo");
    System.out.println("Does my environment contain the 'foo' property? " + containsFoo);
`PropertySource`是键值对的简单抽象，`Spring`的`StandardEnvironment`配置了两个属性源--一个代表JVM系统属性集(`System.getProperties()`)，另一个表示系统环境变量集(`System.getenv()`)。搜索操作是具有层级性的，默认系统属性优先级高于环境变量，对于`StandardServletEnvironment`，完整的层级顺序由高到低如下所示：

 - `ServletConfig`参数
 - `ServletContext`参数(web.xml context-param)
 - `JNDI`环境变量("`java:comp/env/`")
 - `JVM`系统属性("`-D`"命令行参数)
 - `JVM`系统环境变量(操作系统环境变量)
 
整个搜索机制优先级是可以配置的，例如我要添加自定义的属性源作为最高优先级

    ConfigurableApplicationContext ctx = new GenericApplicationContext();
    MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
    sources.addFirst(new MyPropertySource());

@PropertySource
---------------
`@PropertySource`可以往Spring环境中添加属性源，如：
假设文件`"app.properties"`中包含键值对：`testbean.name=myTestBean`，下面的`@Configuration`类会使用`@PropertySource`，调用`testBean.getName()`返回`"myTestBean"`

    @Configuration
    @PropertySource("classpath:/com/myco/app.properties")
    public class AppConfig {
    
        @Autowired
        Environment env;
    
        @Bean
        public TestBean testBean() {
            TestBean testBean = new TestBean();
            testBean.setName(env.getProperty("testbean.name"));
            return testBean;
        }
    }

任何在`@PropertySource`资源定位中使用的`${...}`占位符都将被解析为已经被注册到环境中属性源，例如：

    @Configuration
    @PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
    public class AppConfig {

    @Autowired
    Environment env;

    @Bean
    public TestBean testBean() {
        TestBean testBean = new TestBean();
        testBean.setName(env.getProperty("testbean.name"));
        return testBean;
    }
    }
假如`my.placeholder`已经被注册到属性源中，例如系统属性或者环境变量，那么该占位符就会被解析成对应的值，否则`"default/path"`就会被作为默认值使用，如果没有指定默认值，那么会抛出`IllegalArgumentException`异常
## 1.4 bean作用域 ##
`bean`的作用域：
|作用域|描述|
|:--:|:--:|
|`singleton`|(默认)表明对于每个`Spring IoC`容器，一个`bean`定义对应一个对象实例|
|`proptotype`|一个`bean`定义可以对应多个对象实例|
|`request`|每个`HTTP`请求对应不同的对象实例，仅在`web`环境下有效|
|`session`|每个`HTTP`会话对应对象实例，仅在`web`环境下有效|
|`application`|每个`ServletContext`下对应对象实例，仅在`web`环境下有效|
|`websocket`|每个`WebSocket`下对应对象实例，仅在`web`环境下有效|
`request`、`session`、`application`、`websocket`四种作用域类型仅在`web`环境中才有效，例如`XmlWebApplicationContext`，如果在普通容器中使用这些作用域，会爆出`IllegalStateException`异常。
当高级别作用域`bean`注入到低级别作用域`bean`时，会出现`bean`的作用域协同问题，例如将`session`作用域的`bean`注入到`singleton`作用域的`bean`中

    

    <bean id="userPreferences" class="com.foo.UserPreferences" scope="session"/>
    
    <bean id="userManager" class="com.foo.UserManager">
        <property name="userPreferences" ref="userPreferences"/>
    </bean>

由于`userManager`是`singleton`的，每个容器只会实例化一个实例，因此它的依赖`userPreferences`也只会注入一次，这意味着`userPreferences`的作用域成了`singleton`，这显然不是我们想要的结果。
这个时候需要注入代理类，这个代理类实际上就是个`UserPreferences`实例（和`UserPreferences`拥有同样的公共接口），容器将这个代理对象注入到`userManager`中，但`userManager`并不知道注入的是代理类，因此，在`UserManager`每次调用注入的`UserPreferences`的方法时，实际上调用的是代理对象上的方法，代理对象从`HTTP Session`中获取真正的`UserPreferences`对象，并将方法调用代理给真正的`UserPreferences`对象。

    

    <bean id="userPreferences" class="com.foo.UserPreferences" scope="session">
        <aop:scoped-proxy/>
    </bean>
    
    <bean id="userManager" class="com.foo.UserManager">
        <property name="userPreferences" ref="userPreferences"/>
    </bean>

使用`<aop:scoped-proxy/>`创建代理时，实际上创建了基于`CGLIB`类的代理，这个代理只会拦截公共方法调用，因此不要在此代理上调用非公共方法。

## 1.5 自定义bean ##
## 1.5.1 生命周期回调函数 ##
为了便于和`bean`生命周期的容器管理交互，可以实现`InitializingBean`和`DisposableBean`接口，容器通过调用`afterPropertiesSet()`和`destroy()`方法，允许`bean`在初始化和回收时执行某些操作。
`Spring`框架内部通过`BeanPostProcessor`实现来处理能找到的任何回调接口并调用对应的方法。
## 1.5.2 回调函数 ##
`InitializingBean`接口允许`bean`在所有必须的属性配置好后进行初始化工作，它包含以下方法：

    void afterPropertiesSet() throws Exception;

但不建议实现`InitializingBean`接口，会增加和`Spring`的耦合度，可以使用注解`@PostConstruct`或者指定初始化方法取而代之：

    public class ExampleBean {
        public void init() {
            // do some initialization work
        }
    }

`DisposableBean`包含如下方法：

    void destroy() throws Exception;

## 1.5.3 结合三种生命周期回调 ##
有三种方式控制生命周期行为：`InitializingBean`和`DisposableBean`、自定义`init()`和`destroy()`方法、使用`@PostConstruct`和`@PreDestroy`注解。
多个机制可以应用在同一个`bean`上，但是有调用顺序：

 - `@PostConstruct`和`@PreDestroy`注解
 - `afterPropertiesSet()`和`destroy()`方法
 - 自定义`init()`和`destroy()`方法
 
## 1.5.4 ApplicationContextAware和BeanNameAware ##
当`ApplicationContext`创建了一个实现了`ApplicationContextAware`的实例，该实例就拥有了指向`ApplicationContext`的引用。

    public interface ApplicationContextAware {
        void setApplicationContext(ApplicationContext applicationContext) throws BeansException;
    }

这意味着，`bean`可以反过来操作创建他们的`ApplicationContext`,

## 1.6 容器拓展 ##
`Spring IoC`容器可以通过插入某些特殊的集成接口进行拓展
## 1.6.1 使用BeanPostProcessor定制Bean ##
`BeanPostProcessor`定义了两个回调函数，通过实现这些回调函数可以提供自己的初始化逻辑、依赖解析逻辑等。
`ApplicationContext`回自动检测定义在配置元数据中并实现了`BeanPostProcessor`接口的`bean`，并将它们注册成`post-processors`，以便于能在`bean`已创建立即调用回调函数，`Bean post-processor`就像其他`bean`一样可以被部署到容器中。
当在配置类中使用`@Bean`注解的工厂方法声明一个`BeanPostProcessor`时，返回类型一定是`BeanPostProcessor`本身或者实现类，否则无法被`ApplicationContext`自动检测到，因为`BeanPostProcessor`需要更早被初始化以便用于其他`bean`的初始化过程。
`Note`:
尽管推荐注册`BeanPostProcessor`的方式是通过`ApplicationContext`的自动探测，但我们仍可以通过`ConfigurableBeanFactory`的`addPostProcessor`方法来完成注册，这对于在注册之前计算条件逻辑十分有效，或者跨上下文环境复制`bean post`处理器，通过此种方式注册的`bean post`处理器的执行顺序和其注册顺序相关，并且均在自动探测的`bena post`处理器之前执行
`BeanPostProcessor`源码：
    
        //在初始化回调方法之前调用。例如InitializingBean的afterPropertiesSet()或者自定义的init-method
        Object postProcessBeforeInitialization(Object bean,String beanName) throws BeansException;
        //在初始化回调方法之后调用。例如InitializingBean的afterPropertiesSet()或者自定义的init-method
        Object postProcessAfterInitialization(Object bean,String beanName) throws BeansException;

## 1.6.2 使用BeanFactoryPostProcessor定制配置元数据 ##
`Spring IoC`容器允许`BeanFactoryPostProcessor`操作`bean`的配置元数据。
当`bean factory post-processor`在`ApplicationContext`内部声明时，其会被自动执行，目的是为了将改变应用到定义了容器的配置元数据上，`Spring`包含了一些列预定义的`bean factory post-processor`，例如`ProperOverrideConfiguer`和`PorpertyPlaceholderConfigurer`，同时也可以自定义`BeanFactoryPostProcessor`
`ApplicationContext`会自动检测实现了`BeanFactoryPostProcessor`的`bean`，并将其部署到容器内部
**使用PropertyPlaceholderConfigurer**
`Note`：
如果你想改变实际的`bean`实例(通过配置元数据创建的对象)，应该使用`BeanPostProcessor`，而`BeanFactoryPostProcessor`则作用于容器中的`bean`定义，`Spring`包含了一系列预定义的`bean factory post-processor`，例如`PropererrideConfigurer`和`PropertyPlaceholderConfiguer`
使用`PropertyPlaceholderConfiguer`从`Properties`格式化的文件中导入第三方属性值


    <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="locations" value="classpath:com/foo/jdbc.properties"/>
    </bean>
    
    <bean id="dataSource" destroy-method="close"
            class="org.apache.commons.dbcp.BasicDataSource">
        <property name="driverClassName" value="${jdbc.driverClassName}"/>
        <property name="url" value="${jdbc.url}"/>
        <property name="username" value="${jdbc.username}"/>
        <property name="password" value="${jdbc.password}"/>
    </bean>
    
属性配置文件：

    jdbc.driverClassName=org.hsqldb.jdbcDriver
    jdbc.url=jdbc:hsqldb:hsql://production:9002
    jdbc.username=sa
    jdbc.password=root

因此，`${jdbc.username}`会在运行期间被`sa`替换，`PropertyPlaceholderConfigurer`会检查`bean`定义中大部分的`placeholders`。另外，`placeholder`的前缀后缀可以自定义。
`PropertyPlaceholderConfigurer`不仅会寻找`Properties`文件中的属性，如果无法再指定属性文件中找到某个属性，默认还会检查`Java`系统属性，可以通过设置`systemPropertiesMode`来调整，支持三种模式：

 - `never(0)`:不检查系统属性
 - `fallback(1)`:如果无法从属性文件中找到则检查系统属性(默认值)
 - `override(2)`:首先检查系统属性，允许系统属性覆盖属性源内的值

## 1.7 使用FactoryBean定制初始化逻辑 ##
实现`FactoryBean`接口的对象本身就是一个工厂。
`FactoryBean`提供了三种方法：

 - `Object getObject()`:返回该工厂创建的对象实例
 - `boolean isSingleton()`:`getObject()`返回对象是否是同一个，即单例
 - `Class getObjectType()`:返回`getObject()`返回对象类型

## 1.8 基于注解的容器配置 ##
## 1.8.1 @Required ##
方法级注解(通常是`setter`方法)，表明注解的`setter`方法参数值必须在配置期间就要装配，或通过显式属性值定义，或通过自动装配。但是不关心配置的值是否为`null`

## 1.8.2 @Autowired ##
`@Autowired`可以注解构造函数、`setter`函数、成员域(在`bean`的构造之后立即注入，在所有配置方法调用之前)或者普通配置方法，对于集合或者`Map`，将会自动装配所有匹配类型的`bean`，对于`Map`，其`key`必须是声明成`String`，并且会被解析成`bean`的名称
默认，如果没有候选`bean`，自动装配操作会失败，可以设置其`required=false`来改变策略：

    public class SimpleMovieLister {
        private MovieFinder movieFinder;
        
        @Autowired(required=false)
        public void setMovieFinder(MovieFinder movieFinder) {
            this.movieFinder = movieFinder;
        }
    }
`required`属性更优于`@Required`注解。
可以使用`@Autowired`来注解一些知名的可解析的依赖：`BeanFactory`、`ApplcationContext`、`Environment`、`ResourceLoader`、`ApplicationEventPublisher`、`MessageSource`，这些接口和其拓展接口，如：`ConfigurableApplicationContext`或者`ResourcePatternResolver`都会自动解析，不需要特殊的设置。

    public class MovieRecommender {
    
        @Autowired
        private ApplicationContext context;
    
        public MovieRecommender() {
        }
    
        // ...
    }

**使用@Primary控制@Autowired细粒度**
`@Autowired`是依据类型自动导入，会导致多个候选`bean`，通常需要额外的控制，其中一种方式就是注解`@Primary`。
`配置类`：

    @Configuration
    public class MovieConfiguration {
    
        @Bean
        @Primary
        public MovieCatalog firstMovieCatalog() { ... }
    
        @Bean
        public MovieCatalog secondMovieCatalog() { ... }
    
        // ...
    }

`@Autowired`:

    public class MovieRecommender {
    
        @Autowired
        private MovieCatalog movieCatalog;
    
        // ...
    }

**使用@Qualifier控制@Autowired细粒度**
使用`@Qualifier("name")`即可筛选符合`name`的`bean`。

    public class MovieRecommender {
    
        private MovieCatalog movieCatalog;
    
        private CustomerPreferenceDao customerPreferenceDao;
    
        @Autowired
        public void prepare(@Qualifier("main")MovieCatalog movieCatalog,
                CustomerPreferenceDao customerPreferenceDao) {
            this.movieCatalog = movieCatalog;
            this.customerPreferenceDao = customerPreferenceDao;
        }
    
        // ...
    }
在配置文件中定义了多个`bean`：

    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:context="http://www.springframework.org/schema/context"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
            http://www.springframework.org/schema/beans/spring-beans.xsd
            http://www.springframework.org/schema/context
            http://www.springframework.org/schema/context/spring-context.xsd">
    
        <context:annotation-config/>
    
        <bean class="example.SimpleMovieCatalog">
            <qualifier value="main"/>
    
            <!-- inject any dependencies required by this bean -->
        </bean>
    
        <bean class="example.SimpleMovieCatalog">
            <qualifier value="action"/>
    
            <!-- inject any dependencies required by this bean -->
        </bean>
    
        <bean id="movieRecommender" class="example.MovieRecommender"/>
    
    </beans>

## 1.8.3 @Resource ##
`@Resource`注解接受一个名称属性，可以注解类型、成员域、方法，`Spring`默认会将其翻译成注入的`bean`名称，换句话说，该注解符合`by-name`语义。
如果没有指定名称，默认名称来源于域名称或者`setter`方法，对于域，会采取其域名，对于`setter`方法，会采取`bean`的属性名，而对于类型，则必须指定名称。

## 1.9 类扫描和管理组件 ##
## 1.9.1 @Component和其他stereotype注解 ##
四大注解：

 - `@Component`
 - `@Service`:业务层
 - `@Controller`:展现层，注解控制器
 - `@Repository`:持久层，注解`DAO`类(数据访问对象)
其中`@Service`、`@Repository`、`@Controller`是组合注解(`Composed annotation`，由多个元注解组成)，均采用`@Component`元注解(`Meta-annotation`)

## 1.9.2 自动扫描类和注册bean定义 ##
`Spring`会自动扫描4大注解，并将对应的`bean`定义注册到`ApplicationContext`中

    @Service
    public class SimpleMovieLister {
    
        private MovieFinder movieFinder;
    
        @Autowired
        public SimpleMovieLister(MovieFinder movieFinder) {
            this.movieFinder = movieFinder;
        }
    }
在`@Configuration`注解类上加上`@ComponentScan`注解，并指定`basePackages`

    @Configuration
    @ComponentScan(basePackages = "org.example")
    public class AppConfig  {
        ...
    }

## 1.9.3 使用过滤器自定义扫描 ##
定义注解`@ComponentScan`中的`includeFilter`和`excludeFilter`来包含/排除某些特定的类
`Filter Types`列表：
|`Filter Type`|例子|目标|
|:-:|:-:|:-:|
|`FilterType.ANNOTATION`(默认)|`@Annotation`|含有该注解的组件|
|`FilterType.ASSIGNABLE`|`SomeClass`|继承或实现指定类的组件|
|`FilterType.ASPECT`|||
|`FilterType.REGEX`|正则表达式|符合正则表达式的组件类名|
|`FilterType.CUSTOM`|自定义|实现了`TypeFilter`接口的类|
如下，表示忽略所有`@Repository`注解类，使用`stub repository`。

    @Configuration
    @ComponentScan(basePackages = "org.example",
            includeFilters = @Filter(type = FilterType.REGEX, pattern = ".*Stub.*Repository"),
            excludeFilters = @Filter(Repository.class))
    public class AppConfig {
        ...
    }

## 1.9.4 在components内部定义bean元数据 ##
`Spring component`内部也可以定义`bean`的元数据，这和在`Configuration`内部定义`bean`元数据一样

    @Component
    public class FactoryMethodComponent {
    
        @Bean
        @Qualifier("public")
        public TestBean publicInstance() {
            return new TestBean("publicInstance");
        }
    
        public void doWork() {
            // Component method implementation omitted
        }
    }

## 1.9.5 命名自动探测组件 ##
当一个组件被自动探测时，他的名称会由`BeanNameGenerator`生成，默认的，四大注解的`value`属性都会被用作对应`bean`定义的名称。
如果没有定义`value`属性，则会生成首字母小写的非全类名作为`bean`名称。
如下：会生成`myMovieLister`和`movieFinderImpl`

    @Service("myMovieLister")
    public class SimpleMovieLister {
        // ...
    }
    @Repository
    public class MovieFinderImpl implements MovieFinder {
        // ...
    }
当然可以通过实现`BeanNameGenerator`来自定义名称生成策略，但是一定要包含无参构造函数，并且在`@ComponentScan`中定义`nameGenerator`属性。
`MyNameGenerator`实现了`BeanNameGenerator`接口

    @Configuration
    @ComponentScan(basePackages = "org.example", nameGenerator = MyNameGenerator.class)
    public class AppConfig {
        ...
    }

## 1.10 基于Java的容器配置 ##
## 1.10.1 基本概念：@Bean和@Configuration ##
`@Bean`方法级注解，用来标识一个方法具有实例化、配置和初始化对象，并托管给`Spring IoC`容器。
`@Configuration`类级注解，表明该类是一个`bean`定义源，`@Configuration`注解类允许通过调用其他`@Bean`方法定义`bean`间依赖

    @Configuration
    public class AppConfig {
    
        @Bean
        public MyService myService() {
            return new MyServiceImpl();
        }
    }

## 1.10.2 使用AnnotationConfigApplicationContext初始化Spring容器 ##
和使用`Spring XML`文件作为输入初始化`ClassPathXmlApplicationContext`一样，`@Configuration`注解类也可以用作初始化`AnnotationConfigApplicationContext`的输入：

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        MyService myService = ctx.getBean(MyService.class);
        myService.doStuff();
    }
使用无参构造方法，并调用`register()`方法进行配置：

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(AppConfig.class, OtherConfig.class);
        ctx.register(AdditionalConfig.class);
        ctx.refresh();
        MyService myService = ctx.getBean(MyService.class);
        myService.doStuff();
    }

## 1.10.3 使用@Bean注解 ##
`@Bean`是方法级注解，`@Bean`注解的方法返回值会作为`bean`定义被注册到`ApplicationContext`中，默认`bean`名称和方法名一样：

    @Configuration
    public class AppConfig {
    
        @Bean
        public TransferServiceImpl transferService() {
            return new TransferServiceImpl();
        }
    }
**接受生命周期回调函数**
任何注解`@Bean`的类都支持常规的回调函数，并且可以使用`@PostConstruct`和`@PreDestroy`注解
也支持常规的`Spring`生命周期回调函数，如果一个`bean`实现了`InitializingBean`、`DisposableBean`或者`Lifecycle`，它们对应的回调方法也会被容器调用。
一些标准的`*Aware`接口，例如`BeanNameAware`、`BeanFactoryAware`等也支持。

**指定bean作用域**
1、使用`@Scope`注解

    @Configuration
    public class MyConfiguration {
    
        @Bean
        @Scope("prototype")
        public Encryptor encryptor() {
            // ...
        }
    }

2、使用`Scope`和`scope-proxy`
类似于在`xml`文件中采用`<aop:scope-proxy/>`标签一样，在`java`配置中可以使用`@Scope`标签，并指定`proxyMode`属性，有三种取值:`ScopedProxyMode.NO`、`ScopedProxyMode.TARGET_CLASS`、`ScopedProxyMode.INTERFACES`.

    @Bean
    @SessionScope
    public UserPreferences userPreferences() {
        return new UserPreferences();
    }
    
    @Bean
    public Service userService() {
        UserService service = new SimpleUserService();
        // a reference to the proxied userPreferences bean
        service.setUserPreferences(userPreferences());
        return service;
    }
注解`@SessionScope`是一个组合注解,表示作用在`session`作用域，并且指明`proxyMode`为`ScopedProxyMode.TARGET_CLASS`：

    @Scope(WebApplicationContext.SCOPE_SESSION)//@Scope("session")
    public @interface SessionScope {
        ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
    }

## 1.10.3 使用@Configuration注解 ##
`@Configuration`是一个类级注解，通过定义公共`@Bean`方法，表明一个类是`bean`定义源，在`@Configuration`内部调用`@Bean`方法能够定义`bean`间依赖，如：

    @Configuration
    public class AppConfig {
    
        @Bean
        public Foo foo() {
            return new Foo(bar());
        }
    
        @Bean
        public Bar bar() {
            return new Bar();
        }
    }
以上`bean`间依赖声明只能在`@Configuration`类内部，不能在`@Component`类内部。

## 1.10.4 组合java配置 ##
使用`@Import`注解来加载其他配置类中的`@Bean`定义

    @Configuration
    public class ConfigA {
    
        @Bean
        public A a() {
            return new A();
        }
    }
    
    @Configuration
    @Import(ConfigA.class)
    public class ConfigB {
    
        @Bean
        public B b() {
            return new B();
        }
    }
当实例化上下文时，仅需要提供`ConfigB`，而不需要提供`ConfigA`：

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigB.class);
        A a = ctx.getBean(A.class);
        B b = ctx.getBean(B.class);
    }
这种方式简化了容器的实例化。

## 1.11 BeanFactory ##
`BeanFactory`提供了`Spring IoC`的基础函数功能，但是仅能直接用于和第三方框架进行整合，`BeanFactory`和相关的接口，例如`BeanFactoryAware`、`InitializingBean`、`DisposableBean`由于需要与大量整合`Spring`的第三方框架进行后向兼容依然存在于`Spring`体系中。
## 1.11.1 BeanFactory或者ApplicationContext ##
由于`ApplicationContext`包含了`BeanFactory`的所有功能，因此推荐使用`ApplicationContext`，除了一些诸如运行在资源限制设备上的嵌入式应用，这些应用中一些`kb`的消耗可能有很大影响，对于大多数企业应用和系统，`ApplicationContext`是当之无愧的首选，`Spring`使用了大量的`BeanPostProcessor`拓展点(使能代理)，如果你是用普通的`BeanFactory`，诸如事务和`AOP`均不会起作用。
|特性|`BeanFactory`|`ApplicationContext`|
|:-:|:-:|:-:|
|`bean`的初始化和装配|`yes`|`yes`|
|``BeanPostProcessor`自动注册`|`no`|`yes`|
|`BeanFactoryPostProcessor`自动注册|`no`|`yesy`|
|`MessageSource`访问|`no`|`yes`|
|`ApplicationEvent`发布|`no`|`yes`|
使用`BeanFactory`实现显式注册`bean post-processor`，需要如下代码：

    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
    // populate the factory with bean definitions
    
    // now register any needed BeanPostProcessor instances
    MyBeanPostProcessor postProcessor = new MyBeanPostProcessor();
    factory.addBeanPostProcessor(postProcessor);
    
    // now start using the factory

## 2、资源（Resource） ##
标准的`java.net.URL`类和标准的`URL`前缀处理函数对于低级别的资源访问并不是很充分，比如，没有标准化的`URL`实现用来访问需要从类路径获取或者相对于`ServletContext`的资源，尽管可以注册处理函数处理特殊的`URL`前缀，但是这很复杂，并且`URL`接口缺少一些必要的功能，例如检查所指向资源是否存在的方法。
`Spring`的`Resource`接口就是用来实现这些低级别资源的访问

    public interface Resource extends InputStreamSource {

    boolean exists();

    boolean isOpen();

    URL getURL() throws IOException;

    File getFile() throws IOException;

    Resource createRelative(String relativePath) throws IOException;

    String getFilename();

    String getDescription();

    }
值得注意的是，`Resource`接口并不确定功能，而是包装，例如：`UrlResource`包装了一个`URL`，并且使用被包装的`URL`去执行工作。
## 2.1 内置资源实现(Built-int Resource implementations)##
## 2.1.1 UrlResource 
`UrlResource`可以用来访问任何能够通过`url`访问的对象，包括文件、`http`对象、`ftp`对象等，所有的`URLs`都包涵了标准的字符串表示形式。`UrlResouce`可以有`UrlResource`构造器显式生成，或者在调用以代表路径的字符串为参数的API方法时隐式生成，后者最终由`PropertyEditor`决定生成哪种`Resource`,如果字符串参数中包含前缀如：`classpath:`，则会创建对应的`Resouce`，否则默认创建`UrlResource`实例
        
## 2.1.2 ClassPathResource ##

表示应该从类路径中获取的资源，当字符串参数中包含前缀：`classpath:`时可以隐式创建`ClassPathResouce`实例

## 2.1.3 FileSystemResouce ##
        加载文件资源

## 2.1.4 ServletContextResource ##

## 2.1.5 InputStreamResource ##
        给定`InputStream`的`Resource`实现

## 2.1.6 ByteArrayResource ##
        给定字节数组的`Resource`实现



## 2.2 The ResourceLoader ##
实现`ResourceLoader`接口必须返回`Resource`实例
    
```
    public interface ResourceLoader {

        Resource getResource(String location);

    }
```
所有的应用上下文都实现了`ResourceLoader`接口，因此他们都可以获取`Resource`实例，如果location参数未指定前缀，我们会获取和上下文一致的`Resource`类型，例如在`ClassPathXmlApplicationContext`实例获取`ClassPathResource`:
```
    Resource template = ctx.getResource("some/resource/path/myTemplate.txt");
```
同样在`FileSystemXmlApplicationContext`实例上可以获取`FileSystemResource`，在`WebApplicationContext`上可以获取`ServletContextResource`，当然可以在`location`参数中添加特性的前缀获取指定的`Resource`，例如在`ClassPathXmlApplicationContext`上获取`UrlResource`：

    Rsource template = ctx.getResource("file:///some/resource/path/myTemplate.txt");

## 2.3 Application contexts and Resource paths ##
## 2.3.1 构造应用上下文 ##
应用上下文构造器通常接受一个字符串或者字符串数组作为资源文件的`location path`，当`location path`中没有定义前缀时，会根据上下文类型加载对应的`Resource`,例如：
    
```
    ApplicationContext ctx = new ClassPathXmlApplicationContext("conf/appContext.xml")；
```
`bean`定义会从指定类路径中加载，此时`Resource`类型为`ClassPathResource`。
如果构造一个`FileSystemXmlApplicationContext`


----------
```
    ApplicationContext ctx = new FileSystemXmlApplicationContext("conf/appContext.xml")；
```     
则会从文件系统中加载指定路径的资源文件（相对于工作目录）


----------
```
    ApplicationContext ctx = new FileSystemXmlApplicationContext("classpath:conf/appContext.xml")；
```
此时构建了一个`FileSystemXmlApplicationContext`，但是其bean定义从类路径中加载，如果随后改上下文作为`ResourceLoader`（所有的`applicationContext`均是`ResourceLoader`）加载其他资源文件时，未指定前缀的路径依然被视为文件系统路径

## 2.3.2 应用上下文构造函数资源路径中的通配符 ##
应用上下文构造器中的资源路径可能是一个简单的路径，和目标资源一一对应，可能含有`classpath*:`前缀或者`Ant`风格的正则表达式(使用`PathMatcher`匹配)，后者是有效的通配符。

`Ant`风格模板

    /WEB-INF/*-context.xml
    com/mycompany/**/applicationContext.xml
    file:C:/some/path/*-context.xml
    classpath:com/mycompany/**/applicationContext.xml

`classpath*:`前缀：

    ApplicationContext ctx =
        new ClassPathXmlApplicationContext("classpath*:conf/appContext.xml");

这个特殊的前缀表明所有匹配的类路径资源都将被获取(内部通过`ClassLoader.getResources(...)`)
`classpath:*`前缀还可以和`PathMatcher`模板结合，例如`classpath*:META-INF/*-beans.xml`，解析策略也很简单：调用`ClassLoader.getResources()`去获取类加载器层级下匹配最后一个非通配符片段路径的所有资源，然后在使用`PathMathcher`解析剩下的通配符子路径。
`Note`：
`classpath*:`和`Ant`风格模板组合时在所有通配符之前必须包含根路径，如`classpath*:META-INF/*-beans.xml`，必须包含`META-INF/`
## 3、DAO支持 ##
`Spring`集成的`Data Access Object(DAO)`使得不同的数据访问技术如`JDBC`、`Hibernate`和`JPA`变得一致和简单，不用担心处理特定技术的异常处理，`Spring`将技术相关的异常例如`SQLException`翻译成以`DataAccessExceptio`n为根异常的异常类层级结构，这些异常包装了所有的原始异常信息。
## 3.1 @Repository##
任何`DAO`或者`repository`实现都需要访问持久层资源，这依赖于使用何种持久层技术，例如`JDBC-based repo`需要访问`JDBC DataSource`；`JPA-based repo`需要访问`EntityManager`，可以使用`@Autowired`，`@Inject`，`@Resource`，`@PersistenceContext`注入依赖，如：

    @Repository
    public class JpaMovieFinder implements MovieFinder {
    
        @PersistenceContext
        private EntityManager entityManager;
    
        // ...
    
    }


## 4 Spring Web MVC ##
## 4.1 注解控制器 ##
## 4.1.1 @ModelAttribute ##
`@ModelAttribute`注释可以用在如下地方：

 - `@RequestMapping`注解的方法参数上，用来创建或者获取模型对象，并通过`WebDataBinder`将其与请求绑定
 - 用在`@Controller`或者`@ControllerAdvice`类内部方法级注解，在所有`@RequestMapping`注解方法调用前初始化模型
 - 用在`@RequestMapping`方法上标注其返回值是一个模型属性

一个控制器可以任意个`@ModelAttribute`方法，这些方法均在`@RequestMapping`方法前调用，
code example:

    @ModelAttribute
    public void populateModel(@RequestParam String number, Model model) {
        model.addAttribute(accountRepository.findAccount(number));
        // add more ...
    }
`model.addAttribute(...)`没有显示指定属性名称，会根据对象类型自动分配一个默认属性名

`@ModelAttribute`也可以作为方法级(`method-level`)注解用在`@RequestMapping`注解的方法上，表明方法返回值是模型属性(同上第三条)

    @GetMapping("/accounts/{id}")
    @ModelAttribute("myAccount")
    public Account handle() {
        // ...
        return account;
    }

## 4.1.2 @InitBinder ##
`@InitBinder`注解方法用来初始化`WebDataBinder`实例，这些实例被用作：

 - 绑定请求参数到模型对象
 - 将诸如请求参数、路径变量、`header`、`cookies`和其他字符串请求值转换为控制器方法参数类型
 - 渲染`HTML`表单时将模型对象值格式化成字符串值

`@InitBinder`方法能够注册特定控制器(`controller-specific`)的`java.bean.PropertyEditor`，或者`Spring Converter`和`Formatter`组件，另外，`MVC config`能被用来注册**全局**`Converter`和`Formatter`类型
`@InitBinder`方法不支持`@ModelAttribute`注解参数，通常传入`WebDataBinder`参数用于注册，并且返回值一般为`void`

    @Controller
    public class FormController {

        @InitBinder
        public void initBinder(WebDataBinder binder) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
        }
    
        // ...
    }


## 4.2 MVC配置 ##
## 4.2.1 允许MVC配置 ##
使用`@EnableWebMvc`注解，该注解用来注解`@Configuration`类导入`WebMvcConfigurationSupport`注册的配置：

    @Configuration
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {
    }

`WebMvcConfigurationSupport`中提供了`MVC java`配置背后的主要配置，通过`@EnableWebMvc`注解`@Configuration`来引入，另一种更高级的用法是直接继承自此类，并注解上`@Configuration`，并去掉`@EnableWebMvc`注解

    @Configuration
    public class WebConfig extends WebMvcConfigurationSupport {
        //somecode
    }

那么，`WebMvcConfigurationSupport`中提供了哪些配置呢：
1、注册了以下`HandlerMapping`：

 - `RequestMappingHandlerMapping`：顺序0
 - `HandlerMapping`：顺序1，将`URL`路径映射到视图名称
 - `BeanNameUrlHandlerMapping`：顺序2，将`URL`路径映射到控制器`bean`名称
 - `HandlerMapping`：顺序`Integer.MaxValue-1`，提供静态资源请求
 - `HandlerMapping`:顺序`Integer.MAX_AVALUE`,将请求`forward`到默认的`servlet`

2、注册了以下`HandlerAdapter`：

 - `RequestMappingHandlerAdapter`：通过注解的控制器方法处理请求
 - `HttpRequestHandlerAdapter`:通过`HttpRequestHandler`处理请求
 - `SimpleControllerHandlerAdapter`：通过基于接口的控制器处理请求

3、注册了带有异常解析器链的`HandlerExceptionResolverComposite`：

 - `ExceptionHandlerExceptionResolver`：通过`@ExceptionHandler`注解的方法处理异常
 - `ResponseStatusExceptionResolver`：解析`ResponseStatus`注解的异常
 - `DefaultHandlerExceptionResolver`：解析已知的`spring`异常类型

4、注册了`AntPathMatcher`和`UrlPathHelper`，用在以下场景

 - `RequestMappingHandlerMapping`
 - 视图控制器的`HandlerMapping`
 - 静态资源的`HandlerMapping`
 以上两类`bean`可以通过`PathMatch`

除了直接实现`WebMvcConfigurer`接口来实现`web mvc`的配置之外，还可以继承自`WebMvcConfigurerAdapter`并覆盖其空方法

    @Configuration
    @EnableWebMvc
    public class WebMvcConfig extends WebMvcConfigurerAdapter {
        //override method
    }

## 4.2.2 自定义类型转换配置 ##
通过`Java`配置，注册自定义格式化和转换器

    @Configuration
    @EnableWebMvc
    public class WebConfig implements
    WebMvcConfigurer {
    
        @Override
        public void addFormatters(FormatterRegistry registry) {
            // ...
        }
    }


## 4.2.3 验证 ##
自定义全局`Validator`实例

    @Configuration
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {
    
        @Override
        public Validator getValidator(); {
            // ...
        }
    }

同样，在`xml`中配置：

    <mvc:annotation-driven validator=""/>
    
在控制器内部添加`validator`，相对于上面的全局验证器：

    @Controller
    public class MyController {
    
        @InitBinder
        protected void initBinder(WebDataBinder binder) {
            binder.addValidators(new FooValidator());
        }
    
    }

## 4.2.4 拦截器 ##
此拦截器就是之前`HandlerMapping`接口执行链中的`interceptor`，主要作用是配置多个拦截器组成拦截器链，最后加上`handler`组成请求的处理执行链，配置方式还是分为两种`java config`和`xml config`
`java config`：

    @Configuration
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LocaleChangeInterceptor());
        registry.addInterceptor(new ThemeChangeInterceptor()).addPathPatterns("/**").excludePathPatterns("/admin/**");
        registry.addInterceptor(new SecurityInterceptor()).addPathPatterns("/secure/*");
    }
    }
`xml`配置，使用到了`<mvc:interceptors`：

    <mvc:interceptors>
    <bean class="org.springframework.web.servlet.i18n.LocaleChangeInterceptor"/>
    <mvc:interceptor>
        <mvc:mapping path="/**"/>
        <mvc:exclude-mapping path="/admin/**"/>
        <bean class="org.springframework.web.servlet.theme.ThemeChangeInterceptor"/>
    </mvc:interceptor>
    <mvc:interceptor>
        <mvc:mapping path="/secure/*"/>
        <bean class="org.example.SecurityInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>

## 4.2.5 Message Converters ##
配置消息转换器，
`xml`配置方式：

    <mvc:annotation-driven>
        <mvc:message-converters>
            <bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter">
                <property name="objectMapper" ref="objectMapper"/>
            </bean>
            <bean class="org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter">
                <property name="objectMapper" ref="xmlMapper"/>
            </bean>
        </mvc:message-converters>
    </mvc:annotation-driven>

`java`代码配置方式，实现`WebMvcConfigurer`接口，注意两个方法`configureMessageConverters()`和`extendMessageConverters()`，前者会无视默认消息转换器，后者相当于在默认转换器的基础上添加新的转换器，这和`<mvc:message-converters>`的效果一样，其属性`register-defaults`默认为`true`：

    <mvc:annotation-driven>
        <mvc:message-converters>
            <bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter">
                <property name="objectMapper" ref="objectMapper"/>
            </bean>
            <bean class="org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter">
                <property name="objectMapper" ref="xmlMapper"/>
            </bean>
        </mvc:message-converters>
    </mvc:annotation-driven>


## 4.2.6 视图控制器 ##
直接从`"/"`请求跳转到`"home"`视图：

    @Configuration
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {
    
        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/").setViewName("home");
        }
    }

## 4.2.7 处理静态资源 ##
`xml`配置方式：

    <mvc:resources mapping="/resources/**"
    location="/public, classpath:/static/"
    cache-period="31556926" />

`java`代码配置方式：

    @Configuration
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
            .addResourceLocations("/public", "classpath:/static/")
            .setCachePeriod(31556926);
    }
    }

`<mvc:default-servlet-handler>`的作用：
配置一个`handler`(`DefaultServletHttpRequestHandler`)将静态资源请求转发给`servlet`容器的默认`servlet`，注意这个`handler`的优先级应该最低，可以用`default-servlet-name`属性指定`servlet`名称，例如如果访问带`.html`或者`.js`后缀的资源文件，如果不这样配置，使用`spring`默认的`DispathcerServlet`就会报错，找不到资源

## 4.2.8 路径匹配(Path Matching) ##
`xml`配置方式：

    <mvc:annotation-driven>
        <mvc:path-matching
            suffix-pattern="true"
            trailing-slash="false"
            registered-suffixes-only="true"
            path-helper="pathHelper"
            path-matcher="pathMatcher"/>
    </mvc:annotation-driven>

属性描述：

 - `suffix-pattern`：是否使用后缀路径匹配(`.*`)，如果设置为`true`，匹配路径`/users`的方法也会匹配`/users.*`
 - `trailing-slash`：是否匹配`/user/`，如果设置为`true`，匹配路径`/users`也会匹配`/users/`


## 4.2.9 视图解析器(ViewResolver) ##
配置`jsp`视图解析器,`InternalResourceViewResolver`，这个视图解析器必须放在视图解析器链的最后，即其优先级应该是最低的，因为他会解析所有的视图名，而不管对应的资源是否存在

    <bean id=internalResourceViewResolver class="InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/view/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

配置`freemarker`视图，导入`org.freemarker:freemarker`依赖：

    <bean id="freemarkerConfig" class="org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer">
        <property name="templateLoaderPath" value="/WEB-INF/freemarker/"/>
    </bean>

    <bean id="freemarkerViewResolver" class="org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver">
        <property name="suffix" value=".ftl"/>
        <property name="contentType" value="text/html;charset=UTF-8"/>
        <!--<property name="order" value="0"/>-->
    </bean>
    
配置`ContentNegotiatingViewResolver`，根据请求的文件类型或者`Accept`属性来解析视图，该视图解析器本身不会解析视图，将解析工作代理给其他的视图解析器，因此该解析器的优先级最高，视图解析器通过请求的媒体类型(`MediaType`)来选择一个合适的视图(`View`)，请求的媒体类型通过配置的`ContentNegotiatingManager`确定，查看`ContentNegotiationgManager`的源码，可以看出是通过解析策略(`ContentNegotiatingStrategy`，该接口有很多实现，通常用到的是`ParameterContentNegotiationStrategy,根据请求参数来解析媒体类型，请求参数名称默认为format`和`PathExtensionContentNegotiationStrategy，根据路径后缀来解析媒体类型`)并根据请求来解析媒体类型的，`ContentNegotiatingManagerFactoryBean`提供了两个默认的解析策略，但是如果要配置自己的解析策略，直接注入`ContentNegotiatingManager`即可，附一段配置代码，这段代码注入了两个策略，可以解析请求`url?format=json`和`url/xx.json`，将这两个请求解析为`json`视图：

    <bean id="contentNegotiationManager" class="org.springframework.web.accept.ContentNegotiationManager">
        <constructor-arg name="strategies">
            <list>
                <bean class="org.springframework.web.accept.ParameterContentNegotiationStrategy">
                    <constructor-arg name="mediaTypes">
                        <map>
                            <entry key="json" value="application/json"/>
                            <entry key="xml" value="application/xml"/>
                            <entry key="html" value="text/html"/>
                        </map>
                    </constructor-arg>
                </bean>
                <bean class="org.springframework.web.accept.PathExtensionContentNegotiationStrategy">
                    <constructor-arg name="mediaTypes">
                        <map>
                            <entry key="json" value="application/json"/>
                            <entry key="xml" value="application/xml"/>
                            <entry key="html" value="text/html"/>
                        </map>
                    </constructor-arg>
                </bean>
            </list>
        </constructor-arg>
    </bean>

    <bean id="contentNegotiatingViewResolver" class="org.springframework.web.servlet.view.ContentNegotiatingViewResolver">
        <property name="contentNegotiationManager" ref="contentNegotiationManager"/>
        <property name="viewResolvers">
            <list>
                <ref bean="internalViewResolver"/>
                <ref bean="freemarkerViewResolver"/>
            </list>
        </property>

        <property name="defaultViews">
            <list>
                <bean class="org.springframework.web.servlet.view.json.MappingJackson2JsonView"/>
                <bean class="org.springframework.web.servlet.view.xml.MappingJackson2XmlView"/>
            </list>
        </property>
    </bean>

## 5 Spring Web MVC ##
`Spring MVC`和其他许多框架一样，都是围绕前端控制器模式，其中`DispatcherServlet`这个中央`Servlet`为可配置的代理组件提供共享的请求处理算法，这种模式十分灵活，且支持丰富的工作流。
`DispatherServlet`需要在`web.xml`文件中声明和映射，反过来它要使用`Spring`配置去寻找代理组件(`delegate components`)处理请求、视图解析、异常处理等。

    WebApplicationContext->ApplicationContext
    
## 5.1 特殊bean类型 ##
`DispatcherServlet`将请求处理和响应渲染代理给一些特殊`beans`，这些`bean`指的是由`Spring`管理的对象实例，它们通常内置，但是可配置、继承或者替换.
|BeanType|Explanation|
|---|---|
|`HandlerMapping`|将请求映射到处理函数，并附带拦截器的一系列预-后处理，两个主要实现：`RequestMappingHandlerMapping`(支持`@RequestMapping`)和`SimpleUrlHandlerMapping`(显示注册`URI`路径到处理函数)|
|`HandlerAdapter`|帮助`DispatcherServlet`调用请求处理函数，而不用管如何调用这些细节|
|`HandlerExceptionResolver`|异常解析策略：映射到处理函数、`HTML`错误视图等|
|`ViewResolver`|将处理函数返回逻辑视图名称(字符串格式)解析成实际视图渲染响应|
|`LocaleResolver`,`LocaleContextResolver`|国际化|
|`ThemeResolver`|解析`web`应用能用的主题，如提供个性化布局|
|`MultipartResolver`|利用`multipart`解析库解析`multi-part`请求(例如浏览器上传表单文件)|
|`FlashMapManager`|存储。检索输入和输出`FlashMap`，`FlashMap`可以通过`redirect`将属性在请求间传递|

## 5.2 Web MVC配置 ##
应用可以自定义上述特殊`bean`类型，`DispatcherServlet`会检查`WebApplicationContext`，寻找每个特殊`bean`，如果没有匹配的`bean`类型，就会用`DispatcherServlet.properties`里定义的默认`bean`类型。

## 5.3 Servlet配置 ##
可以使用`web.xml`和`Java`来配置`Servlet`容器
`Java`配置方式，必须实现`AbstractAnnotationConfigDispatcherServletInitializer`接口：

    public class MyWebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    
        @Override
        protected Class<?>[] getRootConfigClasses() {
            return null;
        }
    
        @Override
        protected Class<?>[] getServletConfigClasses() {
            return new Class<?>[] { MyWebConfig.class };
        }
    
        @Override
        protected String[] getServletMappings() {
            return new String[] { "/" };
        }
    }

## 5.4 处理过程 ##
`DispatcherServlet`处理请求过程如下：

 - 寻找`WebApplicationContext`，将其作为在处理过程中控制器和其他元素可以使用的属性值绑定在请求中，默认键值为`DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE`
 - 本地解析器绑定到请求，在处理请求过程(渲染视图、准备数据等)中能够解析本地信息。
 - 主题解析器绑定到请求，让元素(如视图)决定使用什么主题
 - 如果你指定了`multipart`文件解析器，请求被视作`multiparts`，并被封装到`MultipartHttpServletRequest`等待进一步处理
 - 寻找合适的处理函数，如果处理函数找到，和处理函数(预处理器、后处理器和控制器)相关的执行链就会被执行，以准备模型和渲染，而对于注释的控制器，响应可能会被直接渲染(`HandlerAdapter`)而不是返回视图。
 - 如果返回模型，视图就会被渲染，如果没有模型返回(可能由于预处理器和后处理器拦截了请求，抑或由于安全原因)，没有视图被渲染，因为此时请求可能已经被实现

`HandlerExceptionResolver`处理请求处理过程中抛出的异常。

## 5.5 拦截 ##
所有的`HandlerMapping`实现都支持拦截器，这在你想要将某些特定的功能如检查准则应用到请求中时是很有用的，拦截器必须实现`HandlerInterceptor`接口，此接口提供了三个方法进行灵活的预-后处理：

 - `preHandle(...)`--在实际请求执行之前
 - `postHandler(...)`--在请求执行之后
 - `afterCompletion(...)`--在完整的请求结束之后

`preHandler(...)`方法返回一个布尔值，你可以使用这个方法破坏或者继续执行链的处理，当其返回`true`时，执行链继续；当其返回`false`时，`DispatcherServlet`会认为拦截器本身会接手请求(例如渲染合适的视图)而不再继续执行执行链中其他的拦截器和处理函数

## 5.6 异常处理 ##
如果在请求映射期间或者请求处理时发生异常，`DispatcherServlet`会将异常解析代理给`HandlerExceptionResolver`链，并提供错误响应这种备选处理。
`HandlerExceptionResolver`实现：
|类|描述|
|:-:|:-:|
|`SimpleMappingExceptionResolver`|提供异常类名和错误视图名称之间的映射，对于浏览器应用中渲染错误页很有用|
|`DefaultHandlerExceptionResolver`|解析由`Spring MVC`引起的异常，并将它们映射到`HTTP`状态码|
|`ResponseStatusExceptionResolver`|解析`@ResponseStatus`异常，并根据注解值将它们映射到`HTPP`状态码|
|`ExceptionHandlerExceptionResolver`|解析由`@Controller`或者`ontrollerAdvice`注解类中`@ExceptionHandler`注解方法调用引起的异常|
**解析器链**
你可以通过在配置中声明多个`HandlerExceptionResolver`并设置`order`属性构建解析器链，顺序属性越高，异常解析器位置越靠后。
`HandlerExceptionResolver`指定其返回值只能是：

 - `ModelAndView`:指向错误视图
 - 如果在解析器内部处理异常，返回空的`ModelAndView`
 - 无法解析返回`null`，由剩下的解析器解析，如果最后异常仍然存在，则向上冒泡至`Servlet`容器

`MVC Config`自动声明了内检的异常解析器，可用于解析`Spring MVC`异常、`@ResponseStatus`注解异常和`@ExceptionHandler`方法异常。

**相关注解**
`@Controller`和`@ControllerAdvice`类可以拥有`@ExceptionHandler`方法处理控制器方法抛出的异常，例如：

    @Controller
    public class SimpleController {
    
        // ...
    
        @ExceptionHandler
        public ResponseEntity<String> handle(IOException ex) {
            // ...
        }
    
    }
`@ExceptionHandler`注解可以列出匹配的异常类型，或者直接声明目标异常为方法参数。
`@ExceptionHandler`、`@InitBinder`、`@ModelAttribute`注解的方法只能应用在声明他们的`@Controller`类内部，如果想要全局应用这些方法，必须使用`ControllerAdvice`或者`RestControllerAdvice`
`@ControllerAdvice`带有`@Component`注解，表明其可以通过`component scan`被注册为`Spring bean`。
## 5.7 视图解析 ##
`ViewResolver`(视图解析器)提供了视图名称和实际视图之间的映射，在将数据交付给指定的视图技术之前`View`(视图)解决了数据准备，下面是视图解析器的细节。

|ViewResolver|Description|
|---|---|
|`AbstractCachingViewResolver`|所有子类都能缓存他们解析的视图实例，缓存提高了视图技术的性能，可以通过设置`cache=false`关闭缓存功能，如果你现在运行过程中刷新视图，可以使用`removeFromCache(String viewName,Locale loc)`方法|
|`XmlViewResolver`|读取配置文件，默认配置文件地址：`/WEB-INF/view.xml`|
|`ResourceBundleViewResolver`||
|`UrlBasedViewResolver`|`redirect:...`,`forward:...`|
|`InternalResourceViewResolver`|`UrlBasedViewResolver`的子类，支持`InternalResourceView`(`Servlets`和`JSPs`)|
|`FreeMarkerViewResolver`|支持`FreeMarkerView`|
|`ContentNegotiatingViewResolver`|根据请求文件名称或者`Accept`头解析视图|

## 5.8 注解控制器 ##
## 5.8.1 声明 ##
自动扫描`@Controller`注解`bean`，需要加入组件扫描到你的`Java`配置

    @Configuration
    @ComponentScan("org.example.web")
    public class WebConfig {
    
        // ...
    }
`@RestController`是一个组合注解，由`@Controller`和`@ResponseBody`两个元注解组成，表明一个控制的的每个方法都继承了类级注解`@ResponseBody`，因此直接返回值直接写入响应体，而不是用`HTML`模板进行视图解析和渲染

## 5.8.2 请求映射(Request Mapping) ##
`@RequestMapping`直接用来映射请求到方法，根据不同的请求方式可以细分为以下注解：

 - `@GetMapping`
 - `@PostMapping`
 - `@PutMapping`
 - `@DeleteMapping`
 - `@PatchMapping`

`code`:

    @RestController
    @RequestMapping("/persons")
    class PersonController {

    @GetMapping("/{id}")
    public Person getPerson(@PathVariable Long id) {
        // ...
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@RequestBody Person person) {
        // ...
    }
    }
    
**URI patterns**
可以声明`URI`变量，通过`@PathVariable`来访问他们的值：

    @GetMapping("/owners/{ownerId}/pets/{petId}")
    public Pet findPet(@PathVariable Long ownerId, @PathVariable Long petId) {
        // ...
    }
`URI`变量自动转换为合适的类型，否则抛出`TypeMismatchException`异常。

**Consumable media types**
通过请求的`Content-Type`来收紧(?narrow)请求映射

    @PostMapping(path = "/pets", consumes = "application/json")
    public void addPet(@RequestBody Pet pet) {
        // ...
    }
支持取反表达式，如`!text/plain`指除了`text/plain`外的所有类型

**Producible media types**
根据`Accept`请求头和控制器方法生产的内容类型收紧请求映射。

    @GetMapping(path = "/pets/{petId}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Pet getPet(@PathVariable String petId) {
        // ...
    }
同样支持取反表达式

**参数、头部**
根据请求参数条件来收紧请求映射，例如根据出现请求参数`"myParam"`，没有请求参数`"!myParam"`或者取特定的值`"myParam=myValue"`。

    @GetMapping(path = "/pets/{petId}", params = "myParam=myValue")
    public void findPet(@PathVariable String petId) {
        // ...
    }
同样可以用请求头部条件

    @GetMapping(path = "/pets", headers = "myHeader=myValue")
    public void findPet(@PathVariable String petId) {
        // ...
    }

## 5.8.3 处理函数(Handler Methods) ##
一些注解的控制器方法参数表示字符串请求输入，例如`@RequestParam`,`@RequestHeader`,`@PathVariable`,`@MatrixVariable`,`@CookieValu`e，如果声明的参数不是字符串类型，可能需要进行类型转换，类型转换可能通过`WebDataBinder`进行定制，或者注册`Formatters`

**@RequestParam**
将请求参数(查询参数或者表格数据)绑定到控制器方法参数。

    @Controller
    @RequestMapping("/pets")
    public class EditPetForm {
    
        // ...
    
        @GetMapping
        public String setupForm(@RequestParam("petId") int petId, Model model) {
            Pet pet = this.clinic.loadPet(petId);
            model.addAttribute("pet", pet);
            return "petForm";
        }
    
        // ...
使用这个注解的方法参数默认是必需的，可以通过`"required=false"`设置成可选。

**@RequestHeader**
将请求头部绑定到控制器方法参数上
给定头部信息：

    Host                    localhost:8080
    Accept                  text/html,application/xhtml+xml,application/xml;q=0.9
    Accept-Language         fr,en-gb;q=0.7,en;q=0.3
    Accept-Encoding         gzip,deflate
    Accept-Charset          ISO-8859-1,utf-8;q=0.7,*;q=0.7
    Keep-Alive              300
绑定`Accept-Encoding`和`Keep-Alive`头部：

    @GetMapping("/demo")
    public void handle(
            @RequestHeader("Accept-Encoding") String encoding,
            @RequestHeader("Keep-Alive") long keepAlive) {
        //...
    }

**@CookieValue**
将`HTTP cookie`的值绑定到控制器方法参数上
如下`cookie`:

    JSESSIONID=415A4AC178C59DACE0B2C9CA727CDD84
绑定`cookie`：

    @GetMapping("/demo")

    public void handle(@CookieValue("JSESSIONID") String cookie) {
        //...
    }

**@ModelAttribute**
在方法参数上注解`@ModelAttribute`可以从模型中获取属性或者实例化(不存在属性)

    @PostMapping("/owners/{ownerId}/pets/{petId}/edit")
    public String processSubmit(@ModelAttribute Pet pet) { }
`Pet`实例会如下解析：

 - 如果模型中存在就通过`Model`从模型中取值
 - 通过`@SessionAttributes`从`HTTP session`中取值
 - 从`URI`路径变量中取值，并通过`Converter`转换
 - 从默认的构造器调用

尽管普遍通过`Model`来装配模型属性，另一个替代方案是通过`Coverter<String,T>`转换`URI`路径变量，下面的例子就通过上述方式寻找属性，模型属性名`"account"`和`URI`路径变量`"account"`匹配，那么`Account`实例就通过注册的`Converter<String,Account>`进行加载

    `@PutMapping("/accounts/{account}")
    public String save(@ModelAttribute("account") Account account) {
        // ...
    }`
在模型属性实例获取之后，数据绑定就开始应用，`WebDataBinder`将请求参数名称(查询参数和表单域)和魔表对象中的域名称进行匹配，匹配的域就会自动进行装配(必要的类型转换)，数据绑定可能会导致错误，可以通过`BindingResult`来检验错误

    @PostMapping("/owners/{ownerId}/pets/{petId}/edit")
    public String processSubmit(@ModelAttribute("pet") Pet pet, BindingResult result) {
        if (result.hasErrors()) {
            return "petForm";
        }
        // ...
    }
可以通过`@Valid`注解在数据绑定后进行自动验证：

    @PostMapping("/owners/{ownerId}/pets/{petId}/edit")
    public String processSubmit(@Valid @ModelAttribute("pet") Pet pet, BindingResult result) {
        if (result.hasErrors()) {
            return "petForm";
        }
        // ...
    }
    
**@SessionAttributes**
`@SessionAttributes`用来在`HTTP`会话中、请求之间存储模型属性，这是一个类级注解，列出了存储在会话中的模型属性名或者模型属性类型，该注解可以将所有控制器内部声明的模型属性标注为会话级模型属性，从而不同的请求可以访问一个全局的属性，**因为模型属性在不同的请求间会重置**

    @Controller
    @SessionAttributes("pet")
    public class EditPetForm {
        // ...
    }
当第一个请求将名称为`pet`的模型属性加入至`model`时，它会自动提升并存入`HTTP`会话中，直到控制器方法使用`SessionStatus`方法参数清除内存

    @Controller
    @SessionAttributes("pet")
    public class EditPetForm {
    
        // ...
    
        @PostMapping("/pets/{id}")
        public String handle(Pet pet, BindingResult errors, SessionStatus status) {
            if (errors.hasErrors) {
                // ...
            }
                status.setComplete();
                // ...
            }
        }
    }

**@RequestAttribute**
用来访问事先创造的(如`Filter`或者`HandlerInterceptor`)预存在的请求属性。

**Multipart**
要在`pom`中加入`org.apache.commons:commons-io`和`commons-fileupload:commons-fileupload`两个`jar`包依赖
首先在`bean`配置文件中声明`MultipartResolver`，通常是`CommonsMultipartResolver`，带有`"multipart/form-data"`的`POST`请求内容被解析并可通过普通请求参数访问，这里要了解`multipart/form-data`请求的格式，可以参考[multipart][1]，`multipar/form-data`既可以上传文件，也可以上传键值对(`json`)：

    @Controller
    public class FileUploadController {
    
        @PostMapping("/form")
        public String handleFormUpload(@RequestParam("name") String name,
                @RequestParam("file") MultipartFile file) {
    
            if (!file.isEmpty()) {
                byte[] bytes = file.getBytes();
                // store the bytes somewhere
                return "redirect:uploadSuccess";
            }
    
            return "redirect:uploadFailure";
        }
    }
`Multipart`内容也可以与对象实例进行绑定：

    class MyForm {
    
        private String name;
    
        private MultipartFile file;
    
        // ...
    
    }
    
    @Controller
    public class FileUploadController {
    
        @PostMapping("/form")
        public String handleFormUpload(MyForm form, BindingResult errors) {
    
            if (!form.getFile().isEmpty()) {
                byte[] bytes = form.getFile().getBytes();
                // store the bytes somewhere
                return "redirect:uploadSuccess";
            }
    
            return "redirect:uploadFailure";
        }
    
    }
在`RESTful`服务场景中`Multipart`请求可以从无浏览器客户端提交：

    POST /someUrl
    Content-Type: multipart/mixed
    
    --edt7Tfrdusa7r3lNQc79vXuhIIMlatb7PQg7Vp
    Content-Disposition: form-data; name="meta-data"
    Content-Type: application/json; charset=UTF-8
    Content-Transfer-Encoding: 8bit
    
    {
        "name": "value"
    }
    --edt7Tfrdusa7r3lNQc79vXuhIIMlatb7PQg7Vp
    Content-Disposition: form-data; name="file-data"; filename="file.properties"
    Content-Type: text/xml
    Content-Transfer-Encoding: 8bit
    ... File Data ...
可以通过`@RequestParam`访问`"meta-data"`部分得到字符串，但是可能需要从`JSON`反序列化，使用`@RequestPart`注解在通过`HttpMessageConverter`转化后访问`multipart`

    @PostMapping("/")
    public String handle(@RequestPart("meta-data") MetaData metadata,
            @RequestPart("file-data") MultipartFile file) {
        // ...
    }
`@RequestPart`可以和`javax.validation.Valid`或者`Spring`的`@Validated`注解集合使用，进行验证，验证错误默认会导致`MethodArgumentNotValidException`异常，并被转换为`400(BAD_RWQUEST)`响应，另外验证错误可以在控制器内部通过`Errors`或者`BindingResult`进行本地处理。

    @PostMapping("/")
    public String handle(@Valid @RequestPart("meta-data") MetaData metadata,
            BindingResult result) {
        // ...
    }

**@RequesBody**
使用`@RequestBody`注解来读取请求体以及通过`HttpMessageConverter`反序列化成对象

    @PostMapping("/accounts")
    public void handle(@RequestBody Account account) {
        // ...
    }
与`@RequestPart`相同，可以和验证注解一起使用

    @PostMapping("/accounts")
    public void handle(@Valid @RequestBody Account account, BindingResult result) {
        // ...
    }

**@ResponseBody**
使用`@ResponseBody`注解将返回值通过`HttpMessageConverter`将返回值序列化为响应体

    @GetMapping("/accounts/{id}")
    @ResponseBody
    public Account handle() {
        // ...
    }

**Jackson JSON**
`@JsonView`注解：过滤序列化对象的字段属性，简单来说就是定义一个标签，根据控制器的`@JsonView`属性，将实体类中的不同标签属性进行分类显示，子类标签可以显示父类字段，父类标签无法显示子类字段，例如
先定义一个`View`类，有两个接口`Summary`和`SummaryWithDetail`：

    pulbic class View {
        public interface Summary {}
        public interface SummaryWithPassword {}
    }

定义实体类，在字段上用`@JsonView`注解

    public User implements Serializable {
    
    @JsonView(View.Summary.class)
    private int id;
    
    @JsonView(View.Summary.class)
    private String name;
    
    @JsonView(View.SummaryWithPassword.class)
    private String password;
    
    //省略getter/setter和构造方法
    }

在写个控制器类：

    @RestController
    pulbic class UserController {
        
        @RequestMapping("/user")
        @JsonView(View.Summary.class)
        public User user() {
            return new User(1,"yuding","123");
        }
        
        @RequestMapping("/user2")
        @JsonView(View.SummaryWithPassword.class)
        public User user2() {
            return new User(1,"yuding","123");
        }
    }
运行后：

    1、访问"/user"，返回JSON视图中
    {"id":1,"name":"yuding"}
    即只有id和name两个字段，
    2、访问"/user2"，
    {"id":1,"name":"yuding","password":"123"}
    三个字段都显示

## 5.9 Model ##


## 补充 ##
**1、通过静态工厂方法实例化bean**
当通过静态工厂方法定义一个`bean`时，使用`class`属性指定包含静态方法的类，`factory-method`属性指定静态方法的名称，能够通过调用这个方法(指定可选参数)返回对象，该`bean`定义中无需指定返回对象的类型，只需要指定包含静态方法的类，例如：

    <bean id="clientService" class="examples.ClientService" factory-method="createInstance"/>
    
    public class ClientService {
        private static ClientService clientService = new ClientService();
        private ClientService() {}
        
        public static ClientService createInstance() {
            return clientService;
        }
        
        
**2、使用实例的工厂方法实例化bean**
通过调用一个已经存在`bean`的非静态方法创建一个新的`bean`，此时，`class`属性必须为空，`factory-bean`属性指定包含该非静态方法的`bean`实例名称，`factory-method`属性指定方法名称，例如：

    <bean id="serviceLocator" class="someClass">
    
    <bean id="clientService" factory-bean="serviceLocator" factory-method="createClientInstance"/>
    
    public class someClass {
        private static ClientService clientService = new ClientService();
        public ClientService createClientInstance() {
            return clientService;
        }
    }

**3、依赖解析过程**

 - 通过描述`bean`的配置元数据初始化`ApplicationContext`，配合元数据可以通过`xml`、`java`代码或者注解来声明
 - 对于每个`bean`，它的依赖通过属性、构造参数或者静态方法的参数来传递，这些依赖提供给`bean`，然后创建对应的`bean`
 - 每个属性或者构造参数实际上是待设置值的定义或者是另外一个`bean`的引用
 - 类型转换，`Spring`能够将字符串格式的值转换为一些内建类型，例如：`int/long/String/boolean`等


**4、配置java.util.Properties实例**

    <bean id="mappings"
    class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">

    <!-- typed as a java.util.Properties -->
    <property name="properties">
        <value>
            jdbc.driver.className=com.mysql.jdbc.Driver
            jdbc.url=jdbc:mysql://localhost:3306/mydb
        </value>
    </property>
</bean>

`Spring`容器会使用`PropertyEditor`机制将`<value>`元素中的元素转换为`java.util.Properties`，这在配置`shiro`的`filterChainDefinitions`很有效


**关于懒加载lazy-init**
默认情况下，`ApplicationContext`会将创建和配置所有的单例`bean`作为容器实例化过程的一部分，通常，这个预实例化也是需要的(`pre-instantiation`)，因为能够及时的发现配置和环境错误，当不需要这个默认操作时，可以将其`bean`定义中的`lazy-init`属性设置为`true`，容器便不会在启动时实例化该`bean`，而是在首次需要时，例如：

    <bean id="lazy" class="com.foo.ExpensiveToCreateBean" lazy-init="true"/>
    <bean name="not.lazy" class="com.foo.AnotherBean"/>

当非懒加载`bean`依赖于懒加载`bean`时，容器同样会在启动时创建懒加载`bean`，即其懒加载特性无用了，另外，可以在容器级声明懒加载属性

    <beans default-lazy-init="true">
    </beans>

**方法注入**
在大部分情况下，容器中的`bean`都是单例的，如果出现了单例`bean`依赖于原型`bean`，就会出现作用域不匹配的问题，一种方式是单例`bean`类实现`ApplicationContextAware`接口，这样就可以在内部拿到`prototype`的`bean`，这显然不符合`Spring`框架的设计原则

**lookup-method**
查找方法注入允许容器覆盖其管理的`bean`的方法，返回容器中另一个查找的`bean`，查找的`bean`通常都是原型作用域的，`Spring`框架通过`CGLIB`生成的字节码来生成动态的子类覆盖指定方法，从而来实现方法注入

 - 为了这个动态类能够工作，被继承的类不能是`final`，被覆盖的方法也不能是`final`

被覆盖的方法必须满足一下格式：

    public|protected [abstract] <return-type> method-name(no arguments);  //必须为public或者protected，abstract可选，无参

举例：

    <bean id="myCommand" class="someClass" scope="prototype"/>
    <bean id="commandManager" class="CommandManager">
        <lookup-method name="createCommand" bean="myCommand"/>
    </bean>
        
`commandMananer`每次调用它的方法`createCommand`时都会拿到一个全新的`bean`，因此必须将`myCommand`设置为`prototype`，如果是`singleton`，那么返回的`bean`也会是同一个
    
**replace-method**
使用自定义的方法替换掉指定的方法

    <bean id="myValueCalculator" class="">
        <replaced-method name="computeValue" replacer="replacer">
            <arg-type>String</arg-type>
        </replaced-method>
    </bean>
    <bean id="replacer" class=""/>  //replacer必须实现MethodReplacer接口
    
**bean的生命周期**
重要的接口：

 - `InitializingBean`:等价于`bean`定义中的`init-method`，`bean`初始化后的回调接口，其方法为`afterPropertiesSet()`，`JSR-250`规范中的`@PostConstruct`注解也可起到同样的作用
 - `DisposableBean`:等价于`bean`定义中的`destroy-method`，`bean`销毁后的回调接口，`JSR-250`规范中的`@PreDestroy`注解也可以起到同样的作用

***Aware接口**

**BeanPostProcessor**
`BeanPostProcessor`接口是允许自定义修改`bean`实例的工厂钩子，例如检查标注接口或者使用代理包装，它提供了两个方法：`postProcessBeforeInitilization`（在初始化回调方法执行前执行，如`InitializingBean`的`afterPropertiesSet()`）和`postProcessorAfterInitialization`（在初始化回调方法执行后执行）
例子，`Spring`框架中定义了一个`RequiredAnnotationBeanPostProcessor`，这个类检查`bean`中使用`@Required`注解的属性值是否全部设置，这个`bean`在配置文件中使用`<context:component-scan/>`标签时会自动注册，具体参考此类的`javadoc`，其他的`BeanPostProcessor`还有`AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor, PersistenceAnnotationBeanPostProcessor`

**BeanFactoryPostProcessor**
此接口用来修改容器的`bean`定义，其只有一个方法`postProcessBeanFactory`，在容器内部的`bean factory`初始化之后修改，此时，所有的`bean`定义已经被加载，但是没有`bean`被实例化，要注意到懒加载属性对此类`bean`无意义
`Spring`框架中实现了此接口的类有`PropertyPlaceholderConfigurer`,在运行期间读取属性文件中的值替换`bean`定义中的占位符

**ApplicaitionContext的拓展属性**
`ApplicationContext`继承了`BeanFactory`接口，当然继承了其方法，同时也实现了很多其他的接口，因此`ApplicationContext`也具备后者所不具备的一些拓展属性，例如：

 - 通过`MessageSource`接口，访问国际化的消息源
 - 通过`ResourceLoader`接口访问`URLs`和文件等资源
 - 通过`ApplicationEventPublisher`接口向实现了监听器的`bean`发布事件
 - 通过`HierarchicalBeanFactory`接口加载多层上下文

通过声明式(而不是编程式)来使用`ApplicationContext`，依赖`ContextLoader`接口，在`web`应用启动过程中自动实例化`ApplicationContext`

**MessageSource接口**
当`ApplicationContext`加载时，它会自动搜索在上下文中定义的`MessageSource`，这个`bean`必须取名`messageSource`，如果没有找到任何`MessageSource`，`Spring`会自动创建一个空的`DelegatingMessageSource`

**声明式方法实例化ApplicationContext**
可以使用`ContextLoaderListener`来注册`ApplicationContext`

    <context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>/WEB-INF/daoContext.xml /WEB-INF/applicationContext.xml</param-value>
    </context-param>
    
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

`ContextLoaderListener`会读取`contextConfigLocation`中的配置文件，用来实例化`WebApplicationContext`

**BeanFactory OR ApplicationContext**
|特性|`BeanFactory`|`ApplicationContext`|
|:-:|:-:|:-:|
|自动注册`BeanPostProcessor`和`BeanFactoryPostProcessor`|`no`|`yes`|
|国际化|`no`|`yes`|
|`ApplicationEvent`|`no`|`yes`|

`BeanFactory`注册`BeanPostProcessor`的方式：

    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
    // populate the factory with bean definitions
    
    // now register any needed BeanPostProcessor instances
    MyBeanPostProcessor postProcessor = new MyBeanPostProcessor();
    factory.addBeanPostProcessor(postProcessor);

因此在需要使用`BeanPostProcessor`和`BeanFactoryPostProcessor`的场合下，`ApplicationContext`优势极大，例如在占位符替换和`AOP`中。

**FactoryBean**
`FactoryBean`本身就是一个`bean`，但是具有工厂类的功能，实现该接口的`bean`有两种获取方式：

 - `getBean("name")`：获取该工厂`bean`生产的`bean`
 - `getBean("&name")`：获取该工厂`bean`本身

这个工厂`bean`在`spring`中其实有很多应用

## Spring AOP ##
基本概念：

 - `Aspect`切面：横切多个类的模块，事务管理就是一个很好的例子，在`Spring AOP`中，切面是通过普通类(`schema-based`方式)或者注解有`@Aspect`的普通类(`@Aspect`风格)
 - `Joint point`连接点：程序执行过程中的一点，例如方法的执行或者异常的处理，在`Spring AOP`中通常表示方法的执行
 - `Advice`增强或通知：在特定连接点被切面执行的的动作，包括前向`before`、`after`和环绕`around`，很多`aop`框架将`advice`看成是一个拦截器，维护了连接点关联的拦截器链
 - `Pointcut`切点:匹配连接点的断言，`advice`和切点表达式关联，并且可以运行在任何和该切点匹配的连接点上(例如某个特定名称方法的执行)
 - `introduction`：
 - `target object`:被一个或者多个切面增强的对象，因为`Spring AOP`是通过运行时代理实现的，因此目标对象通常是被代理的对象
 - `AOP 代理`：`AOP`框架创建的对象，用来实现取切面，在`Spring`框架中，`AOP`代理是`JDK`动态代理或者`CGLIB`代理
 - `weaving`织入：将切面和其他应用类型或者对象连接创建一个增强的对象，`Spring AOP`框架在运行时织入

增强方式：
前向、后向、环绕、`returning`、`finally`、`throwing`

两种方式：
(1)`@Aspect`注解方式：允许`@Aspect`支持可以使用`@EnableAspectJAutoProxy`或者`<aop:aspect-autoproxy/>`

声明一个切面：

    @Aspect方式
    <bean id="myAspect" class="someClass">
        //...
    </bean>
    
    @Aspect
    public class someClass {
        
    }

声明返回增强(`returning advice`)：`returning`属性值名称必须和方法参数名称保持一致

    @AfterReturning(pointcut="",returning="retVal")
    public void doAccessCheck(Object retVal) {
        //...
    }

声明异常增强(`throwing advice`):`throwing`属性值名称必须和方法参数名称保持一致

    @AfterThrowing(pointcut="",throwing="ex")
    pulbic void doRecoveryActions(DataAccessException ex) {
        //...
    }

声明环绕增强(`around advice`):方法第一个参数必须是`ProceedingJoinPoitnt`类型，执行该类参数的`proceed()`方法实际上会调用`pointcut`匹配的方法，`proceed()`方法可以带一个`Object[]`参数，在执行过程中，该参数会传递到切点方法

    @Around("pointcut expression")
    public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
        //before
        Object retVal = pjp.proceed();
        //after
        return retVal;
    }


(2)`aop`标签方式
声明切面：`<aop:config>`标签包含了切面、切点和增强的定义

    <aop:config>
        <aop:aspect id="" ref="bean"
        //...
        </aop:aspect>
        <aop:advisor/>
        <aop:pointcut/>
    </aop:config>
    <bean id="bean" class="">
    </bean>

声明切点：
可以在`<aop:config>`标签下声明切点

        <aop:config>
        <aop:pointcut id="businessService"
            expression="execution(* com.xyz.myapp.service.*.*(..))"/>
        </aop:config>
也可以在`<aop:aspect>`标签下声明切点：

    <aop:config>

    <aop:aspect id="myAspect" ref="aBean">

        <aop:pointcut id="businessService"
            expression="execution(* com.xyz.myapp.service.*.*(..)) and  this(service)"/>

        <aop:before pointcut-ref="businessService" method="monitor"/>

        ...

        </aop:aspect>
    
    </aop:config>


**Spring AOP代理机制**
`Spring AOP`代理方式有两种：`jdk`动态代理和`CGLIB`代理，更偏向前者
如果目标对象实现了一个或者多个接口，那么`jdk`动态代理(`interface-based proxies`)会被使用，所有目标对象实现的接口都会被代理，如果目标对象没有实现任何接口，那么`CGLIB`代理(`class-based proxies`)会被使用
强行使用`CGLIB`代理：

    <aop:config proxy-target-class="true">
        <!-- other beans defined here... -->
    </aop:config>


## Spring事务管理 ##
**Spring框架的事务抽象**
事务策略由接口`PlatformTransactionManager`描述：

    public interface PlatformTransactionManager {

    TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException;

    void commit(TransactionStatus status) throws TransactionException;

    void rollback(TransactionStatus status) throws TransactionException;
    }
常用的`DataSourceTransactionManager`就实现了此接口

**声明式事务管理**
声明式事务管理对应用代码影响最小，和轻量级非侵入式框架的设计理念保持一致
**理解Spring框架声明式事务管理实现**
首先，`Spring`事务管理框架是通过`AOP`代理实现的，事务增强是通过配置元数据驱动的，`AOP`和事务元数据的结合构建了一个`AOP`代理，这个`AOP`代理使用`TransactionInterceptor`和`PlatformTransactionManager`实现来驱动方法调用的事务管理
事务增强定义：

    <tx:advice id="txAdvice" transaction-manager="txManager">
        <!-- the transactional semantics... -->
        <tx:attributes>
            <!-- all methods starting with 'get' are read-only -->
            <tx:method name="get*" read-only="true"/>
            <!-- other methods use the default transaction settings (see below) -->
            <tx:method name="*"/>
        </tx:attributes>
    </tx:advice>
    
`<aop:config>`标签定义确定事务增强(`<tx:advice>`)能够执行在程序中的某个合适的点，首先你需要定义一个切点去匹配某个接口中的方法，**其次再通过`<aop:advisor>`标签将切点`pointcut`和`<tx:advice>`联系起来**

**使用@Transactional**
除了使用配置方式声明事务，还可以使用注解来声明事务
        
        @Transactional
    public class DefaultFooService implements FooService {

    Foo getFoo(String fooName);

    Foo getFoo(String fooName, String barName);

    void insertFoo(Foo foo);

    void updateFoo(Foo foo);
    }
    
加上`@Transactional`注解之后，需要修改下配置文件：去掉`<tx:advice>`注解，增加`<tx:annotation-driven>`，如下：

    <beans>
        <bean id="fooService" class="x.y.service.DefaultFooService"/>

    <!-- enable the configuration of transactional behavior based on annotations -->
    <tx:annotation-driven transaction-manager="txManager"/><!-- a PlatformTransactionManager is still required -->
    <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <!-- (this dependency is defined somewhere else) -->
        <property name="dataSource" ref="dataSource"/>
        </bean>
    </beans>

所以，我们现在有了**两种**声明事务的方式：

 - `<tx:advice>`标签配置具有事务语义的`AOP`增强
 - `@Transactional`注解方式

事务配置属性详解，首先要清楚一点的是非事务方式是自动提交的，不管抛没抛异常，都能将数据写进数据库，而事务方式则不会，如果发生异常则会回滚：
1、`Propagation`：事务的传播属性

 - `REQUIRED`:支持当前事务，如果没有就新建一个
 - `SUPPORTS`:支持当前事务，如果没有则以非事务方式执行
 - `MANDATORY`:支持当前事务，如果没有则抛出异常
 - `REQUIRES_NEW`:创建一个新的事务，如果存在当前事务则挂起
 - `NOT_SUPPORTED`:以非事务方式执行，如果存在当前事务则挂起
 - `NEVER`:以非事务方式执行，如果存在当前事务则抛出异常

`Spring`事务配置分为三大块：

 - 数据源`DataSource`:针对不同的数据库访问方式提供不同的数据源配置，`JDBC`(`DataSource`)/`Hibernate`(`SessionFactory`)/`JPA`(`EntityManager`)
 - 事务管理`TransactionManager`：同样事务管理和数据源也是一一对应的，分别为：`DataSourceTransactionManager`、`HibernateTransactionManager`、`JpaTransactionManager`
 - 代理机制：`jdk`动态代理(基于接口的代理，默认)和`CBLIB`代理(基于类的代理)，实现方式可以分为`<tx>`标签和`@Transactional`注解


## Spring MVC基础 ##
## 1、redirect ##
`redirect`跳转，会在响应头中加上`302`状态码，并在`Location`字段中填入跳转的`url`，跳转方式有多种：

 - `response.sendRedirect("url")`:跳转到相对应用程序根目录下`root/url`，前面加下划线效果一样
 - 返回逻辑视图名称`redirect:url`跳转到相对应用程序根目录下`root/url`，前面加下划线效果一样

聊一聊`spring MVC`中提供的视图解析器，关键接口：

 - `ViewResolver`:只包含一个方法`View resolveViewName(String viewName,Locale locale)`，根据视图名称解析视图
 - `UrlBasedViewResolver`:视图解析器最重要的实现，直接将逻辑视图名称解析为视图，它是很多视图解析器的父类
 - `InternalResourceViewResolver`:支持`InternalResourceView`(例如`Servlets`和`JSP`,以及`JstlView`的子类)，通常用来指定`jsp`视图
 - 参考`4.2.9`


## Dispatcher初始化源码分析 ##
源码如下：

    protected WebApplicationContext initWebApplicationContext() {
        //获取ServletContext中键值为WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE的WebApplicationContext，这个context是在ContextLoad中初始化后构建的WebApplicationContext
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;
		
        //DispatcherServlet构造函数中是否注入webApplicationContext
		if (this.webApplicationContext != null) {
			// 如果有，则使用该webApplicationContext
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent -> set
						// the root application context (if any; may be null) as the parent
						cwac.setParent(rootContext);
					}
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		//如果没有通过构造函数注入
		if (wac == null) {
			//在当前ServletContext中寻找指定键值对应的context，该键值通过contextAttribute指定
			wac = findWebApplicationContext();
		}
		if (wac == null) {
			//仍然没找到，则根据配置的contextClass创建一个webApplicationContext实例，以rootContext为parent，contextClass通过<servlet>下的子标签<init-param>配置
			wac = createWebApplicationContext(rootContext);
		}

		if (!this.refreshEventReceived) {
			// Either the context is not a ConfigurableApplicationContext with refresh
			// support or the context injected at construction time had already been
			// refreshed -> trigger initial onRefresh manually here.
			onRefresh(wac);
		}
        //publishContext默认为true，发布context
		if (this.publishContext) {
			// 将wac设置为ServletContext的一个属性，属性名是：FrameworkServlet.class.getName() + ".CONTEXT." + getServletName()
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
						"' as ServletContext attribute with name [" + attrName + "]");
			}
		}

		return wac;
	}

## ContextLoader初始化WebApplicationContext源码分析 ##
源码如下：

    public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
        //检查ServletContext是否已有WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE属性，如果有，则抛出异常
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring root WebApplicationContext");
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			//如果context为空，则根据contextClass构建WebApplicationContext实例，contextClass通过<context-param>配置
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			//将该context设置为ServletContext的属性，键值为WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUT
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
						WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
		catch (Error err) {
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}

  [1]: https://blog.csdn.net/ye1992/article/details/49998511
  
  
  
  
  
  
  

