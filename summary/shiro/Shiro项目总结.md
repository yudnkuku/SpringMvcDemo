# Shiro项目总结

标签（空格分隔）： shiro

---

## shiro的逻辑架构 ##
`shiro`的逻辑架构主要包括认证(`Authentication`)、授权(`Authorization`)、会话管理

图：`shiro`逻辑架构

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

图

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