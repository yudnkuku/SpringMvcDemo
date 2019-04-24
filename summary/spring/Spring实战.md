# Spring实战

标签（空格分隔）： 读书笔记

---

## EP1 Spring之旅 ##
## 应用上下文 ##
`Spring`自带了多种类型的应用上下文：

 - `AnnotationConfigApplicationContext`:从一个或者多个基于`java`的配置类中加载`spring`应用上下文
 - `AnnotationConfigWebApplicationContext`:从一个或者多个基于`java`的配置类中加载`Spring Web`应用上下文
 - `ClasspathXmlApplicationContext`:从类路径下一个或多个`XML`配置文件中加载上下文定义，把应用上下文的定义文件作为类资源
 - `FileSystemXmlApplicationContext`:从文件系统下的一个或多个`XML`配置文件中加载上下文定义
 - `XmlWebApplicationContext`:从`Web`应用下的一个或者多个`XML`配置文件中加载上下文定义

## Bean的生命周期 ##
1、对`bean`进行实例化

2、将值和`bean`的引用注入到`bean`对应的属性中

3、如果`bean`实现了`BeanNameAware`接口，`Spring`将`bean`的`ID`传递给`setBeanName()`方法

4、如果`bean`实现了`BeanClassLoader`接口，`spring`会将`classLoader`传入`setBeanClassLoader()`方法

5、如果`bean`实现了`BeanFactoryAware`接口，`Spring`调用`setBeanFactory()`方法将`BeanFactory`实例传入

6、如果`bean`实现了`ApplicationContextAware`接口，`Spring`将调用`setApplicationContext()`方法，将`ApplicationContext`实例传入

7、如果实现了`BeanPostProcessor`接口，将调用其`postProcessBeforeInitialization()`方法

8、如果实现了`InitializingBean`接口，将调用`afterPropertiesSet()`方法，如果`bean`内部声明了`init-method`初始化方法，那么该方法也会被调用

9、如果实现了`BeanPostProcessor`接口，将调用其`postProcessorAfterInitialization()`方法

10、此时`bean`已经初始化完成，会被注册到上下文中等待使用直到最后被销毁

11、如果实现了`DisposableBean`接口，将会调用`destroy()`方法，或者声明了`destroy-method`，该方法也会被调用

这些步骤的流程在`org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#doCreateBean`方法中：

    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
		// Instantiate the bean.
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			instanceWrapper = createBeanInstance(beanName, mbd, args);  //(1)构造bean实例
		}
		final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
		Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				mbd.postProcessed = true;
			}
		}

		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		if (earlySingletonExposure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			addSingletonFactory(beanName, new ObjectFactory<Object>() {
				@Override
				public Object getObject() throws BeansException {
					return getEarlyBeanReference(beanName, mbd, bean);
				}
			});
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
			populateBean(beanName, mbd, instanceWrapper);   //(2)装配属性
			if (exposedObject != null) {
				exposedObject = initializeBean(beanName, exposedObject, mbd);   (3)初始化bean，这个方法可以展开，具体包括调用Aware接口的方法/init-method和BeanPostProcessor回调
			}
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
				throw (BeanCreationException) ex;
			}
			else {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
			}
		}

		if (earlySingletonExposure) {
			Object earlySingletonReference = getSingleton(beanName, false);
			if (earlySingletonReference != null) {
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
						if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
							actualDependentBeans.add(dependentBean);
						}
					}
					if (!actualDependentBeans.isEmpty()) {
						throw new BeanCurrentlyInCreationException(beanName,
								"Bean with name '" + beanName + "' has been injected into other beans [" +
								StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
								"] in its raw version as part of a circular reference, but has eventually been " +
								"wrapped. This means that said other beans do not use the final version of the " +
								"bean. This is often the result of over-eager type matching - consider using " +
								"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}
    
    //initializeBean方法
    protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				@Override
				public Object run() {
					invokeAwareMethods(beanName, bean);
					return null;
				}
			}, getAccessControlContext());
		}
		else {
			invokeAwareMethods(beanName, bean); //调用Aware方法
		}

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName); //调用BeanPostProcessor.postProcessBeforeInitialization方法处理bean
		}

		try {
			invokeInitMethods(beanName, wrappedBean, mbd);  //如果实现了InitializingBean接口，调用其afterPropertiesSet回调；如果声明了init-method，那么调用该方法
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null),
					beanName, "Invocation of init method failed", ex);
		}

		if (mbd == null || !mbd.isSynthetic()) {
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);  //调用BeanPostProcessor.postProcessAfterInitialization方法处理bean
		}
		return wrappedBean;
	}
	
	//invokeAwareMethods
	private void invokeAwareMethods(final String beanName, final Object bean) {
		if (bean instanceof Aware) {
			if (bean instanceof BeanNameAware) {    //实现BeanNameAware，设置bean name
				((BeanNameAware) bean).setBeanName(beanName);
			}
			if (bean instanceof BeanClassLoaderAware) { //实现BeanClassLoader，设置BeanClassLoader
				((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
			}
			if (bean instanceof BeanFactoryAware) { //实现BeanFactoryAware，设置BeanFactoryAware
				((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}
	
	//invokeInitMethods
	protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd)
			throws Throwable {

		boolean isInitializingBean = (bean instanceof InitializingBean);    //实现InitializingBean接口，调用其afterPropertiesSet回调方法
		if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
			}
			if (System.getSecurityManager() != null) {
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						@Override
						public Object run() throws Exception {
							((InitializingBean) bean).afterPropertiesSet();
							return null;
						}
					}, getAccessControlContext());
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
				((InitializingBean) bean).afterPropertiesSet();
			}
		}
        
        //声明了自定义的init-method，调用其初始化方法
		if (mbd != null) {
			String initMethodName = mbd.getInitMethodName();
			if (initMethodName != null && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
					!mbd.isExternallyManagedInitMethod(initMethodName)) {
				invokeCustomInitMethod(beanName, bean, mbd);
			}
		}
	}
	
## EP2 装配Bean ##
`Spring`提供了三种机制来装配`bean`：

 - 在`xml`文件中显式配置
 - 在`java`中显式配置
 - 隐式的`bean`发现机制和自动装配

## 自动化装配`bean` ##
`spring`从两个角度来实现自动化装配：

 - 组件扫描(`component scanning`):`Spring`会自动发现应用上下文中所创建的`bean`
 - 自动装配(`autowiring`):自动满足依赖

**相关注解**

 - `@Component`:类级注解，表示一个`bean`是可以被自动扫描的
 - `@ComponentScan`:组件扫描注解，配置相关属性可以自动扫描某些组件，一般和`@Configuration`注解一起使用
 - `<context:componnet-scan base-package="">`:扫描指定包下的所有`Componnet`，并将它们注册到`Spring`的上下文
 - `@Autowired`:实现自动装配

**相关命名空间**
1、`c`命名空间，用来声明构造器参数

    <beans xmlns="http://www.springframework.org/schema/c">
    <bean id="" class="" c:cd-ref=""/>
    
`c`命名空间语法：`c:cd-ref="bean引用"`，如果是引用`bean`引用，需要加上`ref`，如果是直接引用字面量，则不需要加上`ref`，`c:cd="字面量"`

2、`p`命名空间，用来声明属性
和`c`命名空间语法一样

    <beans xmlns="http://www.springframework.org/schema/p">
    <bean id="" class="">
        <p:title="字面量"/>
        <p:title-ref="bean引用"/>
    </bean>

3、`util`命名空间，用来声明集合类型的`bean`

|元素|描述|
|:-:|:-:|
|`<util:constant>`|引用某个类型的`public static`域，将其暴露为`bean`|
|`<util:list>`|创建一个`java.util.List`类型的`bean`，其中包含值或引用|
|`<util:map>`|创建一个`java.util.Map`类型的`bean`，其中包含值或引用|
|`<util:properties>`|创建一个`java.util.Properties`类型的`bean`|
|`<util:property-path>`|引用一个`bean`的属性(或内嵌属性)，并将其暴露为`bean`|
|`<util:set>`|创建一个`java.util.Set`类型的`bean`，其中包含值或引用|

## EP3 高级装配 ##
## 环境和Profile ##
**Java Config配置profile**

`@Profile`注解可以指定`bean`在哪些`profile`下是有效的，该注解可以注解到方法或者类上

注解在类上，标明该类中所有的`bean`都在该`profile`下才有效，例如：

    @Configuration
    @Profile("dev")
    public class DevProfileConfig {
    
        @Bean
        some bean method;
    }

该注解也可以注解到`bean`的声明方法上，标明该`bean`在该`profile`下才有效

**XML中配置profile**
可以在`<beans>`节点声明`profile`

    <beans profile="dev">
        <bean .../>
    </beans>

 **激活profile**
 `spring`在确定哪个`profile`处于激活状态时，需要依赖两个独立的属性：`spring.profile.active`和`spring.profile.default`，有多种方式来设置这两个属性
 

 - 作为`DispatcherServlet`的初始化参数
 - 作为`Web`应用的上下文参数
 - 作为`JNDI`条目
 - 作为环境变量
 - 作为`JVM`的系统属性
 - 在继承测试类上，使用`@ActiceProfiles`注解设置

例如在`web.xml`中`DispatcherServlet`的初始化参数中设置：

    <servlet>
        <servlet-name></servlet-name>
        <servlet-class></servlet-class>
        <init-param>
            <param-name>spring.profile.active</param-name>
            <param-value>dev</param-value>
        </init-param>
    </servlet>

## 条件化bean/@Conditional注解 ##
看下`@Conditional`注解：

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Conditional {
        Class<? extends Condition>[] value();
    }
    
再看下`Condition`接口：

    public interface Condition {
        boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
    }
    
`@Conditional`注解的`bean`只有在其`value`属性的`matches()`方法返回`true`时才会被创建并注册到`spring`上下文。
之前的`@Profile`就是组合了`@Conditional`注解：

   

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Conditional(ProfileCondition.class)
    public @interface Profile {
        String[] value();
    }
    
    class ProfileCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		if (context.getEnvironment() != null) {
			MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(Profile.class.getName());
			if (attrs != null) {
				for (Object value : attrs.get("value")) {
					if (context.getEnvironment().acceptsProfiles(((String[]) value))) {
						return true;
					}
				}
				return false;
			}
		}
		return true;
	}

    }
  
## @Primary ##
`@Primary`注解标明`bean`的首选性，避免`autowired`时出现歧义

## @Qualifier ##
使用`bean`的`id`限定

    @Autowired
    @Qualifier("iceCream")  //将会寻找id为iceCream的bean装配导入
    public void setDessert(Dessert dessert) {
        this.dessert = dessert;
    }
    
通过组合`@Qualifier`创建自定义注解

    @Qualifier
    public @interface Creamy {}
    
    @Qualifier
    public @interface Cold {}
    
接着便可以使用`@Creamy`和`@Cold`注解来替换`@Qualifier`

`java`本身不允许在同一个条目上多次使用相同的注解，除非该注解在定义的时候带有`@Repeatable`

## Bean的作用域 ##
在默认情况下，`spring`应用上下文中所有的`bean`都是作为单例的形式创建的，也就是说，无论该`bean`会被注入多少次，都是同一个实例。`spring`内部定义了多种作用域，包括：

 - 单例(`singleton`):在整个应用中只有一个实例
 - 原型(`prototype`):创建新的实例
 - 会话(`session`):在`web`应用中，为每个会话创建一个`bean`实例
 - 请求(`request`):在`web`应用中，为每个请求创建一个`bean`实例

可以在`bean`的定义上使用`@Scope`注解声明作用域，例如：

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public class Notepad {}
    
在`web`应用中，如果能实例化在会话和请求范围内共享的`bean`，是非常有价值的事情，例如，在典型的电子商务应用中，可能会有一个`bean`代表用户的购物车，如果购物车是单例的话，那么将会导致所有的用户都会向同一个购物车中添加商品，另一方面，如果购物车是原型域的话，那么在应用中某一地方往购物车中添加商品，在应用中的另一地方可能就不可用了，因为这里注入的是另外一个原型作用域的购物车，就购物车`bean`
而言，会话作用域是最适合的，因为它和用户关联性最大，要指定会话作用域，可以使用`@Scope`注解，例如：

    @Component
    @Scope(value=WebApplicationContext.SCOPE_SESSION,
            proxyMode=ScopedProxyMode.INTERFACES)
    public ShoppingCart cart() {}
    
这将为应用中的每个`session`会话创建一个单独的购物车实例，因此在某个指定的会话中，该购物车实例相当于是单例的

注意上面的`proxyMode`，这里使用了代理，我们在使用该`bean`时，实际上使用的时`bean`的代理，这个代理暴露与`ShoppingCart`相同的方法，但当调用`ShoppingCart`的方法时，代理会对其进行懒解析并将调用委托给会话作用域内真正的`ShoppingCart bean` ，上面的代理模式选择了`INTERFACES`，即基于`JDK`动态代理(接口代理)，还有一种`TARGET_CLASS`，即`CGLIB`代理(类代理)

**XML声明作用域代理**
    
    //引入aop命名空间
    <bean id="cart" class="" scope="session>
        <aop:scoped-proxy proxy-target-class="true"/> //默认使用CGLIB代理，可以设置proxy-target-class属性来调整代理方式
    </bean>
    
## 运行时注入属性 ##
可以在`bean`定义中通过`${}`注入属性，在此之前必须先配置`PropertyPlaceholderConfigurer`，它能基于`Environment`解析占位符

    //引入context命名域
    <context:property-placeholder/>
    
## EP4  AOP编程##
切面思想：我们可以在一个地方定义通用功能，但是可以通过声明的方式定义这个功能要以何种方式在何处应用，而无需修改受影响的类。横切关注点可以被模块化为特殊的类，这些类被称为切面(`aspect`)，这样做有两个好处：首先，现在每个关注点都集中在一个地方，而不是分散在多处代码中，其次，服务模块更简洁，因为它们只包含主要关注点的代码，而次要关注点的代码被转移到切面中。

## 编写切点 ##
首先定义一个接口：

    package concert;
    public interface Performance {
        public void perform();
    }
    
定义切点：

    execution(** concert.Performance.perform(..))    //表示在执行Performance.perform方法时会触发通知的调用，参数任意、返回值任意
    execution(** concert.Performance.perform(..) && within(concert.*))   //&&操作符表示与(and)，表达式表示在执行perform方法并且在concert包下
    
## 创建切面 ##
使用注解创建切面，相关注解：

 - `@Aspect`:声明切面
 
 - `@Before`:声明前置通知，在目标方法调用之前执行
 
 - `@After`:声明后置通知，在目标方法返回或者抛出异常后调用
 
 - `@AfterReturning`:声明返回通知，在目标方法返回后调用
 
 - `@AfterThrowing`:声明异常通知，在目标方法抛出异常后调用
 
 - `@Around`:声明环绕通知，会将目标方法封装起来
 
 - `@Poingcut`:声明切点
 
 完整例子：
 
 
1
    
    @Aspect
    public class Audience {
        @Poingcut("execution(** concert.Performance.perform(..))")
        public void performance() {}    //此方法只是一个切点标识，可以不实现
        
        @Before("performance()")
        public void taskSeats() {
            System.out.println("");
        }
    
    }
    
    //将切面声明为bean，并使用@EnableAspectJAutoProxy注解启用自动代理功能
    @Configuration
    @EnableAspectJAutoProxy
    @ComponentScan
    public class ConcertConfig {
    
        @Bean
        public Audience audience() {
            return new Audience();
        }

    }
    
    //@EnableAspectJAutoProxy定义
    public @interface EnableAspectJAutoProxy {
        
        //默认使用jdk动态代理
    	boolean proxyTargetClass() default false;
    
    	boolean exposeProxy() default false;
    
    }

**xml方式声明aop切面**

    <aop:aspect-proxy/>
    <bean class="concert.Audience"/>
    
## 创建环绕通知 ##
例子：

    @Around("performance()")
    public void watchPerformance(ProceedingJointPoint jp) {
        try {
            System.out.println("before method");
            jp.proceed();
            System.out.println("after method");
        } catch (Throwable e) {
            ...
        }
    }

声明环绕通知方法时，`ProceedingJointPoint`这个参数一定要有，用来指定被通知的方法

## 处理通知中的参数 ##
假如被通知方法带参数，例如将`TrackCounter`声明为这样一个切面：

    @Pointcut("execution(** soundsystem.CompactDisc.playTrack(int)) && args(trackNumber)")
    public void trackPlayed(int trackNumber) {}
    
    @Before("trackPlayed(trackNumber)")
    public void countTrack(int trackNumber) {
        //
    }
    
上述例子中`args(trackNumber)`表示传递给`playTrack(int)`方法的参数也会传递到通知中，参数的名称`trackNumber`也和切点方法签名中的参数相匹配

## XML配置aop ##
将上述`java`配置改写成`xml`配置：
    
    <bean id="trackCounter" class="soundsystem.TrackCounter"/>
    <aop:config>
        <aop:aspect ref="trackCounter">
            <aop:pointcut id="trackPlayed" expression="execution(** soundsystem.CompactDisc.playTrack(int)) and args(trackNumber)"/>
            <aop:before pointcut-ref="trackPlayed" method="countTrack"/>
        </aop-aspect>
    </aop-config>


## EP5 Spring MVC ##
## 配置DispatcherServlet ##
如下代码所示配置`DispatcherServlet`：

    public class SpittrWebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
        @Override
        protected Class<?>[] getRootConfigClasses() {
            return new Class<?>[] {RootConfig.class};
        }
    
        @Override
        protected Class<?>[] getServletConfigClasses() {  //指定配置类
            return new Class<?>[] {WebConfig.class};
        }
    
        @Override
        protected String[] getServletMappings() {   //指定映射路径，"/"表示所有路径都会交给DispatcherServlet处理
            return new String[] {"/"};  //
        }
    }
    
## 两个上下文之间的故事 ##

当`DispatcherServlet`启动的时候，它会创建`Spring`应用上下文，并加载配置文件或配置类中的`bean`，但在`Spring Web`应用中，通常还会存在另外一个应用上下文，这个上下文是由`ContextLoaderListener`创建的。我们希望`DispatcherServlet`加载包含`WEB`组件的`bean`，如**控制器、视图解析器以及处理器映射**，而`ContextLoaderListener`要加载应用中的其他`bean`，这些`bean`通常是**驱动应用后端的中间层和数据层组件**。
事实上，`AbstractAnnotationConfigDispatcherServletInitializer`会同时创建`DispatcherServlet`和`ContextLoaderListener`。
    
## JAVA配置WebConfig ##
如下配置，使用`@EnableWebMvc`注解：

    @Configuration  
    @EnableWebMvc   //开启WebMvc，将WebMvcConfigurationSupport中的配置导入
    @ComponentScan()
    public class WebConfig extends WebMvcConfigAdapter {
        //bean definition or override 
    }
    
## mock MVC 测试 ##
`Spring`现在包含了一种`mock Spring Mvc`并针对控制器执行`HTTP`请求的机制，这样的话在测试控制器的时候就没必要启动`Web`服务器和浏览器了。

    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;    //静态导入
    public class HomeControllerTest {
        @Test
        public void testHomePage() throws Exception {
            HomeController controller = new HomeController();
            MockMvc mockMvc = standaloneSetup(controller).build();  //搭建MockMvc
            mockMvc.perform(get("/")).andExpect(view().name("home"));   //模拟HTTP GET请求，预期得到home视图
        }
    }
    
需要注意的一点是，控制器中可以使用`Map`来代替`ModelMap`，并且当控制器返回值不是`String`类型时，加入返回的是一模型属性`List`集合，那么会将该`List`放入模型中，`key`会根据其类型推断出来，而视图名称会根据请求路径推断出来，如下例子：

    @RequestMapping("/views/model")
    public List<String> model() {
        List<String> list = new ArrayList<String>();
        list.add("java");
        return list;    //那么该控制器会将返回的List添加到模型中，key=stringList，视图名称为/views/model，即根据请求路径来
    }
    
## EP6 视图解析器 ##
将控制器中请求处理的逻辑和视图中的渲染实现解耦是`Spring MVC`的一个重要特性，视图的解析交由视图解析器完成(`ViewResolver`)：
    
    //接收视图名称和Locale对象，解析成视图View对象
    View resolveViewName(String viewName, Locale locale) throws Exception;
    
视图接口：
    
    //接收模型model和Servlet的request、response，渲染视图
    void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception;
    
`Spring`自带多个视图解析器：

|视图解析器|描述|
|:-:|:-:|
|`BeanNameResolver`|将视图解析为`Spring`上下文中的`bean`，其中`bean`的`ID`和视图名称相同|
|`ContentNegotiatingViewResolver`|通过考虑客户端需要的内容类型来解析视图，委托给另外一个能够产生对应内容类型的视图解析器|
|`FreeMarkerViewResolver`|将试图解析为`FreeMarker`模板|
|`InternalResourceViewResolver`|将视图解析为`Web`应用的内部资源(一般为`JSP`)|
|`UrlBasedViewResolver`|直接根据视图的名称解析视图，视图的名称会匹配一个物理视图的定义|

## EP7 Spring MVC的高级技术 ##
## 在web.xml中声明DispathcerServlet ##

    <?xml version="1.0" encoding="UTF-8" ?>
    <web-app xmlns="http://java.sun.com/xml/ns/javaee"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
             version="3.0">
        <context-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath*:spring/applicationContext.xml</param-value> //设置ContextLoaderListener上下文配置文件路径
        </context-param>
        <listener>
            <listener-class>
                org.springframework.web.context.ContextLoaderListener   //ContextLoaderListener上下文
            </listener-class>
        </listener>
        <!--servlet配置-->
        <servlet>
            <servlet-name>dispatcher</servlet-name>
            <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>    //DispatcherServlet上下文
            <load-on-startup>1</load-on-startup>
        </servlet>
        <servlet-mapping>
            <servlet-name>dispatcher</servlet-name>
            <url-pattern>/</url-pattern>
        </servlet-mapping>
    </web-app>
    
以上就是一般`DispathcerServlet`的基础配置，如果要使用基于`java`的配置，我们需要使用`AnnotationConfigWebApplicationContext`，上述代码改成：

    <context-param>
        <param-name>contextClass</param-name>
        <param-value>org.springframework.web.context.support.AnnotationConfigWebApplicationContext</param-value>
    </context-param>
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>RootConfig</param-value>
    </context-param>
    
    <servlet>
        ...
        <init-param>
            <param-name>contextClass</param-name>
            <param-value>AnnotationConfigWebApplicationContext</param-value>
        </init-param>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>WebConfig</param-value>
        </init-param>
    </servlet>

## 处理Multipart形式的数据 ##
首先必须要配置一个`multipart`解析器，通过它来告诉`DispatcherServlet`怎么处理`multipart`请求

**配置multipart解析器**
`DispatcherServlet`并没有实现任何解析`multipart`请求数据的功能，它将该任务委托给了`Spring`的`MultipartResolver`策略接口的实现，通过这个实现类来解析`multipart`请求中的内容：

 - `CommonMultipartResolver`：使用`Jakarta Commons FileUpload`来解析`multipart`请求
 
 - `StandardServletMultipartResolver`:依赖于`Servlet 3.0`对`multipart`请求的支持
 
 **配置Jakarta Commons FileUpload multipart解析器**
 通常来讲，`StandardServletMultipartResolver`会是最佳的选择，但是如果我们要将应用部署到非`Servlet 3.0`的容器中，那么就需要选择替代方案，`Spring`内置了`CommonsMultipartResolver`，声明如下：
 
1

    @Bean
    public MultipartResolver multipartResolver() {
        return new CommonsMultipartResolver();
    }
    
`CommonsMultipartResolver`不需要指定临时文件路径，默认情况下，该路径就是`Servlet`容器的临时目录，不过也可以显式设置临时路径

    @Bean
    public MultipartResolver multipartResolver() throws IOException {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setUploadTempDir(new FileSystemResource("/tmp/spittr/uploads"));  //设置临时文件路径
        multipartResolver.setMaxUploadSize(2097152);    //设置上传文件大小上限
        multipartResolver.setMaxInMemorySize(0);    //上传文件直接存盘，不会存在内存中
        return multipartResolver;
    }
    
**处理multipart请求**
使用`@RequestPart`注解(处理`multipart/form-data`请求)绑定方法参数：

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String processRegistration(@RequestPart("profilePicture") byte[] profilePicture,
                                      @Valid Spitter spitter,
                                      Errors errors) {
        //...
        return "";
    }
    
除此之外，`Spring`还提供了`MultipartFile`接口来接收`multipart`请求

## 处理异常 ##
`Spring`提供了多种方式将异常转换为响应：

 - 特定的`Spring`异常会自动映射为指定的`HTTP`状态码
 
 - 异常上可以添加`@ResponseStatus`注解，从而将其映射为某一个`HTTP`状态码
 
 - 在方法上可以添加`@ExceptionHandler`注解，使其用来处理异常
 
 **将异常映射为状态码**
 `Spring`内部自动将某些异常映射称为状态码
 |`Spring`异常|`HTTP`状态码|
 |:-:|:-:|
 |`BindException`|`400 Bad Request`|
 |`ConversionNotSupportedException`|`500 Internal Server Error`|
 
 
`Spring`还提供了一种机制，`@ResponseStatus`注解将异常转换为`HTTP状态码`，例如：

    //在控制器中抛出异常
    @RequestMapping(value = "/spittle")
    public String spittle(String spittle) {
        if(spittle == null) {
            throw new SpittleNotFoundException();
        }
        return "";
    }
    
    //在异常类上注解@ResponseStatus
    @ResponseStatus(value = HttpStatus.NOT_FOUND,
                reason = "Spittle Not Found")
    public class SpittleNotFoundException extends RuntimeException{
    }
    
**编写处理异常的方法**
在控制器中加入如下异常处理方法，使用`@ExceptionHandler`注解：

    @ExceptionHandler(SpittleNotFoundException.class)
    public String handlerSpittleNotFoundException() {
        return "error/spittle";
    }
以上异常处理方法只会处理该控制器内部抛出的异常，不会处理全局异常，如果需要处理全局异常，则需要使用`@ControllerAdvice`注解：

    @ControllerAdvice
    public class AppWideExceptionHandler {
    
        @ExceptionHandler(SpittleNotFoundException.class)
        public String handleSpittleNotFoundException() {
            return "error/spittle";
        }
    }
    
## 跨重定向请求传递数据 ##

在处理完`POST`请求后，**通常来讲一个最佳实践就是执行一下重定向**，除了其他的一些因素以外，这样能够防止用户点击浏览器的刷新按钮或者后退箭头时，客户端重新执行危险的`POST`请求。
具体来讲，正在发起重定向功能的方法该如何发送数据给重定向的目标方法呢，一般来讲，当一个处理器方法完成之后，该方法指定的模型数据将会复制到请求中，并作为请求的属性，请求会转发(`forward`)到视图上进行渲染，因为控制器方法和视图所处理的是同一个请求，所以在请求转发的过程中，请求属性得以保存。
但是，当控制器的结果是重定向的话，原始的请求也就结束了，并且会发起一个新的`GET`请求，原始请求中的模型数据也就随着请求一起消亡了，在新的请求中，没有任何的模型数据，这个请求必须要自己计算数据。即重定向后模型数据无法存活。
从重定向的方法传递模型数据给处理重定向方法中：
1、使用`URL`模板以路径变量和查询参数的形式传递数据
2、通过`flash`属性发送数据
    
如下代码：

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String processRegistration(Spitter spitter, Model model) {
        model.addAttribute("username", "yuding");
        model.addAttribute("spitterId", 10);
        return "redirect:/spitter/{username}";
    }
    
模型中的`username`会作为占位符填充到`URL`模板中，而不是直接连接到重定向`String`中，同时，模型中所有其他原始类型值都可以添加到`URL`中作为查询参数，如上例中的`spitterId`，因此最终重定向的路径为`/spitter/yuding?spitterId=10`

**使用flash属性**
`Spring`通过`RedirectAttributes`设置`flash`属性的方法，这是`Model`的一个子接口。

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String processRegistration(Spitter spitter, RedirectAttributes model) {
        model.addAttribute("username", "yuding");
        model.addAttribute("spitter", spitter); //设置flash属性，key=spitter，在重定向处理方法中可以访问该属性
        return "redirect:/spitter/{username}";
    }

在处理重定向的方法中就可以拿到`spitter`属性

    @RequestMapping("/{username}")
    public String showSpitterProfile(@PathVariable String username, Model model) {
        if(!model.containsAttribute("spitter")) {   //获取flash属性
            //
        }
        return "profile";
    }


## EP8 Spring集成JDBC ##
## 配置数据源 ##
`Spring`提供了多种方式配置数据源：

 - 通过`JDBC`驱动程序定义的数据源
 
 - 通过`JNDI`查找的数据源
 
 - 连接池的数据源
 
 ## 使用数据源连接池 ##
 `Spring`并没有提供数据源连接池的相关实现，但是我们仍然有多项可用的方案：
 
 - `Apache Commons DBCP `
 
 - `cp30`
 
 - `BoneCP`
 
 这些连接池中的大多数都能配置为`Spring`的数据源，在一定程度上与`Spring`自带的`DriverManagerDataSource`或者`SingleConnectionDataSource`很类似，如下就是配置`DBCP BasicDataSource`：

        <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource"
            p:driverClassName="org.h2.driver"
            p:username="sa"
            p:password=""
            p:initialSize="5"
            p:maxActive="10"/>
        
        
还可以将上述`xml`配置改写成`java`配置，具体就不展示了。

## 配置JDBC模板 ##
`Spring`提供了三种模板供选择：

 - `JdbcTemplate`:最基本的模板，这个模板支持简单的`JDBC`数据库访问功能以及基于索引参数的查询
 
 - `NamedParameterJdbcTemplate`:使用该模板类执行查询时可以将值以命名参数的形式绑定到`SQL`中，而不是使用简单的索引参数
 
 - `SimpleJdbcTemplate`:改模板利用`java 5`的一些特性和自动装箱、泛型以及可变参数列表来简化`JDBC`模板的使用。
 
为了让`JdbcTemplate`正常使用，需要配置`JdbcTemplate`

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

配置完成后，可以在其他位置引用`JdbcTemplate`

## EP9 Spring集成ORM ##
## Spring集成Hibernate ##
**声明Hibernate的Session工厂**
    使用`Hibernate`所需的主要接口是`org.hibernate.Session`接口，该接口提供了基本的数据访问功能，如保存、更新、删除以及从数据库中加载对象的功能，通过`Hibernate`的`Session`接口，应用程序的`Repository`能够满足所有的持久化需求，获取`Session`对象的标准方式是借助于`Hibernate SessionFactory`接口的实现类，除了一些其他的任务，`SessionFactory`主要负责`Hibernate Session`的打开、关闭和管理。
    在`Spring`中，我们要通过`Spring`的某一个`Hibernate Session`工厂`bean`来获取`SessionFactory`

 - `org.springframework.orm.hibernate3.LocalSessionFactoryBean`
 
 - `org.springframework.org.hibernate3.annotation.AnnotationSessionFactoryBean`
 
 - `org.springframework.org.hibernate4.LocalSessionFactoryBean`
 
这些`bean`都是`FactoryBean`接口的实现类，它会产生一个`Hibernate SessionFactory`，它能够装配进任何`SessionFactory`类型的属性中。

## Spring与JPA ##
## 配置实体管理器工厂 ##
简单来说，基于`JPA`的应用程序需要使用`EntityManagerFactory`的实现类来获取`EntityManager`实例，`JPA`定义了两种类型的实体管理：应用程序管理类型和容器管理类型
这两种实体管理器工厂分别由对应的`Spring`工厂`bean`创建：

 - `LocalEntityManagerFactoryBean`:生成应用管理类型的`EntityManagerFactory`
 
 - `LocalContainerEntityManagerFactoryBean`:生成容器管理类型的`EntityManagerFactory`
 
 **配置应用程序管理类型的JPA**
 对于应用程序管理类型的实体管理器工厂来说，它绝大部分配置信息来源于一个`persistence.xml`的配置文件，这个文件必须位于类路径下的`META-INF`目录下
 通过以下的注解方法声明`LocalEntityManagerFactoryBean`:
 

        @Bean
        public LocalEntityManagerFactoryBean entityManagerFactoryBean() {
            LocalEntityManagerFactoryBean emfb = new LocalEntityManagerFactoryBean();
            emfb.setPersistenceUnitName("spitterPU");   //spitterPU是持久化单元的名称，在persistence.xml文件中定义
            return emfb;
        }
    
    <persistence xmlns="http://java.sun.com/xml/ns/persistence" version="1.0">
        <persistence-unit name="spitterPU">
            <class>Spitter</class>
            <properties>
                <property name="" value=""/>
                ...
            </properties>
        </persistence-unit>
    </persistence>

## 编写基于JPA的Repository ##
如下代码：

    @Repository
    @Transactional
    public class JpaSpitterRepository implements SpitterRepository {
        
        @PersistenceUnit
        private EntityManagerFactory emf;
        
        public void addSpitter(Spitter spitter) {
            emf.createEntityManager().persist(spitter);
        }
        ...
    }
    
上述代码在每次调用`createEntityManager()`方法时都会创建一个新的`EntityManager`，而`EntityManager`并不是线程安全的，一般来讲不适合注入到像`Repository`这样的单例`bean`中，因此需要将`@PersistenceUnit`替换成注解`@PersistenceContext`，该注解并没有将真正的`EntityManager`设置给`Repository`，而是给了一个`EntityrManager`代理，真正的`EntityManager`是与当前事务关联的那一个，如果不存在这样的`EntityManager`，那么会新创建一个，那么我们就能始终以线程安全的方式使用实体管理器。

    @Repository
    @Transactional
    public class JpaSpitterRepository implements SpitterRepository {
        
        @PersistenceContext
        private EntityManager em;
        
        public void addSpitter(Spitter spitter) {
            em.persist(spiter);
        }
        
        ...
    }
    
## 借助Spring Data实现自动化的JPA Repository ##
上述的`em.persist(spitter)`代码也是样板代码，对于不同的实体类，其持久化的方法几乎一样，我们可以通过实现`Repository`接口彻底告别这类模板化代码

    public interface SpitterRepository extends JpaRepository<Spitter, Long> {}
    
实现了`JpaRepository`接口，用来持久化`Spitter`对象，并且其`ID`类型时`Long`，另外它还会继承18个执行持久化操作的通用方法，如保存、删除以及根据`ID`查询等。
接下来在`xml`文件中配置:

    xmlns:jpa="http://www.springframework.org/schema/data/jpa"  //引入jpa命名域
    <jpa:repositories base-package="..."/>  //base-package指定Repository所在包
    
如上配置会扫描`base-package`下所有拓展自`Repository`接口的所有接口，并且在应用启动时自动生成该接口的实现类。
如果使用`java`配置的话，需要在配置类上加上`@EnableJpaRepositories`注解。

    @Configuration
    @EnableJpaRepositories
    public class JpaConfiguration() {
        //...
    }
    
`Spring Data JPA`为`Spitter`对象提供了18个便利的方法来进行通用的`JPA`操作，而无需编写任何持久化代码，但是如果你的需求超过了这些方法，需要自己定制。

**定义查询方法**
定义自己的查询方法，修改`SpitterRepository`接口：

    public interface SpitterRepository extends JpaRepository<Spitter, Long> {
        Spitter findByUsername(String username);
    }
        
实际上，我们并不需要实现`findByUsername()`，方法签名已经告诉`Spring Data JPA`足够的信息来创建该方法的实现,`Spring Data`可以通过方法签名来推测方法的目的，`Repository`方法由一个**动词、主题、关键词By、以及一个断言**组成，在`findByUsername()`中，动词是`find`，断言是`Username`，主题没有指定，暗含的主题是`Spitter`。`Spring Data`允许4个动词：`get/read/find/count`。
在断言中，会有一个或者多个限制结果的条件，如`IsNot/Not`等

**声明自定义查询@Query**
上述`DSL`依然具有一定的局限性，不过可以通过`@Query`注解来解决。
例如：

    @Query("select s from Spitter s where s.email like '%gmain.com'")
    List<Spitter> findAllGmailSpitters();
    
搞定，我们仍然不需要写上述方法的实现。

**混合自定义的功能**
当`Spring Data JPA`为`Rpository`接口生成实现的时候，它还会查找名字与接口相同并且添加了`Impl`后缀的一个类，如果这个类存在的话，`Spring Data JPA`将会把它的方法和`Spring Data JPA`所生成的方法合并在一起，对于`SpitterRepository`而言，要查找的类名是`SpitterRepositoryImpl`。

    public SpitterRepositoryImpl implements SpitterSweeper {
        
        @PersistenceContext
        private EntityManager em;
        
        public int eliteSweep() {
            String update = "some sql";
            return em.createQuery(update).executeUpdate();
        }
    }
    
    public interface SpitterSweeper {
        int eliteSweep();
    }
    
    public Interface SpitterRepository 
           extends JpaRepository<Spitter, Long>, SpitterSweeper {}
    
如果你想使用其他后缀，而不是`Impl`的话，可以在`@EnableJpaRepositories`中设置相关属性

    @Configuration
    @EnableJpaRepositories(basePackages="", repositoryImplementPostfix="Helper")
    public class JpaConfiguration() {
        //...
    }
    
也可以通过`xml`配置：

    <jpa:repositories base-package="" repository-impl-postfix="Helper"/>

## EP 缓存数据 ##
`Spring`对缓存的支持有两种方式：
1、注解驱动的缓存
2、`XML`声明的缓存

如下`java`配置启用了缓存：

    @Configuration
    @EnableCaching  //启用缓存
    public class CachingConfig {
        
        @Bean
        public CacheManager cacheManager() {    //声明缓存管理器
            return new ConcurrentMapCacheManager(); //使用ConcurrentHashMap作为缓存管理器
        }
    }
        
如果以`xml`方式配置的话，使用`Spring cache`命名空间的`<cache:annotation-driven>`元素来启用注解驱动的缓存。

    <cache:annotation-driven />
    <bean id="cacheManager" class="org.springframework.cache.concurrent.ConcurrentMapCacheManager"/>
    
本质上，`@EnableCaching`和`<cache:annotation-driven>`的工作方式是相同的，它们都会创建一个切面(`Aspect`)并触发`Spring`缓存注解的切点(`Point`)，根据所使用的注解及缓存的状态，这个切面会从缓存中获取数据，将数据添加到缓存之中或者从缓存中移除某个值。

## 配置缓存管理器 ##
缓存管理器(`CacheManager`)是缓存配置的核心，`Spring`内置了五个缓存管理器实现，如下所示：

    SimpleCacheManager/NoOpCacheManager/ConcurrentMapCacheManager/CompositeCacheManager/EhCacheCacheManager
    
`Spring Data`又提供了2个缓存管理器：

    RedisCacheManager/GemfireCacheManager
    
## 使用Ehcache缓存 ##
`Ehcache`是最为流行的缓存供应商之一，注意其名字`EhcacheCacheManager`

配置`EhcacheCacheManager`代码：

    @Configuration
    @EnableCaching
    public class CachingConfig {
        
        @Bean
        public EhcacheCacheManager cacheManager(CacheManager cm) {
            return new EhcacheCacheManager(cm);
        }
        
        @Bean
        public EhCacheManagerFactoryBean ehcache() {
            EhCacheManagerFactoryBean ehCacheFactoryBean = new EhCacheManagerFactoryBean();
            ehCacheFactoryBea.setConfigLocation(new ClassPathResource("com/spittr/cache/ehcache.xml")); //根据configLocation在类路径上寻找配置文件
            return ehCacheFactoryBean;
        }
    }
    
上述代码看起来有点诡异，实际上，`EhCacheCacheManager`如之前所说是`Spring`提供的`EhCache`缓存管理器，它需要注入`EhCacheManager`，这就需要由`EhCacheManagerFactoryBean`提供

至于`ehcache.xml`的内容，不同的应用之间会有所差别，但是至少要声明一个最小的缓存，如下声明最大的堆存储为`50MB`，存活时间是100秒：

    <ehcache>
        <cache name="spittleCache" maxBytesLocalHeap="50m" timeToLiveSeconds="100">
        </cache>
    </ehcache>
    
## 使用多个缓存 ##
如果需要使用多个缓存，可以尝试`CompositeCacheManager`:

    @Bean
    public CacheManager cacheManager(net.sf.ehcache.CacheManager cm, javax.cache.CacheManager jcm) {
        CompositeCacheManager cacheManager = new CompositeCacheManager();
        List<CacheManager> managers = new ArrayList<CacheManager>();
        managers.add(new JCacheCacheManager(jcm));
        managers.add(new EhCacheCacheManager(cm));
        managers.add(new RedisCacheManager(new RedisTemplate()));
        cacheManager.setCacheManagers(managers);
        return cacheManager;
    }
    
当查找缓存条目时，首先会从`JCacheCacheManager`开始查找`JCache`实现，然后通过`EhCacheCacheManager`检查`Ehcache`，最后会使用`RedisCacheManager`来检查`Redis`，完成缓存条目的查找。

## 缓存注解 ##
`Spring`提供了4个注解来声明缓存规则：

 - `@Cacheable`:表明在调用方法之前首先应该去缓存中查找方法的返回值，如果这个值能够找到，就会返回缓存中的值，否则这个方法就会被调用，返回值会放到缓存中
 
 - `@CachePut`:表明`Spring`应该将方法的返回值放回到缓存中，在方法调用前并不会检查缓存，方法始终会被调用
 
 - `@CacheEvict`:表明`Spring`应该在缓存中清除一个或者多个项目
 
 - `@Caching`:这是一个分组的注解，能够同时应用于其他缓存注解
 
 如上所述，`@Cachable`和`@CachePut`两个注解对缓存的操作有点不同，`@Cachable`会在调用方法之前访问缓存，如果缓存存在返回缓存值，否则调用方法并将方法返回值放入缓存中，而`@CachePut`注解则不会访问缓存，直接调用方法并将方法返回值放入缓存，它们有些共同的属性：
 
 - `value`:缓存名称
 
 - `condition`:`SpEL`表达式，如果得到的是`false`的话，不会将缓存应用到方法调用上
 
 - `key`:`SpEL`表达式，用来计算自定义缓存`key`
 
 - `unless`:`SpEL`表达式，如果得到是`true`的话，返回值不会被放大缓存中
 
 ## 自定义缓存key ##
 当将方法的返回值放入缓存中时，缓存`key`默认是方法参数，有时候需要自定义缓存`key`。
 
 `Spring`提供的`SpEL`扩展：
 |表达式|描述|
 |:-:|:-:|
 |`#root.args`|缓存方法参数数组|
 |`#root.caches`|对应的缓存数组|
 |`#root.target`|目标对象|
 |`#root.targetClass`|目标对象的类|
 |`#root.method`|缓存方法|
 |`#root.methodName`|缓存方法的名字|
 |`#result`|方法调用的返回值，不能应用在`@Cacheable`注解上|
 |`#Argument`|任意的方法参数名(如`#argName`或参数索引(如`#a0`或`#p0`))|
 
 例如，将返回值的`id`作为缓存`key`放入缓存中：
 

    @CachePut(value="spittleCache", key="#result.id")
    Spittle save(Spittle spittle);
    
例如，

    @Cacheable(value="spittleCache", 
                unless="#result.message.contains('NoCache')",
                condition="#id >= 10")
    Spittle findOne(long id);
    
上述注解表示如果参数值小于10将不会使用缓存，返回的`Spittle`也不会放入缓存中，同样如果返回的`Spittle`实例的`message`属性包含`NoCache`，也不会将结果放入缓存。**unless适合和#result一起使用，而condition由于肩负着禁用缓存的任务，因此不能等待方法返回结果时再判断是否使用缓存，最好不和#result一起使用**

## 使用XML声明缓存 ##
    
    //引入命名域
    xmlns:aop="http://www.springframework.org/schema/aop"
    xmlns:cache="http://www/springframework.org/schema/cache"
    <aop:config>
        <aop:advisor advice-ref="cacheAdvice" pointcut="execution(* com.spittr.db.SpittleRepository.*(..))"/>
    </aop:config>
    
    <cache:advice id="cacheAdvice">
        <cache:caching>
            <cache:cacheable cache="spittleCache" method="fingRecent"/>
            <cache:cache-put cache="spittleCache" method="save" key="#result.id"/>
            <cache:cache-evict cache="spittleCache" method="remove"/>
        </cache:caching>
    </cache:advice>
    <bean id="cacheManager" class="org.springframework.cache.ConcurrentMapCacheManager"/>