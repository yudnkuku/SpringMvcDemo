# Spring Boot散记

标签（空格分隔）： SpringBoot

---

1、Spring Boot自动配置
-----------------

`springboot` 自动配置 主要通过`@EnableAutoConfiguration`,`@Conditional`,`@EnableConfigurationProperties`或者`@ConfigurationProperties`几个注解来进行自动配置。
`@EnableAutoConfiguration`开启自动配置，主要作用就是调用`core`包里的`loadFactoryNames()`，将`autoconfig`包里的已经写好的自动配置加载进来。
`@Conditional`条件注解，通过判断类路径下有没有相应配置的jar包来确定是否加载和自动配置这个类，
`@EnableConfigurationProperties`的作用就是，给自动配置提供具体的配置参数，只需要写在`application.properties`中，就可以通过映射写入配置类的`Pojo`属性中。
`autoconfig`包下`META-INF/./spring.factories`定义了spring boot开启的自动配置，例如`DataSourceAutoConfiguration`开启了数据源的自动配置，这个类注解了`@EnableConfigurationProperties(DataSourceProperties.class)`，表明支持`DataSourceProperties`中定义的属性配置，可以在`application.properties`文件中自定义相关配置。
参考文档：[Spring Boot自动配置][1]

## 2 Spring Beans和依赖注入 ##
使用`@ComponentScan`(寻找`beans`)和`@Autowired`(构造方法注入)
如果应用程序代码定义在根包下，可以使用`@ComponentScan`不用带任何参数，所有应用组件(`@Component`,`@Service`,`@Repository`,`@Controller`)都会被自动注册为`Spring Beans`。
如使用`@Service`定义`bean`，并通过`@Autowired`注入依赖

    package com.example.service;
    
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;

    @Service
    public class DatabaseAccountService implements AccountService {
    
    	private final RiskAssessor riskAssessor;
    
    	@Autowired
    	public DatabaseAccountService(RiskAssessor riskAssessor) {
    		this.riskAssessor = riskAssessor;
    	}
    
    	// ...
    
    }
参考[@Autowired注解][2]
如果目标`bean`只有一个构造函数，`@Autowired`可以省略

## 3 外部配置 ##
参考地址：[Spring Boot外部配置参考地址][3]
`Spring Boot`允许外部配置，你可以使用属性文件(`application.properties`)、`YAML`文件(`application.yaml`)、环境变量和命令行参数进行外部配置，属性值可以通过`@Value`注解直接注入`beans`中，或者通过`Spring`的环境抽象访问，或者通过`@ConfigurationProperties`绑定到结构化对象
`Spring Boot`使用了特殊的`PropertySource`顺序，允许覆盖，顺序如下：

 - 命令行参数
 - `Java`系统属性
 - `OS`环境变量
 - 随机属性值(`random.*`)
 - `jar`包外部指定`profile`的`application.properties`(`application-{profile}.properties`和`YAML`文件)
 - `jar`包内部指定`profile`的`application.properties`(`application-{profile}.properties`和`YAML`文件)
 - `jar`包外部属性文件(`application.properties`和`YAML`文件)
 - `jar`包内部属性文件(`application.properties`和`YAML`文件)
 - `@Configuration`类上的`@PropertySource`注解
 - 默认属性(通过`SpringApplication.setDefaultProperties`指定)

## 3.1 配置随机值 ##
`RandomValuePropertySource`用于导入随机值，包括`integers/longs/uuids/strings`

    my.secret=${random.value}
    my.number=${random.int}
    my.bignumber=${random.long}
    my.uuid=${random.uuid}
    my.number.less.than.ten=${random.int(10)}
    my.number.in.range=${random.int[1024,65536]}
## 3.2 访问命令行属性 ##
默认情况下，`SpringApplication`将任何命令行选项参数(以`--`开始，如：`--server.port=9000`)转换为`property`，并将其加入`Spring`环境中([参考Spring环境抽象][4])，

3.3 应用属性文件(property file)
-------------------------

`Spring Application`从`application.properties`文件中加载属性，并将其加入`Spring`环境，加载目录顺序如下：

 - 当前目录的`/config`子目录
 - 当前目录
 - 类路径`/config`包
 - 类路径根

3.4 使用YAML替代Properties
----------------------

`YAML`是`JSON`的超集，对于指定层次型的配置数据是很方便的，当`SnakeYAML`库在类路径上时，`SpringApplication`会自动支持`YAML`。

**3.4.1 加载YAML**
`Spring Framework`提供了两个方便的类加载`YAML`文件，`YamlPropertiesFactoryBean`将`YAML`加载成`Properties`，而`YamlMapFactoryBean`将`YAML`加载城`Map`

`YAML`列表等价于`[index]`
`YAML`文件：

    my:
    servers:
    	- dev.example.com
    	- another.example.com

等价于这些属性：

    my.servers[0]=dev.example.com
    my.servers[1]=another.example.com
`@ConfigurationProperties`是一个类级和方法级注解，注解在类定义或者`@Bean`注解的方法上，用于绑定第三方配置。
`@EnableConfigurationProperties`，类级注解，注册`@ConfigurationProperties`注解的`bean`
`@ConfigurationProperties`注解的`bean`和其他所有`bean`一样，可以通过`@Autowired`注解导入，可以使用`@Component`和`@ConfigurationProperties`注解组合省略`@EnableConfigurationProperties`
使用`@Component`和`@ConfigurationProperties`组合定义`bean`：
   

     @Component
        @ConfigurationProperties(prefix="acme")
        public class AcmeProperties {
        
        	// ... see the preceding example
        
        }
`application.yml`文件：

    # application.yml
    
    acme:
    	remote-address: 192.168.1.1
    	security:
    		username: admin
    		roles:
    		  - USER
    		  - ADMIN
导入`AcmeProperties`：

    @Service
    public class MyService {
    
    	private final AcmeProperties properties;
    
        	@Autowired
        	public MyService(AcmeProperties properties) {
        	    this.properties = properties;
        	}
        
         	//...
        
        	@PostConstruct
        	public void openConnection() {
        		Server server = new Server(this.properties.getRemoteAddress());
        		// ...
        	}
        
        }

`YamlPropertySourceLoader`类可以将`YAML`作为`PropertySource`暴露给`Spring`环境，便可以使用`@Value`来访问这些属性。

**3.4.2 Multi-profile YAML**
可以使用`spring.profiles`来指定文档应用时的`profile`

    server:
    	address: 192.168.1.100
    ---
    spring:
    	profiles: development
    server:
    	address: 127.0.0.1
    ---
    spring:
    	profiles: production
    server:
    	address: 192.168.1.120

3.5 类型安全配置属性
------------

使用`@Value("${property}")`注解注入配置属性显得很繁琐，`Spring Boot`提供了另一种注解方式，即使用`@ConfigurationProperties`，从环境属性中注入值

    @ConfigurationProperties("acme")
    public class AcmeProperties {}
还需要在`@EnableConfigurationProperties`注解中进行注册属性类：

    @Configuration
    @EnableConfigurationProperties(AcmeProperties.class)
    public class MyConfiguration {
    }
当`@ConfigurationProperties` bean以这种方式注册时，这个`bean`就获得了一个传统上的名字：`<prefix>-<fqn>`(`<prefix>`是`@ConfigurationProperties`里定义的`prefix`，`<fqn>`是该类的全名)

**3.5.1 第三方配置**
`@ConfigurationProperties`除了可以注解类，还可以注解`@Bean`方法，如此将属性绑定到第三方组件上。
为了从环境属性中配置`bean`，可以添加`@ConfigurationProperties`至`bean`注册：

    @ConfigurationProperties(prefix = "another")
    @Bean
    public AnotherComponent anotherComponent() {
    	...
    }

**3.5.2 宽松绑定**
`Spring Boot`使用宽松的规则将环境属性绑定到`@ConfigurationProperties bean`，因此没有必要使两者属性名完全一样

    @ConfigurationProperties(prefix="acme.my-project.person")
    public class OwnerProperties {
    
    	private String firstName;
    
    	public String getFirstName() {
    		return this.firstName;
    	}
    
    	public void setFirstName(String firstName) {
    		this.firstName = firstName;
    	}
    
    }
可以匹配的属性名称：
|Property|Note|
|---|---|
|`acme.my-project.person.first-name`|`Kebab case`，通常用于`.properties`和`.yml`文件 |
|`acme.myProject.person.firstName`|驼峰法|
|`acme.my_project.person.first_name`|下划线法，`.properties`和`.yml`文件备选|
|`ACME_MYPROJECT_PERSON_FIRSTNAME`|大写法，使用系统环境变量时推荐使用|

**3.5.3 合并复杂类型**
当`@ConfigurationProperties`注解类中含有集合类时，具体看`.yml`文件的写法

    @ConfigurationProperties("acme")
    public class AcmeProperties {
    
    	private final List<MyPojo> list = new ArrayList<>();
    
    	public List<MyPojo> getList() {
    		return this.list;
    	}
    
    }
如下配置：

    acme:
      list:
        - name: my name
          description: my description
    ---
    spring:
      profiles: dev
    acme:
      list:
        - name: my another name
`profiles`环境不同时，`list`中包含的对象就不同，`map`类似：

    acme:
      map:
        key1:
          name: my name 1
          description: my description 1
    ---
    spring:
      profiles: dev
    acme:
      map:
        key1:
          name: dev name 1
        key2:
          name: dev name 2
          description: dev description 2

**3.5.4 @ConfigurationProperties验证**
结合`@Validated`进行验证

    @ConfigurationProperties(prefix="acme")
    @Validated
    public class AcmeProperties {
    
    	@NotNull
    	private InetAddress remoteAddress;
    
    	// ... getters and setters
    
    }

## 4 开发WEB应用 ##
`Spring Boot`内置了`HTTP`服务器，包括`tOMcat`、`Jetty`、`Undertow`和`Netty`，加入`spring-boot-starter-web`依赖即可。

4.1 Spring MVC
--------------

使用`@Controller`和`@RestController`处理`HTTP request`处理，使用`@RequestMapping`进行请求映射

4.1.1 Spring MVC自动配置
--------------------

`Spring Boot`提供了许多自动配置：

 - `ContentNegotiatingViewResolver`和`BeanNameViewResolver` beans
 - 支持服务静态资源，包括`WebJars`
 - `Converters`,`GenericConverter`和`Formatter`自动注册
 - 支持`HttpMessageConverters`
 - `MessageCodesResolver`自动注册
 - `index.html`静态支持
 - `Favicon`自定义支持
 - `ConfigurableWebBindingInitializer`自动使用

4.1.2 HttpMessageConverters
---------------------------

`Spring MVC`使用该接口转换`HTTP`请求和响应，例如将对象转换为`JSON`或者`XML`

    import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
    import org.springframework.context.annotation.*;
    import org.springframework.http.converter.*;
    
    @Configuration
    public class MyConfiguration {
    
    	@Bean
    	public HttpMessageConverters customConverters() {
    		HttpMessageConverter<?> additional = ...
    		HttpMessageConverter<?> another = ...
    		return new HttpMessageConverters(additional, another);
    	}
    
    }

## 5 Caching ##
`Spring`框架透明地提供了添加缓存的支持，缓存的核心意义就是根据缓存中可用的信息减少操作次数，缓存逻辑一般透明地应用，不会对方法调用有任何干扰，通过`@EnableCaching`注解，`Spring boot`自动开启了缓存支持。

## 5.1 支持的缓存 ##
缓存抽象该并不提供实际的存储，而是依赖`Cache`和`CacheManager`接口的抽象实现
`Spring Boot`提供了如下缓存支持：

 - Generic
 - JCache
 - EhCache
 - Hazelcast
 - Infinispan
 - Couchbase
 - Redis
 - Caffeine
 - Simple

如果`CacheManager`是`Spring Boot`自动配置的，那么可以通过实现`CacheManagerCustomizer`接口进一步配置

## 5.1.1 配置Cache ##

 - `JCache`:`javax.cache.spi.CachingProvider`出现在类路径上时，`JCache`就会引导加载，`JCacheManager`由`spring-boot-starter-cache`提供，
 - `EhCache`：如果类路径上发现`ehcache.xml`文件，则会使用`EhCache`，如果`EhCache`被发现，则可以通过`spring-boot-starter-cache`来引导启动`EhCacheCacheManager`
 - `Redis`：如果`Redis`配置可用，则`RedisCacheManager`，见代码`RedisAutoConfiguration`和`RedisCacheConfiguration`，`RedisAutoConfiguration`是`Spring Boot`针对`redis`的自动配置，需要引入相关`jar`包且在配置文件中配置`redis`，前缀为`spring.redis`，配置完后，会自动进行`RedisCacheConfiguration`配置，而`RedisCacheConfiguration`会配置`RedisCacheManager`，因此可以说如果`Redis`配置可用，那么会自动引配置`RedisCacheManager`

## 5.2 整合缓存 ##
## 5.2.1 声明式注解缓存 ##
对于缓存声明，`Spring`框架提供了一系列`Java`注解

 - `@Cacheable`：触发缓存
 - `@CacheEvict`：触发缓存删除
 - `@CachePut`：不干扰方法执行升级缓存
 - `@Caching`：重组应用在方法上的缓存操作
 - `@CacheConfig`：在类级别上共享一些通用的缓存设置

**@Cacheable**
表明方法是可缓存的，也就是说方法调用的结果会被存到缓存中，对于之后同样参数的方法调用，结果从缓存中拿而不执行方法， 通常会指定缓存名称：

    @Cacheable("books")
    public Book findBook(ISBN isbn) {...}

由于缓存是键值存储，每一次缓存方法调用都会通过合适的`key`去查找缓存，缓存抽象使用了一个简单的`keyGenerator`：

 - 如果没有参数，返回`SimpleKey.EMPTY`
 - 如果有参数给出，则返回该参数
 - 如果给出多个参数，则返回`SimpleKey`，包含所有参数

**@CachePut**
该注解通过执行方法，将方法返回值放入缓存对应`key`中，这和`@Cacheable`完全不同，后者不会执行方法

**@CacheEvict**
从缓存中移除过期或者无用数据
可以指定`allEntries`属性来表明是否删除指定`key`还是清空整个缓存内容：

    @CacheEvict(cacheNames="books",allEntries=true)
    public void loadBooks(InputStream batch)
这个选项在想要清除缓存时会很有用，单独清除每个`entry`会很耗时

**@Caching**
当对同一个方法应用多个注解时例如同时指定`@CachePut`和`@CacheEvict`注解

        @Caching(evict = { @CacheEvict("primary"), @CacheEvict(cacheNames="secondary", key="#p0") })
        public Book importBooks(String deposit, Date date)

**@CacheConfig**
类级注解，提供一些共享的通用配置

**@EnableCaching**
尽管声明了缓存注解，但并不表明会自动触发对应的行为，需要通过`@EnableCaching`注解开启缓存支持，在`@Configuration`注解类上添加`@EnableCaching`开启缓存支持

    @Configuration
    @EnableCaching
    public class AppConfig {
    }

  [1]: https://www.cnblogs.com/leihuazhe/p/7743479.html
  [2]: https://docs.spring.io/spring/docs/5.0.5.RELEASE/spring-framework-reference/core.html#beans-autowired-annotation
  [3]: https://docs.spring.io/spring-boot/docs/2.0.2.RELEASE/reference/htmlsingle/#using-boot-auto-configuration
  [4]: https://docs.spring.io/spring/docs/5.0.5.RELEASE/spring-framework-reference/core.html#beans-environment
