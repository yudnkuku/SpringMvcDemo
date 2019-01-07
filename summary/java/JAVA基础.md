# JAVA基础

标签（空格分隔）： JAVA

---
## static关键字 ##
静态块是`static`的重要应用之一，也是用于初始化一个类的时候操作用的，和静态变量、静态方法一样，静态块里的代码**只执行一次**，且只在初始化类的时候执行，并且**静态块的加载完全是按照其定义顺序来的**
静态资源属于类，在类的初始化加载，而非静态资源属于对象，在`new`的时候加载，因此静态资源先加载，才会继续加载非静态资源：

 - 静态方法不能引用非静态资源，初始化时加载静态方法，非静态方法对其是不可见得
 - 静态方法能引用静态资源
 - 非静态方法能引用静态资源

并且在继承机制当中，先执行父类静态代码，再执行子类静态代码，且**只加载一次**。

## 序列化和反序列化 ##
**序列化**：将对象转换成一串二进制表示的字节数组
**反序列化**：将字节数组重新构造成对象
序列化需要实现`java.io.Serializable`接口，序列化的时候会有一个`serialVersionUID`参数，`Java`序列化机制是通过在运行时判断类的`serialVersionUID`来验证版本一致性的，将字节流中的`serialVersionUID`和本地对象中的`serialVersionUID`对比来判断是否能够序列化，生成`serialVersionUID`有两种方式：

 - 1、默认的1L
 - 2、根据雷鸣、接口名、成员方法以及属性等来生成一个64位的`Hash`字段

`JAVA`提供了`ObjectInputStream`和`ObjectOutputStream`来实现对象的反序列化和序列化
`transient`声明的属性不会被序列化，同样`static`声明的属性也不会被序列化

## 类加载器ClassLoader ##
从虚拟机的角度讲，只有两类不同的类加载器：启动类加载器`Bootstrap ClassLoader`，这个类加载器是用`C++`语言实现的，是虚拟机内核的一部分，还有其他类加载器，这些类加载器是由`Java`语言实现，全部继承自`java.lang.ClassLoader`，例如`ExtClassLoader`和`AppClassLoader`

 - `BootsTrap ClassLoader`:启动类加载器，负责加载`JAVA_HOME/lib`下的类库
 - `ExtClassLoader`：扩展类加载器，这个类加载器由`sun.misc.Launcher$ExtClassLoader`实现，它负责加载`JAVA_HOME/lib/ext`目录中的，或者被`java.ext.dirs`系统变量指定的路径中所有类库
 - `AppClassLoader`：应用程序类加载器，这个类加载器由`sun.misc.Launche$AppClassLoader`实现，也被称为**系统类加载器**(`ClassLoader.getSystemClassLoader()`)
 
上述三个加载器有固定的层级关系，而不是继承关系：
用户自定义类加载器-》`AppClassLoader`-》`ExtClassLoader`-》`BootstrapClassLoader`

    AppClassLoader.parent = ExtClassLoader
    ExtClassLoader.parent = BootstrapClassLoader
    BootstrapClassLoader.parent = null

显式加载一个类的三种方式：

 - `Class.forName()`
 - `ClassLoader.loadClass()`
 - `ClassLoader.findSystemClass()`

`ClassLoader.loadClass()`核心代码：

    synchronized (getClassLoadingLock(name)) {
            // 检查该类是否已经被加载
            Class<?> c = findLoadedClass(name);
            //没有被加载
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                    if (parent != null) {
                        //父加载器不为null,委托给父加载器加载
                        c = parent.loadClass(name, false);
                    } else {
                        //父加载器为null，委托给Bootstrap ClassLoader加载
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }
                if (c == null) {
                    //如果仍旧为找到该类，则调用findClass()加载类,findClass()是一个受保护的空方法，可以由ClassLoader子类自定义实现
                    long t1 = System.nanoTime();
                    c = findClass(name);

                    // this is the defining class loader; record the stats
                    sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                    sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                    sun.misc.PerfCounter.getFindClasses().increment();
                }
            }

## Class.getResourceAsStream和ClassLoader.getResourceAsStream ##
先看`Class`源码：
    
    //1、name为绝对路径(name前面有/，例如/demo/test.txt)，那么资源的绝对路径就是demo/test.txt
    //2、name为相对路径(name前面没有/，例如test.txt或者demo/test.txt)，那么资源的绝对路径会在name前面添加该类的包名(将包名中的.替换为/)，假设包名是'com.exam'，那么资源路径就是com/exam/test.txt或者com/exam/demo/test.txt
    public InputStream getResourceAsStream(String name) {
        //Class加载资源的方法实际上还是代理给了ClassLoader
        //解析name算法，和上面描述一致
        name = resolveName(name);
        ClassLoader cl = getClassLoader0();
        if (cl==null) {
            // A system class.
            return ClassLoader.getSystemResourceAsStream(name);
        }
        return cl.getResourceAsStream(name);    //代理给ClassLoader加载资源
    }

而对于`ClassLoader`：

    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

**双亲委派模型**：如果一个类加载器收到了类加载的请求，他首先不会自己去尝试加载这个类，而是把这个请求委派给父类加载器去完成，当父加载器无法完成加载时，子加载器才会尝试自己去加载

**类加载机制**

类的加载流程分为三大步：加载、连接、初始化，连接又可细分为三步：验证、准备、解析
**1、加载(`Loading`)**

 - 预加载：虚拟机启动时会加载`%JAVA_HOME%\jre\lib`下的`rt.jar`中的`class`文件，可以设置虚拟机参数`-XX:+TraceClassLoading`来查看类加载信息
 - 运行时加载：虚拟机在需要一个类时，会先去内存中查看是否已经加载了此类，如果没有会按照全限定名称来加载这个类，且只会加载一次

那么，在类加载阶段，虚拟机做了哪些事情：
1、获取`class`文件的二进制流
2、将`class`文件中类信息、常量池、字段信息、方法信息等内容放入方法区中
3、在内存中生成此`class`对象实例，作为方法区这个类的各种数据入口，一般这个`class`是在堆上，但`HotSpot`虚拟机比较特殊，此`class`对象放在方法区中

获取`class`文件的二进制流方式有多种：
1、从`zip`包中获取，例如`jar`、`war`、`ear`包等
2、从网络中获取，如`Applet`
3、运行时计算生成，如动态代理技术
4、由其他文件生成，如`jsp`文件生成`class`文件

**2、验证**

验证加载的`class`文件定义是否符合虚拟机规范

**3、准备**

为类变量分配内存和设置初始值，这里是设置初始值而不是赋值，赋值是在初始化阶段，静态常量赋值是在此阶段进行

**4、解析**

解析过程实际上是将符号引用转换为直接引用的过程
符号引用包含三类：
1、类和接口的全限定名
2、字段的名称和描述符
3、方法的名称和描述符

**5、初始化**

初始化是类加载过程的最后一步，初始化过程是一个执行类构造器<clinit()>方法的过程，根据程序员设计初始化类变量和其他资源，说白点，初始化过程就是给静态变量赋值并执行静态代码块

以下场景需要立即对类进行初始化，这些场景被称为主动引用：
1、使用`new`实例化对象、读取或者设置一个类的静态字段、调用一个类的静态方法的时候
2、使用`java.lang.reflect`包中的方法对类进行反射调用
3、初始化一个类，发现其父类还没有初始化的时候
4、虚拟机启动时，会初始化用户指定的包含`main()`方法的那个类

以下场景不会触发类的初始化，这些场景被称为被动引用：
1、子类引用父类的静态变量，不会触发子类的初始化
2、通过数组定义引用类，不会触发此类的初始化
3、引用静态常量时，静态常量在编译阶段会存入类的常量池中，本质上并没有直接引用到定义常量的类

## 自动拆箱 ##
看如下代码：

    Integer i1 = 100;   
    Integer i2 = 100;
    Integer i3 = 200;
    Integer i4 = 200;
    System.out.println(i1 == i2)    //true
    System.out.println(i3 == i4)    //false

自动装箱时会调用`Integer`的静态方法`valueOf(int  i)`，查看源码可以看出

    public static Integer valueOf(int i) {
        if (i >= IntegerCache.low && i <= IntegerCache.high)
            return IntegerCache.cache[i + (-IntegerCache.low)];
        return new Integer(i);
    }

当数值在-128~127之间时，会取缓存中的对象，如果不是则`new`一个新对象

## JAVA WEB ##

    requestUri = contextPath + servletPath + pathInfo

请求`uri`包含上下文路径，`servlet`路径和路径信息，其中上下文和`servlet`路径以`/`开头，但结尾不含`/`，`HttpServletRequest`提供了`getContextPath()`、`getServletPath()`和`getPathInfo()`三个方法提取各个路径

我们常在`web.xml`文件中配置`servlet`，如下：

    <servlet>
     <servlet-name>SayHello</servlet-name>
     <servlet-class>common.SayHello</servlet-class>
    </servlet>
    
    <servlet-mapping>
      <servlet-name>SayHello</servlet-name>
      <url-pattern>/SayHello</url-pattern>
     </servlet-mapping>
    
定义了`servlet`的基本信息和路径映射，当容器接到一个请求后，首先要确定该请求由哪个`web`应用程序来响应，这是通过比较请求`uri`开始部分和`web`应用程序的上下文路径来确定的，映射到`servlet`的路径是请求`uri`减去上下文的路径，用该路径去匹配定义的`servlet`中的`url-pattern`，如果匹配成功，则由该`servlet`处理请求，匹配规则如下：

 - 精确匹配，如果匹配成功，则进行处理，例如：`<url-pattern>/user</url-pattern>`匹配路径`/user`
 - 最长路径匹配，如果匹配成功，匹配的字段就是`servlet`路径，剩下的是`pathInfo`，这主要针对于`url-pattern`中有通配符，例如：`<url-pattern>/user/*</url-pattern>`匹配`/user/name`
 - 如果请求带有后缀，例如`.jsp`等，容器会尝试匹配处理特殊后缀请求的`servlet`，此时请求`uri`就是`servlet`路径，`pathInfo`为空，例如`<url-pattern>*.jsp</url-pattern>`匹配`/hello.jsp`，如果请求是`/user/hello.jsp`则会按最长路径匹配规则匹配`/user/*`的`servlet`
 - 如果还没有找到匹配的`servlet`，则会将请求转发给容器默认的`servlet`处理，如果没有默认`servlet`，则会抛出错误信息

## InputStream和OutputStream ##
`InputStream`是一个抽象类，实现了`Closeable`接口，来看看其主要方法：

 - `markSupported()`:该输入流是否支持`mark`和`reset`方法
 - `mark(int readLimit)`:标记输入流当前位置，随后调用`reset()`方法会重新定位到流中上次标记的位置，并且接下来的读取操作会重读相同的字节，`readLimit`参数标明在`mark`标记失效前允许读取的最大字节数，`mark()`方法的约定如下：如果流的`markSupported()`方法返回`true`，那么流会记住`mark()`方法调用之后读取的所有字节，如果接下来`reset()`方法被调用，那么会重复读取这些字节，当然如果读取的字节数超过了`readLimit`的限制，流不会记住任何字节内容
 - `reset()`:重新定位流的读取位置至上一次调用`mark()`标记的地方，`reset()`方法的约定如下：当`markSupported()`方法返回`true`时，如果从未调用过`mark()`方法或者调用`mark()`方法后读取的字节数超过了`readLimit`，那么会抛出`IOException`,否则会重新定位到`mark()`标记的位置，重新读取流中的内容；如果不支持`mark()`，那么会抛出`IOException`
 - `read()`:从流中读取下一个字节内容，返回字节的`int`值，值范围取`0~255`，如果到达了流的末尾直接返回`-1`

`OutputStream`也是一个抽象类，实现了`Closeable`和`Flushable`接口，具体方法如下：

 - `flush()`：此方法的一般约定如下：如果实现类内部的写操作被缓冲下来(例如`BufferedOutputStream`)，那么调用此方法会立即将它们写到目标中。如果流的输出目标是底层操作系统提供的抽象，例如文件，那么`flush()`操作只会保证将缓冲内容传递给操作系统去写入，不会保证它们被真实的写入硬盘等物理设备
 - `write(int b)`:将`b`以字节形式写入流中，由于传入的参数是`int`类型，占`4`个字节，会写入低`8`位，高`24`位被忽略

 
## 缓冲流源码分析BufferedInputStream & BufferedOutputStream ##
**BufferedInputStream**

`BufferedInputStream`是具有缓冲功能的字节输入流，能提高读取速率，其内部缓冲区默认大小为8KB，每次调用`read()`方法时，**首先从缓冲区中读取数据(实际上就是内存，这样增加了读取效率)，若无数据可读，则调用`fill()`方法填满缓冲区，再从缓冲区中读取数据**

    public synchronized int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count)
                return -1;
        }
        return getBufIfOpen()[pos++] & 0xff;
    }
变量解释：

 - `pos`：`buffer`中下一个要读取数据的索引
 - `count`：`buffer`中字节的总数
 - `markPos`:`mark`标志索引，调用`mark()`方法时，将`pos`的值赋值给`markPos`

`read()`方法解析：
如果`buffer`中没有数据可读，即`pos>=count`，那么调用`fill()`方法填充`buffer`，再读取`pos`指向的`buf`中的字节

`fill()`源码：

    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if (markpos < 0)
            pos = 0;            //没有mark，清空buffer
        else if (pos >= buffer.length)  //buffer已满
            if (markpos > 0) {  //清空markpos之前的数据
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);   //将markpos之后的数据移动到buffer开头
                pos = sz;   //从sz开始读
                markpos = 0;    //将markpos置0
            } else if (buffer.length >= marklimit) {
                markpos = -1;   //buffer容量太大，取消mark
                pos = 0;        //清空buffer
            } else if (buffer.length >= MAX_BUFFER_SIZE) {
                throw new OutOfMemoryError("Required array size too large");
            } else {            //拓展buffer
                int nsz = (pos <= MAX_BUFFER_SIZE - pos) ?
                        pos * 2 : MAX_BUFFER_SIZE;
                if (nsz > marklimit)
                    nsz = marklimit;
                byte nbuf[] = new byte[nsz];
                System.arraycopy(buffer, 0, nbuf, 0, pos);
                if (!bufUpdater.compareAndSet(this, buffer, nbuf)) {
                    // Can't replace buf if there was an async close.
                    // Note: This would need to be changed if fill()
                    // is ever made accessible to multiple threads.
                    // But for now, the only way CAS can fail is via close.
                    // assert buf == null;
                    throw new IOException("Stream closed");
                }
                buffer = nbuf;
            }
        count = pos;    //更新buffer count
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos);   //从物理输入流中读取数据填满buffer
        if (n > 0)
            count = n + pos;    //更新buffer count
    }
    
**BufferedOutputStream**

`BufferedOutputStream`代码就相对简单，也是基于一个内部的`buffer`，默认大小同样是8M，看一下源码中的`write()`方法

    public synchronized void write(int b) throws IOException {
        if (count >= buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte)b;
    }
    
`write`方法解析：如果`buffer`没满，先向`buffer`中写数据，如果`buffer`满了，调用`flushBuffer()`方法，看下`flushBuffer()`方法：

    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }
`flushBuffer()`方法将`buffer`中的数据全部写进`out`流，并将`count`置0

## 转换流 ##
`InputStreamReader`将字节流转换为字符流，其底层实现是通过`StreamDecoder`来实现的，看下源码：

    public int read() throws IOException {
        return sd.read();
    }

`OutputStreamWriter`将字符流转换为字节流，底层是通过`StreamEncoder`来实现

    public void write(char cbuf[], int off, int len) throws IOException {
        se.write(cbuf, off, len);
    }
上述两类流在构造时可以显示指定字符编码格式，因此在字节流和字符流相互转换时需要使编码格式保持一致，否则会出现乱码现象

## JDK动态代理 ##
先看看`jdk`中动态代理的常用使用场景：

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    //获取被代理类实现的接口
    Service serviceImpl = new ServiceImpl();
    Class[] interfaces = serviceImpl.getClass().getInterfaces();
    Object proxy = Proxy.newProxyInstance(cl, interfaces, 
                        new MyInvocationHandler(){
                            
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                //some
                            }
                        });
    
其中最关键的方法是`Proxy.newProxyInstance()`，它用来创建接口的代理类，看一下源码：

    public static Object newProxyInstance(ClassLoader loader,
                                          Class<?>[] interfaces,
                                          InvocationHandler h)
        throws IllegalArgumentException
    {
        Objects.requireNonNull(h);

        final Class<?>[] intfs = interfaces.clone();
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkProxyAccess(Reflection.getCallerClass(), loader, intfs);
        }

        /*
         * Look up or generate the designated proxy class.
         */
        //查找或者生成代理类
        Class<?> cl = getProxyClass0(loader, intfs);

        /*
         * Invoke its constructor with the designated invocation handler.
         */
        try {
            if (sm != null) {
                checkNewProxyPermission(Reflection.getCallerClass(), cl);
            }
            //获取代理类cl带有参数类型为InvocationHandler的构造方法
            final Constructor<?> cons = cl.getConstructor(constructorParams);
            final InvocationHandler ih = h;
            if (!Modifier.isPublic(cl.getModifiers())) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        cons.setAccessible(true);
                        return null;
                    }
                });
            }
            //通过反射构造代理类实例，构造参数就是传进来的InvocationHandler实例
            return cons.newInstance(new Object[]{h});
        } catch (IllegalAccessException|InstantiationException e) {
            throw new InternalError(e.toString(), e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new InternalError(t.toString(), t);
            }
        } catch (NoSuchMethodException e) {
            throw new InternalError(e.toString(), e);
        }
    }

继续看`getProxyClass0`:

    private static Class<?> getProxyClass0(ClassLoader loader,
                                           Class<?>... interfaces) {
        if (interfaces.length > 65535) {
            throw new IllegalArgumentException("interface limit exceeded");
        }

        // If the proxy class defined by the given loader implementing
        // the given interfaces exists, this will simply return the cached copy;
        // otherwise, it will create the proxy class via the ProxyClassFactory
        //上面的英文翻译过来大致意思是如果指定类加载器加载的且实现了指定接口的proxy类存在，那么直接返回缓存中的副本，否则会通过ProxyClassFactory创建
        //从proxyClassCache中获取代理类
        return proxyClassCache.get(loader, interfaces);
    }
    //proxyClassCache定义
    private static final WeakCache<ClassLoader, Class<?>[], Class<?>>
        proxyClassCache = new WeakCache<>(new KeyFactory(), new ProxyClassFactory());

进入`ProxyClassFactory`：

    private static final class ProxyClassFactory
        implements BiFunction<ClassLoader, Class<?>[], Class<?>>
    {
        // prefix for all proxy class names
        //代理类名称前缀："$Proxy"
        private static final String proxyClassNamePrefix = "$Proxy";

        // next number to use for generation of unique proxy class names
        //原子计数器，用来对代理类进行编号
        private static final AtomicLong nextUniqueNumber = new AtomicLong();

        @Override
        public Class<?> apply(ClassLoader loader, Class<?>[] interfaces) {

            Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.length);
            for (Class<?> intf : interfaces) {
                /*
                 * Verify that the class loader resolves the name of this
                 * interface to the same Class object.
                 */
                Class<?> interfaceClass = null;
                try {
                    interfaceClass = Class.forName(intf.getName(), false, loader);
                } catch (ClassNotFoundException e) {
                }
                if (interfaceClass != intf) {
                    throw new IllegalArgumentException(
                        intf + " is not visible from class loader");
                }
                /*
                 * Verify that the Class object actually represents an
                 * interface.
                 */
                //不是接口抛出异常
                if (!interfaceClass.isInterface()) {
                    throw new IllegalArgumentException(
                        interfaceClass.getName() + " is not an interface");
                }
                /*
                 * Verify that this interface is not a duplicate.
                 */
                if (interfaceSet.put(interfaceClass, Boolean.TRUE) != null) {
                    throw new IllegalArgumentException(
                        "repeated interface: " + interfaceClass.getName());
                }
            }
            
            //代理类包路径
            String proxyPkg = null;     // package to define proxy class in
            //初始化访问标志accessFlags
            int accessFlags = Modifier.PUBLIC | Modifier.FINAL;

            /*
             * Record the package of a non-public proxy interface so that the
             * proxy class will be defined in the same package.  Verify that
             * all non-public proxy interfaces are in the same package.
             */
            for (Class<?> intf : interfaces) {
                int flags = intf.getModifiers();
                if (!Modifier.isPublic(flags)) {
                    accessFlags = Modifier.FINAL;
                    String name = intf.getName();
                    int n = name.lastIndexOf('.');
                    String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
                    if (proxyPkg == null) {
                        proxyPkg = pkg;
                    } else if (!pkg.equals(proxyPkg)) {
                        throw new IllegalArgumentException(
                            "non-public interfaces from different packages");
                    }
                }
            }

            if (proxyPkg == null) {
                // if no non-public proxy interfaces, use com.sun.proxy package
                //ReflectUtil.PROXY_PACKAGE="com.sun.proxy"
                proxyPkg = ReflectUtil.PROXY_PACKAGE + ".";
            }

            /*
             * Choose a name for the proxy class to generate.
             */
            long num = nextUniqueNumber.getAndIncrement();
            String proxyName = proxyPkg + proxyClassNamePrefix + num;

            /*
             * Generate the specified proxy class.
             */
            //调用ProxyGenerator生产代理类字节数组
            byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
                proxyName, interfaces, accessFlags);
            try {
                //通过ClassLoader和类字节数组来构造代理类
                return defineClass0(loader, proxyName,
                                    proxyClassFile, 0, proxyClassFile.length);
            } catch (ClassFormatError e) {
                /*
                 * A ClassFormatError here means that (barring bugs in the
                 * proxy class generation code) there was some other
                 * invalid aspect of the arguments supplied to the proxy
                 * class creation (such as virtual machine limitations
                 * exceeded).
                 */
                throw new IllegalArgumentException(e.toString());
            }
        }
    }

将上述生成的字节数组输出到文件，反编译之后查看：




    import demo.dynamicProxy.Subject;
    import java.lang.reflect.InvocationHandler;
    import java.lang.reflect.Method;
    import java.lang.reflect.Proxy;
    import java.lang.reflect.UndeclaredThrowableException;
    
    public final class $Proxy0 extends Proxy implements Subject {
        private static Method m1;
        private static Method m2;
        private static Method m3;
        private static Method m0;

    public $Proxy0(InvocationHandler var1) throws  {
        super(var1);
    }

    public final boolean equals(Object var1) throws  {
        try {
            return (Boolean)super.h.invoke(this, m1, new Object[]{var1});
        } catch (RuntimeException | Error var3) {
            throw var3;
        } catch (Throwable var4) {
            throw new UndeclaredThrowableException(var4);
        }
    }

    public final String toString() throws  {
        try {
            return (String)super.h.invoke(this, m2, (Object[])null);
        } catch (RuntimeException | Error var2) {
            throw var2;
        } catch (Throwable var3) {
            throw new UndeclaredThrowableException(var3);
        }
    }
    //生成接口的add方法实现，实际上还是调用InvocationHandler实例的invoke方法
    public final void add() throws  {
        try {
            super.h.invoke(this, m3, (Object[])null);
        } catch (RuntimeException | Error var2) {
            throw var2;
        } catch (Throwable var3) {
            throw new UndeclaredThrowableException(var3);
        }
    }

    public final int hashCode() throws  {
        try {
            return (Integer)super.h.invoke(this, m0, (Object[])null);
        } catch (RuntimeException | Error var2) {
            throw var2;
        } catch (Throwable var3) {
            throw new UndeclaredThrowableException(var3);
        }
    }
    //静态代码块
    static {
        try {
            m1 = Class.forName("java.lang.Object").getMethod("equals", Class.forName("java.lang.Object"));
            m2 = Class.forName("java.lang.Object").getMethod("toString");
            m3 = Class.forName("demo.dynamicProxy.Subject").getMethod("add");   //获取接口add方法
            m0 = Class.forName("java.lang.Object").getMethod("hashCode");
        } catch (NoSuchMethodException var2) {
            throw new NoSuchMethodError(var2.getMessage());
        } catch (ClassNotFoundException var3) {
            throw new NoClassDefFoundError(var3.getMessage());
        }
    }
    }

动态代理有如下特性：

 - 继承了`Proxy`类，实现了代理的接口，由于`java`不能多继承，这里已经继承了`Proxy`类，不能再继承其他类，所以`JDK`动态代理不支持对实现类的代理，**只支持接口的代理**，`jdk`动态代理也称为接口代理
 - 生成的代理类会提供了一个使用`InvocationHandler`作为参数的构造方法
 - 生成的静态代码块中会初始化三个方法：`equals/toString/hashCode`，并且代理类中还重写了这三个方法，重写方法只是简单的调用了`InvocationHandler`的`invoke`方法，因此该代理类实际上也可以代理这三个方法
 - 代理类中对接口方法的实现实际上也就是调用`InvocationHandler`的`invoke`方法，在`invoke`方法中可以实现自己的逻辑，而调用代理类的`equals/toString/hashCode`同样会触发`invoke`方法

再看看`MyBatis`中对`mapper`接口使用的动态代理实现，源码在`MapperProxy`中：

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    //这里会过滤掉代理类中重写的equals/toString/hashCode方法，避免执行后面的MapperMethod
    if (Object.class.equals(method.getDeclaringClass())) {
      try {
        return method.invoke(this, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    return mapperMethod.execute(sqlSession, args);
      }

 
## 泛型 ##
先来看一个错误：

    List<? extends Foo> list1 = new ArrayList<Foo>();
    list1.add(new Foo());   //won't compile
    

在`Java`语言中，数组时协变的，也就是说，如果`Integer`拓展了`Number`，那么不仅仅`Integer`是`Number`，而且`Integer[]`也是`Number[]`，在要求`NUmber[]`的地方完全可以传递或者赋予`Integer[]`(更正式的说，如果`Number`是`Integer`的超类型，那么`Number[]`也是`Integer[]`的超类型)。但是在泛型类型中`List<Number>`不是`List<Integer>`的超类型，也就是说在需要`List<Number>`的地方不能传递`List<Integer>`

例如如下代码在编译时就会报错：

    List<String> strList = new ArrayList<>();
    List<Object> objList = strList;    //compile error
    
解释：泛型的作用是在编译期告诉编译期参数应该是什么类型，泛型会在运行时期擦除，例如上述`objList`在编译器确定元素类型时`Object`，那么只要是`? extends Object`的元素应该都能插入集合，但是实际上却赋值`strList`，这个时候会破坏类型安全。

**PECS法则**

如下代码：

    HashMap<T extends Foo>
    HashMap<? extends Foo>
    HashMap<T super Foo>
    HashMap<? super Foo>

主要涉及的是`Java`泛型中重要的`PECS`法则：
1、`？ extends T`
类型的上界是`T`，参数化类型可能是`T`或者`T`的子类
    
    class Food{}
    class Fruit extends Food {}
    class Apple extends Fruit {}
    List<? extends Fruit> fruits = new ArrayList<>();
    fruits.add(new Food()); //compile error
    fruits.add(new Fruit());//compile error
    fruits.add(new Apple());//compile error

    fruits = new ArrayList<Fruit>(); //compile success
    fruits = new ArrayList<Food>();  //compile error
    fruits = new ArrayList<Apple>(); //compile success
    fruits = new ArrayList<? extends Fruit>();   //compile error:通配符无法实例化
    Fruit object = fruits.get(0);
    
存入数据：

 - 赋值时参数化类型为`Fruit`的集合和其子类的集合都可以成功，通配符类型无法实例化
 - 编译期会阻止将`Apple`类加入`fruitss`，在向`fruits`集合中添加元素时，编译期会检查类型是否符合要求。`List<? extends Fruit>`这样的声明只会告诉编译期集合中元素是`Fruit`的子类，至于具体是什么类只能在运行时期才能确定，因此为了类型安全，只好阻止向其中加入任何子类，也就是说`List<? extends Fruit>`这样声明的集合无法添加任何子类元素

读取数据：
由于编译期知道它总是`Fruit`的子类型，因此我们总可以从中读取出`Fruit`对象

    Fruit fruit = fruits.get(0)

对于`? extends Foo`声明的集合只能作为生产者，从中获取元素，无法往其中添加元素
 
2、`? super T`
表示类型的下界是`T`，参数化类型可以是`T`或`T`的超类：

    List<? super Fruit> fruits = new ArrayList<>();
    fruits.add(new Food()); //compile error
    fruits.add(new Fruit());//compile success
    fruits.add(new Apple());//compile success

    fruits = new ArrayList<Fruit>(); //compile success
    fruits = new ArrayList<Food>();  //compile success
    fruits = new ArrayList<Apple>(); //compile error
    fruits = new ArrayList<? super Fruit>();   //compile error:通配符无法实例化
    Fruit object = fruits.get(0);   //compile error

存入数据：

 - `super`通配符类型同样不能实例化，对于`List<? super Fruit>`的集合均可被赋值为`Fruit`超类`List`，例如`fruits = new ArrayList<Food>()`
 - 编译器已经知道集合中元素是`Fruit`的超类，因此往其中添加`Fruit`的子类元素是必然没有问题的

读取数据：
编译期在不知道这个超类具体是什么类，只能返回`Object`对象

    Object fruit = fruits.get(0);

 
**PECS原则总结**

总结如下：

 - 如果要从集合中读取类型`T`的数据，并且不能写入，那么该集合作为生产者，必须使用`? extends T`通配符(`Producer Extends`)
 - 如果要往集合中写入类型`T`的数据，并且不能读取，那么该集合作为消费者，必须使用`? super T`通配符(`Consumer Super`)
 - 如果既要写又要读，那么建议不适用任何通配符

  [1]: https://coolshell.cn/articles/9606.html