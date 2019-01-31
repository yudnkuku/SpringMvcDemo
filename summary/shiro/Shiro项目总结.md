# Shiro项目总结

标签（空格分隔）： shiro

---

## shiro的逻辑架构 ##
`shiro`的逻辑架构主要包括认证(`Authentication`)、授权(`Authorization`)、会话管理

![Shiro逻辑架构][1]

## shiro组件间的交互 ##
`Subject`:可以认为是当前参与应用安全部分的主角，所有的`Subject`都需要`SecurityManager`，当你与`Subject`进行交互，这些交互行为实际上被代理到`SecurityManager`，例`Subject`的实现`DelegatingSubject`中的登录方法就是将登录请求代理到内部的`securityManager`。

    public void login(AuthenticationToken token) throws AuthenticationException {
        clearRunAsIdentitiesInternal();
        Subject subject = securityManager.login(this, token);
        ...
    }
    
`SecurityManager`:`shiro`架构的核心，需要手动配置`SecurityManager`，框架提供了默认实现`DefaultSecurityManager`，上面的登录请求就是由该类实现的

`Realms`:通常和用户自己的安全数据打交道，需要自己实现。

架构图：

![Shiro详细架构][2]

流程交互图：

![Shiro流程图][3]
## 项目配置 ##
1、配置缓存：例如后面可能会用到的密码重试缓存(将用户名称和密码重试次数缓存起来，判断用户登录时密码重试次数)

    <bean id="cacheManager" class="org.apache.shiro.cache.ehcache.EhCacheManager">
        <property name="cacheManagerConfigFile" value="classpath:ehcache.xml"/>
    </bean>
    
    //ehcache.xml中密码重试缓存配置
    <cache name="passwordRetryCache"
           maxEntriesLocalHeap="3000"
           eternal="false"
           overflowToDisk="false"
           timeToLiveSeconds="900"
           statistics="true">
    </cache>
    
2、配置`CredentialsMathcer`：`shiro`框架提供了`CredentialsMathcer`的一种常用实现`HashedCredentialsMathcer`，将登录请求的用户的密码通过散列加密后与数据库中存储的密码信息对比，达到登录验证的目的(数据库表中存储的`password`字段通常也是通过同样的加密手段加密后的字符串)。

    <bean id="credentialsMatcher" class="com.fiberhome.jtis.server.jtis.security.RetryLimitHashedCredentialsMatcher">
        <constructor-arg ref="cacheManager"/>   //缓存管理器
        <property name="hashAlgorithmName" value="SHA1"/>   //加密算法
        <property name="hashIterations" value="2"/> //hash迭代值
        <property name="storedCredentialsHexEncoded" value="true"/> //是否存储16进制
    </bean>
    
看一下`RetryLimitedHashedCredentialsMathcer`实现：


    public class RetryLimitHashedCredentialsMatcher extends HashedCredentialsMatcher {
        //密码重试缓存--<用户名，密码重试次数>
        private Cache<String, AtomicInteger> passwordRetryCache;
    
    //构造函数，注入cacheManager
        public RetryLimitHashedCredentialsMatcher(CacheManager cacheManager) {
            this.passwordRetryCache = cacheManager.getCache("passwordRetryCache");
        }
        
        @Override
        public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
            String username = (String) token.getPrincipal();
    
            AtomicInteger retryCount = passwordRetryCache.get(username);
            if (retryCount == null) {
                retryCount = new AtomicInteger();
                passwordRetryCache.put(username, retryCount);
            }
            //重试次数超过5次取消重试，直接抛出异常
            if (retryCount.incrementAndGet() > 5) {
                // retries exceed
                throw new ExcessiveAttemptsException();
            }
    
            boolean matches = super.doCredentialsMatch(token, info);
            if (matches) {
                // clear retries
                passwordRetryCache.remove(username);
            }
            return matches;
        }

    }

3、配置`Realm`

    <bean id="oasystemRealm" class="com.fiberhome.jtis.server.jtis.security.OASystemUserRealm">
        <property name="name" value="OASystemRealm"/>
        <property name="credentialsMatcher" ref="credentialsMatcher"/>  //CredentialiMatcher
        <property name="authorizationCacheName" value="oa-authorizationCache"/>
    </bean>
    
看下`OASystemUserRealm`实现，大致就是获取数据库中存储的相关用户信息，例如身份、权限信息：

    
    public class OASystemUserRealm extends AuthorizingRealm {

        @Autowired
        IUserService userService;
    
        @Autowired
        IPermissionService permissionService;
        
        //获取权限信息
        @Override
        protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
            HashSet<String> permissionContents = new HashSet<String>();
    
            String userName = (String) principalCollection.getPrimaryPrincipal();
            List<Permission> permissions = permissionService.getPermissionsByUserName(userName);
            if (permissions != null && permissions.size() > 0) {
                for (Permission permission : permissions) {
                    List<PermContent> contents = permission.getContents();
    
                    if (null != contents && contents.size() > 0) {
                        for (PermContent content : contents) {
                            if (null != content.getContent() && !content.getContent().isEmpty()) {
                                permissionContents.add(content.getContent());
                            }
                        }
                    }
                }
            }
    
            SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
            authorizationInfo.setStringPermissions(permissionContents);
            return authorizationInfo;
        }
    
        //身份验证
        @Override
        protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {
            User user = userService.getUserBySysName((String) token.getPrincipal());
    
            if (null == user || user.isInvalid()) {
                throw new UnknownAccountException();
            } else {
                SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(
                        user.getSysName(), user.getPassword(),
                        ByteSource.Util.bytes(generatePasswordSalt(user)), getName());
                return authenticationInfo;
            }
        }
    
        public static String generatePassword(User user) {
            SimpleHash hash = new SimpleHash(SysConfigContants.USER_PASSWORD_ENCRYPT_ALGORITHM, user.getPassword(),
                    generatePasswordSalt(user), SysConfigContants.USER_PASSWORD_ENCRYPT_ITERATION);
            return hash.toHex();
        }
    
        public static String generatePasswordSalt(User user) {
            if (null == user.getSalt() || user.getSalt().trim().isEmpty()) {
                user.setSalt(new SecureRandomNumberGenerator().nextBytes().toHex());
            }
            return user.getSysName() + user.getSalt();
        }
    
        public static final String LOGIN_TYPE = JtisToken.TYPE_OA_SYSTEM;
    }

4、`Authenticator`配置：

    <bean id="jtisRealmAuthenticator" class="com.fiberhome.jtis.server.jtis.security.JtisRealmAuthenticator"/>
    
`JtisRealmAuthenticator`实现，继承自`ModularRealmAuthenticator`

    public class JtisRealmAuthenticator extends ModularRealmAuthenticator {
        
        //从Realm中获取身份信息并验证，如果验证失败抛出异
        @Override
        protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken)
            throws AuthenticationException {
            try {
                assertRealmsConfigured();
                JtisToken jtisToken = (JtisToken) authenticationToken;
                String loginType = jtisToken.getLoginType();
    
                Collection<Realm> allRealms = getRealms();
                for (Realm realm : allRealms) {
    
                    Field field = realm.getClass().getDeclaredField(JtisToken.LOGIN_TYPE);
                    field.setAccessible(true);
                    Object value = field.get(realm);
                    if (value != null && value.equals(loginType)) {
                        return this.doSingleRealmAuthentication(realm, authenticationToken);   //调用父类方法
                    }
                }
            } catch (Exception e) {
                LOGGER.error("doAuthenticate() : {}", e);
                e.printStackTrace();
            }
            return null;
        }

    }
    
5、`Authorizor`配置：

    <bean id="jtisRealmAuthorizer" class="com.fiberhome.jtis.server.jtis.security.JtisRealmAuthorizer"/>
    
`JtisRealmAuthorizer`实现，继承自`ModularRealmAuthorizor`类：

    public class JtisRealmAuthorizer extends ModularRealmAuthorizer {

        @Override
        public boolean isPermitted(PrincipalCollection principals, String permission) {
            try {
                assertRealmsConfigured();
                Collection<Realm> realms = getRealms();
    
                Set<String> realmNames = principals.getRealmNames();
                for (String realmName : realmNames) {
                    for (Realm realm : realms) {
                        if (realm.getName().equals(realmName) && realm instanceof Authorizer && ((Authorizer) realm)
                                .isPermitted(principals, permission)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("doAuthenticate() : {}", e);
                e.printStackTrace();
            }
            return false;
        }
    }
    
6、`SessionIdGenerator`配置，给每个`Session`实例分配唯一的`id`:

    public class JavaUuidSessionIdGenerator implements SessionIdGenerator {
        public Serializable generateId(Session session) {
            return UUID.randomUUID().toString();
        }
    }
    
7、`SessionDAO`配置：

    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">
        <property name="sessionIdGenerator" ref="sessionIdGenerator"/>  //设置sessionIdGenerator属性
    </bean>
    
8、`SessionManager`配置

    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
        <property name="globalSessionTimeout" value="7200000"/>
        <property name="sessionDAO" ref="sessionDAO"/>
    </bean>
    
9、配置`SecurityManager`：

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
    
10、配置`Filter`

    <bean id="jtisAuthenticationFilter" class="com.fiberhome.jtis.server.jtis.security.JtisAuthenticationFilter">
        <property name="loginUrl" value="/index.html"/>
    </bean>
    <!-- Permission Filter -->
    <bean id="jtisPermissionFilter" class="com.fiberhome.jtis.server.jtis.security.JtisAuthorizationFilter">
        <property name="loginUrl" value="/index.html"/>
    </bean>
    
11、配置`ShiroFilterFactoryBean`

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
    
12、注册`Shiro`生命周期回调`bean`

    <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>
    
其`postProcessBeforeInitialization`方法：

    public Object postProcessBeforeInitialization(Object object, String name) throws BeansException {
        //如果是Initializable实例，则调用init初始化
        if (object instanceof Initializable) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Initializing bean [" + name + "]...");
                }

                ((Initializable) object).init();
            } catch (Exception e) {
                throw new FatalBeanException("Error initializing bean [" + name + "]", e);
            }
        }
        return object;
    }
    
13、在`web.xml`中声明`shiro`拦截器

    <filter>
        <filter-name>shiroFilter</filter-name>
        <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>     
        <init-param>
            <param-name>targetFilterLifecycle</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>shiroFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
    
这段代码一般放在`web.xml`的最开头，作为第一个对`web`请求拦截的过滤器

## Shiro源码分析 ##
**ShiroFilterFactoryBean**

**内部属性**

内部封装的属性如下：

    private SecurityManager securityManager;    //SecurityManager对象

    private Map<String, Filter> filters;    //过滤器Filter集合

    private Map<String, String> filterChainDefinitionMap;   //url和过滤器链定义的映射 

    private String loginUrl;    //登录url
    private String successUrl;  //登录成功重定向url
    private String unauthorizedUrl; //未授权重定向url

    private AbstractShiroFilter instance;   //内部使用的Filter对象

`ShiroFilterFactoryBean`实现了`FactoryBean`和`BeanPostProcessor`接口

先看看项目中关于`ShiroFilterFactoryBean`的配置：

    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
        <property name="securityManager" ref="securityManager"/> //设置securityManager属性
        <property name="loginUrl" value="/index.html"/>   //设置登录url
        <property name="unauthorizedUrl" value="/index.html"/>   //设置未授权跳转url
        <property name="filters">   //设置filter属性
            <util:map>
                <entry key="jtisauthc" value-ref="jtisAuthenticationFilter"/>
                <entry key="jtisperm" value-ref="jtisPermissionFilter"/>
            </util:map>
        </property>
        <property    name="filterChainDefinitions">  //设置filterChainsDefinitions属性
            <value>
                / = anon
                /index.html = anon
            </value>
        </property>
    </bean>
    
上面除了`filterChainDefinitions`之外，其他的属性直接调用对应的`set`方法，看下`setFilterChainDefinitions`方法：

    public void setFilterChainDefinitions(String definitions) {
        Ini ini = new Ini();
        ini.load(definitions);
        //did they explicitly state a 'urls' section?  Not necessary, but just in case:
        Ini.Section section = ini.getSection(IniFilterChainResolverFactory.URLS);
        if (CollectionUtils.isEmpty(section)) {
            //no urls section.  Since this _is_ a urls chain definition property, just assume the
            //default section contains only the definitions:
            section = ini.getSection(Ini.DEFAULT_SECTION_NAME);
        }
        setFilterChainDefinitionMap(section);  //设置filterChainDefinitionMap属性
    }

**ShiroFilterFactoryBean实现接口**

**1、实现`FactoryBean`接口**

将`getObject`方法返回的实例注册到`Spring`上下文中：

**1.1 实现`getObejct`方法**

    public Object getObject() throws Exception {
        if (instance == null) {
            instance = createInstance();
        }
        return instance;
    }
    
进入`createInstance`方法：

    protected AbstractShiroFilter createInstance() throws Exception {

        log.debug("Creating Shiro Filter instance.");

        SecurityManager securityManager = getSecurityManager();
        if (securityManager == null) {
            String msg = "SecurityManager property must be set.";
            throw new BeanInitializationException(msg);
        }

        if (!(securityManager instanceof WebSecurityManager)) {
            String msg = "The security manager does not implement the WebSecurityManager interface.";
            throw new BeanInitializationException(msg);
        }
        //构造FilterChainManager实例
        FilterChainManager manager = createFilterChainManager();
        //构造PathMatchingFilterChainResolver实例
        PathMatchingFilterChainResolver chainResolver = new PathMatchingFilterChainResolver();
        chainResolver.setFilterChainManager(manager);
        //返回SpringShiroFiler实例，通过SecurityManager和ChainResolver构造
        return new SpringShiroFilter((WebSecurityManager) securityManager, chainResolver);
    }

`createFilterChainManager`方法：

    protected FilterChainManager createFilterChainManager() {
        //(1)构造DefaultFilterChainManager，该实例用来管理Filter和FilterChain
        DefaultFilterChainManager manager = new DefaultFilterChainManager();
        Map<String, Filter> defaultFilters = manager.getFilters();
        //apply global settings if necessary:
        for (Filter filter : defaultFilters.values()) {
            applyGlobalPropertiesIfNecessary(filter);
        }

        //获取配置的Filter，并将其添加到manager的filters属性中
        Map<String, Filter> filters = getFilters();
        if (!CollectionUtils.isEmpty(filters)) {
            for (Map.Entry<String, Filter> entry : filters.entrySet()) {
                String name = entry.getKey();
                Filter filter = entry.getValue();
                applyGlobalPropertiesIfNecessary(filter);
                if (filter instanceof Nameable) {
                    ((Nameable) filter).setName(name);
                }
                manager.addFilter(name, filter, false);
            }
        }

        //获取过滤器链定义map，key=url，value=过滤器链定义
        Map<String, String> chains = getFilterChainDefinitionMap();
        if (!CollectionUtils.isEmpty(chains)) {
            for (Map.Entry<String, String> entry : chains.entrySet()) {
                String url = entry.getKey();
                String chainDefinition = entry.getValue();
                //(2)根据url和chaiinDefinition构建ChainFilter
                manager.createChain(url, chainDefinition);
            }
        }

        return manager;
    }
    
 (1)`DefaultChainFilterManager`：
    
        private FilterConfig filterConfig;
    
        private Map<String, Filter> filters; //pool of filters available for creating chains(用来构建过滤器链的filters)
    
        private Map<String, NamedFilterList> filterChains; //key: chain name, value: chain(过滤器链map，key=url,value=过滤器的集合)
        public DefaultFilterChainManager() {
            this.filters = new LinkedHashMap<String, Filter>();
            this.filterChains = new LinkedHashMap<String, NamedFilterList>();
            addDefaultFilters(false);   //添加默认的filter
        }
        
        //addDefaultFilters
        protected void addDefaultFilters(boolean init) {
            for (DefaultFilter defaultFilter : DefaultFilter.values()) {
                //将默认filter添加到filters中
                addFilter(defaultFilter.name(), defaultFilter.newInstance(), init, false);
            }
        }
        
        //DefaultFilter
        public enum DefaultFilter {
        anon(AnonymousFilter.class),
        authc(FormAuthenticationFilter.class),
        authcBasic(BasicHttpAuthenticationFilter.class),
        logout(LogoutFilter.class),
        noSessionCreation(NoSessionCreationFilter.class),
        perms(PermissionsAuthorizationFilter.class),
        port(PortFilter.class),
        rest(HttpMethodPermissionFilter.class),
        roles(RolesAuthorizationFilter.class),
        ssl(SslFilter.class),
        user(UserFilter.class);
        }


**小结：在`ShiroFilterFactoryBean`中设置的`filter`都会注册到`DefaultFilterChainManager`中，并且由于`ShiroFilterFactoryBean`实现了`BeanPostProcessor`接口，在`postProcessBeforeInitialization`中会将其他独立的实现了`Filter`接口的`bean`注册到`ShiroFilterFactoryBean`的`filters`中，随后在`getObject`获取`bean`实例时会将这些`filter`注册到`DefaultFilterChainManager`中的`filters`属性，`DefaultFilterChainManager`才是真正管理`filter`及过滤器链的对象**


(2)`DefaultFilterChainManager.createChain`方法：
    
    //chainName=url,chainDefinition
    public void createChain(String chainName, String chainDefinition) {
        if (!StringUtils.hasText(chainName)) {
            throw new NullPointerException("chainName cannot be null or empty.");
        }
        if (!StringUtils.hasText(chainDefinition)) {
            throw new NullPointerException("chainDefinition cannot be null or empty.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Creating chain [" + chainName + "] from String definition [" + chainDefinition + "]");
        }

        //parse the value by tokenizing it to get the resulting filter-specific config entries
        //
        //e.g. for a value of
        //
        //     "authc, roles[admin,user], perms[file:edit]"
        //
        // the resulting token array would equal
        //
        //     { "authc", "roles[admin,user]", "perms[file:edit]" }
        //
        String[] filterTokens = splitChainDefinition(chainDefinition);

        //each token is specific to each filter.
        //strip the name and extract any filter-specific config between brackets [ ]
        for (String token : filterTokens) {
            String[] nameConfigPair = toNameConfigPair(token);

            //now we have the filter name, path and (possibly null) path-specific config.  Let's apply them:
            addToChain(chainName, nameConfigPair[0], nameConfigPair[1]);
        }
    }
    
`applyToChain`：

    public void addToChain(String chainName, String filterName, String chainSpecificFilterConfig) {
        if (!StringUtils.hasText(chainName)) {
            throw new IllegalArgumentException("chainName cannot be null or empty.");
        }
        //从filters中获取filterName对应的Filter
        Filter filter = getFilter(filterName);
        if (filter == null) {
            throw new IllegalArgumentException("There is no filter with name '" + filterName +
                    "' to apply to chain [" + chainName + "] in the pool of available Filters.  Ensure a " +
                    "filter with that name/path has first been registered with the addFilter method(s).");
        }

        applyChainConfig(chainName, filter, chainSpecificFilterConfig);
        //构造过滤器链，名称为chainName，该过滤器链实际上是一个filter列表集合
        NamedFilterList chain = ensureChain(chainName);
        //往过滤器链中添加filter
        chain.add(filter);
    }
    
`ensureChain`方法：

    protected NamedFilterList ensureChain(String chainName) {
        //从filterChains中获取该chainName对应的filterChain
        NamedFilterList chain = getChain(chainName);
        if (chain == null) {
            //如果不存在，则新建一个
            chain = new SimpleNamedFilterList(chainName);
           //加入filterChains this.filterChains.put(chainName, chain);
        }
        return chain;
    }
    
生成`DefaultFilterChainManager`实例后，返回`ShiroFilterFactoryBean.createInstance`方法，创建`SpringShiroFilter`实例，方法很简单，设置了两个成员属性--`SecurityManager & FilterChainResolver`：
    
    protected SpringShiroFilter(WebSecurityManager webSecurityManager, FilterChainResolver resolver) {
            super();
            if (webSecurityManager == null) {
                throw new IllegalArgumentException("WebSecurityManager property cannot be null.");
            }
            setSecurityManager(webSecurityManager);
            if (resolver != null) {
                setFilterChainResolver(resolver);
            }
        }

这里的`ChainResolver`实际上是`PathMatchingFilterChainResolver`实例(看`createInstance`源码)，简单介绍一下`PathMatchingFilterChainResolver`.

该类实现了`FilterChainResolver`，实际上就是根据请求来寻找合适的过滤器，提供了比`web.xml`更加灵活的`FilterChain`解析，`FilterChainResolver`只提供了一个方法：

    FilterChain getChain(ServletRequest request, ServletResponse response, FilterChain originalChain);

该方法根据请求获取相应的`FilterChain`，如果返回`null`表示直接使用原始`originalChain`

`PathMatchingFilterChainResolver`实现：

    public FilterChain getChain(ServletRequest request, ServletResponse response, FilterChain originalChain) {
        //获取FilterChainManager实例，该对象通过构造函数传入，实际上是DefaultFilterChainManager实例
        FilterChainManager filterChainManager = getFilterChainManager();
        if (!filterChainManager.hasChains()) {
            return null;
        }

        String requestURI = getPathWithinApplication(request);

        //the 'chain names' in this implementation are actually path patterns defined by the user.  We just use them
        //as the chain name for the FilterChainManager's requirements
        for (String pathPattern : filterChainManager.getChainNames()) {

            // If the path does match, then pass on to the subclass implementation for specific checks:
            if (pathMatches(pathPattern, requestURI)) {
                if (log.isTraceEnabled()) {
                    log.trace("Matched path pattern [" + pathPattern + "] for requestURI [" + requestURI + "].  " +
                            "Utilizing corresponding filter chain...");
                }
                //调用DefaultFilterChainManager.proxy方法生成FilterChain
                return filterChainManager.proxy(originalChain, pathPattern);
            }
        }

        return null;
    }
    
`DefaultFilterChainManager.proxy`方法：

    public FilterChain proxy(FilterChain original, String chainName) {
        //根据chainName获取filterChains中存储的filterChain
        NamedFilterList configured = getChain(chainName);
        if (configured == null) {
            String msg = "There is no configured chain under the name/key [" + chainName + "].";
            throw new IllegalArgumentException(msg);
        }
        //调用NamedFilterList.proxy生成最终的FilterChain
        return configured.proxy(original);
    }
    
`SimpleNamedFilterList.proxy`方法：

    public FilterChain proxy(FilterChain orig) {
        return new ProxiedFilterChain(orig, this);
    }
    
`ProxiedFilterChain`构造函数：

    public ProxiedFilterChain(FilterChain orig, List<Filter> filters) {
        if (orig == null) {
            throw new NullPointerException("original FilterChain cannot be null.");
        }
        this.orig = orig;
        this.filters = filters;
        this.index = 0;
    }
    
`ProxiedFilterChain`实际上将请求先派发给`filters`中的过滤器，再交给`orig`过滤器链处理

**1.2 实现`getObjectType`方法**

返回注册到`spring`上下文中的`bean`类型

    public Class getObjectType() {
        return SpringShiroFilter.class;
    }
    
**1.3 实现`isSingleton`方法**

判断是否单例，返回`true`

    public boolean isSingleton() {
        return true;
    }
    
**2、实现`BeanPostProcessor`接口**

`BeanPostProcessor`接口在每个`bean`的生命周期中的初始化阶段调用，它提供了两个回调方法：
    
    //在bean初始化方法之前调用(例如InitializingBean的afterPropertiesSet和bean自定义的init-method方法)，此时bean的属性已经装配完成
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;
    //在初始化后调用
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
    
**2.1 实现postProcessBeforeInitialization**

该方法会判断`bean`是否为`Filter`实例，如果是则将其注册到`ShiroFilterFactoryBean`的`filters`属性中(该属性是一个`map`),`key=beanName, value=filter`

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        //如果bean是Filter实例，将其注册到filters中，key=beanName,value=filter
        if (bean instanceof Filter) {
            log.debug("Found filter chain candidate filter '{}'", beanName);
            Filter filter = (Filter) bean;
            applyGlobalPropertiesIfNecessary(filter);
            getFilters().put(beanName, filter);
        } else {
            log.trace("Ignoring non-Filter bean '{}'", beanName);
        }
        return bean;
    }
    
**2.2 实现postProcessAfterInitialization**

不作任何处理，直接返回`bean`

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }


  [1]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/shiro%E6%A1%86%E6%9E%B6%E6%A6%82%E8%BF%B0.PNG
  [2]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/shiro%E6%A1%86%E6%9E%B6.PNG
  [3]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/shiro%E6%B5%81%E7%A8%8B.PNG