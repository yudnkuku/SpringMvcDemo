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

可以看到每个`Realm`和`SessionDAO`都配置`Cache`用于缓存，`Shiro`框架中的很多接口都实现了`CacheManagerAware`接口，方便注入`CacheManager`，现在从配置入手分析缓存如何配置的：

1、配置`SessionManager`：

    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">
        <property name="globalSessionTimeout" value="7200000"/>
        <property name="sessionDAO" ref="sessionDAO"/>  //注入SessionDAO
    </bean>

看看`setSessionDAO`方法：

    public void setSessionDAO(SessionDAO sessionDAO) {
        this.sessionDAO = sessionDAO;
        applyCacheManagerToSessionDAO();    //将CacheManager注入到SessionDAO中
    }
    
    private void applyCacheManagerToSessionDAO() {
        if (this.cacheManager != null && this.sessionDAO != null && this.sessionDAO instanceof CacheManagerAware) {
            ((CacheManagerAware) this.sessionDAO).setCacheManager(this.cacheManager);
        }
    }
    
    
2、配置`SecurityManager`：

    //SecurityManager注入CacheManager
    <bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <property name="sessionManager" ref="sessionManager"/>
        <property name="cacheManager" ref="cacheManager"/>  //注入CacheManager，前面配置了EHCacheManager
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

看看`DefaultWebSecurityManager`的`setCacheManager`方法：

        public void setCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            afterCacheManagerSet();
        }

进入`SessionsSecurityManager.afterCacheManagerSet`方法：

    protected void afterCacheManagerSet() {
        super.afterCacheManagerSet();   //(1)
        applyCacheManagerToSessionManager();    //(2)
    }

(1)进入`RealmSecurityManager.afterCacheManagerSet`方法：

    protected void afterCacheManagerSet() {
        applyCacheManagerToRealms();
    }
    
    //将CacheManager注入到Realm中(如果Realm已经注入了)
    protected void applyCacheManagerToRealms() {
        CacheManager cacheManager = getCacheManager();
        Collection<Realm> realms = getRealms();
        if (cacheManager != null && realms != null && !realms.isEmpty()) {
            for (Realm realm : realms) {
                if (realm instanceof CacheManagerAware) {
                    ((CacheManagerAware) realm).setCacheManager(cacheManager);
                }
            }
        }
    }

(2)进入`SessionsSecurityManager.applyCacheManagerToSessionManager`方法，将`CacheManager`注入到`SessionManager`中：

    protected void applyCacheManagerToSessionManager() {
        if (this.sessionManager instanceof CacheManagerAware) {
            ((CacheManagerAware) this.sessionManager).setCacheManager(getCacheManager());
        }
    }

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
        // 将过滤器配置以逗号分隔开
        String[] filterTokens = splitChainDefinition(chainDefinition);

        //each token is specific to each filter.
        //strip the name and extract any filter-specific config between brackets [ ]
        // 去掉配置中的 [] ，例如roles[admin,user] 会变成 "roles" "admin,user"
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
        //(1)应用过滤器链配置
        applyChainConfig(chainName, filter, chainSpecificFilterConfig);
        //(2)构造过滤器链，名称为chainName，该过滤器链实际上是一个filter列表集合
        NamedFilterList chain = ensureChain(chainName);
        //往过滤器链中添加filter
        chain.add(filter);
    }

(1)`applyChainConfig`方法：

    protected void applyChainConfig(String chainName, Filter filter, String chainSpecificFilterConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Attempting to apply path [" + chainName + "] to filter [" + filter + "] " +
                    "with config [" + chainSpecificFilterConfig + "]");
        }
        if (filter instanceof PathConfigProcessor) {    //如果是PathConfigProcessor实例，则调用器processPathConfig方法，过滤器一般都会实现该接口，因此进入这个分支
            ((PathConfigProcessor) filter).processPathConfig(chainName, chainSpecificFilterConfig);
        } else {
            if (StringUtils.hasText(chainSpecificFilterConfig)) {
                //they specified a filter configuration, but the Filter doesn't implement PathConfigProcessor
                //this is an erroneous config:
                String msg = "chainSpecificFilterConfig was specified, but the underlying " +
                        "Filter instance is not an 'instanceof' " +
                        PathConfigProcessor.class.getName() + ".  This is required if the filter is to accept " +
                        "chain-specific configuration.";
                throw new ConfigurationException(msg);
            }
        }
    }

`PathMatchingFilter.processPathConfig`方法：

    public Filter processPathConfig(String path, String config) {
        String[] values = null;
        if (config != null) {
            values = split(config); //将过滤器链配置以逗号分隔开
        }
        
        //存入appliedPaths中，key=url(chainName)，value=config
        this.appliedPaths.put(path, values);
        return this;
    }

(2)`ensureChain`方法：

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

**登录验证**

登录入口：

    subject = SecurityUtils.getSubject();
    JtisToken token = new JtisToken(userName.trim(), 
    JtisToken.strDec(password.trim()),
                        JtisToken.TYPE_OA_SYSTEM);
    subject.login(token);   //登录入口

进入`DelegatingSubject.login`方法：

    public void login(AuthenticationToken token) throws AuthenticationException {
        clearRunAsIdentitiesInternal();
        //调用SecurityManager.login方法实现登录
        Subject subject = securityManager.login(this, token); 

        PrincipalCollection principals;

进入`DefaultSecurityManager.login`方法：

    public Subject login(Subject subject, AuthenticationToken token) throws AuthenticationException {
        AuthenticationInfo info;
        try {
            //调用authenticate方法进行登录验证
            info = authenticate(token);
    
    
    //authenticate方法
    public AuthenticationInfo authenticate(AuthenticationToken token) throws AuthenticationException {
        //调用Authenticator实例的authenticate方法
        return this.authenticator.authenticate(token);
    }
    
我们在`spring-shiro.xml`中声明的`JtisRealmAuthenticator`继承自`AbstractAuthenticator`，因此进入到`AbstractAuthenticator`中：

    //核心代码
    AuthenticationInfo info;
    //验证token，并获取AuthenticationInfo对象
    info = doAuthenticate(token);

这会转到我们自己定义的`Authenticator`中：

    protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken)
        throws AuthenticationException {
        try {
            assertRealmsConfigured();
            JtisToken jtisToken = (JtisToken) authenticationToken;
            String loginType = jtisToken.getLoginType();
            
            //(1)获取相关Realm实例集合
            Collection<Realm> allRealms = getRealms();
            for (Realm realm : allRealms) {

                Field field = realm.getClass().getDeclaredField(JtisToken.LOGIN_TYPE);
                field.setAccessible(true);
                Object value = field.get(realm);
                if (value != null && value.equals(loginType)) {
                    //(2)调用doSingleRealmAuthentication方法进行验证
                    return this.doSingleRealmAuthentication(realm, authenticationToken);
                }
            }
        } catch (Exception e) {
            LOGGER.error("doAuthenticate() : {}", e);
            e.printStackTrace();
        }
        return null;
    }

（1）`getRealms()`方法返回了`Authenticator`实例内部的`Realm`实例集合

首先看一下`SecurityManager`的配置：

    <bean id="securityManager" class="org.apache.shiro.web.mgt.DefaultWebSecurityManager">
        <property name="sessionManager" ref="sessionManager"/>
        <property name="cacheManager" ref="cacheManager"/>
        <property name="authenticator" ref="jtisRealmAuthenticator"/>
        <property name="authorizer" ref="jtisRealmAuthorizer"/>
        //设置realms属性
        <property name="realms">
            <list>
                <ref bean="oasystemRealm"/>
                <ref bean="portalRealm"/>
                <ref bean="weightRealm"/>
            </list>
        </property>
    </bean>

进入`RealmSecurityManager.setRealms`方法：

    public void setRealms(Collection<Realm> realms) {
        if (realms == null) {
            throw new IllegalArgumentException("Realms collection argument cannot be null.");
        }
        if (realms.isEmpty()) {
            throw new IllegalArgumentException("Realms collection argument cannot be empty.");
        }
        this.realms = realms;
        //调用afterRealmSet方法，跳转到子类覆盖方法
        afterRealmsSet();
    }

`AuthorizingSecurityManager.afterRealmSet`方法：

    protected void afterRealmsSet() {
        //继续调用super.afterRealmSet
        super.afterRealmsSet();
        //如果authorizer属性是ModularRealmAuthorizer实例，设置其realms属性，我们自己定义的Authorizer bean就可以设置其realms属性
        if (this.authorizer instanceof ModularRealmAuthorizer) {
            ((ModularRealmAuthorizer) this.authorizer).setRealms(getRealms());
        }
    }

`AuthenticatingSecurityManager.afterRealmSet`方法：

    protected void afterRealmsSet() {
        //继续调用父类方法
        super.afterRealmsSet();
        //如果authenticator是ModularRealmAuthenticator实例，那么设置其realms属性
        if (this.authenticator instanceof ModularRealmAuthenticator) {
            ((ModularRealmAuthenticator) this.authenticator).setRealms(getRealms());
        }
    }

这样就可以通过`getRealms`方法获取`Authenticator`相关`Realm`实例集合了

（2）`doSingleRealmAuthentication`核心代码片段，位于其父类`ModularRealmAuthenticator`中：

    AuthenticationInfo info = realm.getAuthenticationInfo(token);

这里简单介绍下`ModularRealmAuthenticator`，它可以配置多个插入式的`Realm`，内部提供了`setRealms`方法来设置相关`Realm`，其验证方法：
    
    //存在多个Realm就调用doMultiRealmAuthentication，否则调用doSingleRealmAuthentication
    protected AuthenticationInfo doAuthenticate(AuthenticationToken authenticationToken) throws AuthenticationException {
        assertRealmsConfigured();
        Collection<Realm> realms = getRealms();
        if (realms.size() == 1) {
            return doSingleRealmAuthentication(realms.iterator().next(), authenticationToken);
        } else {
            return doMultiRealmAuthentication(realms, authenticationToken);
        }
    }

继续`Realm.getAuthenticationInfo`方法，这里将会从`Realm`实例中获取用户信息：

    public final AuthenticationInfo getAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

        AuthenticationInfo info = getCachedAuthenticationInfo(token);
        if (info == null) {
            //otherwise not cached, perform the lookup:
            //(1)调用子类doGetAuthenticationInfo，转到自己的实现类
            info = doGetAuthenticationInfo(token);
            log.debug("Looked up AuthenticationInfo [{}] from doGetAuthenticationInfo", info);
            if (token != null && info != null) {
                cacheAuthenticationInfoIfPossible(token, info);
            }
        } else {
            log.debug("Using cached authentication info [{}] to perform credentials matching.", info);
        }

        if (info != null) {
            //(2)密码校验
            assertCredentialsMatch(token, info);
        } else {
            log.debug("No AuthenticationInfo found for submitted AuthenticationToken [{}].  Returning null.", token);
        }

        return info;
    }

（1）实现`doGetAuthenticationInfo`方法：
    
    //从db中获取用户信息，构造AuthenticationInfo实例
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

（2）调用`CredentialsMatcher.doCredentialsMatch`方法验证密码，`shiro`提供了`HashedCredentialsMatcher`来进行哈希化匹配，代码中增加了密码重试限制功能`RetryLimitHashedCredentialsMatcher`：

    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        String username = (String) token.getPrincipal();

        AtomicInteger retryCount = passwordRetryCache.get(username);
        if (retryCount == null) {
            retryCount = new AtomicInteger();
            passwordRetryCache.put(username, retryCount);
        }
        if (retryCount.incrementAndGet() > SysConfigUtil.getPasswordRetries()) {
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


**用户权限控制**

首先请求先经过过滤器处理，例如我们自己会定义一个`JtisAuthorizationFilter`过滤器判断用户的访问权限，**权限验证入口还是在Subject接口中(验证权限入口可以从过滤器链入手)**，例如`Subject`接口中定义了很多`isPermitted`方法，那么我们需要在自定义`JtisAuthorizationFilter`中实现访问权限控制逻辑，代码如下：
    
    //覆盖父类isAccessAllowed方法，判断登录用户是否有权限访问当前url
    protected boolean isAccessAllowed(ServletRequest servletRequest,
                                      ServletResponse servletResponse, Object o) throws Exception {
        try {
            Subject subject = getSubject(servletRequest, servletResponse);

            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String url = httpServletRequest.getRequestURI();
            String basePath = httpServletRequest.getContextPath();
            if (null != url && url.startsWith(basePath)) {
                url = url.replaceFirst(basePath, "");
            }

            if (!PermissionCache.singleton().findPermission(url)) {
                return true;
            } else {
                //判断权限方法
                return subject.isPermitted(url);
            }
        } catch (Exception e) {
            LOGGER.error("isAccessAllowed() : {}", e);
            e.printStackTrace();
            throw e;
        }
    }

`DelegatingSubject.isPermitted`方法：

    public boolean isPermitted(String permission) {
        return hasPrincipals() && securityManager.isPermitted(getPrincipals(), permission);
    }

`AuthorizingSecurityManager.isPermitted`方法：

    public boolean isPermitted(PrincipalCollection principals, String permissionString) {
        return this.authorizer.isPermitted(principals, permissionString);
    }

接着就要调用`Authorizer`的`isPermitted`方法，这里的`Authorizer`实例需要我们自己实现，例如我们在`JtisRealmAuthorizer`中实现：

    public boolean isPermitted(PrincipalCollection principals, String permission) {
        try {
            assertRealmsConfigured();
            Collection<Realm> realms = getRealms();

            Set<String> realmNames = principals.getRealmNames();
            for (String realmName : realmNames) {
                for (Realm realm : realms) {
                    //调用AuthorizingRealm.isPermitted方法
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

`AuthorizingRealm.isPermitted`方法：

    public boolean isPermitted(PrincipalCollection principals, String permission) {
        //getPermissionResolver默认返回WildcardPermissionResolver
        Permission p = getPermissionResolver().resolvePermission(permission);
        return isPermitted(principals, p);
    }
    
    public boolean isPermitted(PrincipalCollection principals, Permission permission) {
        //(1)调用getAuthorizingInfo方法获取权限信息
        AuthorizationInfo info = getAuthorizationInfo(principals);
        return isPermitted(permission, info);
    }
    
    //用上面获取到的AuthorizationInfo验证
    protected boolean isPermitted(Permission permission, AuthorizationInfo info) {
        Collection<Permission> perms = getPermissions(info);
        if (perms != null && !perms.isEmpty()) {
            for (Permission perm : perms) {
                if (perm.implies(permission)) {
                    return true;
                }
            }
        }
        return false;
    }

(1)`getAuthorizingInfo`方法，会首先从权限缓存中拿数据，如果没有从数据库中查并保存在缓存中：

    protected AuthorizationInfo getAuthorizationInfo(PrincipalCollection principals) {

        if (principals == null) {
            return null;
        }

        AuthorizationInfo info = null;

        if (log.isTraceEnabled()) {
            log.trace("Retrieving AuthorizationInfo for principals [" + principals + "]");
        }
        //获取配置的缓存，我们在spring-shiro中给相关Realm配置了缓存，因此这里可以拿到Cache实例
        Cache<Object, AuthorizationInfo> cache = getAvailableAuthorizationCache();
        if (cache != null) {
            if (log.isTraceEnabled()) {
                log.trace("Attempting to retrieve the AuthorizationInfo from cache.");
            }
            //获取缓存key
            Object key = getAuthorizationCacheKey(principals);
            info = cache.get(key);
            if (log.isTraceEnabled()) {
                if (info == null) {
                    log.trace("No AuthorizationInfo found in cache for principals [" + principals + "]");
                } else {
                    log.trace("AuthorizationInfo found in cache for principals [" + principals + "]");
                }
            }
        }


        if (info == null) {
            //调用子类实现方法
            info = doGetAuthorizationInfo(principals);
            // If the info is not null and the cache has been created, then cache the authorization info.
            if (info != null && cache != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Caching authorization info for principals: [" + principals + "].");
                }
                Object key = getAuthorizationCacheKey(principals);
                //更新缓存
                cache.put(key, info);
            }
        }

        return info;
    }

子类`OASystemUserRealm`实现的`doGetAuthorizationInfo`方法：

    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        HashSet<String> permissionContents = new HashSet<String>();

        String userName = (String) principalCollection.getPrimaryPrincipal();
        //根据用户名查数据库获取权限信息
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
        //返回AuthorizationInfo实例
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.setStringPermissions(permissionContents);
        return authorizationInfo;
    }


**Shiro过滤拦截机制**

**1、注册过滤器和过滤器链**

首先看一下项目中过滤器的配置：

    <!-- Authentication Filter -->
    <bean id="jtisAuthenticationFilter" class="com.fiberhome.jtis.server.jtis.security.JtisAuthenticationFilter">
        <property name="loginUrl" value="/index.html"/>
    </bean>
    <!-- Permission Filter -->
    <bean id="jtisPermissionFilter" class="com.fiberhome.jtis.server.jtis.security.JtisAuthorizationFilter">
        <property name="loginUrl" value="/index.html"/>
    </bean>

将上述两个`Filter bean`注册到`ShiroFilterFactoryBean`中：

    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
        <property name="filters">
            <util:map>
                <entry key="jtisauthc" value-ref="jtisAuthenticationFilter"/>
                <entry key="jtisperm" value-ref="jtisPermissionFilter"/>
            </util:map>
        </property>
        <property name="filterChainDefinitions">
            <value>
                /user/logout = jtisauthc
                /** = jtisauthc,jtisperm
            </value>
        </property>
    </bean>

可以看到`ShiroFilterFactoryBean`配置了两个属性`filters`和`filterChainDefinitions`，看下两个属性的定义：

    //filters属性，key=过滤器名称，value=Filter实例
    private Map<String, Filter> filters;
    
    //filterChainDefinitionMap，key=url,value=逗号分割的过滤器链定义
    private Map<String, String> filterChainDefinitionMap; //urlPathExpression_to_comma-delimited-filter-chain-definition
    
例如上面的两个过滤器会添加到`filters`属性中，`jtisauthenc -> jtisAuthenticationFilter`,`jtisperm -> jtisPermissionFilter`，过滤器链定义`/** = jtisauthc,jtisperm`会添加到`filterChainDefinitions`属性中变成`/** -> jtisauthc,jtisperm`

**2、过滤器拦截原理**

`ShiroFilterFactoryBean`是一个`FactoryBean`实例，其`getObject`方法返回的实例才是真正用到的`bean`，前面已经介绍了其返回的实例类型，就是`ShiroFilterFactoryBean$SpringShiroFilter`，看一下`SpringShiroFilter`的构造方法：

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

构造方法设置两个成员属性`WebSecurityManager`和`FilterChainResolver`，前者是通过配置文件注入，后者是在`createInstance`方法中构造的`PathMatchingFilterChainResolver`实例，代码片段：

    PathMatchingFilterChainResolver chainResolver = new PathMatchingFilterChainResolver();
        chainResolver.setFilterChainManager(manager);


根据官方文档的描述，在`web.xml`文件中需要配置过滤器，如下所示：

     <filter>
       <filter-name>shiroFilter</filter-name>
       <filter-class>org.springframework.web.filter.DelegatingFilterProxy<filter-class>
       <init-param>
        <param-name>targetFilterLifecycle</param-name>
         <param-value>true</param-value>
       </init-param>
     </filter>

上面的过滤器类是`DelegatingFilterProxy`，这实际上是`Servlet Filter`将过滤功能代理给`Spring`管理的实现了`Filter`接口的`bean`，可以指定`targetBeanName`这个`init-param`，会自动匹配`Spring`上下文中的`targetBeanName`的`bean`作为代理的过滤器，如果没有指定会默认匹配过滤器名字：

    protected void initFilterBean() throws ServletException {
		synchronized (this.delegateMonitor) {
			if (this.delegate == null) {
				// If no target bean name specified, use filter name.
				if (this.targetBeanName == null) {
					this.targetBeanName = getFilterName();  //如果targetBeanName==null，取过滤器名称
				}
				// Fetch Spring root application context and initialize the delegate early,
				// if possible. If the root application context will be started after this
				// filter proxy, we'll have to resort to lazy initialization.
				WebApplicationContext wac = findWebApplicationContext();
				if (wac != null) {
					this.delegate = initDelegate(wac);
				}
			}
		}
	}

那么上述`web.xml`的配置表明会自动匹配`Spring`上下文中的`shiroFilter`这个`bean`，在`spring-shiro.xml`配置文件中，我们配置了`shiroFilter`这个`bean`：

    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean">
    
它是`ShiroFilterFactoryBean`类型，实现了`FactoryBean`接口，那么注册到`Spring`上下文中的类型实际上是`getObjectType`返回的类型，即`SpringShiroFilter`，也就是说实际上我们使用的过滤器类型是`SpringShiroFilter`，它是`ShiroFilterFactoryBean`的内部类。

再看一下`SpringShiroFilter`的类图：

![SpringShiroFilter类图][4]

其`doFilter`方法在`OncePerRequestFilter`中实现，根据过滤器的名字可以看出如果请求处理过，那么不再处理该请求，看下代码：

    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        //获取属性名称，过滤器名字+.FILTERED
        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
        if ( request.getAttribute(alreadyFilteredAttributeName) != null ) {
            //如果请求中有该属性值，那么会交给下一个过滤器处理
            log.trace("Filter '{}' already executed.  Proceeding without invoking this filter.", getName());
            filterChain.doFilter(request, response);
        } else //noinspection deprecation
            if (/* added in 1.2: */ !isEnabled(request, response) ||
                /* retain backwards compatibility: */ shouldNotFilter(request) ) {
            log.debug("Filter '{}' is not enabled for the current request.  Proceeding without invoking this filter.",
                    getName());
            filterChain.doFilter(request, response);
        } else {
            // Do invoke this filter...
            log.trace("Filter '{}' not yet executed.  Executing now.", getName());
            //设置请求属性
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);

            try {
                //调用doFilterInternal处理
                doFilterInternal(request, response, filterChain);
            } finally {
                // Once the request has finished, we're done and we don't
                // need to mark as 'already filtered' any more.
                //移除请求属性
                request.removeAttribute(alreadyFilteredAttributeName);
            }
        }
    }

`doFilterInternal`方法在`AbstractShiroFilter`中实现：

    protected void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, final FilterChain chain)
            throws ServletException, IOException {

        Throwable t = null;

        try {
            final ServletRequest request = prepareServletRequest(servletRequest, servletResponse, chain);
            final ServletResponse response = prepareServletResponse(request, servletResponse, chain);

            final Subject subject = createSubject(request, response);

            //noinspection unchecked
            subject.execute(new Callable() {
                public Object call() throws Exception {
                    updateSessionLastAccessTime(request, response);
                    //执行过滤链
                    executeChain(request, response, chain);
                    return null;
                }
            });
        }

`executeChain`方法：

    protected void executeChain(ServletRequest request, ServletResponse response, FilterChain origChain)
            throws IOException, ServletException {
        FilterChain chain = getExecutionChain(request, response, origChain);
        chain.doFilter(request, response);
    }

`getExecutionChain`方法获取过滤链：

    protected FilterChain getExecutionChain(ServletRequest request, ServletResponse response, FilterChain origChain) {
        FilterChain chain = origChain;
        
        //获取过滤链解析器，这里是PathMatchingFilterChainResolver
        FilterChainResolver resolver = getFilterChainResolver();
        if (resolver == null) {
            log.debug("No FilterChainResolver configured.  Returning original FilterChain.");
            return origChain;
        }
        //获取请求对应的过滤链
        FilterChain resolved = resolver.getChain(request, response, origChain);
        if (resolved != null) {
            log.trace("Resolved a configured FilterChain for the current request.");
            chain = resolved;
        } else {
            log.trace("No FilterChain configured for the current request.  Using the default.");
        }

        return chain;
    }

`PathMatchingFilterChainResolver.getChain`方法：

    public FilterChain getChain(ServletRequest request, ServletResponse response, FilterChain originalChain) {
        //获取FilterChainManager，这里是DefaultFilterChainManager实例，DefaultFilterChainManager管理着所有的Filter和FilterChain
        FilterChainManager filterChainManager = getFilterChainManager();
        if (!filterChainManager.hasChains()) {
            return null;
        }

        String requestURI = getPathWithinApplication(request);
        //遍历所有过滤器链，每个过滤器链的名字就是其对应的url
        for (String pathPattern : filterChainManager.getChainNames()) {
            
            //如果请求uri和过滤器链名称对应(url)，那么久调用DefaultFilterChainManager.proxy方法生成FilterChain实例
            if (pathMatches(pathPattern, requestURI)) {
                if (log.isTraceEnabled()) {
                    log.trace("Matched path pattern [" + pathPattern + "] for requestURI [" + requestURI + "].  " +
                            "Utilizing corresponding filter chain...");
                }
                return filterChainManager.proxy(originalChain, pathPattern);
            }
        }

        return null;
    }

`DefaultFilterChainManager.proxy`方法：

    public FilterChain proxy(FilterChain original, String chainName) {
        NamedFilterList configured = getChain(chainName);
        if (configured == null) {
            String msg = "There is no configured chain under the name/key [" + chainName + "].";
            throw new IllegalArgumentException(msg);
        }
        return configured.proxy(original);
    }
    
`SimpleNamedFilterList.proxy`方法：

    public FilterChain proxy(FilterChain orig) {
        return new ProxiedFilterChain(orig, this);
    }


最后看一下`ProxiedFilterChain`的全貌：

    public class ProxiedFilterChain implements FilterChain {

    private static final Logger log = LoggerFactory.getLogger(ProxiedFilterChain.class);
    //原始过滤器链
    private FilterChain orig;
    //Shiro配置的过滤器列表
    private List<Filter> filters;
    private int index = 0;

    public ProxiedFilterChain(FilterChain orig, List<Filter> filters) {
        if (orig == null) {
            throw new NullPointerException("original FilterChain cannot be null.");
        }
        this.orig = orig;
        this.filters = filters;
        this.index = 0;
    }
    
    //其内部实现的doFilter方法，方法很简单，实际上也是将请求代理给其内部的filter，首先会调用shiro配置的filter的foFilter方法处理请求，再调用原始过滤器链
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (this.filters == null || this.filters.size() == this.index) {
            //we've reached the end of the wrapped chain, so invoke the original one:
            if (log.isTraceEnabled()) {
                log.trace("Invoking original filter chain.");
            }
            this.orig.doFilter(request, response);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Invoking wrapped filter at index [" + this.index + "]");
            }
            this.filters.get(this.index++).doFilter(request, response, this);
        }
    }
}

返回到`AbstractShiroFilter.executeChain`：

    protected void executeChain(ServletRequest request, ServletResponse response, FilterChain origChain)
            throws IOException, ServletException {
        FilterChain chain = getExecutionChain(request, response, origChain);
        chain.doFilter(request, response);
    }

那么这里的`chain`就是`ProxiedFilterChain`实例。

接下来就看一下自己实现的过滤器，下面是`JtisAuthenticationFilter`的类图

![JtisAuthenticationFilter类图][5]

这比`SpringShiroFilter`更加复杂，不过其具有共同的父类`OncePerRequestFilter`，因此过滤器方法还是从`OncePerRequestFilter.doFilter`进入，只是`doFilterInternal`就不同了，具体实现在`AdviceFilter`中，看名字就带有切面的思想：

    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Exception exception = null;

        try {
            //调用preHandler方法，判断是否需要继续执行过滤器链，如果请求被拒绝那么直接跳过过滤链执行
            boolean continueChain = preHandle(request, response);
            if (log.isTraceEnabled()) {
                log.trace("Invoked preHandle method.  Continuing chain?: [" + continueChain + "]");
            }

            if (continueChain) {
                executeChain(request, response, chain);
            }

            postHandle(request, response);
            if (log.isTraceEnabled()) {
                log.trace("Successfully invoked postHandle method");
            }

        } catch (Exception e) {
            exception = e;
        } finally {
            cleanup(request, response, exception);
        }
    }

`preHandler`在子类`PathMatchingFilter`中实现：

    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {

        if (this.appliedPaths == null || this.appliedPaths.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("appliedPaths property is null or empty.  This Filter will passthrough immediately.");
            }
            return true;
        }
        //appliedPaths保存了过滤器url到config之间的映射，因为在配置文件中可以这样定义规则： /** = filter1[config], filter2[config]，那么filter1中的appliedPaths就保存了 /** 到 config的映射，filter2中的appliedPaths就保存了 /** 到 config的映射
        for (String path : this.appliedPaths.keySet()) {
            // If the path does match, then pass on to the subclass implementation for specific checks
            //(first match 'wins'):
            if (pathsMatch(path, request)) {
                log.trace("Current requestURI matches pattern '{}'.  Determining filter chain execution...", path);
                Object config = this.appliedPaths.get(path);
                //调用idFilterChainContinued方法
                return isFilterChainContinued(request, response, path, config);
            }
        }

        //no path matched, allow the request to go through:
        return true;
    }
    
    private boolean isFilterChainContinued(ServletRequest request, ServletResponse response, String path, Object pathConfig) throws Exception {
        //调用onPreHandler方法
        return onPreHandle(request, response, pathConfig);
    }

在子类`AccessControlFilter`中实现`onPreHandler`方法：
    
    //返回isAccessAllowed || onAccessDenied
    public boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        //(1)isAccessAllowed  (2)onAccessDenied
        return isAccessAllowed(request, response, mappedValue) || onAccessDenied(request, response, mappedValue);
    }

(1)`isAccessAllowed`会调用子类`AuthenticatingFilter`的实现：
    
    //调用父类isAccessAllowed方法判断是否有权限，如果有直接返回true，否则判断是否是登录请求，如果是直接方法true，否则返回该请求是否被允许
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return super.isAccessAllowed(request, response, mappedValue) ||
                (!isLoginRequest(request, response) && isPermissive(mappedValue));
    }
    
继续调用父类`AuthenticationFilter.isAccessAllowed`方法：
    
    //在Subject.login方法中如果登录成功，那么该Subject的authenticated会被置为true
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        Subject subject = getSubject(request, response);
        return subject.isAuthenticated();
    }

(2)如果`isAccessAllowed`返回`false`，表示用户可能没有登录，那么会继续调用`onAccessDenied`方法处理，这个方法有多种实现，对于`JtisAuthenticationFilter`其实现在父类`FormAuthenticationFilter`中：

    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        //如果是登录请求
        if (isLoginRequest(request, response)) {
            //如果是登录提交请求
            if (isLoginSubmission(request, response)) {
                if (log.isTraceEnabled()) {
                    log.trace("Login submission detected.  Attempting to execute login.");
                }
                //执行登录，返回登录结果，如果登录失败则返回fasle，这个过滤器连处理结束
                return executeLogin(request, response);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Login page view.");
                }
                //allow them to see the login page ;)
                return true;
            }
        } else {
            //如果不是登录请求，那么302到登录界面，并返回false，结束过滤器链处理
            if (log.isTraceEnabled()) {
                log.trace("Attempting to access a path which requires authentication.  Forwarding to the " +
                        "Authentication url [" + getLoginUrl() + "]");
            }

            saveRequestAndRedirectToLogin(request, response);
            return false;
        }
    }

下面是`JtisAuthorizationFilter`的类图：

![JtisAuthorizationFilter类图][6]

其内部实现的`isAccessAllowed`方法：

    protected boolean isAccessAllowed(ServletRequest servletRequest,
                                      ServletResponse servletResponse, Object o) throws Exception {
        try {
            Subject subject = getSubject(servletRequest, servletResponse);

            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String url = httpServletRequest.getRequestURI();
            String basePath = httpServletRequest.getContextPath();
            if (null != url && url.startsWith(basePath)) {
                url = url.replaceFirst(basePath, "");
            }

            if (!PermissionCache.singleton().findPermission(url)) {
                return true;
            } else {
                //实际上还是调用Subject.isPermitted方法进行权限验证
                return subject.isPermitted(url);
            }
        } catch (Exception e) {
            LOGGER.error("isAccessAllowed() : {}", e);
            e.printStackTrace();
            throw e;
        }
    }

再看看其父类`AuthorizationFilter`中实现的`onAccessDenied`方法：

    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {

        Subject subject = getSubject(request, response);
        // If the subject isn't identified, redirect to login URL
        //如果subject没有鉴别身份，保存当前请求
        if (subject.getPrincipal() == null) {
            saveRequestAndRedirectToLogin(request, response);
        } else {
            // If subject is known but not authorized, redirect to the unauthorized URL if there is one
            // If no unauthorized URL is specified, just return an unauthorized HTTP status code    
            //否则，如果subject鉴别了身份但是没有授权，那么重定向到unauthorizedUrl或者直接返回401响应码
            String unauthorizedUrl = getUnauthorizedUrl();
            //SHIRO-142 - ensure that redirect _or_ error code occurs - both cannot happen due to response commit:
            if (StringUtils.hasText(unauthorizedUrl)) {
                WebUtils.issueRedirect(request, response, unauthorizedUrl);
            } else {
                WebUtils.toHttp(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
        return false;
    }

**Subject**

最开始创建`Subject`实例代码，在`AbstractShiroFilter`的`doFilterInternal`方法中：

    protected void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, final FilterChain chain)
            throws ServletException, IOException {

        Throwable t = null;

        try {
            final ServletRequest request = prepareServletRequest(servletRequest, servletResponse, chain);
            final ServletResponse response = prepareServletResponse(request, servletResponse, chain);
            //(1)创建了一个新的subject
            final Subject subject = createSubject(request, response);

            //(2)执行Callable任务
            subject.execute(new Callable() {
                public Object call() throws Exception {
                    updateSessionLastAccessTime(request, response);
                    executeChain(request, response, chain);
                    return null;
                }
            });
        } catch (ExecutionException ex) {
            t = ex.getCause();
        } catch (Throwable throwable) {
            t = throwable;
        }

        if (t != null) {
            if (t instanceof ServletException) {
                throw (ServletException) t;
            }
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            //otherwise it's not one of the two exceptions expected by the filter method signature - wrap it in one:
            String msg = "Filtered request failed.";
            throw new ServletException(msg, t);
        }
    }

(1)`createSubject`方法：

    protected WebSubject createSubject(ServletRequest request, ServletResponse response) {
        return new WebSubject.Builder(getSecurityManager(), request, response).buildWebSubject();
    }
    
`Subject$Builder.buildObject`方法：

    public Subject buildSubject() {
            return this.securityManager.createSubject(this.subjectContext);
        }

(2)创建完`Subject`实例之后，会调用其`execute`方法执行`Callable`任务

    public <V> V execute(Callable<V> callable) throws ExecutionException {
        //
        Callable<V> associated = associateWith(callable);
        try {
            return associated.call();
        } catch (Throwable t) {
            throw new ExecutionException(t);
        }
    }

`associatedWith`方法，这个方法就是将`Callable`任务 和`Subject`绑定，构造`SubjectCallable`实例，`Subject`和线程的关系是怎么样的呢，这里可以看一下`ThreadContext`类，其中有个成员变量`resources`，它的类型是`ThreadLocal`：

    private static final ThreadLocal<Map<Object, Object>> resources = new InheritableThreadLocalMap<Map<Object, Object>>();
    
绑定`Subject`方法：

    public static void bind(Subject subject) {
        if (subject != null) {
            put(SUBJECT_KEY, subject);
        }
    }
    
绑定`SecurityManager`方法：

    public static void bind(SecurityManager securityManager) {
        if (securityManager != null) {
            put(SECURITY_MANAGER_KEY, securityManager);
        }
    }

`SubjectCallable`:

    public class SubjectCallable<V> implements Callable<V> {
    
    //ThreadState成员变量
    protected final ThreadState threadState;
    private final Callable<V> callable;

    public SubjectCallable(Subject subject, Callable<V> delegate) {
        //传入SubjectThreadState
        this(new SubjectThreadState(subject), delegate);
    }

    protected SubjectCallable(ThreadState threadState, Callable<V> delegate) {
        if (threadState == null) {
            throw new IllegalArgumentException("ThreadState argument cannot be null.");
        }
        this.threadState = threadState;
        if (delegate == null) {
            throw new IllegalArgumentException("Callable delegate instance cannot be null.");
        }
        this.callable = delegate;
    }
    
    //call实现，三步：bind/call/restore
    public V call() throws Exception {
        try {
            //(1)将当前Subject设置为线程私有
            threadState.bind();
            //(2)执行任务
            return doCall(this.callable);
        } finally {
            //(3)恢复执行任务前的状态
            threadState.restore();
        }
    }

    protected V doCall(Callable<V> target) throws Exception {
        return target.call();
    }
}

(1)`ThreadState.bind`，此方法将当前`Subject`与当前线程绑定：

    public void bind() {
        SecurityManager securityManager = this.securityManager;
        if ( securityManager == null ) {
            //try just in case the constructor didn't find one at the time:
            securityManager = ThreadContext.getSecurityManager();
        }
        //先保存原始的资源，便于后面的restore
        this.originalResources = ThreadContext.getResources();
        //清除当前线程相关内容
        ThreadContext.remove();
        //重新绑定当前Subject到当前线程
        ThreadContext.bind(this.subject);
        if (securityManager != null) {
            ThreadContext.bind(securityManager);
        }
    }

`ThreadContext.bind`方法：

    public static void bind(Subject subject) {
        if (subject != null) {
            put(SUBJECT_KEY, subject);
        }
    }
    
    public static void put(Object key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (value == null) {
            remove(key);
            return;
        }
        //确定初始化了，这里的resources就是ThreadLocal类型，存储了线程私有的变量
        ensureResourcesInitialized();
        //更新当前线程的私有变量
        resources.get().put(key, value);
    }

（2）`doCall`，直接调用`call`方法:

    protected V doCall(Callable<V> target) throws Exception {
        return target.call();
    }

(3)`ThreadState.restore`方法，用来恢复`Callable`任务调用之前的状态：

    public void restore() {
        //清除调用之后的修改
        ThreadContext.remove();
        if (!CollectionUtils.isEmpty(this.originalResources)) {
            //恢复
            ThreadContext.setResources(this.originalResources);
        }
    }

说到这里，再回头看一下为什么每次调用`SecurityUtils.getSubject`就能获取当前线程对应的`Subject`呢：
    
    //实际上也是用过ThreadContext获取Subject，这个Subject都是和线程相关的，每个线程都会有自己的Subject实例，这实际上又是ThreadLocal的一个典型应用
    public static Subject getSubject() {
        Subject subject = ThreadContext.getSubject();
        if (subject == null) {
            subject = (new Subject.Builder()).buildSubject();
            ThreadContext.bind(subject);
        }
        return subject;
    }

这里看看`Subject`接口声明的任务相关的方法：

1、`execute`方法：

    //将指定的Callable和此Subject实例绑定，然后在当前线程执行Callable任务，如果你想要在其他线程执行Callable任务，建议先使用associate方法将Callable和Subject绑定，然后结合线程池处理
    
    <V> V execute(Callable<V> callable) throws ExecutionException;
    void execute(Runnable runnable);    //同上
    
实现（`DelegatingSubject`）：

    public <V> V execute(Callable<V> callable) throws ExecutionException {
        Callable<V> associated = associateWith(callable);   //associateWith绑定，返回SubjectCallable实例
        try {
            return associated.call();   //执行任务
        } catch (Throwable t) {
            throw new ExecutionException(t);
        }
    }
    
    //SubjectCallable.call方法，先将subject绑定到当前线程(ThreadLocal)，这里的ThreadLocal就起到Subject线程私有的作用，然后执行任务，最后恢复到原来的线程状态
    public V call() throws Exception {
        try {
            threadState.bind();
            return doCall(this.callable);
        } finally {
            threadState.restore();
        }
    }
    
    //上面是一个编程范式，在ThreadState.bind方法中有说明
    ThreadState state = //acquire or instantiate as necessary
    try {
        state.bind();
        doSomething();  //execute any logic downstream logic that might need to access to state
    } finally {
        state.restore();
    }

2、`associateWith`方法：
    
    //将任务和Subject绑定
    
    <V> Callable<V> associateWith(Callable<V> callable);
    Runnable associateWith(Runnable runnable);

实现（`DelegatingSubject`）：

    public <V> Callable<V> associateWith(Callable<V> callable) {
        return new SubjectCallable<V>(this, callable);
    }

**Session缓存**

先看配置文件：

    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">
        <property name="sessionIdGenerator" ref="sessionIdGenerator"/>
    </bean>
    
`EnterpriseCacheSessionDAO`实现了顶层的`SessionDAO`接口，并且具备缓存特性

贴图`EnterpriseCacheSessionDAO`

看看顶层父类`SessionDAO`声明的方法：

贴图`SessionDAO`

看看`create`方法的实现，`CachingSessionDAO`：

    public Serializable create(Session session) {
        Serializable sessionId = super.create(session); //(1)
        cache(session, sessionId);  (2)
        return sessionId;
    }

(1)`AbstractSessionDAO.create`方法：

    public Serializable create(Session session) {
        Serializable sessionId = doCreate(session); //生成sessionId
        verifySessionId(sessionId);
        return sessionId;
    }

(2)`cache`方法，将`Session`实例缓存：

    protected void cache(Session session, Serializable sessionId) {
        if (session == null || sessionId == null) {
            return;
        }
        Cache<Serializable, Session> cache = getActiveSessionsCacheLazy();
        if (cache == null) {
            return;
        }
        cache(session, sessionId, cache);   //将session作为value缓存，key是sessionId
    }

`getActiveSessionsCacheLazy`方法：

     private Cache<Serializable, Session> getActiveSessionsCacheLazy() {
        if (this.activeSessions == null) {
            this.activeSessions = createActiveSessionsCache();
        }
        return activeSessions;
    }
    
    protected Cache<Serializable, Session> createActiveSessionsCache() {
        Cache<Serializable, Session> cache = null;
        CacheManager mgr = getCacheManager();   //获取CacheManager
        if (mgr != null) {
            String name = getActiveSessionsCacheName(); //获取active sessions的缓存名称，默认是 private String activeSessionsCacheName = ACTIVE_SESSION_CACHE_NAME;(shiro-activeSessionCache)
            cache = mgr.getCache(name);
        }
        return cache;
    }
    
上面的方法实际上是从缓存管理器中获取`active sessions`缓存，因此在缓存配置文件`ehcache.xml`中要配置名为`shiro-activeSessionCache`的缓存：

    <cache name="shiro-activeSessionCache"
           maxEntriesLocalHeap="3000"
           eternal="true"
           overflowToDisk="true"
           diskPersistent="true"
           diskExpiryThreadIntervalSeconds="600"
           statistics="true">
    </cache>


  [1]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/shiro%E6%A1%86%E6%9E%B6%E6%A6%82%E8%BF%B0.PNG
  [2]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/shiro%E6%A1%86%E6%9E%B6.PNG
  [3]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/shiro%E6%B5%81%E7%A8%8B.PNG
  [4]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/SpringShiroFilter.png
  [5]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/JtisAuthenticationFilter.png
  [6]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/JtisAuthorizationFilter.png
  [7]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/shiro/EnterpriseCacheSessionDAO.jpg