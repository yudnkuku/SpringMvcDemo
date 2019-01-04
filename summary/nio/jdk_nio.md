# NIO

标签（空格分隔）： JAVA

---

## EP1 NIO概述 ##
`NIO`由以下核心组件构成：

 - `Channels`：通道
 - `Buffers`：缓冲
 - `Selectors`：选择器

## 通道和缓冲 ##
通道有点像流，可以从通道向缓冲中读取数据，也可以从缓冲中将数据写进通道。
`NIO`中`Channel`的基本实现：

 - `FileChannel`
 - `DatagramChannel`
 - `SocketChannel`
 - `ServerSocketChannel`
这些实现基本覆盖了文件、网络(`UDP/TCP`)`IO`.
`Buffer`的基本实现：
 - `ByteBuffer`
 - `CharBuffer`
 - `DoubleBuffer`
 - `FloatBuffer`
 - `IntBuffer`
 - `LongBuffer`
 - `ShortBuffer`

## 选择器 ##
选择器使得单个线程能够处理多条通道，如果你的应用有多个连接打开，且每个连接上阻塞很低时这是很有用的

## EP2 Java NIO Channel ##
通道和流有一些不同：

 - 你可以在通道上进行读写，但流通常是单向的(读或写)
 - 通道可以进行异步读写
 - 通道通常与`Buffer`缓冲一起工作，实现读写

## Channel实现 ##
一些基本实现：

 - `FileChannel`:文件通道
 - `DatagramChannel`:网络`UDP`通道
 - `SocketChannel`:网络`TCP`通道
 - `ServerSocketChannel`:监听`TCP`连接，每来一个连接就创建一个`SocketChannel`

## 一个例子 ##

    RandomAccessFile aFile = new RandomAccessFile("data/nio-data.txt", "rw");
    FileChannel inChannel = aFile.getChannel();

    ByteBuffer buf = ByteBuffer.allocate(48);
    
    //从通道中读数据存入buf中
    int bytesRead = inChannel.read(buf);
    while (bytesRead != -1) {

      System.out.println("Read " + bytesRead);
      //读buf里的数据之前先flip()，即转换模式由写模式转到读模式
      buf.flip();

      while(buf.hasRemaining()){
          System.out.print((char) buf.get());
      }

      buf.clear();
      bytesRead = inChannel.read(buf);
    }
    aFile.close();

## Java NIO Buffer ##
`Buffer`用来和`Channel`交互，数据是从`Channel`中读取入`Buffer`中，从`Buffer`中读出并将数据写入`Channel`中的。
`Buffer`本质上是可以读写数据的一块内存，该内存块被包装成了`Buffer`对象，提供了一系列方法操作内存块。

## Buffer基本使用 ##
分四步：

 - 往`Buffer`里写数据
 - 调用`buffer.flip()`方法
 - 从`Buffer`里读数据
 - 调用`buffer.clear()`或者`buffer.compact()`方法

当你往`buffer`里写数据的时候，`buffer`会跟踪你写了多少数据，一旦要读数据，需要调用`flip()`方法将`buffer`从写模式切换到读模式，在读模式下，`buffer`允许你读取写入的所有数据。
一旦你读取了所有的数据，你需要清空`buffer`，以便再次写入，有两种方式清空，`clear()`或者`compact()`方法，`compact()`方法只会清空已经读取的数据，没有读取的数据移至`buffer`的开头，随后写入的数据会跟在后面。
一个例子：

    RandomAccessFile aFile = new RandomAccessFile("data/nio-data.txt", "rw");
    FileChannel inChannel = aFile.getChannel();
    
    //create buffer with capacity of 48 bytes
    ByteBuffer buf = ByteBuffer.allocate(48);
    
    int bytesRead = inChannel.read(buf); //read into buffer.
    while (bytesRead != -1) {
    
      buf.flip();  //make buffer ready for read
    
      while(buf.hasRemaining()){
          System.out.print((char) buf.get()); // read 1 byte at a time
      }
    
      buf.clear(); //make buffer ready for writing
      bytesRead = inChannel.read(buf);
    }
    aFile.close();

## Buffer的三个变量：Capacity/Position/Limit ##
`Capacity`:`buffer`的固定大小
`Position`:读取或写入的位置
`Limit`:可以读取或写入的数据大小阈值

## 常用方法 ##
`flip()`:由写模式切换到读模式，每次要读数据之前都要调用`flip()`方法。看源码可知，其使`limit=postion,position=0`

    public final Buffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }
    
`equals()`和`compareTo()`方法，两个`buffer`在以下条件满足下才会`equals`：

 - 数据类型相同
 - 有相同数量的剩余数据
 - 所有剩余数据也一致

`compareTo()`方法逻辑：

 - 比较第一个元素
 - 所有元素都相同，比较元素数量

 
## EP3 Java NIO Socket Channel ##
`Java NIO SocketChannel`是连接到`tcp socket`上的通道，可以以两种方式创建：

 - 手动开启一个`SocketChannel`并连接到网络上的服务器
 - 当`ServerSocketChannel`收到连接时会创建`SocketChannel`

创建`SocketChannel`：

    SocketChannel sc = new SocketChannel.open();
    sc.connect(new InetSocketAddress("127.0.0.1", 80));

从`SocketChannel`读数据：

    ByteBuffer buf = ByteBuffer.allocate(48);
    int bytesRead = sc.read(buf);
    
往`SocketChannel`写数据：

    String newData = "New String to write to file..." + System.currentTimeMillis();
    
    ByteBuffer buf = ByteBuffer.allocate(48);
    buf.clear();
    buf.put(newData.getBytes());
    
    //读buf之前要flip
    buf.flip();
    
    while(buf.hasRemaining()) {
        channel.write(buf);
    }

## 非阻塞模式 ##
可以设置`SocketChannel`为非阻塞模式，此时`connect()`/`read()`和`write()`方法都会异步模式调用
如果`SocketChannel`是非阻塞模式，那么调用`connect()`方法会在连接建立之前就返回方法，**为了检测连接是否建立，你可以调用`finishConnect()`方法**:

    sc.configureBlocking(false);
    sc.connect(new InetSocketAddress("127.0.0.1", 80));
    while(!sc.finishConnect()) {
        //wait or do sth else
    }
    
**write()**

**在非阻塞模式中`write()`方法返回时可能不会写任何数据**，因此你需要在循环中调用`write()`方法

**read()**

同样，**在非阻塞模式下`read()`方法可能不会读任何数据**，因此需要注意返回值-读取的数据长度

## EP4 Java NIO ServerSocketChannel ##
`Java NIO SocketChannel`是用来建立`tcp`连接的通道，和`ServerSocket`一样，一个典型的例子：

    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    
    serverSocketChannel.socket().bind(new InetSocketAddress(9999));
    
    while(true){
        SocketChannel socketChannel =
                serverSocketChannel.accept();
    
        //do something with socketChannel...
    }

## 非阻塞模式 ##
`ServerSocketChannel`可以被设置为非阻塞模式，在非阻塞模式中，`accept()`方法会立即返回，如果没有任何连接可能返回`null`，因此**需要检查返回的`SocketChannel`是否返回`null`**，如：

    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    
    serverSocketChannel.socket().bind(new InetSocketAddress(9999));
    serverSocketChannel.configureBlocking(false);
    
    while(true){
        SocketChannel socketChannel =
                serverSocketChannel.accept();
    
        if(socketChannel != null){
            //do something with socketChannel...
            }
    }
    
## EP5 Java NIO DatagramChannel ##
`Java NIO DategramChannel`是可以发送和接收`UDP`包的通道，由于`UDP`是无连接的网络协议，因此不能像其他`Channel`一样读写数据，而是发送和接收数据。

`Receiving Data`:

        ByteBuffer buf = ByteBuffer.allocate(48);
    buf.clear();
    
        channel.receive(buf);
    
`receive()`方法接收的数据包复制到`buffer`中，如果数据包大于`Buffer`的容量，那么多余的数据会被抛弃。

`Sending Data`:

        String newData = "New String to write to file..." + System.currentTimeMillis();
        
        ByteBuffer buf = ByteBuffer.allocate(48);
        buf.clear();
        buf.put(newData.getBytes());
        buf.flip();
    
        int bytesSent = channel.send(buf, new InetSocketAddress("jenkov.com", 80));
        
## EP6 Java IO & NIO ##
`IO`和`NIO`的主要区别：

|`IO`|`NIO`|
|:-:|:-:|
|面向流|面向缓冲|
|阻塞`IO`|非阻塞`IO`，选择器|

## 面向流 VS 面向缓冲 ##
面向流意味着一次可以读一个或多个字节，如何处理这些字节取决于你，它们没有在任何位置缓存，因此你不能在流中前移或者后退，除非你将这些数据缓存下来。
面向缓冲意味着读取的数据先被存入`buffer`中，随后会被处理，你可以在`buffer`中前移或者后退，这使得在数据处理过程中更具灵活性，然而，你必须确认`buffer`中是否包含所有的数据，而且当你继续往`buffer`中读数据时，不会覆盖之前未被处理的数据。

## Blocking IO & Non Blocking IO ##
`Java IO`的所有流都是阻塞的，这意味着当一个线程调用`read()`或者`write()`方法时，该线程会被阻塞直到有数据被读取或者所有数据被写入，在这期间该线程不能做其他任何事情。
`Java NIO`的非阻塞模式允许线程在从通道请求读数据时，只会读取当前可用的数据或者没有任何数据，此过程如果没有可读数据线程不会阻塞而是可以去做其他事情。

## 选择器 ##
`Java NIO`的`Selector`允许一个线程监控多条输入通道，你可以给一个`Selector`注册多条通道，然后开辟一条线程去选择有输入数据的通道或者准备读数据的通道。

## NIO和IO处理数据的不同 ##
假设处理以行为基础的文本数据流，如下所示：

    Name: Anna
    Age: 25
    Email: anna@mailserver.com
    Phone: 1234567890

文本行流的处理如下：

    InputStream input = ... ; // get the InputStream from the client socket
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    
    String nameLine   = reader.readLine();
    String ageLine    = reader.readLine();
    String emailLine  = reader.readLine();
    String phoneLine  = reader.readLine();

可以看到程序只有在有新数据可读时才会继续进行，并且每一步你必须直到读取的数据是什么，一旦正在执行的线程处理过代码中的某些数据，线程便不会回复数据。

而`NIO`实现会有些不同：

    ByteBuffer buffer = ByteBuffer.allocate(48);
    
    int bytesRead = inChannel.read(buffer);
    
`NIO Channel`的读取操作返回时，你并知道`buffer`中是否包含你所需要的所有数据，你所知道的是它只会包含某些字节。

## 总结 ##
`NIO`允许你使用一条或一些线程来管理多条通道(网络连接或文件)，但是在处理数据可能会更加比从阻塞流中读取数据更加复杂。
如果**你需要同时管理上千开启的连接，每个连接只会发送一些数据**，那么使用`NIO`实现服务端可能是一个优势，类似地如果你需要维护大量连接，例如`P2P`网络，使用一条线程管理所有连接是一个优势。
如果你有少量连接，带宽大，同时发送大量的数据，那么典型的`IO`服务器实现可能是最佳选择。

## EP7 选择器 ##
选择器是检查一个或者多个`NIO`通道的组件，并决定哪个通道可以读或者写，这样可以使得单条线程能够管理多个通道和多条网络连接。

## 为何使用选择器 ##
使用单条线程管理多条通道的好处在于你可以使用更少的线程资源，事实上，你可以使用一条线程管理所有的`channel`，线程间的切换是十分耗费操作系统资源的，并且每条线程都会占用系统的内存资源，因此线程越少越好。
现代的操作系统都支持多任务，如果是多核`CPU`，那么你不使用多任务是在浪费`CPU`的性能，对于选择器来说，它能够使用单条线程处理多条`channel`。

## 创建Selector ##

    Selector selector = Selector.open();
    
## 注册Channel ##
    //将通道设置为非阻塞模式
    channel.configureBlocking(false);
    
    SelectionKey key = channel.register(selector, SelectionKey.OP_READ);

使用`Selector`之前通道必须设置为非阻塞模式，因此不能和`FileChannel`使用`Selector`，因为`FileChannel`不能被设置为非阻塞模式，其他的`Socket Channel`可以。
第二个参数是`interest set`(兴趣集)，表示通道中通过`Selector`监听的你感兴趣的事件，有四种取值：`connect/accept/read/write`

一个`channel`产生了事件称为对该事件准备好，例如一个成功连接服务器的`channel`是`connect ready`，一个`server socket channel`接受了到来的连接称为`accept ready`，通道可以读写数据称为`read/write ready`。

四个`SelectionKey`常量：`SelectionKey.OP_CONNECT/OP_ACCEPT/OP_READ/OP_WRITE`，它们的定义：

    public static final int OP_READ = 1 << 0;
    public static final int OP_WRITE = 1 << 2;
    public static final int OP_CONNECT = 1 << 3;
    public static final int OP_ACCEPT = 1 << 4;

如果你对多个事件感兴趣，直接使用`OR`，例如：
    
    //读写
    int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;  
    
    
    
## SelectionKey ##
`SelectionKey`包含了如下一些属性：

 - 兴趣集(`interest set`)
 - 准备集(`ready set`)
 - 通道
 - 选择器
 - 关联对象
 
## 兴趣集 ##
兴趣集就是在选择器注册通道(`SelectionKey key = channel.register(selector, interestSet)`)中设置的第二个参数，可以通过`SelectionKey`获取兴趣集

    int interestSet = selectionKey.interestOps();
    
    //将兴趣集和某个常量与，就可以知道兴趣集中是否包含该常量
    boolean isInterestedInAccept  = interestSet & SelectionKey.OP_ACCEPT;
    boolean isInterestedInConnect = interestSet & SelectionKey.OP_CONNECT;
    boolean isInterestedInRead    = interestSet & SelectionKey.OP_READ;
    boolean isInterestedInWrite   = interestSet & SelectionKey.OP_WRITE;
    
## 准备集 ##
标明通道准备好某些操作，可以通过`select()`方法返回准备好的通道数

    //获取准备集
    int readySet = selectionKey.readyOps();
    //判断是否准备好某个操作
    selectionKey.isAcceptable();
    selectionKey.isConnectable();
    selectionKey.isReadable();
    selectionKey.isWritable();
    
## Channel&Selector ##

    Channel channel = selectionKey.channel();
    Selector selector = selectionKey.selector();
    
## 通过Selector选择通道 ##
一旦你使用`Selector`注册了一个或多个通道，便可以调用`select()`方法返回那些对你感兴趣的事件准备好的通道，有三个`select()`方法：

 - `select()`：阻塞直到有通道对注册的事件准备好
 - `select(long timeout)`:和`select()`方法一样，但是设置了阻塞时间限制
 - `selectNow()`:非阻塞，无论什么通道准备好立即返回
 
上述方法的`int`返回值表明了准备好的`channel`数量，也就是自从上一次`select()`方法调用后准备好的通道数量。

**selectedKeys()**

一旦你通过`select()`方法反悔了一个或者多个准备的通道，那么可以通过`selected key set`访问准备好的通道，如下：

    Set<SelectionKey> selectedKeys = selector.selectedKeys();
    
当你通过`channel.register()`方法向`selector`注册`channel`的时候返回的就是一个`SelectionKey`对象，这个对象代表了通道和选择器间的注册关系，通过`selectedKeySet()`可以得到准备好的通道对应的`SelectionKey`集合，遍历该集合可以访问准备好的所有通道：

    Set<SelectionKey> selectedKeys = selector.selectedKeys();
    
    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
    
    while(keyIterator.hasNext()) {
    
        SelectionKey key = keyIterator.next();
    
        if(key.isAcceptable()) {
            // a connection was accepted by a ServerSocketChannel.
    
        } else if (key.isConnectable()) {
            // a connection was established with a remote server.
    
        } else if (key.isReadable()) {
            // a channel is ready for reading
    
        } else if (key.isWritable()) {
            // a channel is ready for writing
        }
    
        keyIterator.remove();
    }

注意上面每次遍历之后的`remove()`，`Selector`不会从集合中移除`SelectionKey`实例，你需要在处理完`channel`后主动移除，下一次该通道变成`ready`，`Selector`会再一次将该`SelectionKey`加入集合。遍历`SelectionKey`集合处理通道时，应该将`SelectionKey.channel()`返回的`SelecableChannel`通道转换为你需要的`channel`，例如`SocketChannel`或者`ServerSocketChannel`等。

## wakeUp() ##
调用`select()`方法被阻塞的线程可以通过其它线程调用相同`selector`的`wakeup()`方法来唤醒，在`select()`方法内部等到的线程会立即返回。
如果有不同的线程调用了`wakeup()`方法，而当前没有线程阻塞在`select()`方法， 那么下一个调用`select()`方法的线程会立即返回。

## close() ##
当你完成`Selector`的相关操作时，直接调用`close()`方法，这会关闭`Selector`并使所有注册在`Selector`上的`SelectionKey`失效，但是`channel`本身不会关闭。

## 一个完整的选择器例子 ##

    Selector selector = Selector.open();
    
    channel.configureBlocking(false);
    
    SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
    
    
    while(true) {
    
      int readyChannels = selector.select();
    
      if(readyChannels == 0) continue;
    
    
      Set<SelectionKey> selectedKeys = selector.selectedKeys();
    
      Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
    
      while(keyIterator.hasNext()) {

        SelectionKey key = keyIterator.next();
    
        if(key.isAcceptable()) {
            // a connection was accepted by a ServerSocketChannel.
    
        } else if (key.isConnectable()) {
            // a connection was established with a remote server.
    
        } else if (key.isReadable()) {
            // a channel is ready for reading
    
        } else if (key.isWritable()) {
            // a channel is ready for writing
        }
    
        keyIterator.remove();
      }
    }

## EP8 设计一个非阻塞服务器 ##
## 非阻塞管道 ##
![非阻塞管道][1]
## 非阻塞管道和阻塞管道的区别 ##
其根本区别是数据如何从底层的`Channel`读取
`IO`管道从流中(`socket`或者文件)读取数据并将数据切分成一些相关的消息，这类似于将数据流切分成`tokens`，然后再用`tokenizer`解析。相反，我们会将数据流切分成更大的消息，这个组件称为`Message Reader`

![MessageReader][2]

阻塞式`IO`管道使用类似`InputStream`的接口，一次只能从底层的`channel`中读取一个字节的数据，并且会阻塞直到有数据可读，使用阻塞式的`IO`接口简化了`Message Reader`的实现，阻塞式的`Message Reader`无法处理无数据可读或者只读取了部分数据并且随后需要恢复消息解析的情况。类似的，阻塞式`Message Writer`也无法处理只写入部分消息或者写入的部分消息随后需要恢复的情况。

## 阻塞式IO管道的缺点 ##
尽管阻塞式`Message Reader`很容易实现，但是也会有致命的缺点，它必须为每个需要切分成消息的流分配单独的线程，原因在于每个流的`IO`接口会阻塞直到流中有数据可读，这意味着一条线程如果从某个流中读数据，而刚好没有数据可读，那么该线程将会被阻塞而无法从另外一个流中读数据，只要线程尝试从流中读取数据，它就会被阻塞直到流中有数据可读。
如果这种阻塞式`IO`管道是服务器的一部分，那么在处理大量的并发连接时，服务器会给每个活跃的连接提供一条线程，如果同时存在几百的连接这当然不是问题，但是如果服务器有百万级的并发连接，这种阻塞式的设计会出现问题，每条线程会占用`320k`(`32`为虚拟机)和`1024k`(`64`位虚拟机)的栈内存，因此百万线程会消耗`1TB`的内存资源，这还是在服务器为处理消息分配内存资源之前就已经存在。
为了降低线程数量，许多服务器会使用线程池设计。

## 通用非阻塞IO管道设计 ##
非阻塞`IO`管道可以使用一条线程从多个流中读数据，这需要将流切换到非阻塞模式，当处于非阻塞模式时，如果尝试从流中读取数据，会返回0个或者多个字节，0字节表示流中没有数据可读，而多字节表示流中有数据可读。
为了避免检查没有字节可读的流，我们使用可`NIO Selector`，一个或者多个`SelectableChannel`可以注册到`Selector`，当你在`Selector`上调用`select()`或者`selectNow()`方法时，会返回已经准备好的`channel`数量。
![NON Blocking IO Pipeline][3]

## 读取部分消息数据 ##
当我们从`SelectableChannel`中读取数据块时，我们并不知道数据块是否包含了一个完整的消息，一个数据块可能包含部分消息(少于一个完整的消息)、完整的消息抑或是多于一个完整的消息.

![Data Block][4]

这些消息统称为部分消息，处理这些消息有两个难点：

 - 如何检查数据块中是否包含一个完整的消息
 - 在接收余下的消息之前如何处理这些部分消息
 
检查完整的消息需要`Message Reader`查看数据块内部是否包含了至少一个完整的消息体，如果数据块包含了一个或者多个完整的消息体，那么这些消息会被沿着管道继续进行处理，由于查找完整消息体的操作是重复性动作，因此此过程必须尽可能快。
无论何时在数据块中存在部分消息，那么这些部分消息需要被存储直到余下的消息被接收。
检测完整消息体和存储部分消息都由`Message Reader`来完成，为了避免混淆不同`Channel`实例中的消息数据，每个通道会使用一个`Message Reader`
![Message Reader][5]
    
在遍历可读的(通过`Selector`的`select()`方法判定)`Channel`之后，和该`Channel`关联的`Message Reader`(上面说到给每个`Channel`配置一个单独的`Message Reader`实例)就会读取数据并尝试将数据块切分成消息，如果存在任何完整的消息，这些消息会被沿着读通道传递给任何需要处理它们的组件。
`Message Reader`必须指明协议，它需要知道读取的消息格式，如果我们的服务器实现需要跨协议可重用，那么需要进行`Message Reader`的插件式实现，可能会接受`Message Reader`的工厂类作为配置参数。

## 存储部分消息 ##
我们已经知道`Message Reader`负责存储部分消息直到一个完整的消息被接收，那么如何实现这些部分消息的存储呢。
有两点应该考虑：

 - 尽量少地复制消息数据，复制得越多，性能越差
 - 完整的消息需要被存储在连续的字节序列中，以便解析消息
 
**每个Message Reader配一个Buffer**

很显然部分消息需要被存储在`buffer`中，一个比较直接的实现是在`Message Reader`内部设置一个`buffer`，那么， 这个`buffer`应该设置为多大呢，它应该能够容纳允许的最大消息，因此如果允许的最大消息时`1MB`，那么`Message Reader`内部的`buffer`至少是`1MB`。但这并不现实，如果一个连接配置`1MB`的`buffer`，那么当连接数达到百万级时，将会耗费`1TB`的内存。

**可变大小的Buffer**

另外一个选项就是在每个`Message Reader`内部实现一个可变大小的`buffer`，这个可变的`buffer`初始化时可能会很小，如果来的消息很大，`buffer`就会扩容，这样对于每个连接就没必要配置固定大小`1MB`的`buffer`，而是针对其消息大小配置相应的`buffer`。实现可变大小`buffer`的方法如下：

**复制法**

假设`buffer`的初始化大小是`4KB`，如果消息无法存入该`buffer`，就会重新再内存中开辟一个`8KB`的`buffer`，然后将`4KB buffer`中的数据复制到`8KB buffer`中。复制法的一个好处就是消息数据可以被保存在一个连续的字节数组中，这会使后续的消息解析更简单。而缺点也很明显，对于更大的消息会产生更多的数据复制。
为了减少数据复制，你可以分析流经系统的消息大小，从而找到一个可以减少复制数量的`buffer`大小，例如，你可能发现大部分消息由于包含很小的请求/响应都小于`4KB`，这意味着第一个`buffer`大小可以被设置为`4KB`，然后你发现如果消息大小由于包含文件大于`4KB`，而大部分文件都小于`128KB`，那么你可以设置第二个`buffer`为`128KB`，最后你发现如果消息大小大于`128KB`，并且这些消息没有固定的格式，那么你应该设置最后一个`buffer`为所有消息大小的最大值。
有了这三个`buffer`，你就可以减少一定的数据复制。小于`4KB`的消息永远不会被复制；介于`4KB`和`128KB`间的消息只会复制一次，并且只有`4KB`的数据会被复制到`128KB`的`buffer`中；介于`128KB`和最大值之间的消息会被复制两次，首先`4KB`被复制，第二次`128KB`被复制，因此总共有`132KB`的数据会被复制，如果这类大型的消息不多，这些复制量是可以接受的。
如果消息被处理过，那么开辟的内存会被释放，下一次从相同连接进来的消息又会先存入最小的`buffer`，因此在连接之间有效的共享内存是非常必要的，大多数情况下，不是所有的连接会同时需要较大的`buffer`。

**续尾法(append)**

另外一个实现可变大小`buffer`的方式就是将其拆分成多个数组，当你需要扩容`buffer`的时候只需要开辟额外的数组并将数据写入即可。
有两种方式实现，其一是开辟多个字节数组并维护一个这些数组组成的列表集合；另一个方式是`to allocate slices of a larger, shared byte array, and then keep a list of the slices allocated to the buffer.`
这两种方式实现可变大小的`buffer`的有点很明显，就是不需要数据复制，直接可以将从`channel`读取到的数据复制到数组或者切片中。缺点也很明显，即消息数据不是存储在一个连续的数组中，这会使消息解析很困难。

**TLV编码消息**

一些协议消息会使用`TLV`格式进行编码(`Type/Length/Value`)，这意味着当收到消息时，消息的总长度存储在消息的开头，这样你就可以直接知道了应该为整个消息分配多大的内存。
`TLV`编码消息使得内存管理更加简单，你知道需要为消息分配多少内存，在`buffer`的末端不会有多余的内存被浪费。
`TLV`编码的一个缺点是你需要在所有的消息数据都接收之前就分配好所有的内存，有一些发送大消息的慢连接会使你用掉所有的内存，导致服务无法响应。
一个解决方案是使用包含多个`TLV`域的消息格式，这样可以为每个域分配内存，而不是为整个消息，并且只有当域接收时才会分配内存，但是一个很大的域和大消息一样会影响你的内存管理。
另一个解决方式是给每个还未接收的消息计时(`10-15s`)，这可以使服务在许多大消息同步达到的情况下恢复，但仍然会使服务器无法响应一会，另外，`DOS`也会导致你的服务器内存爆满。
`TLV`编码存在一些变形，具体使用多少字节来指明域的类型和长度依赖于`TLV`的编码细节，也有一些`TLV`编码会将域的长度放在开头，然后是类型，最后才是值(也就是`LTV`编码)，尽管域的序列不同，它仍然算是`TLV`编码。
`HTTP 1.1`协议如此糟糕的原因之一是`TLV`编码使得内存管理更加简单，这也是`HTTP 2.0`正在解决的一个问题，在`HTTP 2.0`中数据是通过`LTV`编码帧来传输的。

## 写部分消息 ##
在非阻塞`IO`管道中写数据也是一个挑战，当你在非阻塞的`channel`上调用`write(ByteBuffer)`时，无法保证`ByteBuffer`中有多少字节被写出的，`write(ByteBuffer)`方法返回了写出的字节数，因此可以追踪已写的字节数量，这就是关键所在：跟踪部分写出的数据，保证最后消息的所有字节都被发送出去。
为了管理向`channel`写入部分消息，我们会创建`Message Writer`，和`Message Reader`一样，对于每一个`channel`配置一个`Message Writer`，在`Message Writer`内部我们会跟踪写入的消息字节数量。
`Message Writer`内部维护了一个队列，当更多的消息到达时，需要入队，`Message Writer`会尽快将其写入`Channel`。

![Message Writer][6]

如果你有大量的连接，那么会存在大量的`Message Writer`实例(对于每个`channel`都会分配一个`Message Writer`)，检查大量的`Message Writer`实例是否有数据可写是非常慢的，首先很多`Message Writer`实例没有任何消息发送，这些实例没必要检查；其次不是所有的`channel`实例都准备好被写入数据，这些通道也无法写入数据。
检查`channel`是否写准备可以通过注册`Selector`，但是我们不想给每个`channel`都注册选择器，因为可能会有大量的连接是空闲的，它们默认是写准备的。
为了避免检查所有的`Message Writer`和`Channel`实例，采用两种方式：

 - 当消息写入`Message Writer`时，`Message Writer`就会将其关联的`Channel`注册到`Selector`上(如果还没有注册)
 - 当你的服务器空闲时，它会检查`Selector`确定哪个`Channel`写准备，对于每个写准备的`Channel`，它关联的`Message Writer`就会将数据写入`Channel`，如果`Message Writer`将所有消息数据全部写入，那么会解除`Channel`和`Selector`的注册关系
 
这两点确保只有存在数据写入的`Channel`才会和`Selector`注册。

## 汇总 ##
非阻塞服务器需要时不时检查接收的数据来判断是否收到完整的消息。同样也要随时检查是否有数据要写。
总之，一个非阻塞服务器最后都会有三条管道：

 - 读管道：从开启的连接检查新接收的数据
 - 处理管道：处理接收的完整的消息
 - 写管道：检查是否能够向任何开启的连接写入消息

这三条管道会在循环中反复执行，你可以优化这个过程，例如，如果没有消息排队，你可以直接跳过写管道，或者如果没有新的、完整的消息接受，你可以跳过处理管道。
 
![三条管道流程][7]

## 服务器线程模型 ##
这个非阻塞服务器实现使用了2个线程的线程模型，第一个线程从`ServerSocketChannel`接收到来的连接，第二个线程处理接受的连接，这意味着读消息、处理消息和写入相应。

![Non Blocking Server Thread Model][8]


  [1]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-1.png
  [2]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-2.png
  [3]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-4.png
  [4]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-5.png
  [5]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-6.png
  [6]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-8.png
  [7]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-9.png
  [8]: http://tutorials.jenkov.com/images/java-nio/non-blocking-server-10.png