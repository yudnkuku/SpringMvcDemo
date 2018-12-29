# MyBatis学习笔记   

标签（空格分隔）： MyBatis

---
## 1 入门 ##
## 1.1 从XML中构建SqlSessionFactory ##
每个基于`MyBatis`的应用都是以一个`SqlSessionFactory`的实例为中心的，`SqlSessionFactory`的实例可以通过`SqlSessionFactoryBuilder`获得，而`SqlSessionFactoryBuilder`可以从`xml`配置文件或一个预先定制的`Configuration`的实例构建出`SqlSessionFactory`实例。
`MyBatis`提供了一个`Resources`的工具类，可以从`classpath`或者其他位置加载资源文件。

    String resource = "org/mybatis/example/mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        
    //常用配置文件
    <?xml version="1.0" encoding="UTF-8" ?>
    <!DOCTYPE configuration
      PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
      "http://mybatis.org/dtd/mybatis-3-config.dtd">
    <configuration>
      <environments default="development">
        <environment id="development">
          <transactionManager type="JDBC"/>
          <dataSource type="POOLED">
            <property name="driver" value="${driver}"/>
            <property name="url" value="${url}"/>
            <property name="username" value="${username}"/>
            <property name="password" value="${password}"/>
          </dataSource>
        </environment>
      </environments>
      <mappers>
        <mapper resource="org/mybatis/example/BlogMapper.xml"/>
      </mappers>
    </configuration>

**关于命名空间namespace**
`namespace`属性必填，并且需要和`mapper`接口的全类名一致。

**关于SqlSession**
每个线程都应该有它自己的`SqlSession`实例，`SqlSession`的实例不是线程安全的，因此是不能共享的，所以它的最佳作用域是请求或方法作用域，绝对不能将`SqlSession`实例引用放在一个类的静态域，甚至一个类的实例变量也不行，也绝不能将 `SqlSession` 实例的引用放在任何类型的管理作用域中，比如 `Servlet` 架构中的 `HttpSession`。如果你现在正在使用一种 `Web` 框架，要考虑 `SqlSession` 放在一个和 `HTTP` 请求对象相似的作用域中。换句话说，每次收到的 `HTTP` 请求，就可以打开一个 `SqlSession`，返回一个响应，就关闭它。这个关闭操作是很重要的，你应该把这个关闭操作放到 `finally` 块中以确保每次都能执行关闭。下面的示例就是一个确保 `SqlSession` 关闭的标准模式：

    SqlSession session = sqlSessionFactory.openSession();
    try {
        BlogMapper mapper = session.getMapper(BlogMapper.class);
      // do work
    } finally {
      session.close();
    }

## 2 XML映射配置文件 ##
## 2.1 properties ##
这些属性都是可外部配置且可动态替换的，既可以在典型的Java属性文件中配置，也可以通过`properties`元素的子元素来传递，例如：

    <properties resource="org/mybatis/example/config.properties">
      <property name="username" value="dev_user"/>
      <property name="password" value="F2Fa3!33TYyg"/>
    </properties>
然后上述属性可以在整个配置文件中被用来替换需要动态配置的属性值

    <dataSource type="POOLED">
      <property name="driver" value="${driver}"/>
  <property name="url" value="${url}"/>
      <property name="username" value="${username}"/>
  <property name="password" value="${password}"/>
    </dataSource>
`username`和`password`由`properties`元素中相应值替换，而`driver`和`url`将会由`config.properties`文件中对应的值来替换
如果属性值不只在一个地方进行了配置，`MyBatis`将按照下面的顺序来加载：

 - 在`properties`元素体内指定的属性首先被读取
 - 然后根据`properties`元素中的`resource`属性读取类路径下属性文件或根据`url`属性指定的路径读取属性文件，并覆盖已读取的同名属性
 - 最后读取作为方法参数传递的属性，并覆盖已读取的同名属性

## 2.2 setting ##
`MyBatis`参数设置，调整`MyBatis`的运行时行为，参考：[MyBatis设置][1]
常用`setting`配置：

|设置参数|描述|有效值|默认值|
|:-:|:-:|:-:|:-:|
|`cacheEnabled`|是否启用二级缓存，包括一级缓存和二级缓存|`true/false`|`true`|
|||||
## 2.3 typeAliases ##
类型别名是为`Java`类型设置一个短的名字，它只和`XML`配置有关，存在的意义仅在于减少类完全限定名的冗余

    <typeAliases>
     <typeAlias alias="Blog" type="domain.blog.Blog"/>
    </typeAliases>
当这样配置时，`Blog`可以用在任何使用`domain.blog.Blog`的地方，`JavaBean`的默认别名是其首字母小写非限定类名，例如`domain.blog.Blog`的别名默认为`blog`,即小写的类名。
还有一些常见的`Java`类型內建类型别名，参考[常见Java类型內建别名][2]

## 2.4 typeHandlers ##
无论`MyBatis`在预处理语句(`PreparedStatement`)中设置一个参数时，还是从结果集中取出一个值时，都会用类型处理器将获取的的值以合适的方式转换成`java`类型，下表描述了一些默认的类型处理器：

|类型处理器|`java`类型|`jdbc`类型|
|:-:|:-:|:-:|
|`BooleanTypeHandler`|`java.lang.Boolean`,`boolean`|数据库兼容的`BOOLEAN`|
|`StringTypeHandler`|`java.lang.String`|`CHAR`/`VARCHAR`|
|`DateTypeHandler`|`java.util.Date`|`TIMESTAMP`|
|`DateOnlyTypeHandler`|`java.util.Date`|`DATE`|
|`TimeOnlyTypeHandler`|`java.util.Date`|`TIME`|
|`EnumTypeHandler`|`Enumeration Type`|`VARCHAR`任何兼容的字符串类型，存储枚举的名称(而不是索引)|
|`EnumOrdinalTypeHandler`|`Enumeration Type`|任何兼容的`NUMERIC`或`DOUBLE`类型，存储枚举的索引|

修改类型处理器处理的`JAVA`类型：

 - 在类型处理器的配置元素(`typeHandler element`)上增加一个`javaType`属性(比如`javaType="String"`)
 - 在类型处理器的类上(`TypeHandler class`)增加一个`@MappedTypes`注解来指定与其关联的`java`类型列表，如果在`javaType`属性中也同时指定，则注解方式将被忽略

可以通过两种方式指定被关联的`jdbc`类型：

 - 在类型处理器的配置元素上增加一个`jdbcType`属性(比如：`jdbcType="VARCHAR"`)
 - 在类型处理器的配置元素上(`TypeHandler class`)增加一个`@MappedJdbcTypes`注解来指定与其关联的`JDBC`类型列表，如果在`jdbcType`属性中也指定，则注解方式将被忽略

**EnumTypeHandler和EnumOrdinalTypeHandler**
默认情况下，`MyBatis`会利用`EnumTypeHandler`来把`Enum`值转换成对应的名字(字符串类型)，不过我们可能不想存储名字，相反我们的`DBA`会坚持使用整形值代码，那也一样轻而易举：在配置文件中把`EnumOrdinalTypeHandler`加到`typeHandlers`中即可，这样每个`RoundingMode`将通过它们的序数值来映射成相应的整形。

    <!-- mybatis-config.xml -->
    <typeHandlers>
      <typeHandler handler="org.apache.ibatis.type.EnumOrdinalTypeHandler" javaType="java.math.RoundingMode"/>
    </typeHandlers>

具体可见`MyBatis`源码解析笔记中的`EnumTypeHandler`源码解析
 
## 3 XML映射文件 ##
`The Mapper XML`文件减少大量的`JDBC`代码，包括以下类元素(`class elements`)

 - `cache`-指定命名空间的缓存配置
 - `cache-ref`-指向另一命名空间的缓存配置
 - `resultMap`-描述如何从数据库结果集中加载对象
 - `sql`-可复用(可被其他语句引用)的`SQL`块
 - `insert`-映射`INSERT`语句
 - `update`-映射`UPDATE`语句
 - `delete`-映射`DELETE`语句
 - `select`-映射`SELECT`语句
 

3.1 select
----------

    <select id="selectPerson" parameterType="int" resultType="hashmap">
        SELECT * FROM PERSON WHERE ID = #{id}
    </select>
以上配置合如下`JDBC`代码实现相似的功能，`#{id}`表示查询参数，替代SQL语句中的"?"：

    // Similar JDBC code, NOT MyBatis…
    String selectPerson = "SELECT * FROM PERSON WHERE ID=?";
    PreparedStatement ps = conn.prepareStatement(selectPerson);
    ps.setInt(1,id);
除此之外，`MyBatis`还做了很多工作，例如展开查询结果以及提供其余`JavaBean`之间的映射等等。

3.2 insert/update/delete
--------------------

一些例子：

    <insert id="insertAuthor">
      insert into Author (id,username,password,email,bio)
        values (#{id},#{username},#{password},#{email},#{bio})
    </insert>
    
    <update id="updateAuthor">
      update Author set
        username = #{username},
        password = #{password},
        email = #{email},
        bio = #{bio}
      where id = #{id}
    </update>
    
    <delete id="deleteAuthor">
      delete from Author where id = #{id}
    </delete>

对于能自动生成键值的数据库(`MySQL`和`SQL Server`)，可以直接设置`userGeneratedKeys="true"`和`keyProperty`,传入的`java`对象对应的属性值会被设置成自增值，`keyProperty`填写`java`对象中对应的属性名称，在执行完`sql`语句后，该属性会被复制自增值，如：
    
    //执行完insert之后，传入的Author对象的id属性会被赋值自增的列值
    <insert id="insertAuthor" useGeneratedKeys="true"
    keyProperty="id">
    insert into Author (username,password,email,bio)
      values (#{username},#{password},#{email},#{bio})
    </insert>
    
如果支持多行插入：

    <insert id="insertAuthor" useGeneratedKeys="true"
    keyProperty="id">
    insert into Author (username, password, email, bio) values
      <foreach item="item" collection="list" separator=",">
        (#{item.username}, #{item.password}, #{item.email}, #{item.bio})
      </foreach>
    </insert>
    

3.3 sql
---

sql元素可以作为可重用的SQL代码片段，包含在其他语句中

    <sql id="userColumns"> ${alias}.id,${alias}.username,${alias}.password </sql>
引用上述sql代码片段的语句(通过id引用，将<include>元素内容替换为定义的sql代码片段)：

    <select id="selectUsers" resultType="map">
      select
        <include refid="userColumns"><property name="alias" value="t1"/></include>,
        <include refid="userColumns"><property name="alias" value="t2"/></include>
      from some_table t1
        cross join some_table t2
    </select>
    

3.4 Result Maps
-----------

提供数据库表格列和JavaBean属性之间基于名称的映射，假如我们定义了一个JavaBean:

    package com.somapp.model;
    public class User{
        private int id;
        private String userName;
        private String hashedPassword;
        
        //省略getter/setter
    }
这样的JavaBean可以直接映射到结果集

    <select id="selectUsers" resultType="com.someapp.model.User">
    select id, userName, hashedPassword
      from some_table
      where id = #{id}
    </select>
如果列名称和JavaBean属性名称不一致，可以通过以下方式指定映射

    <select id="selectUsers" resultType="User">
      select
        user_id             as "id",
        user_name           as "userName",
        hashed_password     as "hashedPassword"
      from some_table
      where id = #{id}
    </select>
也可以在`resultMap`元素中解决列名称不匹配问题

    <resultMap id="userResultMap" type="User">
      <id property="id" column="user_id" />
      <result property="username" column="user_name"/>
      <result property="password" column="hashed_password"/>
    </resultMap>
在`select`元素中引入`resultMap`属性：

    <select id="selectUsers" resultMap="userResultMap">
      select user_id, user_name, hashed_password
      from some_table
      where id = #{id}
    </select>
    
**高级结果映射**
**一对一关联**
关联元素处理“有一个”类型的关系，比如，一个博客有一个作者用户，分为两种类型：

 - 嵌套查询：通过执行另外一个`SQL`语句映射语句来返回预期的复杂类型
 - 嵌套结果：使用嵌套结果映射来处理重复的联合结果的子集。

关联嵌套查询示例：

    <resultMap id="blogResult" type="Blog">
      <association property="author" column="author_id" javaType="Author" select="selectAuthor"/>
    </resultMap>
    
    <select id="selectBlog" resultMap="blogResult">
      SELECT * FROM BLOG WHERE ID = #{id}
    </select>
    
    <select id="selectAuthor" resultType="Author">
      SELECT * FROM AUTHOR WHERE ID = #{id}
    </select>

我们有两个查询语句，一个用来加载博客，另一个用来加载作者，而且博客的结果映射描述了`selectAuthor`语句应该被用来加载它的`author`属性，其他的属性将会被自动加载，假设它们的列名和属性名相匹配

这种方式很简单，但是对于大型数据集合和列表将不会表现很好，问题就是我们熟知的`N+1`查询问题，概括地讲，`N+1`问题是这样引起的：

 - 执行了一个单独的`SQL`语句来获取结果列表(就是“+1”)
 - 对返回的每条记录，执行了一个查询语句来为每个加载细节(就是"N")

这通常会导致成百上千的`SQL`语句被执行，这通常不是所期望的。

所以引出了关联的嵌套结果，用`resultMap`属性替代`select`(**就要修改`sql`语句，使用连接查询语句**)，示例如下：

    <select id="selectBlog" resultMap="blogResult">
      select
        B.id            as blog_id,
        B.title         as blog_title,
        B.author_id     as blog_author_id,
        A.id            as author_id,
        A.username      as author_username,
        A.password      as author_password,
        A.email         as author_email,
        A.bio           as author_bio
      from Blog B left outer join Author A on B.author_id = A.id
      where B.id = #{id}
    </select>
    
`resultMap`映射如下：
    
    //blogResult结果集映射一对一关联authorResult结果集映射
    <resultMap id="blogResult" type="Blog">
      <id property="id" column="blog_id" />
      <result property="title" column="blog_title"/>
      <association property="author" column="blog_author_id" javaType="Author" resultMap="authorResult"/>
    </resultMap>
    
    <resultMap id="authorResult" type="Author">
      <id property="id" column="author_id"/>
      <result property="username" column="author_username"/>
      <result property="password" column="author_password"/>
      <result property="email" column="author_email"/>
      <result property="bio" column="author_bio"/>
    </resultMap>

当然可以将上述两个`resultMap`整合到一个`resultMap`中

    <resultMap id="blogResult" type="Blog">
      <id property="id" column="blog_id" />
      <result property="title" column="blog_title"/>
      <association property="author" javaType="Author">
        <id property="id" column="author_id"/>
        <result property="username" column="author_username"/>
        <result property="password" column="author_password"/>
        <result property="email" column="author_email"/>
        <result property="bio" column="author_bio"/>
      </association>
    </resultMap>
    
但是假如`blog`中还有一个`co-author`就会很麻烦，这时可以使用`columnPrefix`属性：
    
    //select语句
    <select id="selectBlog" resultMap="blogResult">
      select
        B.id            as blog_id,
        B.title         as blog_title,
        A.id            as author_id,
        A.username      as author_username,
        A.password      as author_password,
        A.email         as author_email,
        A.bio           as author_bio,
        CA.id           as co_author_id,
        CA.username     as co_author_username,
        CA.password     as co_author_password,
        CA.email        as co_author_email,
        CA.bio          as co_author_bio
      from Blog B
      left outer join Author A on B.author_id = A.id
      left outer join Author CA on B.co_author_id = CA.id
      where B.id = #{id}
    </select>

    <resultMap id="blogResult" type="Blog">
      <id property="id" column="blog_id" />
      <result property="title" column="blog_title"/>
      <association property="author"
        resultMap="authorResult" />
      <association property="coAuthor"
        resultMap="authorResult"
        columnPrefix="co_" />
    </resultMap>

**集合**
集合描述一对多的关系，例如一个博客有很多文章，同样结果集映射可以用集合的嵌套查询和嵌套结果。

    <resultMap id="blogResult" type="Blog">
      <collection property="posts" javaType="ArrayList" column="id" ofType="Post" select="selectPostsForBlog"/>
    </resultMap>
    
    <select id="selectBlog" resultMap="blogResult">
      SELECT * FROM BLOG WHERE ID = #{id}
    </select>
    
    <select id="selectPostsForBlog" resultType="Post">
      SELECT * FROM POST WHERE BLOG_ID = #{id}
    </select>

4 动态SQL(Dynamic SQL)
------------------

 - `if`
 - `choose(when,otherwise)`
 - `trim(where,set)`
 - `foreach`
 

4.1 if
--

`example`:

    <select id="findActiveBlogLike"
         resultType="Blog">
      SELECT * FROM BLOG WHERE state = ‘ACTIVE’
      <if test="title != null">
        AND title like #{title}
      </if>
      <if test="author != null and author.name != null">
        AND author_name like #{author.name}
      </if>
    </select>
    

4.2 choose,when,otherwise
---------------------------

类似于`switch`,`case`

    <select id="findActiveBlogLike"
         resultType="Blog">
      SELECT * FROM BLOG WHERE state = ‘ACTIVE’
      <choose>
        <when test="title != null">
          AND title like #{title}
        </when>
        <when test="author != null and author.name != null">
          AND author_name like #{author.name}
        </when>
        <otherwise>
          AND featured = 1
        </otherwise>
      </choose>
    </select>
    

4.3 trim,where,set
--------------------


**`<where>`标签：**

    <select id="findActiveBlogLike"
         resultType="Blog">
      SELECT * FROM BLOG
      <where>
        <if test="state != null">
             state = #{state}
        </if>
        <if test="title != null">
            AND title like #{title}
        </if>
        <if test="author != null and author.name != null">
            AND author_name like #{author.name}
        </if>
      </where>
    </select>

`where`标签只有在其包含的标签返回内容时才会插入`WHERE`，另外如果其内容以`AND`或者`OR`开始，会自动将其去除
`where`标签等价如下`trim`标签，注意`AND`后面的空格是必须的：

    <trim prefix="WHERE" prefixOverrides="AND |OR ">
      ...
    </trim>

**`<set>`标签：**

    <update id="updateAuthorIfNecessary">
      update Author
        <set>
          <if test="username != null">username=#{username},</if>
          <if test="password != null">password=#{password},</if>
          <if test="email != null">email=#{email},</if>
          <if test="bio != null">bio=#{bio}</if>
        </set>
      where id=#{id}
    </update>
`<set>`标签会动态添加`SET`关键字，并去除末尾的逗号，等价于：

    <trim prefix="SET" suffixOverrides=",">
      ...
    </trim>

4.4 foreach
-------------

遍历集合，通常用于`IN`条件

    <select id="selectPostIn" resultType="domain.blog.Post">
      SELECT *
      FROM POST P
      WHERE ID in
      <foreach item="item" index="index" collection="list"
          open="(" separator="," close=")">
            #{item}
      </foreach>
    </select>

`foreach`元素的功能非常强大，它允许你指定一个集合，声明可以在元素体内使用的集合项(`item`)和索引(`index`)变量，它也允许你指定开头与结尾的字符串
以及在迭代结果之间放置分隔符。这个元素是很智能的，因此它不会偶然地附加多余的分隔符。
你可以将任何可迭代的对象(如`List/Set`等)、`Map`对象或者数组对象传递给`foreach`作为集合参数。当使用迭代对象或者数组时，`index`是当前迭代的次数，
`item`的值是本次迭代获取的元素。当使用`Map`对象(或者`Map.Entry`对象的集合)时，`index`是键，`item`是值

## 4.5 缓存 ##
`MyBatis`中的缓存分为一级缓存(本地缓存)和二级缓存，一级缓存是在`SqlSession`层面进行缓存的，即同一个`SqlSession`，多次调用同一个`Mapper`的同一个方法同一个参数只会进行一次数据库查询，第一次查询会将结果缓存到本地缓存`localcache`中，以后如果做同样的查询那么直接从缓存中拿结果，而不去查数据库。
设置二级缓存的方法也很简单：

    <cache />

声明`<cache>`元素后起到的效果如下：
1、映射语句文件中的所有`select`语句将会被缓存
2、映射语句文件中的所有`insert/update/delete`语句会刷新缓存
3、缓存会使用`Least Recently Used`(`LRU`最近最少使用的)算法来回收
4、根据时间表(比如`no Flush Interval`没有刷新间隔)，缓存不会以任何时间顺序来刷新
5、缓存会存储列表集合或者对象(无论查询方法返回什么)的1024个引用
6、缓存会被视为`read/write`的缓存，意味着对象检索不是共享的，而且可以安全地被调用者修改，而不干扰其他调用者或者线程所做的潜在修改。

只需要在`Mapper`定义中声明`cache`元素，它会自动使用默认值构建`Cache`：
    
    //解析cache元素节点
    private void cacheElement(XNode context) throws Exception {
        if (context != null) {
          String type = context.getStringAttribute("type", "PERPETUAL");    //获取type属性，默认PERPETUAL
          Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
          String eviction = context.getStringAttribute("eviction", "LRU");  //获取eviction属性，默认LRU，Least Recently Used，缓存回收算法
          Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
          Long flushInterval = context.getLongAttribute("flushInterval");   //flushInterval属性，刷新间隔，没有设置就是null
          Integer size = context.getIntAttribute("size");
          boolean readWrite = !context.getBooleanAttribute("readOnly", false);
          boolean blocking = context.getBooleanAttribute("blocking", false);
          Properties props = context.getChildrenAsProperties();
          builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
        }
    }

另外在配置`select/update/insert/delete`节点时，可以配置`userCache`和`flushCache`属性，这些属性也是缓存相关的属性，`userCache`表示是否使用二级缓存，`flushCache`表示是否清空缓存。

    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect); //如果是select，那么默认是不刷新缓存
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);    //如果是select，默认是使用二级缓存


## 5. 项目中实际应用 ##
项目中使用`MyBatis`首先要进行相关`bean`的配置，通常按顺序进行如下`bean`配置：

 - 数据源`DataSource`:配置数据源，属性字段包括`username`、`password`、`url`、`driverClassName`等
 - `SqlSessionFactoryBean`:提供`SqlSessionFactory`,属性字段包括`datasource`(即上面的数据源)、`configLocation配置文件位置，在配置文件中定义mapper.xml的路径，这些xml文件路径也可以在SqlSessionFactoryBean中配置mapperLocaotions字段`
 - `MapperScannerConfigurer`:配置属性包括`basePackage`(`dao`接口所在全路径)、`sqlSessionFactory`(上述`SqlSessionFactoryBean`)
 - `DataSourceTransactionManager`:事务管理`bean`，配置属性包括`datasource`(即上述数据源`bean`)

`MyBatis`在集成过程中只写了`DAO`层接口(加上接口对应的)，并没有写接口实现类，却可以在业务代码中通过`@Autowired`注入，其中原理参考[mapper接口代理][3]

接下来简要介绍`mapper`接口对应的配置文件规范。
在文件开头引入：

    <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"        
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
之后根节点写：

    <mapper namespace="这里填DAO接口名">
        <填写各种sql标签,例如resultMap/select/insert/update等>
    </mapper>
参考：[Spring MVC集成MyBatis][4]

## 其他一些注意事项 ##
1、为什么要设置`jdbcType`
官方文档有这样一句话，`The JDBC Type is required by JDBC for all nullable columns, if null is passed as a value. You can investigate this yourself by reading the JavaDocs for the PreparedStatement.setNull() method.`,对于可能出现的`null`值，在插入的时候如果没有指定`jdbcType`，`MyBatis`会默认加上`jdbcType.OTHER`，并报错，此时加上`jdbcType`即可，参考[Mybatis插入null值加jdbcType][5]，另可参考`javaType`和`jdbcType`的[映射表][6]

2、${}和#{}的区别
一般的，通过`#{}`传参时，`sql`解析会自动套上单引号，比如：

    select * from user where name = #{name}
如果`name`传入`yuding`，则对应的`sql`语句为：

    select * from user where name = 'yuding'
会当成字符串来解析，能够防止`sql`注入，如果传入的参数是单引号，那么使用`${}`会报错

在排序`order by`或者分组`group by`或者插入固定表及字段时，可以考虑使用`${}`，比如：

    select * from user order by ${param}
当入参是`age`时，`sql`语句为：

    select * from user order by age
**实际应用**

    <select id="selectUserInfoByMixed" parameterType="map" resultType="com.cckj.bean.UserInfo">
        select * from userinfo where ${param} = #{value}
    </select>

`DAO`接口定义：

    List<UserInfo> selectUserInfoByMixed(HashMap map)

`controller`层调用

        @RequestMapping(value = "/getUserInfoByMixed", produces = "application/json;charset=utf-8")
    public Map<String,Object> getUserInfoByMixed(HttpServletResponse response,String param,String value){
        response.setHeader("Access-Control-Allow-Origin","*");
        HashMap paramMap = new HashMap();
        paramMap.put("param",param);
        paramMap.put("value",value);
        List<UserInfo> userList = userInfoService.selectUserInfoByMixed(paramMap);
        Map<String,Object> map = new HashMap<>();
        map.put("userlist",userList);
        map.put("status",1);
        return map;
    }
加入`param`和`value`分别传入`username`和`yd`，那么`sql`语句等价于

    select * from userinfo where username = 'yd'


3、关于`mybatis`配置文件

 - `mybatis-config.xml`：`mybatis`配置文件，里面常见配置如下：


    <configuration>
        <settings>
            <setting name="logimpl" value="STD_LOGGING"/> //控制台输出sql语句
        <typeAliases>
            <package name="..."/>   //定义包下所有类等价于其小写类名

 - `applicationContext-mybatis.xml`：`spring-mybatis`配置文件，通常定义了`datasource`、`sqlSessionFactoryBean`、`MapperScannerConfigurer`以及`TranscationManager`
 - 其他组织结构：`DAO接口`、`mapper`映射`xml`文件，注意可以在`MapperScannerConfigurer`里设置`mapperLocations`，`DAO`接口和`mapper xml`通过`mapper`里的`namespace`属性关联


## MyBatis面试相关 ##
1、`MyBatis`是什么
`mybatis`是一个优秀的持久层框架，它对`jdbc`操作数据库的过程进行了封装，使开发者只用关注`sql`语句本身，不用去关注例如注册驱动，加载连接，得到`statement`，处理结果集等复杂的过程。 
`mybatis`通过`xml`或者注解的方式，将要执行的各种`sql`语句配置起来，并通过`Java`对象和`statement`中的`sql`语句映射生成最终的`sql`语句，最后由`mybatis`框架执行`sql`语句，并将结果映射成`Java`对象返回

2、`MyBatis`工作原理
`mybatis`通过配置文件创建`sqlsessionFactory`，`sqlsessionFactory`根据配置文件，配置文件来源于两个方面:一个是`xml`，一个是`Java`中的注解，获取`sqlSession`。**`SQLSession`包含了执行`sql`语句的所有方法**，可以通过`SQLSession`直接运行映射的`sql`语句，完成对数据的增删改查和事务的提交工作，用完之后关闭`SQLSession`。 
`MyBatis`是基于`jdk`动态代理的(基于接口，接口就是`DAO`接口)，因此在执行`DAO`接口的方法时，会触发代理类`MapperProxy`的方法调用(该代理类实现了`InvocationHandler`接口，这个接口在`jdk`动态代理中常用，`Proxy.newProxtInstance(classLoader, Class[]{}, InvocationHandler)`用于构造代理对象)，由于动态代理，会继续调用`InvocationHandler`实例的`invoke()`方法，查看源码，`invoke()`方法内部实际上调用了`sqlSession`实例的方法(看来最终还是回到了`sql`语句的执行)，执行对应的`sql`语句，因此说白了就是调用`DAO`方法最后还是执行了对应的`sql`语句(具体源码可以查看`MapperProxy`，参考`mybatis`源码解析)，可以看出`MyBatis`框架同样只声明了`DAO`接口，没有具体的实现类，这和`Spring Data JPA`有点类似，在项目实际运行时，`MyBatis`框架实际上利用了`JDK`动态代理策略将`DAO`接口的方法调用交给代理类执行，最后落脚到`sql`语句的执行，实现数据库相关的操作。

3、工作流程
`mapper`接口：
接口的全类名是`xml`文件中`namespace`属性的值

 - 接口中的方法名是`xml`文件中`MappedStatement`的`id`值
 - 接口中方法的参数就是传递给`sql`的参数
 - `mapper`接口是没有实现类的，当调用一个方法时，接口的全类名定位一个配置文件，接口的方法名定位这个配置文件中的一个`MappedStatement`，所以说`mapper`的方法名是不能重载的，因为`MappedStatement`的保存和寻找策略
 - `mapper`接口的工作原理是，`MyBatis`会使用`jdk`动态代理(基于接口)方式为`mapper`接口创建`proxy`对象，代理对象会拦截接口中的方法，转而执行`MappedStatement`所代表的`sql`语句，然后将执行的结果封装返回

4、`MyBatis`要解决的问题

 - 使用数据库连接池管理连接，避免了频繁创建、关闭连接，浪费资源、影响性能的问题
 - 用`xml`管理`sql`语句，让`java`代码和`sql`语句分离，使代码更易于维护
 - 解决了`sql`语句参数不定的问题，`xml`中可以通过`where`条件决定`sql`语句的条件参数，`MyBatis`将`Java`对象映射到`sql`语句，通过`statement`的`parameterType`定义输入参数的类型
 - `MyBatis`自动将结果集封装成`Java`对象，通过`statement`的`resultType`定义输出的类型。避免了因`sql`变化，对结果集处理麻烦的问题

5、`#{}`和`${}`的区别是什么
`#{}`是预编译处理，`${}`是字符串替换，`MyBatis`在处理`#{}`时，会自动插入单引号，而在处理`${}`时，不会插入单引号，使用`#{}`可以有效防止`SQL`注入，提高系统安全性

6、当实体类中属性名和表中字段名称不一致怎么处理
方法一：通过在查询的`sql`语句中定义字段名的别名，让字段名别名和实体类的属性名一致

    <select id=”selectorder” parametertype=”int” resultetype=”me.gacl.domain.order”> 
       select order_id id, order_no orderno ,order_price price form orders where order_id=#{id}; 
    </select> 
    
方法二：通过`<resultMap>`标签来映射字段名和实体属性名

        <select id="getOrder" parameterType="int" resultMap="orderresultmap">
            select * from orders where order_id=#{id}
        </select>
       <resultMap type=”me.gacl.domain.order” id=”orderresultmap”> 
            <!–用id属性来映射主键字段–> 
            <id property=”id” column=”order_id”> 
            <!–用result属性来映射非主键字段，property为实体类属性名，column为数据表中的属性–> 
            <result property = “orderno” column =”order_no”/> 
            <result property=”price” column=”order_price” /> 
        </reslutMap>

7、模糊查询`like`语句怎么写
方法一：在`java`代码中添加`sql`通配符

    string wildcardname = “%smi%”; 
    list<name> names = mapper.selectlike(wildcardname);

    <select id=”selectlike”> 
        select * from foo where bar like #{value} 
    </select>

方法二：在`sql`语句中拼接通配符，会引起`sql`注入

    string wildcardname = “smi”; 
    list<name> names = mapper.selectlike(wildcardname);

    <select id=”selectlike”> 
        select * from foo where bar like "%"#{value}"%"
    </select>
    
8、通常一个`Xml`映射文件，都会写一个`Dao`接口与之对应，请问，这个`Dao`接口的工作原理是什么？`Dao`接口里的方法，参数不同时，方法能重载吗？

`Dao`接口，就是人们常说的`Mapper`接口，接口的全限名，就是映射文件中的`namespace`的值，接口的方法名，就是映射文件中`MappedStatement`的`id`值，接口方法内的参数，就是传递给`sql`的参数。`Mapper`接口是没有实现类的，当调用接口方法时，接口全限名+方法名拼接字符串作为`key`值，可唯一定位一个`MappedStatement`，举例：`com.mybatis3.mappers.StudentDao.findStudentById`，可以唯一找到`namespace`为`com.mybatis3.mappers.StudentDao`下面`id = findStudentById`的`MappedStatement`。在`Mybatis`中，每一个`<select>、<insert>、<update>、<delete>`标签，都会被解析为一个`MappedStatement`对象。

`Dao`接口里的方法，是不能重载的，因为是全限名+方法名的保存和寻找策略。

`Dao`接口的工作原理是`JDK`动态代理(基于接口)，`Mybatis`运行时会使用`JDK`动态代理为`Dao`接口生成代理`proxy`对象(`MapperProxyFactory`用于构造`DAO`接口代理类的工厂类，`MapperProxy`实现了`InvocationHandler`接口)，代理对象`proxy`会拦截接口方法，转而执行`MappedStatement`所代表的`sql`，然后将`sql`执行结果返回。
`MapperProxyFactory`源码：

        public class MapperProxyFactory<T> {
      //代理的接口
      private final Class<T> mapperInterface;
      //用于缓存接口中的Method对象和MapperMethod的映射，如果重复调用接口中某方法，可以直接从此缓存中拿到对应的MapperMethod对象，执行相应的sql语句
      private Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();
    
      public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
      }
    
      public Class<T> getMapperInterface() {
        return mapperInterface;
      }
    
      public Map<Method, MapperMethod> getMethodCache() {
        return methodCache;
      }
    
      @SuppressWarnings("unchecked")
      //通过jdk动态代理构造代理对象，代理对象实现了mapperInterface接口
      protected T newInstance(MapperProxy<T> mapperProxy) {
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
      }
      
      //传入sqlSession构造MapperProxy
      public T newInstance(SqlSession sqlSession) {
        final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
        return newInstance(mapperProxy);
      }
    
    }
    
`MapperProxy`源码：
    
        public class MapperProxy<T> implements InvocationHandler, Serializable {
    
      private static final long serialVersionUID = -6424540398559729838L;
      private final SqlSession sqlSession;
      private final Class<T> mapperInterface;
      private final Map<Method, MapperMethod> methodCache;
    
      public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
      }
    
    //实现的InvocationHandler接口的invoke()方法
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
          try {
            return method.invoke(this, args);
          } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
          }
        }
        //获取MapperMethod对象
        final MapperMethod mapperMethod = cachedMapperMethod(method);
        //执行sql语句
        return mapperMethod.execute(sqlSession, args);
      }
      
      //缓存Method对应的MapperMethod对象
      private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = methodCache.get(method);
        if (mapperMethod == null) {
          mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
          methodCache.put(method, mapperMethod);
        }
        return mapperMethod;
      }
    
    }


9、`MyBatis`如何进行分页的？分页插件的原理是什么？


10、`MyBatis`如何将`sql`执行结果封装为目标对象并返回的，都有哪些映射形式？
第一种是使用`<resultMap>`标签，逐一定义列名和对象属性名之间的映射关系。第二种是使用`sql`列的别名功能，将列别名书写为对象属性名，比如`T_NAME AS NAME`，对象属性名一般是`name`，小写，但是列名不区分大小写，`Mybatis`会忽略列名大小写，智能找到与之对应对象属性名，你甚至可以写成`T_NAME AS NaMe`，`Mybatis`一样可以正常工作。

有了列名与属性名的映射关系后，`Mybatis`通过反射创建对象，同时使用反射给对象的属性逐一赋值并返回，那些找不到映射关系的属性，是无法完成赋值的。

11、动态`sql`

12、`MyBatis`的`xml`映射文件中，不同的`xml`映射文件，`id`是否可以重复？
不同的`Xml`映射文件，如果配置了`namespace`，那么`id`可以重复；如果没有配置`namespace`，那么id不能重复；毕竟`namespace`不是必须的，只是最佳实践而已(这句描述有问题，`<mapper>`标签必须加上`namespace`属性)。

原因就是`namespace+id`是作为`Map<String, MappedStatement>`的`key`使用的，如果没有`namespace`，就剩下`id`，那么，`id`重复会导致数据互相覆盖。有了`namespace`，自然`id`就可以重复，`namespace`不同，`namespace+id`自然也就不同。


  [1]: http://www.mybatis.org/mybatis-3/zh/configuration.html
  [2]: http://www.mybatis.org/mybatis-3/zh/configuration.html
  [3]: https://blog.csdn.net/mingtian625/article/details/47684271
  [4]: https://blog.csdn.net/ljheee/article/details/76618762
  [5]: https://blog.csdn.net/sinat_38899493/article/details/78586916
  [6]: https://www.cnblogs.com/tongxuping/p/7134113.html
