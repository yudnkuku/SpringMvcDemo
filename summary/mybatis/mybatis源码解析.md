﻿# mybatis源码解析

标签（空格分隔）： 源码解析

---

## 引言 ##
`MyBatis`是支持定制化`SQL`、存储过程以及高级映射的优秀的持久层框架。`MyBatis` 避免了几乎所有的`JDBC`代码和手动设置参数以及获取结果集。MyBatis可以对配置和原生`Map`使用简单的`XML`或注解，将接口和 `Java` 的`POJOs(Plain Old Java Objects`,普通的`J
ava`对象)映射成数据库中的记录
`MyBatis`框架一般执行流程：

 - 通过`MyBatis`的配置文件`mybatis-config.xml`来配置相关信息，注入映射接口文件、类型别名、其他设置等
 

        <configuration>
    
        <typeAliases>
            <!-- 这个包下面的所有类的别名为类名的小写 -->
            <package name="com.fiberhome.jtis.server.jtis.entity"/>
        </typeAliases>
    
    
        <!-- configuration location -->
        <mappers>
            <mapper resource="mybatis/userDao.xml"/>
        </mappers>
    
    
    </configuration>

 - 配置数据源、`SqlSessionFactory`和事务管理等其他信息，例如在于`Spring MVC`集成时，可以直接配置`SqlSessionFactoryBean`,该工厂`bean`可以创建`SqlSessionFactory`，详细可以看源码
 - 通过`SqlSessionFactory`创建`SqlSession`，通过`SqlSession`中的方法来操作数据库，调用`commit()`方法提交事务，最后关闭`SqlSession`释放资源
 

## SqlSessionFactoryBean ##

一些成员变量，可以通过`set`方法注入
      
      //配置文件mybatisConfig.xml的路径，将文件读取成Resource对象
      private Resource configLocation;
      //mapper文件路径
      private Resource[] mapperLocations;
      //数据源
      private DataSource dataSource;
      //事务管理器
      private TransactionFactory transactionFactory;
      //配置属性
      private Properties configurationProperties;

在继续之前必须先看看`Configuration`的定义：
      
      //成员变量定义
      protected Environment environment;    //环境
      protected boolean safeRowBoundsEnabled = false;
      protected boolean safeResultHandlerEnabled = true;
      protected boolean mapUnderscoreToCamelCase = false;
      protected boolean aggressiveLazyLoading = true;
      protected boolean multipleResultSetsEnabled = true;
      protected boolean useGeneratedKeys = false; 
      protected boolean useColumnLabel = true;
      protected boolean cacheEnabled = true;    //是否使用二级缓存，默认true
      protected boolean callSettersOnNulls = false;
      protected String logPrefix;
      protected Class <? extends Log> logImpl;  //日志实现
      protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
      protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
      protected Set<String> lazyLoadTriggerMethods = new HashSet<String>(Arrays.asList(new String[] { "equals", "clone", "hashCode", "toString" }));
      protected Integer defaultStatementTimeout;
      protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE; //实际上运行sql的执行类，默认是SIMPLE
      protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
    
      protected Properties variables = new Properties();
      protected ObjectFactory objectFactory = new DefaultObjectFactory();
      protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();
      protected MapperRegistry mapperRegistry = new MapperRegistry(this);   //MapperRegistry实例，维护了接口和代理工厂的映射
    
      protected boolean lazyLoadingEnabled = false;
      protected ProxyFactory proxyFactory;
    
      protected String databaseId;
      protected Class<?> configurationFactory;
    
      protected final InterceptorChain interceptorChain = new InterceptorChain();
      protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();  //类似于MapperRegistry，管理TypeHandler，TypeHandler在设置sql参数和解析result时转换java类型和jdbc类型
      protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();    //类似于MapperRegistry和TypeHandlerRegistry，管理等价名和类间的映射关系，例如Integer可用integer或者int替换
      protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();
    
      protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection"); //维护接口方法全限定名称和MappedStatement间的映射关系，如果此map中没有某方法的对应键值，表明该方法没有在任何xml映射文件中声明
      protected final Map<String, Cache> caches = new StrictMap<Cache>("Caches collection");    //缓存映射
      protected final Map<String, ResultMap> resultMaps = new StrictMap<ResultMap>("Result Maps collection");   //ResultMap映射
      protected final Map<String, ParameterMap> parameterMaps = new StrictMap<ParameterMap>("Parameter Maps collection");
      protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<KeyGenerator>("Key Generators collection");
    
      protected final Set<String> loadedResources = new HashSet<String>();
      protected final Map<String, XNode> sqlFragments = new StrictMap<XNode>("XML fragments parsed from previous mappers");
    
      protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<XMLStatementBuilder>();
      protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<CacheRefResolver>();
      protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<ResultMapResolver>();
      protected final Collection<MethodResolver> incompleteMethods = new LinkedList<MethodResolver>();
      protected final Map<String, String> cacheRefMap = new HashMap<String, String>();

该接口实现了`InitializingBean`接口，因此在实例化该`bean`时会回调`afterPropertiesSet()`方法：

    public void afterPropertiesSet() throws Exception {
        notNull(dataSource, "Property 'dataSource' is required");
        notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");

        this.sqlSessionFactory = buildSqlSessionFactory();
    }

`buildSqlSessionFactory()`方法用来构建`SqlSessionFactory`实例：

    protected SqlSessionFactory buildSqlSessionFactory() throws IOException {

    Configuration configuration;
    //解析<Configuration>节点
    XMLConfigBuilder xmlConfigBuilder = null;
    if (this.configLocation != null) {
      xmlConfigBuilder = new XMLConfigBuilder(this.configLocation.getInputStream(), null, this.configurationProperties);
      configuration = xmlConfigBuilder.getConfiguration();
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Property 'configLocation' not specified, using default MyBatis Configuration");
      }
      configuration = new Configuration();
      configuration.setVariables(this.configurationProperties);
    }
    //接着是一大段解析一些属性的代码，这些属性在<configuration>节点下也可以进行配置，诸如<typeAliases>、<plugins>等，这些代码比较明白就不贴了
    if (xmlConfigBuilder != null) {
      try {
        //解析mybatis-config.xml
        xmlConfigBuilder.parse();

        if (logger.isDebugEnabled()) {
          logger.debug("Parsed configuration file: '" + this.configLocation + "'");
        }
      } catch (Exception ex) {
        throw new NestedIOException("Failed to parse config resource: " + this.configLocation, ex);
      } finally {
        ErrorContext.instance().reset();
      }
    }
    ..
    //使用配置信息configuration构建DefaultSqlSessionFactory实例
    return this.sqlSessionFactoryBuilder.build(configuration);
    }
    
`xmlConfigBuilder.parse()`方法：

    public Configuration parse() {
    //判断是否解析过
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    //开始解析
    parseConfiguration(parser.evalNode("/configuration"));
    return configuratin;
    }
      //解析<configuration>下的所有节点
      private void parseConfiguration(XNode root) {
        try {
          propertiesElement(root.evalNode("properties")); //issue #117 read properties first
          typeAliasesElement(root.evalNode("typeAliases"));
          pluginElement(root.evalNode("plugins"));
          objectFactoryElement(root.evalNode("objectFactory"));
          objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
          settingsElement(root.evalNode("settings"));
          environmentsElement(root.evalNode("environments")); // read it after objectFactory and objectWrapperFactory issue #631
          databaseIdProviderElement(root.evalNode("databaseIdProvider"));
          typeHandlerElement(root.evalNode("typeHandlers"));
          //解析<mappers>子节点
          mapperElement(root.evalNode("mappers"));
        } catch (Exception e) {
          throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
      }
      
`mapperElement()`:

    private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      //遍历所有的<mapper>节点
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          //如果是<package>子节点
          String mapperPackage = child.getStringAttribute("name");
          //将该package下的所有类全部注册到MapperRegistry
          configuration.addMappers(mapperPackage);
        } else {
          //通过resource、url、class三个维度属性来定义mapper
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          //如果只定义了resource属性
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            //如果只定义了url属性
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            //如果只定义了class属性，注册到MapperRegistry
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            //不能同时定义多个元素，否则抛出异常
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }

  
`MapperRegistry`源码：

    public class MapperRegistry {
    private Configuration config;
     //已经添加的mapper，是一个HashMap，key表示mapper类，value表示MapperProxyFactory，用来实例化代理对象
      private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();
    
      public MapperRegistry(Configuration config) {
        this.config = config;
      }
      ..
      //添加mapper，key是type，value是new MapperProxyFactory(type),MapperProxyFactory可以用来实例化type接口的代理对象
      public <T> void addMapper(Class<T> type) {
    if (type.isInterface()) {
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        knownMappers.put(type, new MapperProxyFactory<T>(type));
        // It's important that the type is added before the parser is run
        // otherwise the binding may automatically be attempted by the
        // mapper parser. If the type is already known, it won't try.
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
        loadCompleted = true;
      } finally {
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
    }
    }
     
`MapperProxyFactory`源码：

        public class MapperProxyFactory<T> {
      //接口
      private final Class<T> mapperInterface;
      //方法缓存，使用sqlSession构建代理对象时，会传给MapperProxy
      private Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();
      //之前addMapper(type)就是通过这个方法实例化MapperProxyFactory，设置了代理的接口
      public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
      }
    
      public Class<T> getMapperInterface() {
        return mapperInterface;
      }
    
      public Map<Method, MapperMethod> getMethodCache() {
        return methodCache;
      }
      //使用MapperProxy来构建代理对象
      @SuppressWarnings("unchecked")
      protected T newInstance(MapperProxy<T> mapperProxy) {
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
      }
      //使用sqlSession来构建代理对象
      public T newInstance(SqlSession sqlSession) {
        final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
        return newInstance(mapperProxy);
      }
    
    }
    
`MapperProxy`：
    

    //这个类实现了InvocationHandler接口，该接口是JDK动态代理的核心接口
        public class MapperProxy<T> implements InvocationHandler, Serializable {
    
      private static final long serialVersionUID = -6424540398559729838L;
      //sqlSession
      private final SqlSession sqlSession;
      //代理的接口
      private final Class<T> mapperInterface;
      //方法缓存，Method->MapperMethod，这个缓存可以提升效率
      private final Map<Method, MapperMethod> methodCache;
    
      public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
      }
      //实现invoke()方法，对接口方法的调用实际上会代理到此方法
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
          try {
            return method.invoke(this, args);
          } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
          }
        }
        //从缓存中获取MapperMethod，如果没有则new一个
        final MapperMethod mapperMethod = cachedMapperMethod(method);
        //调用mapperMethod的execute()方法，这里真正的执行sql语句
        return mapperMethod.execute(sqlSession, args);
      }
      //从缓存中获取MapperMethod
      private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = methodCache.get(method);
        if (mapperMethod == null) {
          mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
          methodCache.put(method, mapperMethod);
        }
        return mapperMethod;
      }
    
    }
    
`MapperMethod`源码：
    
    //构造方法
    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.command = new SqlCommand(config, mapperInterface, method); //构造SqlCommand对象
        this.method = new MethodSignature(config, method);
    }
    
    //执行sql
    public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    //insert
    if (SqlCommandType.INSERT == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      //使用sqlSession的insert()方法，再将sql结果转换为方法返回类型
      result = rowCountResult(sqlSession.insert(command.getName(), param));
    } else if (SqlCommandType.UPDATE == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.update(command.getName(), param));
    } else if (SqlCommandType.DELETE == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.delete(command.getName(), param));
    } else if (SqlCommandType.SELECT == command.getType()) {
      if (method.returnsVoid() && method.hasResultHandler()) {
        executeWithResultHandler(sqlSession, args);
        result = null;
      } else if (method.returnsMany()) {
        result = executeForMany(sqlSession, args);
      } else if (method.returnsMap()) {
        result = executeForMap(sqlSession, args);
      } else {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = sqlSession.selectOne(command.getName(), param);
      }
    } else {
      throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName() 
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
      }
      
    private Object rowCountResult(int rowCount) {
        final Object result;
        if (method.returnsVoid()) {
          result = null;
        } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
          result = rowCount;
        } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
          result = (long) rowCount;
        } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
          result = (rowCount > 0);
        } else {
          throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
        }
        return result;
      }
      
`SqlCommand`构造方法：
    
    //此方法主要检查接口方法是否在xml文件中定义了映射关系
    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) throws BindingException {
      //获取接口方法全限定名称
      String statementName = mapperInterface.getName() + "." + method.getName();
      MappedStatement ms = null;
      //Configuration实例中的mappedStatements维护了方法名称到对应MappedStatement对象间的映射map
      if (configuration.hasStatement(statementName)) {
        ms = configuration.getMappedStatement(statementName);
      } else if (!mapperInterface.equals(method.getDeclaringClass().getName())) { // issue #35
        String parentStatementName = method.getDeclaringClass().getName() + "." + method.getName();
        if (configuration.hasStatement(parentStatementName)) {
          ms = configuration.getMappedStatement(parentStatementName);
        }
      }
      //如果没有绑定映射关系，抛出异常，例如接口里的方法在.xml文件中未声明
      if (ms == null) {
        throw new BindingException("Invalid bound statement (not found): " + statementName);
      }
      name = ms.getId();
      type = ms.getSqlCommandType();
      if (type == SqlCommandType.UNKNOWN) {
        throw new BindingException("Unknown execution method for: " + name);
      }
    }

`XMLMapperBuilder`源码：

    public void parse() {
    if (!configuration.isResourceLoaded(resource)) {
      configurationElement(parser.evalNode("/mapper"));
      configuration.addLoadedResource(resource);
      bindMapperForNamespace();
    }

    parsePendingResultMaps();
    parsePendingChacheRefs();
    parsePendingStatements();
    }

      private void configurationElement(XNode context) {
        try {
          String namespace = context.getStringAttribute("namespace");
          if (namespace.equals("")) {
        	  throw new BuilderException("Mapper's namespace cannot be empty");
          }
          builderAssistant.setCurrentNamespace(namespace);
          cacheRefElement(context.evalNode("cache-ref"));
          cacheElement(context.evalNode("cache"));
          parameterMapElement(context.evalNodes("/mapper/parameterMap"));
          //解析resultMap子节点放到configuration中
          resultMapElements(context.evalNodes("/mapper/resultMap"));
          //解析sql子节点，并存到sqlFragments中，sqlFraments是一个Map结构，key为<sql>节点的id属性，value为代表该节点的XNode实例
          sqlElement(context.evalNodes("/mapper/sql"));
          //解析select|insert|update|delete节点，并实例化MappedStatement存入configuration中
          buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
        } catch (Exception e) {
          throw new BuilderException("Error parsing Mapper XML. Cause: " + e, e);
        }
      }
      
      
## TypeHandler源码 ##
这里主要解析`EnumTypeHandler`和`EnumOrdinalTypeHandler`的源码。

首先看看`SqlRunner`中如何执行`sql`语句的，比如插入数据：
    
    //插入数据，sql表示sql语句，args表示mapper方法传入的java对象参数
    public int insert(String sql, Object... args) throws SQLException {
    PreparedStatement ps;
    //构造PreparedStatement对象
    if (useGeneratedKeySupport) {
      ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    } else {
      ps = connection.prepareStatement(sql);
    }

    try {
      //在预处理语句中设置参数
      setParameters(ps, args);
      //执行预处理语句
      ps.executeUpdate();
      if (useGeneratedKeySupport) {
        List<Map<String, Object>> keys = getResults(ps.getGeneratedKeys());
        if (keys.size() == 1) {
          Map<String, Object> key = keys.get(0);
          Iterator<Object> i = key.values().iterator();
          if (i.hasNext()) {
            Object genkey = i.next();
            if (genkey != null) {
              try {
                return Integer.parseInt(genkey.toString());
              } catch (NumberFormatException e) {
                //ignore, no numeric key suppot
              }
            }
          }
        }
      }
      return NO_GENERATED_KEY;
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
     }

`setParamters()`方法：
    
    //在预处理语句ps中设置参数
    private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
    for (int i = 0, n = args.length; i < n; i++) {
      if (args[i] == null) {
        throw new SQLException("SqlRunner requires an instance of Null to represent typed null values for JDBC compatibility");
      } else if (args[i] instanceof Null) {
        ((Null) args[i]).getTypeHandler().setParameter(ps, i + 1, null, ((Null) args[i]).getJdbcType());
      } else {
        //从TypeHandlerRegistry中获取参数类型对应的TypeHandler
        TypeHandler typeHandler = typeHandlerRegistry.getTypeHandler(args[i].getClass());
        if (typeHandler == null) {
          throw new SQLException("SqlRunner could not find a TypeHandler instance for " + args[i].getClass());
        } else {
          //调用TypeHandler的setParameter()方法设置ps参数
          typeHandler.setParameter(ps, i + 1, args[i], null);
        }
      }
    }
      }

`TypeHandlerRegistry`中的一些重要的成员变量：
    
    //双map，第一层map键是java type，值是第二个map，第二个map键是jdbc type，值是TypeHandler对象，这说明一个java type可以对应一个jdbcHandlerMap，jdbcHandlerMap中可以包含多个jdbc type和TypeHandler的映射。
    //举个例子：java.util.Date是java type，然而其对应的jdbc type有很多，比如TIMESTAMP/DATE/TIME，依次对应DateTypeHandler/DateOnlyTypeHandler/TimeOnlyTypeHandler
    
    private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new ConcurrentHashMap<Type, Map<JdbcType, TypeHandler<?>>>();

另外，`TypeHandlerRegistry`在构造方法中注册了很多默认`TypeHandler`，具体可以查看源码

`typeHandlerRegistry.getTypeHandler()`方法:
    
    
    public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
        //jdbcType=null调用
        return getTypeHandler((Type) type, null);
    }
    
    //type是传来的参数Class对象，jdbcType=null
    private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
    //获取type类型对应的jdbcHandlerMap
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
    TypeHandler<?> handler = null;
    if (jdbcHandlerMap != null) {
      handler = jdbcHandlerMap.get(jdbcType);
      if (handler == null) {
        handler = jdbcHandlerMap.get(null);
      }
      if (handler == null) {
        // #591
        handler = pickSoleHandler(jdbcHandlerMap);
      }
    }
    // type drives generics here
    return (TypeHandler<T>) handler;
      }
      
      //获取type类型对应的jdbcHandlerMap
      private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
    if (NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap)) {
      return null;
    }
    if (jdbcHandlerMap == null && type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      //获取type父类型的jdbcHandlerMap
      jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
      if (jdbcHandlerMap != null) {
        TYPE_HANDLER_MAP.put(type, jdbcHandlerMap);
      } else if (clazz.isEnum()) {
        //如果type是枚举类型，那么注册对应的EnumTypeHandler类型处理器
        register(clazz, new EnumTypeHandler(clazz));
        //返回type对应的jdbcHandlerMap
        return TYPE_HANDLER_MAP.get(clazz);
      }
    }
    if (jdbcHandlerMap == null) {
      TYPE_HANDLER_MAP.put(type, NULL_TYPE_HANDLER_MAP);
    }
    return jdbcHandlerMap;
      }
      
      

## 一级缓存(本地缓存)，二级缓存 ##
在`<configuration>`节点中，`<settings>`子元素中定义了一些缓存相关的属性，例如

 - `cacheEnabled`：是否开启一级缓存，默认值`true`
 - `defaultExecutorType`：配置的执行器类型，有三种取值`SIMPLE/REUSE/BATCH`，这三种都继承自`BaseExecutor`，还有一种是`CachingExecutor`，默认是`SIMPLE`，`Executor`实例在`DefaultSqlSessionFactory`中构建`SqlSession`实例是会进行初始化
 
在`mapper`文件中，也有很多与缓存相关的属性配置，例如：
 - `<cache>`：二级缓存配置，这个在`CachingExecutor`中会用到
 - `<cache-ref>`：二级缓存配置的引用
 - `flushCache`属性：这个属性在`sql`语句中声明，如果是`true`，则表明一级缓存和二级缓存都会被清空，默认值是`!isSelect`，表示如果不是`select`语句，那么`flushCache`属性就是`true`，否则默认是`false`
 - `useCache`属性：将其设置为`true`，将会导致本条语句的结果被二级缓存，默认值：`select`语句为`true`，非`select`语句是`false`

`DefaultSqlSessionFactory`构建`SqlSession`实例：

    private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      final Environment environment = configuration.getEnvironment();
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      //根据配置的execType创建Executor实例
      final Executor executor = configuration.newExecutor(tx, execType);
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
      }
      
    //构建Executor实例
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        executorType = executorType == null ? defaultExecutorType : executorType;
        executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
        Executor executor;
        if (ExecutorType.BATCH == executorType) {
          executor = new BatchExecutor(this, transaction);
        } else if (ExecutorType.REUSE == executorType) {
          executor = new ReuseExecutor(this, transaction);
        } else {
          executor = new SimpleExecutor(this, transaction);
        }
        if (cacheEnabled) {
        //如果cacheEnabled为true(默认值为true)，那么使用BaseExecutor对象构建CachingExecutor，CachingExecutor实际上是BaseExecutor的代理类，很多功能都是由BaseExecutor完成的；如果是false，那么表明不会使用二级缓存，即使定义了<cache>元素也没有用，但还是会使用一级缓存
          executor = new CachingExecutor(executor);
        }
        executor = (Executor) interceptorChain.pluginAll(executor);
        return executor;
    }


`MyBatis Cache`流程图：

![MyBatis流程图](https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/mybatis/MyBatis%20Cache%E6%B5%81%E7%A8%8B%E5%9B%BE.jpg)

以`select`为例分析缓存源码，`DefaultSqlSession`：
    
    //所有的select方法最终都会调用此方法，statement:查询语句id，parameter:查询参数，rowBounds:分页相关
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      //获取MappedStatement对象
      MappedStatement ms = configuration.getMappedStatement(statement);
      //执行查询方法，这里进入CachingExecutor
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
      }

`CachingExecutor`：

    public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameterObject);
        //创建缓存key
        CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
        return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
    }
    
    public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
        //代理给BaseExecutor中的createCacheKey()方法
        return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
    }
    
    //CachingExecutor的query()方法
    public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
        //获取<cache>元素定义的Cache
        Cache cache = ms.getCache();
        if (cache != null) {
          //如果开启了二级缓存
          //是否需要清空二级缓存，例如调用了insert/update/delete并commit，此时需要清空二级缓存
          flushCacheIfRequired(ms);
          if (ms.isUseCache() && resultHandler == null) {
            //如果使用二级缓存且resultHandler为空
            ensureNoOutParams(ms, parameterObject, boundSql);
            @SuppressWarnings("unchecked")
            //从二级缓存中获取查询结果
            List<E> list = (List<E>) tcm.getObject(cache, key);
            if (list == null) {
              //如果没有就代理到BaseExecutor的query方法，此时会将结果缓存到一级缓存中
              list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
              //同时将查询结果放入到二级缓存中
              tcm.putObject(cache, key, list); // issue #578 and #116
            }
            return list;
          }
        }
        return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
    }
      
`BaseExecutor`：
    
    //BaseExecutor的query方法
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
        ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
        if (closed) {
          throw new ExecutorException("Executor was closed.");
        }
        if (queryStack == 0 && ms.isFlushCacheRequired()) {
          //如果设置flushCache为true或者是insert/update/delete(非select语句)，则清空一级缓存，本地缓存的默认定义是PerpetualCache，其内部结构就是一个id和一个HashMap，键是缓存key，值是db sql结果
          clearLocalCache();
        }
        List<E> list;
        try {
          queryStack++;
          //从本地缓存中获取key对应的结果
          list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
          if (list != null) {
            //不为空
            handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
          } else {
            //缓存中不存在走db查询
            list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
          }
        } finally {
          queryStack--;
        }
        if (queryStack == 0) {
          for (DeferredLoad deferredLoad : deferredLoads) {
            deferredLoad.load();
          }
          // issue #601
          deferredLoads.clear();
          if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
            // issue #482
            clearLocalCache();
          }
        }
        return list;
    }

缓存小结：

    首先如果在setting元素中设置cacheEnabled属性为false，那么会禁止使用二级缓存，即使定义了<cache>元素也没用，那么每次调用mapper方法时，会首先检查一级缓存是否有缓存值，如果有直接返回缓存值，否则从数据库中查询，如果是非select语句，在查询之前会先将缓存清空，也就是说这些语句会直接走db；如果cacheEnabled属性为true，同时定义了`<cache>`元素，那么会使用二级缓存，在调用mapper方法时，会首先检查二级缓存中是否有缓存值，如果有直接返回，否则将操作代理给包装的BaseExecutor，剩下的操作和之前一样

一级缓存实例：

    @Test
    public void testCache() {
        SqlSession sqlSession = SqlSessionUtil.getSession();
        try {
            IStudentDao dao = sqlSession.getMapper(IStudentDao.class);
            String name = "yuding";
            //第一次查
            dao.getStudentInfoByName(name);
            //第二次查从一级缓存中拿值
            dao.getStudentInfoByName(name);

        } finally {
            if(sqlSession != null) {
                sqlSession.close();
            }
        }
    }

结果，只进行了一次查询：

    19-03-03 12:04:021 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Opening JDBC Connection
    2019-03-03 12:04:021 [main      ]  DEBUG    o.a.i.d.p.PooledDataSource  - Created connection 1632497828.
    2019-03-03 12:04:021 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Setting autocommit to false on JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:04:021 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==>  Preparing: select student_id as id, student_name as name from student where student_name=? 
    2019-03-03 12:04:021 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==> Parameters: yuding(String)
    2019-03-03 12:04:022 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==    Columns: id, name
    2019-03-03 12:04:022 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==        Row: 1, yuding
    2019-03-03 12:04:022 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - <==      Total: 1
    2019-03-03 12:04:022 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Resetting autocommit to true on JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:04:022 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Closing JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:04:022 [main      ]  DEBUG    o.a.i.d.p.PooledDataSource  - Returned connection 1632497828 to pool.

执行`delete`语句并提交，会清空一级缓存(二级缓存同样会清空)：

    @Test
    public void testCache() {
        SqlSession sqlSession = SqlSessionUtil.getSession();
        try {
            IStudentDao dao = sqlSession.getMapper(IStudentDao.class);
            String name = "yuding";
            //第一次查
            dao.getStudentInfoByName(name);
            //删除，清空1级缓存
            dao.deleteStudent(2);
            sqlSession.commit();
            //第二次查，此时一级缓存被清空，会从db查询
            dao.getStudentInfoByName(name);

        } finally {
            if(sqlSession != null) {
                sqlSession.close();
            }
        }
    }

结果，第二此`select`没有从缓存中拿值，而是走的`db`：

    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Opening JDBC Connection
    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.d.p.PooledDataSource  - Created connection 1632497828.
    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Setting autocommit to false on JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==>  Preparing: select student_id as id, student_name as name from student where student_name=? 
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==> Parameters: yuding(String)
    2019-03-03 12:08:042 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==    Columns: id, name
    2019-03-03 12:08:042 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==        Row: 1, yuding
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - <==      Total: 1
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.deleteStudent  - ==>  Preparing: delete from student where student_id=? 
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.deleteStudent  - ==> Parameters: 3(Integer)
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.deleteStudent  - <==    Updates: 1
    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Committing JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==>  Preparing: select student_id as id, student_name as name from student where student_name=? 
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==> Parameters: yuding(String)
    2019-03-03 12:08:042 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==    Columns: id, name
    2019-03-03 12:08:042 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==        Row: 1, yuding
    2019-03-03 12:08:042 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - <==      Total: 1
    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Resetting autocommit to true on JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Closing JDBC Connection [com.mysql.jdbc.JDBC4Connection@614df0a4]
    2019-03-03 12:08:042 [main      ]  DEBUG    o.a.i.d.p.PooledDataSource  - Returned connection 1632497828 to pool.

开启二级缓存：

    @Test
    public void testCache() {
        SqlSession sqlSession = SqlSessionUtil.getSession();
        try {
            IStudentDao dao = sqlSession.getMapper(IStudentDao.class);
            String name = "yuding";
            dao.getStudentInfoByName(name);
            //关闭sqlSession，随后新建一个，验证二级缓存跨sqlSession
            sqlSession.close();
            sqlSession = SqlSessionUtil.getSession();
            dao = sqlSession.getMapper(IStudentDao.class);
            dao.getStudentInfoByName(name);

        } finally {
            if(sqlSession != null) {
                sqlSession.close();
            }
        }
    }

结果会从二级缓存中拿值：

    2019-03-03 12:12:019 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Opening JDBC Connection
    2019-03-03 12:12:020 [main      ]  DEBUG    o.a.i.d.p.PooledDataSource  - Created connection 1671590089.
    2019-03-03 12:12:020 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Setting autocommit to false on JDBC Connection [com.mysql.jdbc.JDBC4Connection@63a270c9]
    2019-03-03 12:12:020 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==>  Preparing: select student_id as id, student_name as name from student where student_name=? 
    2019-03-03 12:12:020 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - ==> Parameters: yuding(String)
    2019-03-03 12:12:020 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==    Columns: id, name
    2019-03-03 12:12:020 [main      ]  TRACE    s.d.I.getStudentInfoByName  - <==        Row: 1, yuding
    2019-03-03 12:12:020 [main      ]  DEBUG    s.d.I.getStudentInfoByName  - <==      Total: 1
    2019-03-03 12:12:020 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Resetting autocommit to true on JDBC Connection [com.mysql.jdbc.JDBC4Connection@63a270c9]
    2019-03-03 12:12:020 [main      ]  DEBUG    o.a.i.t.j.JdbcTransaction  - Closing JDBC Connection [com.mysql.jdbc.JDBC4Connection@63a270c9]
    2019-03-03 12:12:020 [main      ]  DEBUG    o.a.i.d.p.PooledDataSource  - Returned connection 1671590089 to pool.
    2019-03-03 12:12:021 [main      ]  DEBUG    s.dao.IStudentDao  - Cache Hit Ratio [spring.dao.IStudentDao]: 0.5

**TransactionalCache**

先看看`CachingExecutor`的`query`方法：

    public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
        Cache cache = ms.getCache();
        if (cache != null) {
          flushCacheIfRequired(ms);
          if (ms.isUseCache() && resultHandler == null) {
            ensureNoOutParams(ms, parameterObject, boundSql);
            @SuppressWarnings("unchecked")
            List<E> list = (List<E>) tcm.getObject(cache, key);
            if (list == null) {
              list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
              tcm.putObject(cache, key, list); // issue #578. Query must be not synchronized to prevent deadlocks
            }
            return list;
          }
        }
        return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
      }

这里的`delegate`实际上就是`BaseExecutor`实例，`CachingExecutor`在数据库操作时实际上将操作代理给了`BaseExecutor`，这里的`tcm`实际上是`TransactionalCacheManager`实例，其内部维护了一个`Map`：

    private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();
    
这个`Map`是`Cache`实例到`TransationalCache`的映射，`Cache`就是缓存实例， 存储了缓存`key`到查询结果的映射，下面重要解析`TransactionalCache`：
    
    //实现了Cache接口
    public class TransactionalCache implements Cache {
      
      //将操作代理给delegate
      private Cache delegate;
      //标志位，提交时是否清空delegate，这里的提交是指提交给delegate
      private boolean clearOnCommit;
      //待新增数据，key是缓存key，value是AddEntry实例
      private Map<Object, AddEntry> entriesToAddOnCommit;
      //待删除数据，key是缓存key，value是RemoveEntry实例
      private Map<Object, RemoveEntry> entriesToRemoveOnCommit;
    
      public TransactionalCache(Cache delegate) {
        this.delegate = delegate;
        this.clearOnCommit = false;
        this.entriesToAddOnCommit = new HashMap<Object, AddEntry>();
        this.entriesToRemoveOnCommit = new HashMap<Object, RemoveEntry>();
      }
      
      //所有的操作都是代理给delegate来操作
      @Override
      public String getId() {
        return delegate.getId();
      }
    
      @Override
      public int getSize() {
        return delegate.getSize();
      }
    
      @Override
      public Object getObject(Object key) {
        if (clearOnCommit) return null; // issue #146
        return delegate.getObject(key);
      }
    
      @Override
      public ReadWriteLock getReadWriteLock() {
        return null;
      }
      
      //往缓存中新增内容，从remove entries中删除key对应的内容，在add entries中加入key对应的内容，等待commit
      @Override
      public void putObject(Object key, Object object) {
        entriesToRemoveOnCommit.remove(key);
        entriesToAddOnCommit.put(key, new AddEntry(delegate, key, object));
      }
      
      //同上
      @Override
      public Object removeObject(Object key) {
        entriesToAddOnCommit.remove(key);
        entriesToRemoveOnCommit.put(key, new RemoveEntry(delegate, key));
        return delegate.getObject(key);
      }
      
      //清空缓存，这里的reset方法实际上将add entries和remove entries清空，然后将clearOnCommit标志位设为true
      @Override
      public void clear() {
        reset();
        clearOnCommit = true;
      }
      
      //提交，这是核心方法，如果clearOnCommit是true，那么清空delegate；否则对add entries和remove entries提交，实际上是将对应的内容从delegate中新增或者删除
      public void commit() {
        if (clearOnCommit) {
          delegate.clear();
        } else {
          for (RemoveEntry entry : entriesToRemoveOnCommit.values()) {
            entry.commit();
          }
        }
        for (AddEntry entry : entriesToAddOnCommit.values()) {
          entry.commit();
        }
        //最后reset，清空add entries和remove entries
        reset();
      }
      
      //回滚，直接调用reset
      public void rollback() {
        reset();
      }
      
      //清空add entries和remove entries
      private void reset() {
        clearOnCommit = false;
        entriesToRemoveOnCommit.clear();
        entriesToAddOnCommit.clear();
      }
    
      private static class AddEntry {
        private Cache cache;
        private Object key;
        private Object value;
    
        public AddEntry(Cache cache, Object key, Object value) {
          this.cache = cache;
          this.key = key;
          this.value = value;
        }
        
        //AddEntry的提交方法， 将内容放入缓存中
        public void commit() {
          cache.putObject(key, value);
        }
      }
    
      private static class RemoveEntry {
        private Cache cache;
        private Object key;
    
        public RemoveEntry(Cache cache, Object key) {
          this.cache = cache;
          this.key = key;
        }
    
        public void commit() {
          cache.removeObject(key);
        }
      }
    
    }

`CachingExecutor`中的很多操作都是调用的`TransactionalCacheManager`，进而调用对应的`TransactionalCache`：

      public void commit(boolean required) throws SQLException {
        delegate.commit(required);
        tcm.commit();
      }
    
      public void rollback(boolean required) throws SQLException {
        try {
          delegate.rollback(required);
        } finally {
          if (required) {
            tcm.rollback();
          }
        }
      }
      
      //找到Cache对饮的TransactionalCache，调用其clear方法，清空add entries和remove entries
      private void flushCacheIfRequired(MappedStatement ms) {
        Cache cache = ms.getCache();
        if (cache != null && ms.isFlushCacheRequired()) {      
          tcm.clear(cache);
        }
      }
      
二级缓存是跨`SqlSession`的，`SqlSession.commit`方法相当于是将**数据提交到缓存**中（注意这里的底层缓存对象是`PerpetualCache`，不同的`SqlSession`共用一个相同的缓存实例，这也是二级缓存跨`SqlSession`的原因），如果没有清空缓存(`flushCache`为`false`)，那么两个不同的`SqlSession`进行同样的操作会从缓存中拿值，而不会查询数据库，举个例子：

    @Test
    public void testCache() {
        SqlSession sqlSession = SqlSessionUtil.getSession();
        try {
            IStudentDao dao = sqlSession.getMapper(IStudentDao.class);
            String name = "Deacon";
            dao.getStudentInfoByName(name);
            sqlSession.commit();    //将查询结果提交到缓存中
            sqlSession = SqlSessionUtil.getSession();
            dao = sqlSession.getMapper(IStudentDao.class);
            dao.getStudentInfoByName(name); //将会从缓存中取值

        } finally {
            if(sqlSession != null) {
                sqlSession.close();
            }
        }
    }

如果在`xml`中配置了`flushCache=true`(`flushCache=true`时会将一级缓存清空，将`clearOnCommit`标志位置为`true`，即在`commit`时会清空二级缓存)，那么在调用`commit`方法后会清空所有缓存，包括一级缓存和二级缓存，那么下次查询会走`db`：

    public void commit() {
        //clearOnCommit在flushCache时会置为true
        if (clearOnCommit) {
          //清空delegate缓存
          delegate.clear();
        } else {
          for (RemoveEntry entry : entriesToRemoveOnCommit.values()) {
            entry.commit();
          }
        }
        for (AddEntry entry : entriesToAddOnCommit.values()) {
          entry.commit();
        }
        reset();
    }

**小结：一级缓存在同一个SqlSession内执行相同的查询操作会走缓存，如果执行insert/update/delete/flushCache=true会清空一级缓存，走db；二级缓存是跨SqlSession的，多个SqlSession共用一个底层PerpetualCache，MyBatis提供了一个事务缓存类TransactionalCache，其内部提供了commit方法将数据库操作提交到底层缓存中，因此如果使用了二级缓存(在mapper.xml中声明了<cache>元素)，在两个SqlSession中进行两次同样的查询，且第一次查询后调用了sqlSession.commit提交(必须提交，否则缓存中没有记录)，那么第二次查询会走二级缓存，如果进行了insert/update/delete/flushCache=true，那么在commit时会清空二级缓存，下一次进行同样的查询会重新走db**

**Mapper配置文件<cache>元素解析**

`Mapper`配置文件中的`<cache>`元素解析在`XMLMapperBuilder`中完成，其源码如下：

    private void cacheElement(XNode context) throws Exception {
        if (context != null) {
          String type = context.getStringAttribute("type", "PERPETUAL");    //缓存类型，默认是PerpetualCache
          Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
          String eviction = context.getStringAttribute("eviction", "LRU");  //清除策略，默认是LRU，即最近少使用算法
          Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
          Long flushInterval = context.getLongAttribute("flushInterval");   //缓存刷新时间间隔
          Integer size = context.getIntAttribute("size");   //引用数目
          boolean readWrite = !context.getBooleanAttribute("readOnly", false);  //是否只读
          boolean blocking = context.getBooleanAttribute("blocking", false);
          Properties props = context.getChildrenAsProperties(); //获取<cache>的<property>子元素，设置name,value对修改缓存参数
          builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);   //构建缓存实例，传入props
        }
      }

`MapperBuilderAssistant.useNewCache`方法：

    public Cache useNewCache(Class<? extends Cache> typeClass,
      Class<? extends Cache> evictionClass,
      Long flushInterval,
      Integer size,
      boolean readWrite,
      boolean blocking,
      Properties props) {
        Cache cache = new CacheBuilder(currentNamespace)    //使用CacheBuilder构建缓存实例，currentNamespace即namespace属性
            .implementation(valueOrDefault(typeClass, PerpetualCache.class))
            .addDecorator(valueOrDefault(evictionClass, LruCache.class))
            .clearInterval(flushInterval)
            .size(size)
            .readWrite(readWrite)
            .blocking(blocking)
            .properties(props)
            .build();
        configuration.addCache(cache);  //添加到Configuration对象的缓存Map中
        currentCache = cache;   //设置为当前缓存
        return cache;
      }

`CacheBuilder.build`方法：

    public Cache build() {
        setDefaultImplementations();
        Cache cache = newBaseCacheInstance(implementation, id); //反射创建PerpetualCache实例
        setCacheProperties(cache);  //设置props属性，调用PerpetualCache的setter方法
        // issue #352, do not apply decorators to custom caches
        if (PerpetualCache.class.equals(cache.getClass())) {
          //设置装饰器
          for (Class<? extends Cache> decorator : decorators) {
            cache = newCacheDecoratorInstance(decorator, cache);
            setCacheProperties(cache);  //设置属性
          }
          cache = setStandardDecorators(cache); //再次封装cache
        } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
          cache = new LoggingCache(cache);
        }
        return cache;
      }

`setStandardDecorators`方法，再次封装`Cache`：

    private Cache setStandardDecorators(Cache cache) {
        try {
          MetaObject metaCache = SystemMetaObject.forObject(cache);
          if (size != null && metaCache.hasSetter("size")) {
            metaCache.setValue("size", size);
          }
          if (clearInterval != null) {
            cache = new ScheduledCache(cache);
            ((ScheduledCache) cache).setClearInterval(clearInterval);   //如果指定flushInterval，清理时间间隔，那么会再封装一层ScheduledCache，实现定时清理的功能
          }
          if (readWrite) {
            cache = new SerializedCache(cache); //如果readWrite属性为true(默认)，套一层SerializedCache，实现序列化功能
          }
          cache = new LoggingCache(cache);  //套一层LogginCache，打印缓存命中日志
          cache = new SynchronizedCache(cache); //套一层SynchronizedCache，实现同步
          if (blocking) {
            cache = new BlockingCache(cache);
          }
          return cache;
        } catch (Exception e) {
          throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
        }
      }

## 注册DAO接口bean ##
首先先看一下配置文件中的定义：

    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="com.fiberhome.jtis.server.jtis.dao"/>
        <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
    </bean>
    
这个`bean`的作用就是搜索指定的`basePackage`下的所有接口，并将它们注册为`MapperFactoryBean`。

接着看一下`MapperScannerConfigurer`的类图：

    public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware 

继承自`BeanDefinitionRegistryPostProcessor`

        public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
        //在注册完bean definition之后调用
    	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;
    }
    
看下具体的实现：

        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            if (this.processPropertyPlaceHolders) {
              processPropertyPlaceHolders();
            }
            //实例化ClassPathMapperScanner
            ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
            scanner.setAddToConfig(this.addToConfig);
            scanner.setAnnotationClass(this.annotationClass);
            scanner.setMarkerInterface(this.markerInterface);
            scanner.setSqlSessionFactory(this.sqlSessionFactory);
            scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
            scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
            scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
            scanner.setResourceLoader(this.applicationContext);
            scanner.setBeanNameGenerator(this.nameGenerator);
            scanner.registerFilters();
            //扫描basePackage下的所有接口，并将其注册到spring上下文中
            scanner.scan(StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
      }
      
`ClassPathBeanDefinitionScanner.scan`方法：

    public int scan(String... basePackages) {
		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();
        //调用doScan
		doScan(basePackages);

		// Register annotation config processors, if necessary.
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}
        //返回新增注册的bean数量
		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
	}
	
`ClassPathMapperScanner.doScan`方法：

    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        //(1)调用父类的doScan方法
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
    
        if (beanDefinitions.isEmpty()) {
          logger.warn("No MyBatis mapper was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
        } else {
          for (BeanDefinitionHolder holder : beanDefinitions) {
            GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
    
            if (logger.isDebugEnabled()) {
              logger.debug("Creating MapperFactoryBean with name '" + holder.getBeanName() 
                  + "' and '" + definition.getBeanClassName() + "' mapperInterface");
            }
    
            // the mapper interface is the original class of the bean
            // but, the actual class of the bean is MapperFactoryBean
            //Mapper接口是bean的原始类型，但是实际类型是MapperFactoryBean
            definition.getPropertyValues().add("mapperInterface", definition.getBeanClassName());
            definition.setBeanClass(MapperFactoryBean.class);
    
            definition.getPropertyValues().add("addToConfig", this.addToConfig);
    
            boolean explicitFactoryUsed = false;
            //如果设置了sqlSessionFactoryBeanName属性
            if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
              definition.getPropertyValues().add("sqlSessionFactory", new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
              explicitFactoryUsed = true;
            } else if (this.sqlSessionFactory != null) {
              definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
              explicitFactoryUsed = true;
            }
    
            if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
              if (explicitFactoryUsed) {
                logger.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
              }
              definition.getPropertyValues().add("sqlSessionTemplate", new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
              explicitFactoryUsed = true;
            } else if (this.sqlSessionTemplate != null) {
              if (explicitFactoryUsed) {
                logger.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
              }
              definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
              explicitFactoryUsed = true;
            }
    
            if (!explicitFactoryUsed) {
              if (logger.isDebugEnabled()) {
                logger.debug("Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
              }
              definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            }
          }
        }
    
        return beanDefinitions;
      }
      
(1)`ClassPathBeanDefinitionScanner.doScan`方法：

    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<BeanDefinitionHolder>();
		for (String basePackage : basePackages) {           
		    //获取basePacakge下所有的DAO接口，并将其转化成BeanDefinition
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
			for (BeanDefinition candidate : candidates) {
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				if (candidate instanceof AbstractBeanDefinition) {
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
				if (checkCandidate(beanName, candidate)) {
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}
	
最后看一下`MapperFactoryBean`的定义：

    public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T>

继承自`SqlSessionDaoSupport`类，该类需要主任`SqlSessionFactory`实例用来构造`SqlSession`，其内部获取`bean`对象的方法：

    public T getObject() throws Exception {
        return getSqlSession().getMapper(this.mapperInterface);
    }
    
