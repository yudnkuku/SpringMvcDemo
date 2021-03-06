﻿# Netty实战

标签（空格分隔）： 读书笔记

---

## ChannelFuture ##

`channel`的每个`outbound I/O`操作都会返回一个`ChannelFuture`，`ChannelFuture`继承自`Future`，此`Future`接口继承自`juc`包中的`Future`接口，下面看看`Channel`中的一些`outbound`方法

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
    
1、异步连接到远程对端节点，调用立即返回`ChannelFuture`实例
2、操作完成后注册一个`ChannelFutureListener`
3、当操作成功后
4、创建一个`ByteBuf`来保存数据
5、异步写数据，并返回`ChannelFuture`
6、如果操作失败，打印错误原因

`ChannelFuture`表示异步`IO`操作的结果，`Netty`中所有的`IO`操作都是异步进行的，你可以添加`ChannelFutureListener`监听`IO`操作是否完成。

`ChannelFuture`的状态要么是`completed`或者`uncompleted`，如果`IO`操作完成（成功、失败或者是取消），那么返回的`ChannelFuture`实例被标记为完成并携带更多的信息，例如失败的原因等，下面是状态映射表：

    Uncompleted:               isDone() = false
                               isSuccess() = false
                               isCancelled() = false
                               cause() = null
                 
    Completed Successfully:    isDone() = true
                               isSuccess() = true
    Completed with failure:    isDone() = true
                               isSuccess() = false
                               cause()  = non-null
    Completed by cancellation: isDone() = true
                               isCancelled() = true

在等待`IO`操作完成时推荐使用`addListener`方法而不要使用`await`方法，因为`addListener`方法是非阻塞的，只是简单地将`ChannelFutureListener`监听添加到`ChannelFuture`，`IO`线程会在相关的`IO`操作完成后通知监听，`ChannelFutureListener`由于其非阻塞的特性具备良好的性能和资源利用，但是如果不适用事件驱动模型编程很难实现线性逻辑，相反，`await`是一个阻塞操作，调用线程会一直阻塞直到操作完成，尽管`await`方法很容易实现线性逻辑，但是调用线程会出现阻塞，在某些情况下还可能出现死锁，另外千万不要在`ChannelHandler`中调用`await`，因为`ChannelHandler`中的方法通常是在`IO`线程中调用，`await`会阻塞其正在等待的`IO`操作(等待`IO`操作完成，又阻塞了该`IO`线程，形成死锁)，也就是出现了死锁，如果在`IO`线程中调用`await`方法会抛出`BlockingOperationException`异常

    //Bad 
    public void channelRead(ChannelHandlerContext ctx, Message msg) {
        ChannelFuture future = ctx.channel().close();
        future.awaitUninterruptibly(); //不要在ChannelHandler中调用await方法
    }
    
    //Good
    public void channelRead(ChannelHandlerContext ctx, Message msg) {
        ChannelFuture future = ctx.channel().close();
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                //Perform post-closure operation
            }
        })
    }

看一下`DefaultPromise.await`的实现：

    public Promise<V> await() throws InterruptedException {
        //如果已经完成直接返回
        if (isDone()) {
            return this;
        }
        
        //收到中断请求，抛出中断异常
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        synchronized (this) {   //获取对象锁
            while (!isDone()) {
                //checkDeadLock会检查是否出现死锁
                checkDeadLock();
                incWaiters();
                try {
                    wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }
    //checkDeadLock
    protected void checkDeadLock() {
        //判断当前线程是否是IO线程，如果是则抛出BlockingOperationException
        EventExecutor e = executor();
        if (e != null && e.inEventLoop()) {
            throw new BlockingOperationException(toString());
        }
    }

另外，不要将`IO`超时时间和`await`超时时间混淆，如果`IO`超时时间到了，`future`返回结果是`completed with failure`，`IO`超时时间应该通过传输参数来配置：

    Bootstrap b = new Bootstrap();
    b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
    ChannelFuture future = b.connect();
    f.awaitUninterruptibly();
    if (f.isCancelled()) {
        //取消
    } else if (!future.isSuccess()) {
        future.cause().printStackTrace();
    } else {
        //成功
    }

## 事件流 ##

`Netty`将事件分为`inbound`和`outbound`两类事件，在`Channel`进行`I/O`相关操作时会触发事件，这些事件会在`ChannelPipeline`中传递，并交给对应的`ChannelHandler`处理。其内部的事件流如下所示：

![Netty Event Flow][1]

`Inbound`事件从左到右由`ChannelHandler`处理，`Inbound`事件通常由`IO`线程触发(**类似于自动触发**)，当`Channel`的状态发生改变时(例如新建立连接或者关闭连接或者从对端读取数据)都会唤醒`ChannelHandler`调用相关代码处理，如果`Inbound`事件向右到达了`TailHandler`，那么根据你的日志等级会选择丢弃或者存入日志。例如`DefaultChannelPipeline.TailHandler`的`channelRead`实现：

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                //将消息存入日志
                logger.debug(
                        "Discarded inbound message {} that reached at the tail of the pipeline. " +
                                "Please check your pipeline configuration.", msg);
            } finally {
                //释放？
                ReferenceCountUtil.release(msg);
            }
        }

而`Outbound`事件则是从右向左传递，`Outbound`事件通常是由用户请求`IO`操作的代码触发(**用户代码触发**)，例如写请求或者连接请求，如果`Outbound`事件最后到达了`HeadHandler`，那么会由`Channel`相关的`IO`线程来处理该事件，`IO`线程会调用底层的`Unsafe`类执行相关的操作，例如`DefaultChannelPipeline.HeadHandler`中的`write`方法：

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            //调用Channel相关的Unsafe类进行操作，在IO线程中执行
            unsafe.write(msg, promise);
        }
        
        //AbstractChannel$AbstractUnsafe
        @Override
        public void write(Object msg, ChannelPromise promise) {
            if (!isActive()) {
                // Mark the write request as failure if the channel is inactive.
                if (isOpen()) {
                    promise.tryFailure(NOT_YET_CONNECTED_EXCEPTION);
                } else {
                    promise.tryFailure(CLOSED_CHANNEL_EXCEPTION);
                }
                // release message now to prevent resource-leak
                ReferenceCountUtil.release(msg);
            } else {
                outboundBuffer.addMessage(msg, promise);
            }
        }

常见的`inbound`事件传播方法(基于`ChannelHandlerContext`)：

    fireChannelRegistered/fireChannelActive/fireChannelRead/fireChannelReadComplete/fireExceptionCaught/fireUserEventTriggered/fireChannelWritabilityChanged/fireChannelInactive

这些传播方法会继续传播事件，触发下一个能够处理该事件的`ChannelHandler`，例如对于`ctx.fireChannelRegistered`方法(`DefaultChannelHandlerContext`)：

    public ChannelHandlerContext fireChannelRegistered() {
        DefaultChannelHandlerContext next = findContextInbound(MASK_CHANNEL_REGISTERED);    //在ChannelPipeline上找下一个能够处理注册事件的ChannelHandlerContext(其内部封装了对应的ChannelHandler)
        next.invoker.invokeChannelRegistered(next); //调用ChannelHandler的channelRegistered方法
        return this;
    }
    
    //DefaultChannelHandlerInvoker
    public void invokeChannelRegistered(final ChannelHandlerContext ctx) {
        if (executor.inEventLoop()) {   //如果当前线程是IO线程，那么直接执行
            invokeChannelRegisteredNow(ctx);
        } else {
            executor.execute(new Runnable() {   //否则开启新的线程执行任务
                @Override
                public void run() {
                    invokeChannelRegisteredNow(ctx);
                }
            });
        }
    }
    
其他事件的传播方法同理。

常见的`outbound`事件的传播方法如下(基于`ChannelHandlerContext`)：

    bind/connect/write/flush/read/disconnect/close
    
这些事件都是由用户代码主动触发的，例如绑定端口、建立连接、写数据、读数据、关闭连接等等，例如`MessageToMessageEncoder`，将消息编码并写入`outbuffer`中，调用`ctx.write`方法，进一步会触发`pipeline`中能够处理`write`事件(`outbound`事件)的`ChannelHandler`，最后会由`DefaultChannelPipeline.HeadHandler`处理：

        public void write(Object msg, ChannelPromise promise) {
            if (!isActive()) {
                // Mark the write request as failure if the channel is inactive.
                if (isOpen()) {
                    promise.tryFailure(NOT_YET_CONNECTED_EXCEPTION);
                } else {
                    promise.tryFailure(CLOSED_CHANNEL_EXCEPTION);
                }
                // release message now to prevent resource-leak
                ReferenceCountUtil.release(msg);
            } else {
                outboundBuffer.addMessage(msg, promise);
            }
        }


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

在`ServerBoostrap`中初始化`Channel`的时候会构造`ChannelInitializer`实例添加到`Channel`的`ChannelPipeline`中：

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new ServerBootstrapAcceptor(currentChildHandler, currentChildOptions,
                        currentChildAttrs));    //构造ServerBoostrapAcceptor加入ChannelPipeline
            }
        });

## ServerBootstrap和Bootstrap ##

两者都用来引导`Netty`的本地配置。
`ServerBootstrap`用于服务端，绑定本地端口，可以声明两个`EventLoopGroup`，一个用来处理客户端的连接请求(`Acceptor`线程池,还未创建`Channel`)，一个用来处理`I/O`操作(处理即将创建的`Channel`的所有事件)。
`Bootstrap`用于客户端，连接到远程主机和端口，只能声明一个`EventLoopGroup`用于处理`Channel`的所有事件。

![ServerBootsrap和Bootstrap][2]

如何理解上面的图示，其实可以从源码入手：

    //构造NioEventLoopGroup实例，这个类可以认为是NioEventLoop的集合，默认数量是处理器核心*2，这里的两个线程组就对应图中左右两个EventLoop集合
    NioEventLoopGroup boss = new NioEventLoopGroup()
    NioEventLoopGroup worker = new NioEventLoopGroup()
    
    //构造ServerBootstrap实例，启动ServerChannel
    ServerBoostrap b = new ServerBootstrap()
    
    //设置EventLoop线程组，boss是ServerChannel处理线程组(接受连接)，worker是Channel处理线程组(IO网络操作，读写消息)
    b.group(boss, worker)
    
    //构造ServerChannel并初始化
    Channel createChannel() {
        EventLoop eventLoop = group().next();
        return channelFactory().newChannel(eventLoop, childGroup);  //反射初始化，传入EventLoop和childGroup(这个线程组用来处理接收连接后创建的SocketChannel网络IO事件)
    }
    
    //初始化操作就不细说了，最主要的是将ServerBootstrapAcceptor添加到Channel的pipeline中
    p.addLast(new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(new ServerBootstrapAcceptor(currentChildHandler, currentChildOptions,
                    currentChildAttrs));
        }
    });


那么服务端`NioServerSocketChannel`初始化后，如何接收客户端连接呢，在`NioEventLoop`中，当服务端`ServerChannel`完成初始化之后，进行`selector`的注册：
    
    //AbstractChannel.AbstractUnsafe
    public final void register(final ChannelPromise promise) {
            if (eventLoop.inEventLoop()) {  //当前线程是否是IO线程
                register0(promise); //如果是，直接执行注册
            } else {
                try {
                    eventLoop.execute(new Runnable() {  //否则，调用execute方法
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
    
    //SingleThreadEventExecutor
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }

        boolean inEventLoop = inEventLoop();
        if (inEventLoop) {  //如果当前线程是IO线程，那么将任务加入任务队列
            addTask(task);
        } else {    //否则，开启新的线程
            startThread();
            addTask(task);
            if (isShutdown() && removeTask(task)) {
                reject();
            }
        }

        if (!addTaskWakesUp) {
            wakeup(inEventLoop);
        }
    }
    
    //开启新的线程后会执行SingleThreadEventExecutor.this.run()方法， 进入NioEventLoop.run()方法，这个方法就是NioEventLoop的核心了，它会不断地检查IO事件和执行任务，会为每个接收的连接构造一个新的NioSocketChannel，并绑定EventLoop：
    //NioServerSocketChannel
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = javaChannel().accept();

        try {
            if (ch != null) {
                buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch)); //childEventLoopGroup就是b.group(boss, worker)传入的第二个参数
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }
    

## ByteBuf ##

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

## ChannelHandler/ChannelPipeline/ChannelHandlerContext ##

`ChannelHandler`用来处理`IO`事件或者拦截`IO`操作，并将这些事件或者操作传递给`ChannelPipeline`中的下一个`ChannelHandler`，`Netty`提供了其抽象实现类`ChannelHandlerAdapter`，一般在实际编程过程中也会直接继承`ChannelHandlerAdapter`，而不是去实现`ChannelHandler`，`ChannelHandler`是通过`ChannelHandlerContext`对象来提供(可以查看`DefaultChannelHandlerContext`的构造方法)，也是通过这个`context`对象来和它从属的`pipeline`进行交互，`ChannelHandler`可以向上或向下传递事件，动态地改变`pipeline`

`ChannelHandler`提供的方法：

第一类：生命周期方法

1、`handlerAdded`：当`handler`添加进`pipeline`后调用

2、`handlerRemoved`:当`handler`从`pipeline`移除时会调用

第二类：`Inbound`(入站)事件

1、`exceptionCaught`:当抛出异常时调用

2、`channelRegistered`:当`Channel`注册到`EventLoop`关联的`Selector`上时调用

3、`channelActive`:当`Channel`绑定端口后调用

4、`channelInactive`:当`Channel`解绑端口后调用

5、`channelRead`:当从对端读取数据后调用

6、`channelReadComplete`:读取完成后调用

第三类：`Outbound`(出站)事件

1、`bind`:绑定

2、`connect`:连接

3、`disconnect`:断开连接

4、`close`:关闭

5、`read`:读数据

6、`write`:写数据

7、`flush`:清空缓冲区数据

这些方法都不会是自动调用，而是通过`ChannelPipeline`的各个对应的方法调用，可以看看`ChannelPipeline`提供的方法

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

**ChannelPipeline源码分析**

在通过`ServerBootstrap`引导启动服务端时，会构建一个`NioServerSocketChannel`实例，该实例最顶层父类是`AbstractChannel`，可以看下其构造方法代码：

    protected AbstractChannel(Channel parent, EventLoop eventLoop) {
        this.parent = parent;
        this.eventLoop = validate(eventLoop);
        //构造NioMessageUnsafe实例
        unsafe = newUnsafe();
        //构造ChannelPipeline实例
        pipeline = new DefaultChannelPipeline(this);
    }

`DefaultChannelPipeline`构造方法：

    public DefaultChannelPipeline(AbstractChannel channel) {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        this.channel = channel;
        
        //构造TailHandler，处理channelRead事件，什么都不做，只是打印达到尾部日志    
        TailHandler tailHandler = new TailHandler();
        tail = new DefaultChannelHandlerContext(this, null, generateName(tailHandler), tailHandler);
        //构造HeadHandler
        HeadHandler headHandler = new HeadHandler(channel.unsafe());
        head = new DefaultChannelHandlerContext(this, null, generateName(headHandler), headHandler);
        //构建链式结构
        head.next = tail;
        tail.prev = head;
    }

**ChannelPipeline触发事件**

例如触发`Inbound`事件，`Inbound`事件一般从`head`开始往后遍历`pipeline`，`DefaultChannelPipeline.fireChannelRegistered()`代码：

    public ChannelPipeline fireChannelRegistered() {
        //
        head.fireChannelRegistered();
        return this;
    }
    
继续跟进去`DefaultChannelHandlerContext.fireChannelRegistered()`，

    public ChannelHandlerContext fireChannelRegistered() {
        DefaultChannelHandlerContext next = findContextInbound(MASK_CHANNEL_REGISTERED);
        next.invoker.invokeChannelRegistered(next);
        return this;
    }

继续`findContextInbound`方法：

    private DefaultChannelHandlerContext findContextInbound(int mask) {
        DefaultChannelHandlerContext ctx = this;
        //从head节点往后遍历，直到ChannelHandlerContext的skipFlags&mask == 0
        do {
            ctx = ctx.next;
        } while ((ctx.skipFlags & mask) != 0);
        return ctx;
    }

`skipFlags`如何计算的，通过`DefaultChannelHandlerContext.skipFlags()`方法：

    private static int skipFlags(ChannelHandler handler) {
        //使用了缓存，skipFlagsCache是缓存数组，每个数组元素都是一个WeakHashMap，key是ChannelHandler的class对象，value是对应的skipFlag
        WeakHashMap<Class<?>, Integer> cache =
                skipFlagsCache[(int) (Thread.currentThread().getId() % skipFlagsCache.length)];
        Class<? extends ChannelHandler> handlerType = handler.getClass();
        int flagsVal;
        //获取cache的对象锁，这里为什么要用锁
        synchronized (cache) {
            Integer flags = cache.get(handlerType);
            if (flags != null) {
                flagsVal = flags;
            } else {
                flagsVal = skipFlags0(handlerType);
                cache.put(handlerType, Integer.valueOf(flagsVal));
            }
        }

        return flagsVal;
    }

继续查看`skipFlags0()`方法：

    private static int skipFlags0(Class<? extends ChannelHandler> handlerType) {
        int flags = 0;
        try {
            //通过反射判断ChannelHandler的事件回调方法是否注解有`Skip`，如果注解则将其和掩码相或，那么在上面的pipeline遍历时，只需要判断skipFlag&mask是否等于0，如果等于0表示没有注解Skip，否则注解了Skip，表示该ChannelHandler在该事件的处理中会跳过
            if (handlerType.getMethod(
                    "handlerAdded", ChannelHandlerContext.class).isAnnotationPresent(Skip.class)) {
                flags |= MASK_HANDLER_ADDED;
            }
            if (handlerType.getMethod(
                    "handlerRemoved", ChannelHandlerContext.class).isAnnotationPresent(Skip.class)) {
                flags |= MASK_HANDLER_REMOVED;
            }
            
`ChannelPipeline`类图

![ChannelPipeline][3]

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

## 线程模型 ##
1、单线程模型

首先看一段代码：

    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup)
     .channel(NioServerSocketChannel.class)
     ...

上述代码就是一个典型的单线程模型，即`acceptor`处理和`handler`处理都在同一个线程中进行。
    
    //实际上设置了parentGroup=childGroup
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }
    
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }

2、多线程模型

先看一段代码：

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup) //设置了bossGroup&workerGroup
     .channel(NioServerSocketChannel.class)
     ...

分别设置了`bossGroup`和`workerGroup`线程组，也就是说将`acceptor`和`handler`两个处理过程分配到两个线程中进行

## 线程模型源码分析 ##
服务端在启动时，创建了两个`NioEventLoopGroup`，它们实际上是两个独立的`Reactor`线程池，一个用于接收客户端的`TCP`连接，另一个用于处理`I/O`相关的读写操作，或者执行系统`Task`、定时任务`Task`。
`Netty`的`NioEventLoop`并不是一个纯粹的`I/O`线程，它除了负责`I/O`的读写之外，还兼顾处理以下两类任务。
1、系统`Task`：通过调用`NioEventLoop`的`execute(Runnable task)`方法实现，`Netty`有很多系统`Task`，创建它们的主要原因是：当`I/O`线程和用户线程同时操作网络资源时，**为了防止并发操作导致的锁竞争，将用户线程的操作封装成`Task`放入任务队列中，由`I/O`线程负责执行，这样就实现了局部无锁化**。
2、定时任务：通过调用`NioEventLoop`的`schedule(Runnble command, long  delay, TimeUnit unit)`方法实现

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
    //nThreads=0，使用DEFAULT_EVENT_LOOP_THREADS，默认是处理器核心的2倍
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
        //调用NioServerSocketChannel底层的注册方法，并fireChannelRegistered()
        channel.unsafe().register(regFuture);   //(3)
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }

        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    i.e. It's safe to attempt bind() or connect() now beause the channel has been registered.
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    i.e. It's safe to attempt bind() or connect() now:
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         because register(), bind(), and connect() are all bound to the same thread.
        //上面这段话意思是：如果Channel的注册是在event loop线程中(eventLoop.inEventLoop()方法返回true)执行，那么到此时注册过程已经完成；否则如果是在其他线程中进行Channel的注册，那么注册请求会被封装成任务添加到event loop的任务队列taskQueue中等待后续在event loop相关的线程中执行该任务，因此此时尝试bind或者connect操作是安全的，因为这些操作全部都绑定到某个相同的线程中，并且会顺序执行(register->bind(connect))。注册过程参考(3)的源码解析
        return regFuture;
    }
        
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
                    //调用execute执行Runnable，这里会进入SingleThreadEventLoopExecutor中
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
            //如果当前线程就是NioEventLoop所属线程(NioEventLoop有个内置thread属性)，直接将任务添加到任务队列中
            addTask(task);
        } else {
            //否则执行这里
            startThread();  //(1)开启一个新的线程执行NioEventLoop中的run()方法
            addTask(task);  //(2)将其他任务添加到任务队列
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
        //executor是在构造NioEventLoop传入的参数，它是ThreadPerTaskExecutor，会为每一个Runnable任务创建一个线程执行，下面的代码相当于开启一个单独的线程执行run()方法
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
                    SingleThreadEventExecutor.this.run();   //此处会跳转到NioEventLoop的run()方法
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
                    //fireChannelActive，设置channel关联的selector感兴趣的ops
                    pipeline.fireChannelActive();
                }

`doRegister()`，：

    protected void doRegister() throws Exception {
        boolean selected = false;
        //死循环
        for (;;) {
            try {
                //将selector注册到channel，selector是在NioEventLoop构造时通过openSelector方法生成的，注意这里的attachment就是当前NioServerSocketChannel实例，ops参数传的是0，可见注册时只是初始化了ops，而真正设置ops是在后面的pipeline.fireChannelActive方法中
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

    首先调用方法initAndRegister初始化并注册NioServerSocketChannel，通过反射构造NioServerSocketChannel实例，再添加ChannelInitializer响应注册事件，再进入注册流程，首先通过代码channel.unsafe().register(regFuture)手动注册NioServerSocketChannel，注册完后会触发pipeline.fireChannelRegistered()方法，触发该NioServerSocketChannel的channelRegistered()回调，将ServerBootstrapAcceptor添加到该NioServerSocketChannel的ChannelPipeline上，接着IO线程不断轮询NioServerSocketChannel相关的selector是否有关心的IO操作，例如客户端的连接请求，当有客户端的连接请求时，会调用unsafe.read()方法读取客户端消息(客户端连接)，在这过程中会实例化NioSocketChannel，并触发pipeline.fireChannelRead(msg)方法，并将该NioSocketChannel作为msg参数传入，接着会调用ServerBootstrapAcceptor的channelRead方法，其中传递的msg就是服务端等待连接后生成的NioSocketChannel实例，在channelRead回调方法中将ServerBootstrap设置的childHandler添加到该NioSocketChannel的ChannelPipeline，该NioSocketChannel只对读事件感兴趣，同时会在该channelRead回调最后调用child.unsafe().register()方法，开启单独的线程执行NioEventLoop的run方法，接下来就可以通过selector的轮询来读取客户端发送的字节消息
    
`step1`:
`AbstractBootstrap.initAndRegister()`方法：

    final ChannelFuture initAndRegister() {
        Channel channel;
        try {
            //通过反射构建NioServerSocketChannel实例
            channel = createChannel();
        } catch (Throwable t) {
            return VoidChannel.INSTANCE.newFailedFuture(t);
        }

        try {
            //初始化channel，这个方法中就已经将ServerBootstrapAcceptor通过ChannelInitializer添加到NioServerSocketChannel的pipeline，ServerBootstrapAcceptor中定义了channelRead回调方法，读取的msg就是accept到的客户端连接通道，具体可以debug
            init(channel);
        } catch (Throwable t) {
            channel.unsafe().closeForcibly();
            return channel.newFailedFuture(t);
        }

        ChannelPromise regFuture = channel.newPromise();
        //调用channel.unsafe().register()注册，这里会触发NioServerSocketChannel的channelRegistered事件，会将ServerBootstrapAcceptor添加到NioServerSocketChannel的pipeline中
        channel.unsafe().register(regFuture);
        ...   
    }
    
    //AbstractUnsafe.register()方法
    public final void register(final ChannelPromise promise) {
            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    //将register0封装成task放在IO线程中执行
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } 
    //register0方法    
    private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // call was outside of the eventLoop
                if (!ensureOpen(promise)) {
                    return;
                }
                //核心是调用doRegister()方法(1)
                doRegister();
                registered = true;
                promise.setSuccess();
                //注册完后调用pipeline.fireChannelRegistered()方法
                pipeline.fireChannelRegistered();
                if (isActive()) {
                   //调用pipeline.fireChannelActive方法，这里会最终进入到AbstractNioChannel.doBeginRead方法中，修改该channel的selector感兴趣的io操作(2)
                   pipeline.fireChannelActive();   
                }
            }
    
    (1)//AbstractNioChannel.doRegister()方法
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                selectionKey = javaChannel().register(eventLoop().selector, 0, this);
                return;
            } 
    
    (2)//AbstractNioChannel.doBeginRead方法
    @Override
    protected void doBeginRead() throws Exception {
        if (inputShutdown) {
            return;
        }

        final SelectionKey selectionKey = this.selectionKey;
        if (!selectionKey.isValid()) {
            return;
        }

        final int interestOps = selectionKey.interestOps();
        if ((interestOps & readInterestOp) == 0) {
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }
            
`step2`：`boss`线程会不断轮询`NioServerSocketChannel`相关的`selector`是否有关心的操作(客户端的连接请求)，此轮询过程在`NioEventLoop`的`run`方法中，在死循环中不断轮询，最后进入方法`processSelectedKey`：

    private static void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        final NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {
            // close the channel if the key is not valid anymore
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            int readyOps = k.readyOps();
            //如果准备好读或者接收客户端连接
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                //调用unsafe.read()方法，这里会有两种不同的实现，NioServerSocketChannel对应NioMessageUnsafe，NioSocketChannel对应NioByteUnsafe
                unsafe.read();
                if (!ch.isOpen()) {
                    // Connection already closed - no need to handle write.
                    return;
                }
            }
    
    //对于NioServerSocketChannel进入NioMessageUnsafe.read方法，其核心就是doReadMessages方法，核心代码如下：
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = javaChannel().accept();

        try {
            if (ch != null) {
                //构建NioSocketChannel实例，添加到buf中
                buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));
                return 1;
            }
        }
    
    //在NioMessageUnsafe.read方法中调用fireChannelRead方法：
    int size = readBuf.size();
    for (int i = 0; i < size; i ++) {
        //上面的buf里保存的内容作为参数msg传递给pipeline.fireChannelRead()
        pipeline.fireChannelRead(readBuf.get(i));
    }
    
`step2`:接着上面会进入`ServerBootstrapAcceptor.channelRead()`
`ServerBootstrapAcceptor.channelRead()`方法：

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //msg实际上是accept生成的Channel实例，即NioSocketChannel
            Channel child = (Channel) msg;
            
            //将childHandler添加到该Channel的pipeline
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
            //调用unsafe的注册方法，将register0封装成task，会开启单独的线程执行NioEventLoop的run()方法，接下来就可以接收客户端发来的消息数据
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


## NioEventLoop源码解析 ##
`NioEventLoop`内部维护了一个线程(线程的实例化在`ThreadPerTaskExecutor`类中)，线程启动时会调用`NioEventLoop`的`run`方法，执行`I/O`任务和非`I/O`任务

1、`I/O`任务即`selectionKey`中`ready`的事件，如`accept`、`connect`、`read`、`write`等，由`processSelectedKeysOptimized`或者`processSelectedKeysPlain`方法触发

2、非`I/O`任务则为添加到`taskQueue`中的任务，如`register0`、`bind0`等任务，由`runAllTasks`方法触发

3、两种任务的执行时间由变量`ioRatio`控制，默认值是50，则表示允许非`IO`任务执行的时间和`IO`任务执行的事件相等

这里解读`NioEventLoop`中的`run`方法，该方法会在一个单独的线程中运行(具体参考`ThreadPerTaskExecutor`类，在`MultithreadEventExecutorGroup`(`NioEventLoopGroup`的父类)中会维护一个`NioEventLoop`类型的`children`数组)

    protected void run() {
        for (;;) {
            oldWakenUp = wakenUp.getAndSet(false);
            try {
                if (hasTasks()) {
                    //如果任务队列中有任务，那么执行selectNow()方法，该方法会立即返回
                    selectNow();
                } else {
                    //否则执行select()方法
                    select();
                    if (wakenUp.get()) {
                        selector.wakeup();
                    }
                }

                cancelledKeys = 0;
                //开始执行I/O操作
                final long ioStartTime = System.nanoTime();
                needsToSelectAgain = false;
                if (selectedKeys != null) {
                    processSelectedKeysOptimized(selectedKeys.flip());
                } else {
                    processSelectedKeysPlain(selector.selectedKeys());
                }
                //统计I/O操作时间
                final long ioTime = System.nanoTime() - ioStartTime;
                //根据ioRatio计算其他任务执行时间
                final int ioRatio = this.ioRatio;
                //调用runAllTasks()执行其他任务，例如注册、绑定
                runAllTasks(ioTime * (100 - ioRatio) / ioRatio);

                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        break;
                    }
                }
            } catch (Throwable t) {
                logger.warn("Unexpected exception in the selector loop.", t);

                // Prevent possible consecutive immediate failures that lead to
                // excessive CPU consumption.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
        }
    }
    
    
`select()`方法：

    private void select() throws IOException {
        Selector selector = this.selector;
        try {
            int selectCnt = 0;
            long currentTimeNanos = System.nanoTime();
            long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);
            for (;;) {
                long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
                if (timeoutMillis <= 0) {
                    if (selectCnt == 0) {
                        selector.selectNow();
                        selectCnt = 1;
                    }
                    break;
                }

                int selectedKeys = selector.select(timeoutMillis);
                selectCnt ++;

                if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks()) {
                    // Selected something,
                    // waken up by user, or
                    // the task queue has a pending task.
                    break;
                }

                if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                        selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                    // The selector returned prematurely many times in a row.
                    // Rebuild the selector to work around the problem.
                    logger.warn(
                            "Selector.select() returned prematurely {} times in a row; rebuilding selector.",
                            selectCnt);

                    rebuildSelector();
                    selector = this.selector;

                    // Select again to populate selectedKeys.
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                currentTimeNanos = System.nanoTime();
            }

            if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row.", selectCnt - 1);
                }
            }
        } catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector - JDK bug?", e);
            }
            // Harmless exception - log anyway
        }
    }

如果触发了`epoll cpu 100%`的`bug`，那么`selector.select(timeoutMills)`会立即返回，不会阻塞`timeoutMills`，变量`selectCnt`会逐渐变大，当达到阈值`512`时，会触发`rebuildSelector()`方法重建`selector`
`rebuildSelector()`方法：

    public void rebuildSelector() {
        if (!inEventLoop()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector();
                }
            });
            return;
        }

        final Selector oldSelector = selector;
        final Selector newSelector;

        if (oldSelector == null) {
            return;
        }

        try {
            //重新新建一个selector
            newSelector = openSelector();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector. 注册
        int nChannels = 0;
        for (;;) {
            try {
                for (SelectionKey key: oldSelector.keys()) {
                    Object a = key.attachment();
                    try {
                        if (key.channel().keyFor(newSelector) != null) {
                            continue;
                        }
                        //获取感兴趣的操作
                        int interestOps = key.interestOps();
                        //取消channel和selector的注册关系
                        key.cancel();
                        //重新注册channel到新的selector    
                       key.channel().register(newSelector, interestOps, a);
                        nChannels ++;
                    } catch (Exception e) {
                        logger.warn("Failed to re-register a Channel to the new Selector.", e);
                        if (a instanceof AbstractNioChannel) {
                            AbstractNioChannel ch = (AbstractNioChannel) a;
                            ch.unsafe().close(ch.unsafe().voidPromise());
                        } else {
                            @SuppressWarnings("unchecked")
                            NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                            invokeChannelUnregistered(task, key, e);
                        }
                    }
                }
            } catch (ConcurrentModificationException e) {
                // Probably due to concurrent modification of the key set.
                continue;
            }

            break;
        }

        selector = newSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
    }
总结`rebuildSelector()`过程：

1、通过方法`openSelector`创建一个新的`new selector`

2、遍历`old selector`关联的`selectionKey`，调用`key.cancel()`取消注旧的注册关系，将`channel`重新注册到新的`new selector`

3、将内部的`selector`更新为新的`new selector`，调用`oldSelector.close()`方法关闭`old selector`

接着继续执行`I/O`操作和其他任务：
        
        //开始执行I/O操作
        final long ioStartTime = System.nanoTime();
        needsToSelectAgain = false;
        if (selectedKeys != null) {
            processSelectedKeysOptimized(selectedKeys.flip());
        } else {
            processSelectedKeysPlain(selector.selectedKeys());
        }
        final long ioTime = System.nanoTime() - ioStartTime;

        final int ioRatio = this.ioRatio;
        //执行其他任务，例如register0和bind0
        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);

        if (isShuttingDown()) {
            closeAll();
            if (confirmShutdown()) {
                break;
            }
        }
看一下`processSelectedKeysPlain`方法：

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();
            //这里为什么和SelectionKey关联的attachment为什么是AbstractNioChannel类型？
            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

原因可以在初始化注册时找到，`AbstractNioChannel.doRegister()`方法：

    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
                //这里将NioServerSocketChannel注册到NioEventLoop内部的selector，并将该Channel作为attachment，所以上面的attachment实际上是NioServerSocketChannel类型，其父类就是AbstractNioChannel
                selectionKey = javaChannel().register(eventLoop().selector, 0, this);
                return;
                ...
            }
        }

`processSelectedKeysOptimized()`和`processSelectedKeysPlain`方法都会使用到一个公共的方法-`processSelectedKey`:
    
    private static void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        //这里实际上是NioMessageUnsafe
        final NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {
            // close the channel if the key is not valid anymore
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            //获取SelectionKey
            int readyOps = k.readyOps();
            // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
            // to a spin loop
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();  //注意这里，当通道有数据读取时，会调用unsafe.read()读数据
                if (!ch.isOpen()) {
                    // Connection already closed - no need to handle write.
                    return;
                }
            }
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
                ch.unsafe().forceFlush();
            }
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);

                unsafe.finishConnect();
            }
        } catch (CancelledKeyException e) {
            unsafe.close(unsafe.voidPromise());
        }
    }

查看`unsafe.read()`方法，进入`NioMessageUnsafe`，查看核心代码：

        try {
                for (;;) {
                    //调用doReadMessages()，将消息读取到readBuf中
                    int localRead = doReadMessages(readBuf);
                    if (localRead == 0) {
                        break;
                    }
                    if (localRead < 0) {
                        closed = true;
                        break;
                    }

                    if (readBuf.size() >= maxMessagesPerRead | !autoRead) {
                        break;
                    }
                }
            } catch (Throwable t) {
                exception = t;
            }

            int size = readBuf.size();
            //遍历readBuf，触发channelRead事件
            for (int i = 0; i < size; i ++) {
                pipeline.fireChannelRead(readBuf.get(i));
            }

进入`NioServerSocketChannel`的`doReadMessages()`方法：

    protected int doReadMessages(List<Object> buf) throws Exception {
        //调用accept()方法接收客户端连接，返回SocketChannel实例
        SocketChannel ch = javaChannel().accept();

        try {
            if (ch != null) {
                //构建一个NioSocketChannel实例，添加到readBuf中，传递给ServerBootstrapAcceptor中的channelRead()回调方法
                buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));
                return 1;
            }
        }

看下`NioSocketChannel`的构造方法：

    public NioSocketChannel(Channel parent, EventLoop eventLoop, SocketChannel socket) {
        super(parent, eventLoop, socket);
        config = new DefaultSocketChannelConfig(this, socket.socket());
    }
    
    //这里是AbstractNioByteChannel，和NioServerSocketChannel的父类AbstractNioMessageChannel不同
    protected AbstractNioByteChannel(Channel parent, EventLoop eventLoop, SelectableChannel ch) {
        //只对读事件感兴趣
        super(parent, eventLoop, ch, SelectionKey.OP_READ);
    }
    
    //后面的流程和NioServerSocketChannel一样了，只是在下面的代码中会存在多态情况：
    protected AbstractChannel(Channel parent, EventLoop eventLoop) {
        this.parent = parent;
        this.eventLoop = validate(eventLoop);
        unsafe = newUnsafe();   //newUnsafe()会进入AbstractNioUnsafe，返回NioByteUnsafe实例，而不是NioMessageUnsafe实例
        pipeline = new DefaultChannelPipeline(this);
    }
    
    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }
    
**NioServerSocketChannel & NioSocketChannel**

![NioServerSocketChannel类图][4]

![NioSocketChannel类图][5]

可以看到`NioServerSocketChannel`继承自`AbstractNioMessageChannel`，这个`channel`是用来读写消息的基类，而`NioSocketChannnel`继承自`AbstractNioByteChannel`，这个`channel`是用来读写字节的基类。

`NioServerSocketChannel`和`NioSocketChannel`在的构造方法对比：
    
    //NioServerSocketChannel
    public NioServerSocketChannel(EventLoop eventLoop, EventLoopGroup childGroup) {
        super(null, eventLoop, childGroup, newSocket(), SelectionKey.OP_ACCEPT);    //只对Accept事件有兴趣，接收客户端的连接
        config = new DefaultServerSocketChannelConfig(this, javaChannel().socket());
    }
    
    //NioSocketChannel
    public NioSocketChannel(Channel parent, EventLoop eventLoop, SocketChannel socket) {
        super(parent, eventLoop, socket);
        config = new DefaultSocketChannelConfig(this, socket.socket());
    }
    
    protected AbstractNioByteChannel(Channel parent, EventLoop eventLoop, SelectableChannel ch) {
        super(parent, eventLoop, ch, SelectionKey.OP_READ); //只对读事件有兴趣
    }
    
这两个构造方法最后都会调用顶层父类`AbstractChannel`的构造方法：

    protected AbstractChannel(Channel parent, EventLoop eventLoop) {
        this.parent = parent;
        this.eventLoop = validate(eventLoop);
        unsafe = newUnsafe();   //这里会有不同的实现
        pipeline = new DefaultChannelPipeline(this);
    }

上面的`newUnsafe()`会有不同的实现，`NioServerSocketChannel`对应`AbstractNioMessageChannel`中的`newUbsafe()`实现：

    protected AbstractNioUnsafe newUnsafe() {
        return new NioMessageUnsafe();
    }
    
`NioMessageUnsafe`的`read`方法：

    try {
                for (;;) {
                    int localRead = doReadMessages(readBuf);    //进入NioServerSocketChannel
                    if (localRead == 0) {
                        break;
                    }
                    if (localRead < 0) {
                        closed = true;
                        break;
                    }

                    if (readBuf.size() >= maxMessagesPerRead | !autoRead) {
                        break;
                    }
                }
            } catch (Throwable t) {
                exception = t;
            }

            int size = readBuf.size();
            for (int i = 0; i < size; i ++) {
                pipeline.fireChannelRead(readBuf.get(i));   //再触发通道pipeline的channelRead事件
            }

`NioServerSocketChannel`中的`doReadMessages()`方法：

        //接收客户端连接，生成SocketChannel实例
        SocketChannel ch = javaChannel().accept();

        try {
            if (ch != null) {
                buf.add(new NioSocketChannel(this, childEventLoopGroup().next(), ch));  //构造NioSocketChannel添加到buf中
                return 1;
            }
        }

`AbstractNioByteChannel`(`NioSocketChannel`的父类)中的`newUnsafe()`方法：

    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

这个类就比较简单，直接从通道中读字节数据到`buf`中，核心实现：

    protected int doReadBytes(ByteBuf byteBuf) throws Exception {
        return byteBuf.writeBytes(javaChannel(), byteBuf.writableBytes());
    }
    

## bind过程解析 ##
首先看看`AbstractBootstrap.bind`方法：

    private ChannelFuture doBind(final SocketAddress localAddress) {
        //初始化channel并注册
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        final ChannelPromise promise;
        if (regFuture.isDone()) {
            //注册完成，调用doBind0开始绑定端口
            promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            promise = new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    doBind0(regFuture, channel, localAddress, promise);
                }
            });
        }

        return promise;
    }
    
    //继续进入doBind0
    private static void doBind0(
            final ChannelFuture regFuture, final Channel channel,
            final SocketAddress localAddress, final ChannelPromise promise) {

        // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
        // the pipeline in its channelRegistered() implementation.
        //将bind封装成task执行
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                //如果注册成功
                if (regFuture.isSuccess()) {
                    channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

进入`AbstractChannel.bind`方法：

    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        //调用DefaultChannelPipeline.bind方法绑定
        return pipeline.bind(localAddress, promise);
    }

进入`DefaultChannelPipeline.bind`：
    
    //bind事件作为用户代码触发的Outbound事件，从tail向head传递
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return tail.bind(localAddress, promise);
    }

进入`DefaultChannelHandlerContext.bind`：

    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        DefaultChannelHandlerContext next = findContextOutbound(MASK_BIND);
        next.invoker.invokeBind(next, localAddress, promise);
        return promise;
    }

进入`DefaultChannelHandlerInvoker.invokeBind`方法：

    public void invokeBind(
            final ChannelHandlerContext ctx, final SocketAddress localAddress, final ChannelPromise promise) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        validatePromise(ctx, promise, false);

        if (executor.inEventLoop()) {
            invokeBindNow(ctx, localAddress, promise);
        } else {
            safeExecuteOutbound(new Runnable() {
                @Override
                public void run() {
                    invokeBindNow(ctx, localAddress, promise);
                }
            }, promise);
        }
    }
    
    //继续invokeBindNow
    public static void invokeBindNow(
            final ChannelHandlerContext ctx, final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            ctx.handler().bind(ctx, localAddress, promise);
        } catch (Throwable t) {
            notifyOutboundHandlerException(t, promise);
        }
    }

进入`HeadHandler.bind`：

    public void bind(
                ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
                throws Exception {
            unsafe.bind(localAddress, promise);
        }
        
进入`AbstractUnsafe.bind`：

    public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            if (!ensureOpen(promise)) {
                return;
            }

            boolean wasActive = isActive();
            try {
                //(1)调用NioServerSocketChannel.doBind方法
                doBind(localAddress);
            } catch (Throwable t) {
                promise.setFailure(t);
                closeIfClosed();
                return;
            }
            if (!wasActive && isActive()) {
                //(2)绑定完成后执行任务，fireChannelActive
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireChannelActive();
                    }
                });
            }
            promise.setSuccess();
        }

(1)进入`NioServerSocketChannel.doBind`方法：

    protected void doBind(SocketAddress localAddress) throws Exception {
        javaChannel().socket().bind(localAddress, config.getBacklog());
    }

(2)进入`DefaultChannelPipeline.fireChannelActive`：

    public ChannelPipeline fireChannelActive() {
        head.fireChannelActive();

        if (channel.config().isAutoRead()) {
            //调用channel.read()方法
            channel.read();
        }

        return this;
    }
    
    //AbstractChannel.read
    public Channel read() {
        pipeline.read();
        return this;
    }
    
    //DefaultChannelPiepeline.read
    public ChannelPipeline read() {
        tail.read();
        return this;
    }
    
    //DefaultChannelHandlerContext.read
    public ChannelHandlerContext read() {
        DefaultChannelHandlerContext next = findContextOutbound(MASK_READ);
        next.invoker.invokeRead(next);
        return this;
    }
    
    //DefaultChannelHandlerInvoker.invokeRead
    public void invokeRead(final ChannelHandlerContext ctx) {
        if (executor.inEventLoop()) {
            invokeReadNow(ctx);
        } else {
            DefaultChannelHandlerContext dctx = (DefaultChannelHandlerContext) ctx;
            Runnable task = dctx.invokeReadTask;
            if (task == null) {
                dctx.invokeReadTask = task = new Runnable() {
                    @Override
                    public void run() {
                        invokeReadNow(ctx);
                    }
                };
            }
            executor.execute(task);
        }
    }
    
    //invokeReadNow
    public static void invokeReadNow(final ChannelHandlerContext ctx) {
        try {
            ctx.handler().read(ctx);
        } catch (Throwable t) {
            notifyHandlerException(ctx, t);
        }
    }
    
    //HeadHandler.read
    public void read(ChannelHandlerContext ctx) {
            unsafe.beginRead();
        }
        
    //AbstractUnsafe.beginRead
    public void beginRead() {
            if (!isActive()) {
                return;
            }

            try {
                doBeginRead();
            } catch (final Exception e) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireExceptionCaught(e);
                    }
                });
                close(voidPromise());
            }
        }
        
    //进入子类AbstractNioChannel.doBeginRead
    protected void doBeginRead() throws Exception {
        if (inputShutdown) {
            return;
        }
        //this.selectionKey是在执行注册方法doRegister时构造的，如下设置了selectionKey的感兴趣操作为0
        //selectionKey = javaChannel().register(eventLoop().selector, 0, this)
        final SelectionKey selectionKey = this.selectionKey;
        if (!selectionKey.isValid()) {
            return;
        }
        //selectionKey.interestOps初始值是0
        final int interestOps = selectionKey.interestOps();
        //readInterestOp在构造Channel时就指定了，对于NioServerSocketChannel是SelectionKey.OP_ACCEPT，对于NioSocketChannel是SelectionKey.OP_READ
        if ((interestOps & readInterestOp) == 0) {
            //设置selectionKey感兴趣的操作为 interestOps | readInterestOp
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }
  [1]: https://7n.w3cschool.cn/attachments/image/20170808/1502159113476213.jpg
  [2]: https://7n.w3cschool.cn/attachments/image/20170808/1502159260674064.jpg
  [3]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/nio/ChannelPipeline.png
  [4]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/nio/NioServerSocketChannel.png
  [5]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/nio/NioSocketChannel.png