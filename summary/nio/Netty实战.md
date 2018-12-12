# Netty实战

标签（空格分隔）： 读书笔记

---

## Future ##
`channel`的每个`outbound I/O`操作都会返回一个`ChannelFuture`，`ChannelFutrue`继承自`Future`，此`Future`接口继承自`juc`包中的`Future`接口，下面看看`Channel`中的一些`ountbound`方法

    ChannelFuture bind(SocketAddress localAddress);
    ChannelFuture connect(SocketAddress remoteAddress);
    ChannelFuture write(Object msg);
    ChannelFuture disconnect();
    ChannelFuture close();

以上方法均不会阻塞，并返回了`ChannelFuture`实例，当完成`Channel`的`I/O`操作后，可以在返回的`ChannelFuture`上添加`GenericFutureListener`来监听事件完成，例如：

        Channel channel = ...;
    //不会阻塞
    ChannelFuture future = channel.connect(            //1
            new InetSocketAddress("192.168.0.1", 25));
    future.addListener(new ChannelFutureListener() {  //2
    @Override
    public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {       //3
            ByteBuf buffer = Unpooled.copiedBuffer(
                    "Hello", Charset.defaultCharset()); //4
            ChannelFuture wf = future.channel().writeAndFlush(buffer);                //5
            // ...
        } else {
            Throwable cause = future.cause();        //6
            cause.printStackTrace();
        }
    }
    });
    
1、异步连接到远程对等节点，调用立即返回`ChannelFuture`实例
2、操作完成后注册一个`ChannelFutureListener`
3、当操作成功后
4、创建一个`ByteBuf`来保存数据
5、异步写数据，并返回`ChannelFuture`
6、如果操作失败，打印错误原因

## 事件流 ##
`Netty`将事件分为`inbound`和`outbound`两类事件，在`Channel`进行`I/O`相关操作时会触发事件，这些事件会在`ChannelPipeline`中传递，并交给对应的`ChannelHandler`处理。其内部的事件流如下所示：

![Netty Event Flow][1]

## ChannelInitializer ##
它是一个特殊的`ChannelHandler`，当`Channel`注册到`EventLoop`中时初始化`Channel`，直接看源码：

    public abstract class ChannelInitializer<C extends Channel> extends ChannelHandlerAdapter {
        
        //初始化Channel，由子类实现
        protected abstract void initChannel(C ch) throws Exception;
        
        //channel register回调方法，调用initChannel()方法初始化Channel，并将当前的ChannelInitializer实例从ChannelPipeline中移除
        public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ChannelPipeline pipeline = ctx.pipeline();
        boolean success = false;
        try {
            initChannel((C) ctx.channel());
            pipeline.remove(this);
            ctx.fireChannelRegistered();
            success = true;
        } catch (Throwable t) {
            logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), t);
        } finally {
            if (pipeline.context(this) != null) {
                pipeline.remove(this);
            }
            if (!success) {
                ctx.close();
            }
        }
    }
    }

## ServerBootstrap和Bootstrap ##
两者都用来引导`Netty`的本地配置。
`ServerBootstrap`用于服务端，绑定本地端口，可以声明两个`EventLoopGroup`，一个用来处理客户端的连接请求(`Acceptor`线程池,还未创建`Channel`)，一个用来处理`I/O`操作(处理即将创建的`Channel`的所有事件)。
`Bootstrap`用于客户端，连接到远程主机和端口，只能声明一个`EventLoopGroup`用于处理`Channel`的所有事件。

![ServerBootsrap和Bootstrap][2]

## 基于传输的API ##
`ChannelPipeline`实现了常用的`InterceptingFilter`模型(拦截过滤器)，`Unix`管道是另一例子：命令链接在一起，一个命令的输出连接到下一行中的输入。你可以在运行时根据需要添加`ChannelHandler`实例到`ChannelPipeline`，或者从`ChannelPipeline`中删除，这能帮助你构建高度灵活的`Netty`程序。
如下代码，写数据到远程已连接的客户端：

    Channel channel = ...; // 获取channel的引用
    ByteBuf buf = Unpooled.copiedBuffer("your data", CharsetUtil.UTF_8); //使用Unpooled工具类构建ByteBuf
    ChannelFuture cf = channel.writeAndFlush(buf); //调用writeAndFlush()方法异步写数据，返回ChannelFuture
    
    cf.addListener(new ChannelFutureListener() {    //添加监听回调
        @Override
        public void operationComplete(ChannelFuture future) {
            if (future.isSuccess()) {                //写数据成功
                System.out.println("Write successful");
            } else {
                System.err.println("Write error");    //写数据失败
                future.cause().printStackTrace();
            }
        }
    });

## Netty核心功能之ByteBuf ##
`ByteBuf`是一个随机、顺序访问的字节序列，通常我们通过`Unpooled`中的帮助方法来创建一个新的`buffer`。
**随机访问**
就像一个普通的字节数组，可以随机访问`ByteBuf`

    for(int i = 0; i < buffer.capacity(); i++) {
        byte b = buffer.getByte(i);
        System.out.println((char) b);
    }

**序列化访问**
`ByteBuf`提供了两个指针以支持读写操作：`readerIndex`和`writeIndex`
    
    0<=readerIndex<=writeIndex<=capacity
    |丢弃字节 |  可读字节  |可写字节|

可读字节就是实际的内容，这里面存储着实际的数据，诸如`read`或者`skip`操作都会增加当前的`readerIndex`，增量就是读取的字节数，如果没有足够的字节可读，那么会抛出`IndexOutOfBoundException`异常。
可写字节区域需要去填充数据，任何`write`操作都会在当前`writerIndex`处写入数据，并将`writerIndex`增加写入的字节数，如果没有足够的可写空间，那么会抛出`IndexOutOfBoundException`异常。
可丢弃区域包含了已经读取的区域，如果手动调用`discardReadBytes()`方法会将`readerIndex`置0，`writerIndex`降低相应的值。
调用`clear`方法只会将`readerIndex`和`writerIndex`两个指针清零，而不会将内容清空(例如全部填充0)。

**直接缓冲区Direct Buffer**

## ChannelHandler和ChannelPipeline ##
**Channel生命周期**
|状态|描述|
|:-:|:-:|
|`channelRegisterd`|`channel`注册到一个`EventLoop`(例如服务端接收到远程客户端的连接会触发该事件)|
|`channelActive`|`channel`变为活跃状态，在`channelRegisterd`回调之后执行|
|`channelInactive`|`channel`处于非活跃状态，没有连接到远程主机|
|`channelUnregistered`|`channel`已经创建但未注册到一个`EventLoop`|

一般`Channel`的回调流程如下：

    channelRegistered->channelActive->channelInActive->channelUnregistered

源码：

    public final void register(final ChannelPromise promise) {
            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            //调用注册方法
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    promise.setFailure(t);
                }
            }
        }
        
    private void register0(ChannelPromise promise) {
                try {
                    // check if the channel is still open as it could be closed in the mean time when the register
                    // call was outside of the eventLoop
                    if (!ensureOpen(promise)) {
                        return;
                    }
                    doRegister();
                    registered = true;
                    promise.setSuccess();
                    //调用pipeline.fireChannelRegistered()方法，进入ChannelPipeline，最后会走到自定义的ChannelHandler的channelRegisterd()方法
                    pipeline.fireChannelRegistered();
                    if (isActive()) {
                        //如果channel绑定成功，调用pipeline.fireChannelActive()，最后会走到自定义ChannelHandler的channelActive()方法
                        pipeline.fireChannelActive();
                    }
                } catch (Throwable t) {
                    // Close the channel directly to avoid FD leak.
                    closeForcibly();
                    closeFuture.setClosed();
                    if (!promise.tryFailure(t)) {
                        logger.warn(
                                "Tried to fail the registration promise, but it is complete already. " +
                                        "Swallowing the cause of the registration failure:", t);
                    }
                }
            }
            
**ChannelHandler**
`Handler`生命周期方法:

 - `handlerAdd(ChannelHandlerContext ctx)`:当`handler`添加到`ctx`中时回调
 - `handlerRemoved(ChannelHandlerContext ctx)`:当`handler`从`ctx`中移除时回调

`Inbound`事件处理方法：

 - `exceptionCaught(ctx, cause)`:抛出异常时回调
 - `channelRegistered(ctx)`:和`ctx`相关的`channel`注册到`EventLoop`上时回调
 - `channelActive(ctx)`:`ctx`相关的`channel`处于激活状态时回调
 - `channelInactive(ctx)`:`channel`处于非激活状态时回调
 - `channelRead(ctx)/channelReadComplete(ctx)`:`channel`读数据/读数据完成时回调
 - `userEventTrigerred(ctx, evt)`:当用户事件触发时回调
 - `channelWritabilityChanged(ctx)`:`channel`可写状态改变时回调

`Outbound`事件处理方法：

 - `bind(ctx, localAddress, ChannelPromise promis)`:当绑定到本地地址上时回调
 - `connect(ctx, remoteAddresss, localAddress, promise)/disconnect(ctx, promise)`:当连接/断开连接到远端主机时回调
 - `close(ctx, promise)`:执行`close`操作时回调
 - `read(ctx)`:拦截`ChannelHandlerContext`的`read()`操作
 - `write(ctx, msg, promis)`:一旦写操作执行就会调用，写操作会将消息沿着`ChannelPipeline`写，随后调用`Channel.flush()`方法，这些消息会被`flush`进真实的`Channel`
 - `flush(ctx)`:一旦`flush`操作执行就会调用，`flush`操作会将之前写入的数据全部`flush`

提供了两个注解：

 - `@Sharable`：类级注解，该注解指定的`ChannelHandler`实例可以被多次添加进一个或者多个`ChannelHandler`而不会引起`race condition`
 - `@Skip`:方法级注解，`ChannelHandler`中带有该注解的方法在事件发生时不会调用而直接跳过。

`Inbound`事件流程：

    
    
`Outbound`事件流程，以`connect`事件为例：

    Bootstrap.connect()->Bootstrap.doConnect()->Bootstrap.doConnect0()->AbstractChannel.connect(remoteAddress, promise)->DefaultChannelPipeline.connect(remoteAddress, promise)->tail.connect(remoteAddress, promise)->DefaultChannelHandlerContext.connect(remoteAddress, localAddress, promise)->DefaultChannelHandlerInvoker.invokeConnect()->ChannelHandlerInvokerUtil.invokeConnectNow()->ChannelHandler.connect(ctx, remoteAddress, localAddress, promise)


**ChannelHandlerContext**
在`ChannelHandler`添加到`ChannelPipeline`时会创建一个实例，就是接口`ChannelHandlerContext`，在源码中可以看到：
    
    //调用addLast方法添加ChannelHandler实例，会最终构造一个ChannelHandlerContext实例，并将其添加进ChannelPipeline
    public ChannelPipeline addLast(ChannelHandlerInvoker invoker, final String name, ChannelHandler handler) {
        synchronized (this) {
            checkDuplicateName(name);

            DefaultChannelHandlerContext newCtx =
                    new DefaultChannelHandlerContext(this, invoker, name, handler);

            addLast0(name, newCtx);
        }

        return this;
    }
    
    private void addLast0(final String name, DefaultChannelHandlerContext newCtx) {
        checkMultiplicity(newCtx);
        //在tail节点之前添加newCtx
        DefaultChannelHandlerContext prev = tail.prev;
        newCtx.prev = prev;
        newCtx.next = tail;
        prev.next = newCtx;
        tail.prev = newCtx;

        name2ctx.put(name, newCtx);

        callHandlerAdded(newCtx);
    }
    
    //再看看tail的初始化
    //构造函数，
    public DefaultChannelPipeline(AbstractChannel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;

        TailHandler tailHandler = new TailHandler();
        tail = new DefaultChannelHandlerContext(this, null, generateName(tailHandler), tailHandler);

        HeadHandler headHandler = new HeadHandler(channel.unsafe());
        head = new DefaultChannelHandlerContext(this, null, generateName(headHandler), headHandler);

        head.next = tail;
        tail.prev = head;
    }

可以这样理解，`ChannelPipeline`实际上是`ChannelHandlerContext`的一个`pipeline`，而`ChannelHandlerContext`实例中封装了`ChannelHandler`，当`Channel`产生相应事件时，该事件会在`ChannelPipeline`上传递，最终会由能够处理该事件的`ChannelHandler`处理。

因为`ChannelHandler`可以属于多个`ChannelPipeline`，它可以绑定多个`ChannelHandlerContext`实例，然而`ChannelHandler`必须添加`@Sharable`注解，此外它必须是线程安全的。例如：

    @ChannelHandler.Sharable
    public class SharableHandler extends ChannelHandlerAdapter {
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            System.out.println("channel read message : " + msg);
            //传递到下一个ChannelHandler
            ctx.fireChannelRead(msg);   
        }
    }

当`ChannelHandler`实例保持了特有状态时，可能会出现问题：

    @ChannelHandler.Sharable  //1
    public class NotSharableHandler extends ChannelInboundHandlerAdapter {
        //共享变量
        private int count;
    
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            count++;  //2
    
            System.out.println("inboundBufferUpdated(...) called the "
            + count + " time");  //3
            ctx.fireChannelRead(msg);
        }
    
    }

`channelRead()`方法需要加入同步处理。


## 线程模型源码分析 ##
不管是在客户端还是在服务端启动时，都会用到`NioEventLoopGroup`，例如：

    EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ServerChannelHandler());
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

查看其构造方法：

    public NioEventLoopGroup() {
        this(0);
    }
    public NioEventLoopGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }
    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }
    
    //父类MultithreadEventLoopGroup
    //nThreads=0,使用DEFAULT_EVENT_LOOP_THREADS
    protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
    }

    DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                    "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));
                    
    //继续调用父类MultithreadEventExecutorGroup的构造方法
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }
        //传进来的executor=null，构造ThreadPerTaskExecutor，每个任务开一个线程
        if (executor == null) {
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }
        //内部维护了一个EventExecutor数组，大小为nThreads
        children = new EventExecutor[nThreads];
        for (int i = 0; i < nThreads; i ++) {
            boolean success = false;
            try {
                //调用子类的newChild()方法构造EventExecutor，具体实现在NioEventLoopGroup中
                children[i] = newChild(executor, args);
                success = true;

`NioEventLoopGroup`中`newChild()`方法：
    
    //executor是ThreadPerTaskExecutor实例
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return new NioEventLoop(this, executor, (SelectorProvider) args[0]);
    }
    
    //继续调用
    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider) {
        super(parent, executor, false);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        provider = selectorProvider;
        selector = openSelector();
    }
    
    //openSelector()，初始化selector
    private Selector openSelector() {
        final Selector selector;
        try {
            selector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }
        
        //默认是false
        if (DISABLE_KEYSET_OPTIMIZATION) {
            return selector;
        }


再看看`Channel`和`EventLoop`是如何绑定的，跟着上面的`b.bind()`方法进入：

    private ChannelFuture doBind(final SocketAddress localAddress) {
        //初始化channel并注册selector
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        final ChannelPromise promise;
        if (regFuture.isDone()) {
            promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
        } 
    
    //initAndRegister()
    final ChannelFuture initAndRegister() {
        Channel channel;
        try {
            //构造channel
            channel = createChannel();  //(1)
        } catch (Throwable t) {
            return VoidChannel.INSTANCE.newFailedFuture(t);
        }

        try {
            //初始化channel
            init(channel);  //(2)
        } catch (Throwable t) {
            channel.unsafe().closeForcibly();
            return channel.newFailedFuture(t);
        }

        ChannelPromise regFuture = channel.newPromise();
        //注册，并fireChannelRegistered()
        channel.unsafe().register(regFuture);   //(3)
        
(1)首先看构造`channel`方法`createChannel()`:
    
    //ServerBootstrap.createChannel()方法
    Channel createChannel() {
        //group()返回group，ServerBootstrap.group(group, childGroup),也就是传入的第一个EventLoopGroup，即NioEventLoopGroup实例，那么跳转至父类MultithreadEventExecutorGroup中
        EventLoop eventLoop = group().next();
        return channelFactory().newChannel(eventLoop, childGroup);
    }

`MultithreadEventExecutorGroup`中：

    public EventExecutor next() {
        //返回children数组中下一个对象，环形数组
        return children[Math.abs(childIndex.getAndIncrement() % children.length)];
    }

获取到`next EventLoop`之后，执行`channelFactory().newChannel()`:
    
    //这里采用反射构造NioServerSocketChannel实例，传入EventLoop和EventLoopGroup实例作为参数
    public NioServerSocketChannel(EventLoop eventLoop, EventLoopGroup childGroup) {
        //构造nio ServerSocketChannel实例，并注册OP_ACCEPT事件
        super(null, eventLoop, childGroup, newSocket(), SelectionKey.OP_ACCEPT);
        config = new DefaultServerSocketChannelConfig(this, javaChannel().socket());
    }
    
至此服务端的`Channel`就已经基本初始化完成，并添加了`OP_ACCEPT`事件，将`EventLoop`和`Channel`关联了起来：
    
    //AbstractChannel构造方法，此时已经构造了unsafe和pipeline
    protected AbstractChannel(Channel parent, EventLoop eventLoop) {
        this.parent = parent;
        this.eventLoop = validate(eventLoop);
        unsafe = newUnsafe();
        pipeline = new DefaultChannelPipeline(this);
    }

(2)`init()`方法初始化`channel`，下面看看`ServerBootstrap.init()`方法:

    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options();
        synchronized (options) {
            channel.config().setOptions(options);
        }

        final Map<AttributeKey<?>, Object> attrs = attrs();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();
        //如果调用了b.handler(ChannelHandler)，方法设置handler，将其添加至pipeline
        if (handler() != null) {
            p.addLast(handler());
        }
        
        //ServerBootstrap必须调用b.childHandler()设置childHandler，否则会抛出异常
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(childOptions.size()));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
        }
        
        //构建新的ChannelInitializer添加至pipeline，ChannelInitializer具体实现见下文
        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                //将currentChildHandler封装成ServerBootstrapAcceptor添加进服务端NioServeSocketChannel的ChannelPipeline
                //ServerBootstrapAcceptor也是一个ChannelHandler，它实现了channelRead，当accept到客户端的连接时会触发channelRead方法回调
                ch.pipeline().addLast(new ServerBootstrapAcceptor(currentChildHandler, currentChildOptions,
                        currentChildAttrs));
            }
        });
    }

`ChannelInitializer`源码：
    
    //注解@Sharable，表示可以被多个ChannelPipeline共享，ChannelInitializer也继承自ChannelHandlerAdapter，ChannelInitializer在执行完channelRegistered()之后会从pipeline中移除
    @Sharable
    public abstract class ChannelInitializer<C extends Channel> extends ChannelHandlerAdapter {

        private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);
        
        //抽象方法initChannel，给子类实现
        protected abstract void initChannel(C ch) throws Exception;
    
        @Override
        @SuppressWarnings("unchecked")
        public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            ChannelPipeline pipeline = ctx.pipeline();
            boolean success = false;
            try {
                //invoke initChannel()
                initChannel((C) ctx.channel());
                //将此ChannelInitializer从pipeline中移除
                pipeline.remove(this);
                //fireChannelRegistered()
                ctx.fireChannelRegistered();
                success = true;
            } catch (Throwable t) {
                logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), t);
            } finally {
                if (pipeline.context(this) != null) {
                    pipeline.remove(this);
                }
                if (!success) {
                    ctx.close();
                }
            }
        }
    }
`ServerBootstrapAcceptor`源码：

    private static class ServerBootstrapAcceptor extends ChannelHandlerAdapter {

        private final ChannelHandler childHandler;
        private final Entry<ChannelOption<?>, Object>[] childOptions;
        private final Entry<AttributeKey<?>, Object>[] childAttrs;

        ServerBootstrapAcceptor(ChannelHandler childHandler, Entry<ChannelOption<?>, Object>[] childOptions,
                Entry<AttributeKey<?>, Object>[] childAttrs) {
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;
        }
        
        //这里实现了channelRead方法，当accept到客户端的连接请求时触发，这里的msg实际上是NioSocketChannel类型
        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //将msg向下转型为Channel类型
            Channel child = (Channel) msg;
            //将childHandler添加到Channel的ChannelPipeline中
            child.pipeline().addLast(childHandler);

            for (Entry<ChannelOption<?>, Object> e: childOptions) {
                try {
                    if (!child.config().setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
                        logger.warn("Unknown channel option: " + e);
                    }
                } catch (Throwable t) {
                    logger.warn("Failed to set a channel option: " + child, t);
                }
            }

            for (Entry<AttributeKey<?>, Object> e: childAttrs) {
                child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }
            //执行注册任务，将selector注册到channel上
            child.unsafe().register(child.newPromise());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final ChannelConfig config = ctx.channel().config();
            if (config.isAutoRead()) {
                // stop accept new connections for 1 second to allow the channel to recover
                // See https://github.com/netty/netty/issues/1328
                config.setAutoRead(false);
                ctx.channel().eventLoop().schedule(new Runnable() {
                    @Override
                    public void run() {
                       config.setAutoRead(true);
                    }
                }, 1, TimeUnit.SECONDS);
            }
            // still let the exceptionCaught event flow through the pipeline to give the user
            // a chance to do something with it
            ctx.fireExceptionCaught(cause);
        }
    }
    
(3)`channel.unsafe().register(regfuture)`方法源码，`channel.unsafe()`返回`Channel`在构造时产生的实例，见上文`AbstractChannel`构造方法。
    
    //register()方法
    public final void register(final ChannelPromise promise) {
            //判断是否EventLoop的内部线程是否是当前线程，第一次执行应该进入else
            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    //调用execute执行Runnable
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    promise.setFailure(t);
                }
            }
        }

`SingleThreadEventExecutor`的`execute()`方法：

    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        boolean inEventLoop = inEventLoop();
        if (inEventLoop) {
            addTask(task);
        } else {
            //执行这里
            startThread();  //(1)
            addTask(task);  //(2)
            if (isShutdown() && removeTask(task)) {
                reject();
            }
        }

        if (!addTaskWakesUp) {
            wakeup(inEventLoop);
        }
    }

(1)`startThread()`:

    private void startThread() {
        synchronized (stateLock) {
            if (state == ST_NOT_STARTED) {
                state = ST_STARTED;
                delayedTaskQueue.add(new ScheduledFutureTask<Void>(
                        this, delayedTaskQueue, Executors.<Void>callable(new PurgeTask(), null),
                        ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL));
                //继续
                doStartThread();
            }
        }
    }
    
    private void doStartThread() {
        assert thread == null;
        //executor是在构造NioEventLoop传入的参数，它是ThreadPerTaskExecutor，会为每一个Runnable任务创建一个线程执行
        executor.execute(new Runnable() {
            @Override
            public void run() {
                thread = Thread.currentThread();
                if (interrupted) {
                    thread.interrupt();
                }

                boolean success = false;
                updateLastExecutionTime();
                try {
                    SingleThreadEventExecutor.this.run();   //此处见另外的分析
                    success = true;

(2)`addTask()`：

    protected void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (isShutdown()) {
            reject();
        }
        //将task放入taskQueue
        taskQueue.add(task);
    }

`register()`方法会继续调用`register0()`：

    private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // call was outside of the eventLoop
                if (!ensureOpen(promise)) {
                    return;
                }
                doRegister();
                registered = true;
                promise.setSuccess();
                //fireChannelRegistered，这里会触发之前设置的ChannelInitializer执行channelRegistered()方法
                pipeline.fireChannelRegistered();
                if (isActive()) {
                    //fireChannelActive
                    pipeline.fireChannelActive();
                }

`doRegister()`：

    protected void doRegister() throws Exception {
        boolean selected = false;
        //死循环
        for (;;) {
            try {
                //将selector注册到channel，selector是在NioEventLoop构造时通过openSelector方法生成的
                selectionKey = javaChannel().register(eventLoop().selector, 0, this);
                return;

`openSelector`方法：

    private Selector openSelector() {
        final Selector selector;
        try {
            selector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }

        if (DISABLE_KEYSET_OPTIMIZATION) {
            return selector;
        }


捋一捋事件回调的顺序：

    当客户端发起连接请求时产生注册事件，回调ChannelInitializer的channelRegistered方法，将ServerBootstrapAcceptor添加到NioServerSocketChannel的ChannelPipeline上，接着回调ServerBootstrapAcceptor的channelRead方法，其中传递的msg就是服务端等待连接后生成的Channel实例，可以向下转型为NioSocketChannel，在channelRead方法中将ServerBootstrap设置的childHandler添加到该Channel的ChannelPipeline，接着就是触发添加的childHandler的回调方法了。

`step1`:
`ServerBootstrap.init`方法：

    p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new ServerBootstrapAcceptor(currentChildHandler, currentChildOptions,
                        currentChildAttrs));
            }
        });

服务端主动触发注册事件，执行上面的`initChannel`方法，将`ServerBootstrapAcceptor`注册到`NioServerSocketChannel`的`ChannelPipeline`上

`step2`:
接收到客户端的连接，会触发`ServerBootstrapAcceptor`的`channelRead`回调，该回调收到的`msg`参数实际上是服务端生成的`NioSocketChannel`，然后将`ServerBootstrap`配置的`childHandler`添加到该`NioSocketChannel`上，再次触发注册事件：

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Channel child = (Channel) msg;

            child.pipeline().addLast(childHandler);

            for (Entry<ChannelOption<?>, Object> e: childOptions) {
                try {
                    if (!child.config().setOption((ChannelOption<Object>) e.getKey(), e.getValue())) {
                        logger.warn("Unknown channel option: " + e);
                    }
                } catch (Throwable t) {
                    logger.warn("Failed to set a channel option: " + child, t);
                }
            }

            for (Entry<AttributeKey<?>, Object> e: childAttrs) {
                child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }

            child.unsafe().register(child.newPromise());
        }

`step3`:
接下来就是服务端`ServerBootstrap`配置的`childHandler`执行自己实现的回调函数，例如`channelRead`读取客户端消息。

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("receive message from client : " + ctx.channel().remoteAddress());
            ((ByteBuf) msg).release();
            ByteBuf buf = ctx.alloc().buffer(retVal.length());
            buf.writeBytes(retVal.getBytes());
            ctx.writeAndFlush(buf);
        }

  [1]: https://7n.w3cschool.cn/attachments/image/20170808/1502159113476213.jpg
  [2]: https://7n.w3cschool.cn/attachments/image/20170808/1502159260674064.jpg