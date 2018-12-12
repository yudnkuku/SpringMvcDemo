# Netty权威指南    

标签（空格分隔）： 读书笔记

---

## EP1 TCP粘包/拆包 ##
`TCP`是基于流的协议，在传输时可能会将一个完整的包拆分成多个包，也可能将多个小的包封装成一个大的数据包发送。
产生粘包/拆包的原因如下：

 - 应用程序`write`写入的字节大小大于套接字发送缓冲区大小
 - 进行`MSS`大小的`TCP`分段
 - 以太网帧的`payload`大于`MTU`进行`IP`分片

粘包问题的解决策略，由于底层的`TCP`协议无法理解上层的业务数据，所以在底层是无法保证数据包不被拆分和重组的，这个问题只能通过上层的应用协议栈设计来解决：

 - 消息定长，例如每个报文的大小为固定长度200字节，如果不够，空位补空格
 - 在包尾增加回车换行符进行分割，例如`FTP`协议
 - 将消息分为消息头和消息体，消息头中包含表示消息总长度(或者消息体长度)的字段，通常设计思路为消息头的第一个字段使用`int32`来表示消息的总长度
 - 更复杂的应用层协议

**利用LineBasedFrameDecoder解决TCP粘包问题**
`Netty`提供了多种编解码器来处理半包，首先是`LineBasedFrameDecoder`，顾名思义是基于行的帧解码器，它会依次遍历`ByteBuf`中的可读字节，判断看是否有`\n`或者`\r\n`，如果有，就以此为结束为止，从可读索引到结束位置区间的字节数组就组成了一行，支持配置单行的最大长度，如果连续读取到最大长度后仍然没有发现换行符，就会抛出异常，同时忽略掉之前读到的异常码流。
`StringDecoder`的功能非常简单，将接受到的对象转换成字符串，然后继续调用后面的`handler`，`LineBasedFrameEncoder`和`StringDecoder`组合就是按行切换的文本解码器

## 分割符和定长解码器 ##
`TCP`以流的方式进行数据传输，上层的应用协议为了对消息进行区分，往往采用4中方式：

 - 消息长度固定，累计读取到长度总和为定长`LEN`的报文后，就认为读取到了一个完整的消息，将计数器置位，重新开始读取下一个数据报
 - 将回车换行符作为消息结束符，例如`FTP`协议，这种方式在文本协议中应用比较广泛
 - 将特殊的分割符作为消息的结束标志，回车换行符就是一种特殊的结束分割符
 - 通过在消息头中定义长度字段来标识消息的总长度

**DelimiterBasedFrameDecoder**
此解码器是以特殊字符作为消息结束符的解码器，例如`$_`

**FixedLengthFrameDecoder**
此解码器是固定长度解码器，它能够按照指定的长度对消息进行自动解码，开发者不需要考虑`TCP`的粘包问题。

    public FixedLengthFrameDecoder(int frameLength) {
        if (frameLength <= 0) {
            throw new IllegalArgumentException(
                    "frameLength must be a positive integer: " + frameLength);
        }
        this.frameLength = frameLength;
    }


## EP2 编解码技术 ##
`jdk`提供了`ObjectOutputStream`和`ObjectInputStream`，可以直接把`java`对象作为可存储的字节数组写入文件，也可以传输到网络上。

**java序列化的缺点**
 1、无法跨语言
 2、序列化后的码流太大
 

    //二进制编码
    public byte[] codeC() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] value = this.userName.getBytes();
        buffer.putInt(value.length);
        buffer.put(value);
        buffer.putInt(this.userId);
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }
    
    //jdk序列化
    public static void main(String[] args) throws IOException {
        UserInfo userInfo = new UserInfo();
        userInfo.buildUserId(10).buildUserName("yuding");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(userInfo);
        oos.flush();
        oos.close();
        byte[] b = bos.toByteArray();
        bos.close();
        System.out.println("the jdk serializable length is : " + b.length);
        System.out.println("------------------------");
        System.out.println("the byte array serializable length is : " + userInfo.codeC().length);
    }

3、序列化性能太低，`jdk`序列化和二进制编码序列化的效率差别明显，前者会更耗时

**业界主流编解码框架**
1、`Google`的`Protobuf`
2、`Facebook`的`Thrift`
3、`JBoss`的`Marshalling`
 
## EP3 机制解读 ##
网络`I/O`操作时讲到它会触发`ChannelPipeline`中对应的事件方法，`Netty`是基于事件驱动的，我们也可以理解为当`Channel`进行`I/O`操作时会产生对应的`I/O`事件，然后驱动事件在`ChannelPipeline`中传播，由对应的`ChannelHandler`对事件进行拦截和处理，不关心的事件可以直接忽略。
`Netty`的`ChannelPipeline`和`ChannelHandler`机制类似于`Servlet`和`Filter`过滤器，这类拦截器实际上是职责链模式的一种变形，主要是为了方便事件的拦截和用户业务逻辑的定制。`Netty`的`Channel`的过滤器实现原理和`Servlet Filter`机制一致，它将`Channel`的数据管道抽象为`ChannelPipeline`，消息在`ChannelPipeline`中流动和传递，`ChannelPipeline`持有`I/O`事件拦截器`ChannelHandler`的链表，由`ChannelHandler`对`I/O`事件进行拦截和处理，可以通过新增和删除`ChannelHandler`来实现不同的业务逻辑定制，不需要对已有的`ChannelHandler`进行修改，能够实现对修改封闭和对拓展的支持。

ChannelPipeline
---------------

一系列处理和拦截`Channel`的`inbound`事件和`outbound`操作的`ChannelHandler`，每条`Channel`都有自己的`pipeline`，当通道创建时，该`pipeline`会自动创建。
事件在`pipeline`中的流动：
`I/O`事件被`ChannlHandler`处理并转发给下一个`ChannelHandler`，同时`ChannelHandler`也能触发任意的`I/O`事件。转发或者触发事件可以通过调用`ChannelHandlerContext`的事件传播方法，例如`fireChannelRead()`或者`write()`方法来实现。
`pipeline`模型：

    ChannelHandler 1 -> ChannelHandler 2 ->ChannelHandler N

`inbound`事件由`I/O`线程触发，当`ChannelHandler`被通知`Channel`的状态发生改变时(新建连接和关闭连接)。
`outbound`事件通常由用户请求`outbound I/O`操作(例如写请求和连接请求)的代码触发。

之前说过可以通过调用`ChannelHandlerContext`的事件传播方法来讲事件转发给下一个`handler`
`inbound`事件传播方法：

    fireChannelRegistered()/fireChannelActive()/fireChannelRead()/fireChannelReadComplete()/fireExceptionCaught()/fireUserEventTrigered()/fireChannelWritabilityChanged()/fireChannelInactive()
    
`outbound`事件传播方法：

    bind()/connect()/write()/flush()/read()/disconnect()/close()
    
一个使用事件传播方法的例子：

    public class MyInboundHandler extends ChannelHandlerAdapter {
        public void channelActive(ChannelHandlerContext ctx) {
                    System.out.println("Connected!");
                    ctx.fireChannelActive();
        }
    }

下面的例子展示了拦截`Channel Active`事件，打印`TCP`链路建立成功日志：

    public class MyInboundHandler extends ChannelHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("TCP connected!");
            ctx.fireChannelActive();
        }
    }

下面的例子展示了如何在链路关闭的时候释放资源：

    public class MyOutboundHandler extends ChannelHandlerAdapter {
        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
            System.out.println("TCP closing...");
            Object.release();
            ctx.close(promise);
        }
    }

事实上，用户不需要自己创建`pipeline`，因为使用`ServerBootstrap`或者`Bootstrap`启动服务端或者客户端时，`Netty`会为每个`Chnanel`连接创建一个独立的`pipeline`。对于使用者而言，只需要将自定义的拦截器`ChannelHandler`加入到`pipeline`中即可：

    pipeline = ch.pipeline();
    pipeline.addLast("decoder", new MyProtocolDecoder());
    pipeline.addLast("encoder", new MyProtocolEncoder());
    
对于类似编解码这样的`ChannelHandler`，它存在先后顺序，例如`MessageToMessageDecoder`，在它之前往往需要有`ByteToMessageDecoder`将`ByteBuf`解码为对象，然后对对象做二次解码得到最终的`POJO`对象，`Pipeline`支持指定位置添加或者删除拦截器。

ChannelPipeline处理事件
-------------------

**Inbound Event**
`pipeline`中以`fireXXX`命名的方法都是从`IO`线程流向用户业务`Handler`的`inbound`事件，它们的实现因功能而异，但是处理步骤类似，总结如下：
(1)调用`HeadHandler`对应的`fireXXX`方法
(2)执行相关的逻辑操作
    
    //DefaultChannelPipelin中fireChannelActive()方法
    public ChannelPipeline fireChannelActive() {
        //调用head.fireChannelActive()，head是DefaultChannelHandlerContext实例
        head.fireChannelActive();

        if (channel.config().isAutoRead()) {
            channel.read();
        }

        return this;
    }
    
    //DefaultChannelHandlerContext的fireChannelActive()方法
    public ChannelHandlerContext fireChannelActive() {
        //从head开始找到能够处理CHANNEL_ACTIVE事件的ChannelHandlerContext
        DefaultChannelHandlerContext next = findContextInbound(MASK_CHANNEL_ACTIVE);
        //调用ctx内部的ChannelHandler
        next.invoker.invokeChannelActive(next);
        return this;
    }
    
    //DefaultChannelHandlerInvoker的invokeChannelActive()方法
    public void invokeChannelActive(final ChannelHandlerContext ctx) {
        if (executor.inEventLoop()) {
            invokeChannelActiveNow(ctx);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    invokeChannelActiveNow(ctx);
                }
            });
        }
    }
    //go on 
    public static void invokeChannelActiveNow(final ChannelHandlerContext ctx) {
        try {
            //调用ctx.handler().channelActive(ctx)方法
            ctx.handler().channelActive(ctx);
        } catch (Throwable t) {
            notifyHandlerException(ctx, t);
        }
    }
    

**Outbound Event**

`bind/connect/write/flush/read/disconnect/close`都是`pipeline`的`outbound event`传播方法

    public ChannelFuture bind(SocketAddress localAddress) {
        return tail.bind(localAddress);
    }

例如上面的代码就从`tail`开始传播`bind`事件
    
    //next
    public ChannelFuture bind(SocketAddress localAddress) {
        return bind(localAddress, newPromise());
    }
    //next
    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        //从tail开始向head传播bind事件，事件掩码为MASK_BIND
        DefaultChannelHandlerContext next = findContextOutbound(MASK_BIND);
        //调用next.invoker.invokeBind()犯法
        next.invoker.invokeBind(next, localAddress, promise);
        return promise;
    }
    //DefaultChannelHandlerInvoker invokeBind()实现
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
    //invokeBindNow()，调用相关的ChannelHandler处理事件
    public static void invokeBindNow(
            final ChannelHandlerContext ctx, final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            //调用ChannelHandler的bind()方法
            ctx.handler().bind(ctx, localAddress, promise);
        } catch (Throwable t) {
            notifyOutboundHandlerException(t, promise);
        }
    }
    //HeadHandler的bind()实现,
    public void bind(
                ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
                throws Exception {
            //委托给Unsafe.bind()方法
            unsafe.bind(localAddress, promise);
        }

ChannelHandler功能说明
------------------

处理`I/O`事件或者拦截`I/O`操作，并且将其转发给`ChannelPipeline`中的下一个`ChannelHandler`，可以直接继承`ChannelHandlerAdapter`类来实现处理器方法的定制，`ChannelHandler`的方法都提供有`ChannelHandlerContext`对象，`ChannelHandler`通过这个`context`对象来和它属于的`ChannelPipeline`进行交互，通过`context`对象，`ChannelHandler`可以向上或向下传递事件，动态修改`pipeline`，或者存储特定的信息。

**状态管理**
举个简单的例子：

    public interface Message {}
    
    @Sharable
    public class DataServerHandler extends SimpleChannelInboundHandler<Message> {
        private boolean loggedIn;
        
        @Override
        public void channelRead(ChannelHandlerContext ctx, Message message) {
            Channel ch = e.getChannel();
            if(message instanceof LoginMessage) {
                authenticate((LoginMessage) message);
                loggedId = true;
            } else {
                if(loggedIn) {
                    ch.write(fetchSecret((GetDataMessage) message));
                } else {
                    fail();
                }
            }
        }
    }
    
如果`ChannelHandler`注解有`@Sharable`，表示这个`ChannelHandler`一旦创建可以被多个`ChannelPipeline`共用多次而不会出现竞争情况(`race condition`)，如果没有此注解，你需要在每次在加入`ChannelPipeline`中时都需要`new`一个`ChannelHandler`实例

**ChannelHandlerAdapter**
`ChannelHandlerAdapter`是`ChannelHandler`的骨架实现，提供了接口方法的默认实现，子类可以覆盖某个方法来实现特定的功能，其中的方法都注解有`@Skip`，表明该方法不会被`ChannelPipeline`调用，而只是会将事件传递给下一个`handler`，如果`ChannelHandler`只关心某个事件，只需要覆盖`ChannelHandlerAdapter`对应的方法即可，对于不关心的可以直接继承父类的方法(注解有`@Skip`，在传递事件时并不会调用)

例如`channelActive()`方法：

    @Skip
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

处理`@Skip`注解，代码在`DefaultChannelHandlerContext`中：
    
    //通过反射获取ChannelHandler的skipFlags，如果响应某个事件的方法注解有@Skip，就会与上对应的MASK，MASK是根据方法在接口中的定义位置左移生成，参考下面的接口
    private static int skipFlags0(Class<? extends ChannelHandler> handlerType) {
        int flags = 0;
        try {
            if (handlerType.getMethod(
                    "handlerAdded", ChannelHandlerContext.class).isAnnotationPresent(Skip.class)) {
                flags |= MASK_HANDLER_ADDED;
            }
            
            
    public interface ChannelHandler {
        //MASK_HANDLER_ADDED = 1;
        void handlerAdded(ChannelHandlerContext ctx) throws Exception;
        //MASK_HANDLER_REMOVED = 1 << 1;
        void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
        //MASK_EXCEPTION_CAUGHT = 1 << 2;
        void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
    }
    
    //这里举个例子，响应inbound event，从head->tail，找到能够响应mask对应事件的ChannelHandler
    private DefaultChannelHandlerContext findContextInbound(int mask) {
        DefaultChannelHandlerContext ctx = this;
        do {
            ctx = ctx.next;
        } while ((ctx.skipFlags & mask) != 0); //skipFlags & mask ！= 0表示该ChannelHandler跟mask对应的事件处理方法没有被@Skip注解，会执行
        return ctx;
    }
    
    //找到了对应的ChannelHandlerContext，调用响应的invoker
    public ChannelHandlerContext fireChannelActive() {
        DefaultChannelHandlerContext next = findContextInbound(MASK_CHANNEL_ACTIVE);
        next.invoker.invokeChannelActive(next);
        return this;
    }
    
**LengthFieldBasedFrameDecoder**
如何区分一个整包消息：

 - 固定长度，例如每120个字节代表一个整包消息，不足的前面补0，解码器在处理这类定长消息的时候比较简单，每次读到指定长度的字节后再进行解码。
 - 通过回车换行符区分消息，例如`FTP`协议，这类区分消息的方式多用于文本协议
 - 通过分隔符区分整包消息
 - 通过指定长度来表示整包消息
 
如果是通过长度进行区分的，`LengthFieldBasedFrameDecoder`都可以自动处理粘包和半包的问题，只需要传入正确的参数，即可轻松搞定读半包的问题。

`LengthFieldBasedFrameDecoder`构造函数提供了4个参数用于实现不同的读取策略：

        this.byteOrder = byteOrder;
        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;  //消息长度起始偏移地址
        this.lengthFieldLength = lengthFieldLength;  //消息长度值(字节)
        this.lengthAdjustment = lengthAdjustment;   //长度修正
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;  //长度field终止偏移地址
        this.initialBytesToStrip = initialBytesToStrip;    //需要strip的消息字节长度
        this.failFast = failFast;
        
    //消息解码方法
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        //获取in中消息长度field的实际偏移地址
        int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
        //获取修正之前的消息帧长度，lengthFieldLength只能取值1,2,3,4,8，否则会抛异常
        long frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder);
        //修正消息帧长度，加上修正值和length field的终止偏移地址
        frameLength += lengthAdjustment + lengthFieldEndOffset;
        
        //转为int
        int frameLengthInt = (int) frameLength;
        //略过initialBytesToStrip
        in.skipBytes(initialBytesToStrip);

        // extract frame
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        //读取in中的消息到frame中
        ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
        in.readerIndex(readerIndex + actualFrameLength);
        return frame;
    }
    
**LengthFieldPrepender**
将消息长度以二进制形式添加到消息的前面，例如`LengthFieldPrepender(2)`会将以下12字节的字符串编码：

    "Hello, World" -> 0x000C "HELLO, WORLD"

如果设置了`lengthIncludesLengthFieldLength`标志位，那么长度为`0x000E`

    0x000E "HELLO, WORLD
    

## EventLoop和EventLoopGroup ##
`Netty`框架的主要线程是`I/O`线程，线程模型的好坏决定了系统的吞吐量、并发性和安全性等架构质量属性

`Reactor`线程模型：
1、单线程模型
所有的`I/O`操作都在同一个`NIO`线程上面完成，`NIO`线程的职责如下：

 - 作为`NIO`服务端，接收客户端的`TCP`连接
 - 作为`NIO`客户端，向服务端发起`TCP`连接
 - 读取通信对端的请求或者应答消息
 - 向通信对端发送消息请求或者应答消息
 
    由于`Reactor`模式使用的是异步非阻塞`I/O`,所有的`I/O`操作都不会导致阻塞，理论上一个线程可以独立处理所有`I/O`相关的操作，从架构层面来看，一个`NIO`线程确实可以完成其承担的所有职责，例如，通过`Acceptor`类接收到客户端的`TCP`连接请求消息，当链路建立成功之后，通过`Dispatch`将对应的`ByteBuffer`派发到指定的`Handler`上，进行消息解码，用户线程消息编码后通过`NIO`线程将消息发送给客户端。   
    在一些小容量应用场景下，可以使用单线程模型。但是这对于高负载、大并发的应用场景并不合适，主要原因如下：
 - 一个`NIO`线程同时处理成百上千的链路，性能无法支撑，几遍`NIO`线程的`CPU`符合达到100%，也无法满足海量消息的编码、解码。读取和发送。
 - 当`NIO`线程负载过重之后，处理速度将变慢，这会导致大量客户端连接超时，超时之后往往会进行重发，这更加重了`NIO`线程的负载，最终会导致大量消息挤压和处理超时，称为系统的性能瓶颈。
 - 可靠性问题，一旦`NIO`线程意外跑飞，或者进入死循环，会导致整合系统通信模块不可用，不能接受消息和处理外部消息，造成节点故障。
 
2、多线程模型
 - 有一个专门的的`NIO`线程-`Acceptor`线程来监听服务端，接收客户端的`TCP`连接请求
 - 网络`I/O`操作-读、写等由一个`NIO`线程池负责，线程池可以采用标准的`jdk`线程池来实现，它包含了一个任务队列和`N`个可用的线程，由这些`NIO`线程负责消息的读取、解码、编码和发送。
 - 一个`NIO`线程可以同时处理多条链路，但是一个链路只能对应一个`NIO`线程，防止出现并发操作问题。

大部分场景下，多线程模型都能满足需求，但是在个别特殊场景例如需要安全认证时，单独的一个`Acceptor`线程可能无法满足要求。

3、主从`Reactor`多线程模型
服务端用于接收客户端连接的不再是一个单独的`NIO`线程，而是一个独立的`NIO`线程池，`Acceptor`接收到客户端`TCP`连接请求并处理完成后(可能包括接入认证)，将新创建的`SocketChannel`注册到`I/O`线程池(`sub reactor`线程池)的某个`I/O`线程上，由它负责`SocketChannel`的读写和解编码工作，`Acceptor`线程池仅仅用于客户端的登录、握手和安全认证，一旦链路建立成功，就将链路注册到后端`subReactor`线程池的`I/O`线程上，由`I/O`线程负责后续的`I/O`操作

看一段代码：

    public void bind(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());

            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        
上面这段代码就开启了两个`NIO`线程池，一个用于接收客户端的`TCP`连接，另一个用于处理`I/O`相关的读写操作，或者执行系统`Task`、定时任务`Task`等。

`Netty`用于接收客户端请求的线程池职责如下。
(1)接收客户端`TCP`连接，初始化`Channel`参数
(2)将链路变更事件通知给`ChannelPipeline`

`Netty`处理`I/O`操作的`Reactor`线程池职责如下。
(1)异步读取通信对端的数据报，发送读事件到`ChannelPipeline`
(2)异步发送消息到通信对端，调用`ChannelPipeline`的消息发送接口
(3)执行系统调用`task`
(4)执行定时任务`task`，例如链路空闲状态监测定时任务

通过调整线程池的线程个数、是否共享线程池等方式，`Netty`的`Reactor`线程模型可以在单线程、多线程和主从多线程之间切换。

`Netty`的多线程编程最佳实践：

 - 创建两个`NioEventLoopGroup`，用于逻辑隔离`NIO Acceptor`和`NIO I/O`线程。
 - 尽量不要在`ChannelHandler`中启动用户线程(解码后用于将`POJO`消息派发到后端业务线程的除外)
 - 解码要放在`NIO`线程调用的解码`Handler`中进行，不要切换到用户线程中完成消息的解码
 - 如果业务逻辑操作非常简单，没有复杂的业务逻辑计算，没有可能会导致线程被阻塞的磁盘操作、数据库操作、网路操作等，可以直接再`NIO`线程上完成业务逻辑编排，不需要切换到用户线程
 - 如果业务逻辑处理复杂，不要在`NIO`线程上完成，建议将解码后的`POJO`消息封装成`task`，派发到业务线程池中由业务线程执行，以保证`NIO`线程尽快被释放，处理其他的`I/O`操作
 
## NioEventLoop源码分析 ##
**NioEventLoop设计原理**
`Netty`的`NioEventLoop`并不是一个纯粹的`I/O`线程，它除了负责`I/O`的读写之外，还兼顾处理一下两类任务：
 - 系统`task`：通过调用`NioEventLoop`的`execute(Runnable task)`方法实现，`Netty`有很多系统`task`，创建它们的主要原因是：当`/IO`线程和用户线程同时操作网络资源时，为了防止并发操作导致的锁竞争，将用户线程的操作封装成`task`放入消息队列中，由`I/O`线程负责执行，这样就实现了局部无锁化
 - 定时任务：通过调用`NioEventLoop`的`schedule(Runnable command, long delay, TimeUnit unit)`方法实现
