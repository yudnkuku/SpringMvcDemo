# shiro学习笔记

标签（空格分隔）： shiro

---

## 术语 ##

 - `Subject`：应用用户的特定安全用户视图，可以是人、第三方进程或者连接到应用的服务，甚至是一个`cron`任务，基本上所有和你的应用交互的一切都可以称作`subject`
 - `Principals`：主题鉴别的属性，例如：姓、名、社会安全号码、用户名
 - `Credentials`：保用来验证身份的保密数据，例如密码
 - `Realms`：特定的安全`DAO`，数据访问接口，和后端数据源通信的组件，针对每个后端数据源你都会使用一个`realm`
 
## Authentication ##
认证是鉴别身份的过程，证明某个用户就是他说说的，因此用户需要提供一些系统理解和信任的证明
`shiro`认证三步骤：
 - 收集主题的属性和证书
 - 向认证系统提交属性和证书
 - 允许访问，重新发起认证或者阻止访问

`example`：
**第一步**：收集主题的属性和证书

    UsernamePasswordToken token = new UsernamePasswordToken(username, password);
    token.setRememberMe(true);

**第二步**：向认证系统提交属性和证书
我们已经在`token`中收集了信息，并且将其设置为记住返回用户，下一步就是向认证系统提交`token`，认证系统在`shiro`中实际上就是`Realms`，使用一行代码就可以实现提交

    Subject currentUser = SecurityUtils.getSubject();
    currentUser.login(token);

首先我们要获得当前执行的用户，通过`subject`引用，`subject`实际上就是用户的安全特定视图，在`shiro`中，对于每个当前执行的线程都有一个`subject`实例可用，`subject`概念是`Shiro`的核心，框架的大部分都围绕`subjects`,直接通过`SecurityUtils`获取主题对象，对于上述代码，获取的`subject`当前用户是匿名的，没有任何身份和他绑定，因此可以通过`login()`方法认证并提交`token`

**第三步**：允许访问，重新认证或者阻止访问
如果`login()`方法调用成功，则用户登录并且和用户账户或者身份绑定，从此，用户可以在当前会话或者其他作用域中获取身份
如果认证失败，则会抛出异常，得益于`Shiro`丰富的异常结构

        try {
        currentUser.login(token);
        } catch  ( UnknownAccountException uae ) { ...
        } catch  ( IncorrectCredentialsException ice ) { ...
        } catch  ( LockedAccountException lae ) { ...
        } catch  ( ExcessiveAttemptsException eae ) { ...
    } ...  your own ...
        } catch ( AuthenticationException ae ) {
        //unexpected error?
    }

## Authorization ##
授权或者间访问控制，是知名资源访问权限的功能，换句话说就是**谁可以访问什么**
授权检查的例子：用户是否允许查看网页、修改数据、或者打印
授权有三大要素：`permissions`、`roles`和`users`

**用户、角色、权限**
用户关联角色`[users]`，角色关联权限`[roles]`

## INI配置文件 ##
**[main]**
`[main]`部分是配置应用的`SecurityManager`实例和其依赖例如`Realms`的地方，例如：

    [main]
    sha256Matcher = org.apache.shiro.authc.credential.Sha256CredentialsMatcher  //定义一个CredentialsMatcher实例
    
    myRealm = com.company.security.shiro.DatabaseRealm  //定义一个Realms实例
    myRealm.connectionTimeout = 30000
    myRealm.username = jsmith
    myRealm.password = secret
    myRealm.credentialsMatcher = $sha256Matcher //通过：$实例名引用对象
    
    securityManager.sessionManager.globalSessionTimeout = 1800000

**[users]**
`[users]`部分定义静态用户集合，将**用户和角色关联**，格式为：

    username = password,roleName1,roleName2...,roleNameN

等式左边为用户名，右边第一个是密码，后面的都是该用户拥有的角色名

**[roles]**
`[roles]`将`[users]`中定义的**用户和权限关联**起来，格式为：

    rolename = permissionDefinition1,permissionDefi
    #权限语法  * 表示所有权限 一般语法是 权限类型：权限动作：权限资源id  比如 user:delete:1 表示拥有删除1号用户的权限
    
    [roles]
    admin = *   //admin角色拥有所有权限
    schwartz = lightsaber:* //schwartz能够使用lightsaber做任何事
    goodguy = winnebago:drive:eagle5    //


**[urls]**
`[urls]`部分定义`url`对应的过滤链,格式：

    _URL_Ant_Path_Expression_ = _Path_Specific_Filter_Chain_
    
    [urls]
    
    /index.html = anon
    /user/create = anon
    /user/** = authc
    /admin/** = authc, roles[administrator]
    /rest/** = authc, rest
    /remoting/rpc/** = authc, perms["remote:invoke"]
    
假设你有如下定义：

    /account/** = ssl, authc
    
以上定义表明任何请求到`/account`或者子路径`/account/foo`等都会触发`ssl`、`authc`过滤链

`url`的匹配遵循第一匹配原则，即按照过滤链定义的顺序匹配，如果匹配到则不会进行接下来的匹配，例如

    /account/**  =ssl, authc
    /account/signup = anno
如果请求路径为`/account/signup/index.html`，则会匹配`/account/**`而不会匹配`/account/signup`

**过滤链定义**
等式右边定义了匹配请求后执行的过滤链，格式：

    filter1[optinal_config1],filter2[optional_config2]...

过滤器名称在`[main]`中定义，当然`shiro`提供了内置的一系列过滤器

    [main]
    ...
    myFilter = com.company.web.some.FilterImplementation
    myFilter.property1 = value1
    ...
    
    [urls]
    ...
    /some/path/** = myFilter
    
**默认过滤器**
`shiro`提供了默认的过滤器实例，可以在配置文件中直接引用

    [main]
    ...
    # Notice how we didn't define the class for the FormAuthenticationFilter ('authc') - it is instantiated and available already:
    authc.loginUrl = /login.jsp     //跳转的登录界面
    ...
    
    [urls]
    ...
    # make sure the end-user is authenticated.  If not, redirect to the 'authc.loginUrl' above,
    # and after successful authentication, redirect them back to the original account page they
    # were trying to view:
    /account/** = authc     //匹配的url需要登录验证
    
默认的过滤器实例在`DefaultFilter`枚举类中声明，包括以下过滤器：
|`Filter Name`|`Class`|
|:-:|:-:|
|`anno`|`org.apache.shiro.web.filter.authc.AnonymousFilter`无需验证，直接访问|
|`authc`|`org.apache.shiro.web.filter.authc.FormAuthenticationFilter`跳转到配置的`loginUrl`，如果登录失败，则会产生`AuthenticationException`，并且其全名会作为`failureKeyAttribute`键值的属性|
|`authcBasic`|`org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter`|
|`logout`|`org.apache.shiro.web.filter.authc.LogoutFilter`在接收请求之前先调用当前`subject.logout()`方法退出，然后再跳转到指定的`redirectUrl`|
|`noSessionCreation`|`org.apache.shiro.web.filter.session.NoSessionCreationFilter`禁止请求过程中新建`session`|
|`perms`|`org.apache.shiro.web.filter.authz.PermissionAuthorizationFilter`如果当前用户拥有指定权限则允许访问，否则拒绝访问，|
|`port`|`org.apache.shiro.web.filter.authz.PortFilter`是否在指定端口请求，如果不是转发到指定端口重新发起请求，默认80端口|
|`rest`|`org.apache.shiro.web.filter.authz.HttpMethodPermissionFilter`将`http`请求方法对应的权限动作添加到配置的权限后面构造新的权限，再判断当前用户是否支持新权限，`http`方法权限动作映射表参考源码，一般来说`GET`->`read`,`POST`->`create`,`PUT`->`update`，例如如下配置：`/user/** = rest[user]`，接收到`POST /user/`请求，则会生成新的`user:create`权限，接着`subject`对该权限进行验证|
|`roles`|`org.apache.shiro.web.filter.authz.RolesAuthorizationFilter`当前用户是否有这个角色，配置`roles[role,...]`|
|`ssl`|`org.apache.shiro.web.filter.authz.SslFilter`|
|`user`|`org.apache.shiro.web.filter.authz.UserFilter`判断当前用户是否是已知用户，如果是则允许访问，否则重定向到`loginUrl`|

## Spring MVC整合shiro ##
`spring-shiro.xml`配置文件
1、定义`CacheManager`

    <bean id="cacheManager" class="">
        <property name="cacheManagerConfigFile" value="classpath:ehcache.xml"/>
    </bean>

2、定义`CrenditialMatcher`，`RetryLimitHashedCredentialsMatcher`继承自`HashedCredentialMatcher`，在将`AuthenticationToken`与数据库中的`AuthenticationInfo`对比之前进行了哈希化

    <bean id="credentialsMatcher" class="com.fiberhome.jtis.server.jtis.security.RetryLimitHashedCredentialsMatcher">
        <constructor-arg ref="cacheManager"/>
        <property name="hashAlgorithmName" value="SHA1"/>
        <property name="hashIterations" value="2"/>
        <property name="storedCredentialsHexEncoded" value="true"/>
    </bean>


3、定义`SecurityManager`，可以看到需要的属性有`SessionManager`、`Authenticator`、`Authorizor`和`Realm`

    <bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <property name="sessionManager" ref="sessionManager"/>
        <property name="cacheManager" ref="cacheManager"/>
        <property name="authenticator" ref="jtisRealmAuthenticator"/>
        <property name="authorizer" ref="jtisRealmAuthorizer"/>
        <property name="realms">
            <list>
                <ref bean="oasystemRealm"/>
                <ref bean="portalRealm"/>
                <ref bean="weightRealm"/>
            </list>
        </property>
    </bean>
    
因此我们在之前分别定义上述需要的`bean`
（1）`SessionManager`，管理`session`

    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
        <property name="globalSessionTimeout" value="7200000"/>
        <property name="sessionDAO" ref="sessionDAO"/>
    </bean>

定义`SessionDAO`

    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">
        <property name="sessionIdGenerator" ref="sessionIdGenerator"/>
    </bean>

(2)定义`Authenticator`,用作身份鉴定，继承自`ModularRealmAuthenticator`

    <bean id="jtisRealmAuthenticator" class="com.fiberhome.jtis.server.jtis.security.JtisRealmAuthenticator"/>

(3)定义`Authorizer`，用于权限鉴定，继承自`ModularRealmAuthorizer`

    <bean id="jtisRealmAuthorizer" class="com.fiberhome.jtis.server.jtis.security.JtisRealmAuthorizer"/>
    
`ModularRealmAuthenticator`和`ModularRealmAuthorizer`都允许配置多个`Realm`，在用户鉴定和权限鉴定时会遍历配置的所有`Realm`

4、`ShiroFilter` 配置`shiro`过滤器，这个`bean`的名称应该和`web.xml`中的`shiro filter`名称保持一致
`web.xml`中定义的`shiro filter`

    <filter>
        <filter-name>shiroFilter</filter-name>
        <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>shiroFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>

`spring-shiro.xml`中定义：

    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
        <property name="securityManager" ref="securityManager"/> 
        <property name="loginUrl" value="/index.html"/>
        <property name="unauthorizedUrl" value="/index.html"/>
        <property name="filters">
            <util:map>
                <entry key="jtisauthc" value-ref="jtisAuthenticationFilter"/>
                <entry key="jtisperm" value-ref="jtisPermissionFilter"/>
            </util:map>
        </property>
        <property name="filterChainDefinitions">
            <value>
                / = anon
                /index.html = anon
                /*.css = anon
                /*.eot = anon
                /*.ico = anon
                /*.jpg = anon
                /*.js = anon
                /*.json = anon
                /*.png = anon
                /*.svg = anon
                /*.ttf = anon
                /*.woff = anon
                /*.woff2 = anon
                /*.js = anon
                /portal/userLogin = anon
                /portal/registerUser = anon
                /portal/forgetPassword = anon
                /portal/verify = anon
                /portal/getDynamicOrder = anon
                /user/login = anon
                /user/weightLogin = anon
                /user/logout = jtisauthc
                /** = jtisauthc,jtisperm
            </value>
        </property>
    </bean>
    
定义了`ShiroFilterFactoryBean`，该`bean`定义了多个属性，包括`securityManager`、`filters`、`filterChainDefinitions`，以及多个全局属性，例如`loginUrl`、`successUrl`以及`unauthorizedUrl`，如果在这个`bean`中定义了这些全局属性，所有的`filter`都会拥有这些属性，当然如果单独在某个`filter`中定义这些属性会覆盖全局属性