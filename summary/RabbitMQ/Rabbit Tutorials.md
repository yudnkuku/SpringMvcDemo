# Rabbit Tutorials

标签（空格分隔）： Rabbit

---

此系列是`Rabbit MQ`的官方教程。
## 1、Hello World ##
**引言**

`RabbitMQ`是一个消息代理：它接收并转发消息，你可以将它看成一个邮局，当你将邮件放进邮箱时，你可以确定邮件最后会被送到接收方，在这个比喻中，`RabbitMQ`就是邮箱、邮局和送信人。这其中的主要区别就是`RabbitMQ`不会处理邮件，相反他只会接收、存储和转发二进制数据-消息。

`RabbitMQ`由三部分组成：

 - `Producer`：发送消息一方
 - `Queue`：`RabbitMQ`内部类似于邮箱的定义，尽管消息会在`RabbitMQ`和你的应用中流转，但他们只能存储在队列内部，队列只会受限于主机的内存和磁盘大小，本质上是一个很大的消息`buffer`，多个生产者可以往同一个队列发送消息，多个消费者可以从同一个队列接收消息
 - `Consumer`：消费者

生产者、消费者和消息代理不一定在同一个主机上，一个应用可以同时是生产者和消费者。
 
 
**发送**

创建连接到`Rabbit MQ`服务器：

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
        
    }
    
连接抽象了底层的`socket`连接，并且屏蔽了协议版本的协商和认证细节，这里我们连接到本机的`broker`代理，所以填写的`localhost`，如果是连接到其他主机上的`broker`代理，那么需要指明其`ip`地址，下一步，创建了一个`channel`，可以使用`try-with-resources`，因为`Connection`和`Channel`都实现了`java.io.Closeable`接口，就没必要再代码中显式关闭连接或者通道。

为了发送数据还需要声明一个发送队列：

    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    String message = "Hello World!";
    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
    System.out.println(" [x] Sent '" + message + "'");
    
如果第一次使用`RabbitMQ`没有看到发送的消息，可能是由于代理启动时没有配置足够的硬盘空间(默认需要至少200`MB`)，因此会拒绝接受消息，检查代理的日志文件，必要时减少限制值`disk_free_limit`。

**接收**

## 2、Work Queues ##
工作队列的主要思想是避免立即执行资源密集型任务并等待它完成，想法我们会将该任务延后执行，我们会将任务包装成一个`message`，并将其发送到队列中，工作线程会从任务队列中弹出任务并执行，当你开启多个工作线程时，这些任务会共享。

这个概念在`web`应用中非常有效，在非常短的`http`请求窗口期内无法处理复杂的任务。

**Round-robin(轮询调度)**

如果存在多个消费者，`Rabbit MQ`会将消息均匀地分派到多个消费者，即每个消费者都会拿到相同数量的消息，这种分配消息的方式称为`round-robin`

**消息确认机制**

处理任务会花费几秒钟，如果在执行任务的过程中消费者挂了会发生什么？一旦`RabbitMQ`将消息发送到消费者，它会将消息立即标记为可删除状态，这种情况下，如果手动杀死了工作线程，那么会丢失正在处理的消息，我们也会丢失那些分配到这个工作线程上还没有处理的所有消息，但是我们不想丢失任何任务，如果工作线程挂了，我们希望任务重新分配到一个新的工作线程处理。

为了确保消息不丢失，`Rabbit MQ`支持消息确认机制，一个`ack`信号会由消费者返回告诉`Rabbit MQ`该消息已经收到、处理且可以自由删除之。

如果消费者挂了(通道、连接或者`tcp`连接关闭)且没有返回`ack`，那么`Rabbit MQ`就知道该消息没有完整的处理，并重新入队，随后将其派发到另一个新的消费者，这样就可以确保即使工作线程偶尔会挂掉，也不会有任何消息丢失。

消息确认默认是打开的，可以通过设置`autoAck=true`来关闭之，并在处理完任务之后手动发送确认。

    channel.basicQos(1); // accept only one unack-ed message at a time (see below)
    
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
    
      System.out.println(" [x] Received '" + message + "'");
      try {
        doWork(message);
      } finally {
        System.out.println(" [x] Done");
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }
    };
    boolean autoAck = false;
    channel.basicConsume(TASK_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });

上述代码就可以确保消息不会丢失，确认必须和接收在同一个通道上，否则可能会造成通道级别的协议异常。

如果忘记`basicAck`，那么可能会造成严重的错误，`RabbitMQ`会由于无法释放未确认的消息吃掉越来越多的内存。

**消息持久化**

前面已经说到了在消费者挂掉之后如何确保消息不丢失，但是如果`RabbitMQ`服务宕机，任务如果不丢失呢。这时我们需要将队列和消息都标记为持久化的，例如将队列声明为持久化队列：

    boolean durable = true;
    channel.queueDeclare("hello", durable, false, false, null);

使用`MessageProperties`相关属性来设置消息的持久级别：

    channel.basicPublish("", "task_queue", MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes());


上述设置还无法保证消息不会丢失，尽管这些配置会告诉`RabbitMQ`将消息持久化到硬盘，但是仍然由短暂的窗口期`RabbitMQ`接收到消息但没有保存，或者`RabbitMQ`还没有对每个消息执行`fsyn(2)`，仅仅只是将消息存入缓存没有写入硬盘，这种持久化方式还不够，可以通过`publisher confirm`机制来获取更强的保证。

**公平分发**

在某个场景中，例如所有的奇数号任务耗时长，偶数号任务耗时短，一个工作线程一直繁忙，另一个工作线程却一直空闲，可以使用`basicQos`方法，并来实现更加合理的分发，`RabbitMQ`直到某工作线程处理完某个任务并应答后才会向其再次分发新的消息，否则会将消息分发到下一个空闲的工作线程。

    int prefetchCount = 1;
    channel.basicQos(prefetchCount);


如果所有的工作线程都处于忙碌状态，队列可能会满载，这时需要添加更多的消费者线程或者使用其他策略。

## 3、发布/订阅 ##

## 4、路由 ##
**绑定**

之前我们这样神恶鬼名队列：

    channel.queueBind(queueName, EXCHANGE_NAME, "");
    
绑定是交换器和队列之间的关系，可以简单地理解为：队列对这个交换器中的消息感兴趣。

绑定可以声明额外的路由键参数`routingKey`，例如如下代码：

    channel.queueBind(queueName, EXCHANGE_NAME, "black");
    
绑定键的意义取决于交换器的类型，对于`fanout`类型的交换器，会忽略这个参数

## 5、主题 ##
**主题交换器**

发往主题交换器的消息不能使用任意的路由键，它必须是由单词组成，由点号分割，一些合理的路由键如下：`stock.usd.nyse`，`nyse.vmw`等，绑定键必须是同样的格式，主题交换器和`direct`交换器背后的逻辑类似：带有特定路由键的消息会被发送到所有绑定到匹配的绑定键的队列，绑定键的格式通常如下：

 - `*`表示一个单词
 - `#`表示0个或者多个单词


 
## 7、发布者确认 ##
发布者确认是`RabbitMQ`的实现了可靠发布的拓展，当通道上发布者确认开启时，客户端发布的消息会被代理异步确认，意味着服务端已经接收到该消息。

**开启发布确认**

发布确认是`RabbitMQ`针对`AMQP 0.9.1`协议的拓展，默认是关闭的，可以通过`confirmSelect`开启：

        Channel channel = connection.createChannel();
    channel.confirmSelect();
    
**策略1 单独发布消息**

代码段：

    while (thereAreMessagesToPublish()) {
    byte[] body = ...;
    BasicProperties properties = ...;
    channel.basicPublish(exchange, queue, properties, body);
    // uses a 5 second timeout
    channel.waitForConfirmsOrDie(5_000);
}

这个例子会发布消息，并通过`Channel#waitForConfirmasOrDie(long)`来等待服务端确认，方法会在消息确认后立即返回，如果消息在超时时间内没有确认或者处于`nack-ed`状态(代理无法处理该消息)，那么方法会抛出一个异常，该异常的处理通常是将错误信息记入日志并重新发送消息。

这个方法非常直接，但是有个明显的缺点：会显著降低发布的速度，因为消息的确认会阻塞随后所有消息的发布。

**策略2 批量发布消息**

我们可以批量发布消息，并等待整个批量消息的确认，例如下面的例子批量发送100条消息：

    int batchSize = 100;
    int outstandingMessageCount = 0;
    while (thereAreMessagesToPublish()) {
        byte[] body = ...;
        BasicProperties properties = ...;
        channel.basicPublish(exchange, queue, properties, body);
        outstandingMessageCount++;
        if (outstandingMessageCount == batchSize) {
            ch.waitForConfirmsOrDie(5_000);
            outstandingMessageCount = 0;
        }
    }
    if (outstandingMessageCount > 0) {
        ch.waitForConfirmsOrDie(5_000);
    }

这种批量确认的方式可以提升吞吐量，缺点是如果失败不知道失败原因，因此需要将整个批量消息都保存在内存中并记录相关日志信息，或者重新发布消息，这种方式也是同步的，依然会阻塞消息的发布。

**策略3 异步处理消息确认**

代理异步确认发布的消息，只需要注册一个监听即可。

    Channel channel = connection.createChannel();
    channel.confirmSelect();
    channel.addConfirmListener((sequenceNumber, multiple) -> {
        // code when message is confirmed
    }, (sequenceNumber, multiple) -> {
        // code when message is nack-ed
    });
    
这里有两个回调：一个是确认消息，一个是未确认消息(可以认为是代理丢失的消息)，每个回调都有2个参数：

 - 序列编号(`sequence number`):识别确认或者未确认的消息编号
 - `multiple`:布尔值，如果是`false`，只有一个消息是确认或者未确认，否则小于等于该`sequence number`的消息都是确认或者未确认状态
 
将消息和序列号关联起来的一个简单方式是使用`map`，例如：

    ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
    // ... code for confirm callbacks will come later
    String body = "...";
    outstandingConfirms.put(channel.getNextPublishSeqNo(), body);
    channel.basicPublish(exchange, queue, properties, body.getBytes());
    
发布代码会通过这个`map`来跟踪发送的消息，如果确认到达，需要清理这个`map`，当`nack-ed`到达时需要记录警告日志。

    ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
    ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
        if (multiple) {
            ConcurrentNavigableMap<Long, String> confirmed = outstandingConfirms.headMap(
              sequenceNumber, true
            );
            confirmed.clear();
        } else {
            outstandingConfirms.remove(sequenceNumber);
        }
    };
    
    channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
        String body = outstandingConfirms.get(sequenceNumber);
        System.err.format(
          "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
          body, sequenceNumber, multiple
        );
        cleanOutstandingConfirms.handle(sequenceNumber, multiple);
    });

// ... publishing code

**如何追踪未处理的确认消息**

例子中使用了`ConcurrentNavigableMap`来跟踪未处理的确认消息，使用这个数据结构有多个原因，它可以很容易的将消息和序列号关联起来，很容易清理特定序列号以下的`entries`，最后支持并发访问，因为确认回调会在客户端库拥有的线程中调用，这和发布线程不是同一个线程。

总结一下，异步处理发布确认通常需要一下几步：

 - 提供一种方式将发布序列号和消息绑定起来
 - 在通道上注册确认监听，当发布确认或者未确认到达时触发监听执行相关操作，例如记录日志或者重新发布未确认消息，序列号-消息关联机制也需要执行相关的清理工作
 - 在发布消息之前追踪发布序列号
 
**是否需要重新发布未确认的消息**

在相应的回调中重新发布未确认的消息是具有吸引力的，但是应该避免这一操作，因为确认回调通常在`I/O`线程中调用，在该线程中通常不应该对`channel`执行任何操作，一个更好的替代方案是将消息压入一个内存队列中，发布线程从该队列中拉取消息，例如`ConcurrentLinkedQueue`就是个不错的选择，可以在确认回调线程和发布线程之间传递消息。

**总结**

确保发布的消息成功到达代理对于某些应用是至关重要的，发布确认是`RabbitMQ`实现这个需求的重要特性，发布确认天生就是异步的，但是也可以同步处理，没有一种确定的方式去实现发布确认，通常和应用的限制和和整个系统有关，典型的技术如下：

 - 单个发布消息，同步等待确认：简单但是吞吐量不高
 - 批量发布消息，同步等待批量消息的确认：简单，吞吐量客观但是很难排错
 - 异步处理：性能、资源利用率最佳，极强的错误控制