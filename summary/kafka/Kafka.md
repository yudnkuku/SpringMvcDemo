# Kafka

标签（空格分隔）： MQ

---

## Kafka创建背景 ##
`Kafka`是一个消息系统，原本开发自`LinkedIn`，用作`LinkedIn`的活动流和运营数据处理管道的基础，现在它已被多家不同类型的公司作为多种类型的数据管道和消息系统使用。

活动流数据是几乎所有站点在对其网站使用情况做报表时都要用到的数据中最常规的部分，活动数据包括页面访问量、被查看内容方面的信息以及搜索情况等内容，这种数据通常的处理方式先把各种活动以日志的形式写入某种文件，然后周期性地对这些文件进行统计分析，运营数据指的是服务器的性能数据(`CPU`、`IO`使用率、请求时间、服务日志等等数据)。运营数据的统计方法种类繁多。

## Kafka简介 ##
`Kafka`是一种分布式的，基于发布/订阅的消息系统，主要设计目标如下：

 - 以时间复杂度为`O(1)`的方式提供消息持久化能力，即使对`TB`级以上数据也能保持常数时间复杂度的访问性能
 - 高吞吐率，即使在非常廉价的商用机器上也能做到单机支持每秒100K条以上消息的传输
 - 支持`Kafka Server`间的消息分区以及分布式消费，同时保证每个`Partition`内的消息顺序传输
 - 同时支持离线数据处理和实时数据处理
 - `Scale Out`：支持在线水平拓展

## Kafka基础概念 ##

**1、生产者和消费者**

生产者创建消息，消费者负责消费或者读取消息

**2、主题(Topic)和分期(Partition)**

在`Kafka`中，消息以主题来分类，每一个主题都对应一个消息队列，`Topic`只是一个逻辑概念，每个`Topic`都包含一个或多个`Partition`，不同的`Partition`可位于不同的节点，同时`Partition`在物理上对应一个本地文件夹，每个`Partition`包含一个或者多个`Segment`，每个`Segment`包含一个数据文件和一个与之对应的索引文件，在逻辑上，可以把一个`Partition`当做一个非常长的书作，可通过这个数组的索引(`offset`)来访问数据

**3、Broker和集群(Cluster)**

一个`Kafka`服务器也称为`Broker`，它接受生产者发送的消息并存入磁盘，`Broker`同时服务消费者拉取分区消息的请求，返回目前已经提交的消息。若干个`Broker`组成一个集群(`Cluster`)，其中集群内某个`Broker`会成为集群控制器(`Cluster Controller`)，它负责管理集群，包括分配分区到`Broker`、监控`Broker`故障等。在集群内，一个分区由一个`Broker`负责，这个`Broker`也称为这个分区的`Leader`；当然一个分区可以被复制到多个`Broker`上实现冗余，这样当`Broker`故障时可以将其分区重新分配到其他`Broker`来负责

## Kafka的设计与实现 ##
**1、Kafka存储在文件系统之上**

`Kafka`高度依赖文件系统来存储和缓存消息，上述的`Topic`其实是逻辑上的概念，面向生产者和消费者，物理上存储的其实是`Partition`，每一个`Partition`最终对应一个目录，里面存储所有的消息和索引文件，默认情况下，每一个`Topic`在创建时如果不指定`Partition`数量时只会创建一个`Partition`。比如，我创建了一个`Topic`名字为`test`，没有指定`Partition`的数量，那么会默认创建一个`test-0`的文件夹，这里的命名规则是：`<topic_name>-<partition_id>`

任何发布到`Partition`分区的消息都会被追加到`Partition`数据文件的尾部，这样的顺序写磁盘操作让`Kafka`的效率非常高，每一条消息都被发送到`Broker`中，会根据`Partition`规则选择被存储到哪一个`Partition`，如果`Partition`规则设置的合理，所有消息可以均匀分布到不同的`Partition`中

**2、Kafka中的底层存储设计**

假设现在`Kafka`集群中只有一个`Broker`，我们创建2个`Topic`名称分别为：`topic1`和`topic2`，`Partition`数量分别为1和2，那么我们的根目录下就会创建如下三个文件夹：

    topic1-0
    topic2-0
    topic2-1
    
在`Kafka`的文件存储中，同一个`Topic`下有多个不同的`Partition`，每个`Partition`都为一个目录，而每一个目录又被平均分成多个大小相等的`Segment File`中，`Segment File`又由`index file`和`data file`组成，它们总是成对出现，后缀`.index`和`.log`分别表示`Segment`索引文件和数据文件，现在假设我们设置每个`Segment`大小为500`MB`，并启动生产者向`topic1`中写入大量数据，`topic1-0`文件夹中就会产生类似如下的一些文件：

    topic1-0
        0000000000000000.index
        0000000000000000.log
        0000000000036879.index
        0000000000036879.log
    topic2-0
    topic2-1
    
`Segment`是`Kafka`文件存储的最小单位，`Segment`文件命名规则：`Partition`全局的第一个`Segment`从0开始，后续每一个`Segment`文件名为上一个`Segment`文件最后一条消息的`offset`值，其中以索引文件中元数据`<3,497>`为例，依次在数据文件中表示第3个`message`(在全局`Partition`表示第368769+3=368772个`message`)以及该消息的物理偏移地址为497。

因为其文件名为上一个 `Segment` 最后一条消息的 `offset` ，所以当需要查找一个指定 `offset` 的 `message` 时，通过在所有 `segment` 的文件名中进行二分查找就能找到它归属的 `segment` ，再在其 `index` 文件中找到其对应到文件上的物理位置，就能拿出该 `message` 。

![segment_file][1]

**3、消费者设计概要**

假设这么个场景：我们从`Kafka`中读取消息，并且进行检查，最后产生结果数据，我们可以创建一个消费者实例去做这件事情，但如果生产者写入消息的速度比消费者读取的速度快怎么办呢，随着时间的增长，消息堆积越来越严重，对于这种场景，我们需要增加多个消费者来进行水平拓展。

`Kafka`消费者是消费者组的一部分，当多个消费者形成一个消费组来消费主体时，每个消费者会收到不同分区的消息，假设有一个`T1`主题，该主题有4个分区；同时我们有一个消费组`G1`，这个消费组只有一个消费者`C1`，那么消费者`C1`将会收到这4个分区的消息，如下所示：

![4-1][2]

如果我们增加新的消费者`C2`到消费组`G1`，那么每个消费者将会分别收到两个分区的消息，如下所示：

![4-4][3]

如果增加4个消费者，那么每个消费者将会分别收到一个分区的消息，如下所示：

![2groups][4]

但如果我们继续增加消费者到这个消费组，剩余的消费者将会空闲，不会收到任何消息：

![more_consumers][5]

总而言之，我们可以通过增加消费组的消费者来进行水平扩展提升消费能力，这也是为什么建议创建主题时使用比较多的分区数，这样可以在消费负载高的情况下增加消费者来提升性能。另外，消费者的数量不应该比分区数多，因为多出来的消费者是空闲的，没有任何帮助。

`Kafka`一个很重要的特性就是，只需写入一次消息，可以支持任意多的应用读取这个消息，换句话说，每个应用都可以读到全量的消息，为了使得每个应用都能读到全量消息，应用需要有不同的消费组，对于上面的例子，假如我们新增了一个新的消费组`G2`，而这个消费组有两个消费者，那会是这样：

![2groups][6]

在这个场景中，消费组`G1`和消费组`G2`都能收到`T1`主题的全量消息，在逻辑意义上来说它们属于不同的应用，最后总结起来就是：如果应用需要读取全量消息，那么请为该应用设置一个消费组，如果该应用消费能力不足，那么可以考虑在这个消费组中增加消费者。

**消费者与分区重平衡**

可以看到，当新的消费者加入消费组，它会消费一个或多个分区，而这些分区之前是由其他消费者负责的；另外，当消费者离开消费组（比如重启、宕机等）时，它所消费的分区会分配给其他分区。这种现象称为重平衡（`rebalance`）。重平衡是 `Kafka` 一个很重要的性质，这个性质保证了高可用和水平扩展。不过也需要注意到，在重平衡期间，所有消费者都不能消费消息，因此会造成整个消费组短暂的不可用。而且，将分区进行重平衡也会导致原来的消费者状态过期，从而导致消费者需要重新更新状态，这段期间也会降低消费性能。后面我们会讨论如何安全的进行重平衡以及如何尽可能避免。

消费者通过定期发送心跳（`hearbeat`）到一个作为组协调者（`group coordinator`）的 `broker` 来保持在消费组内存活。这个 `broker` 不是固定的，每个消费组都可能不同。当消费者拉取消息或者提交时，便会发送心跳。

如果消费者超过一定时间没有发送心跳，那么它的会话（`session`）就会过期，组协调者会认为该消费者已经宕机，然后触发重平衡。可以看到，从消费者宕机到会话过期是有一定时间的，这段时间内该消费者的分区都不能进行消息消费；通常情况下，我们可以进行优雅关闭，这样消费者会发送离开的消息到组协调者，这样组协调者可以立即进行重平衡而不需要等待会话过期。

**Partition与消费模型**

上面提到，`Kafka`中一个`topic`中的消息是被打散分配在多个`Partition`中存储的，`Consumer Group`在消费时需要从不同的`Partition`获取消息，那最终如何重建出`Topic`中消息的顺序呢？

答案是：没有办法，`Kafka`只会保证在`Partition`内消息是有序的，而不管全局的情况

下一个问题是：`Partition`中的消息可以被不同的`Consumer Group`多次消费，那么`Partition`中被消费的消息是何时删除的？`Partition`又是如何知道一个`Consumer Group`当前消费的位置？

无论消息是否被消费，除非消息到期`Partition`从不删除消息，例如设置保留时间为2天，则消息发布2天内任何`Group`都可以消费，2天后，消息自动被删除，`Partition`会为每个`Consumer Group`保存一个偏移量，记录`Group`消费到的位置，如下图所示：

![consumer_group_offset][7]

**消费组**

使用`Consumer high level API`时，同一`topic`的一条消息只能被同一个`Consumer Group`内的一个`Consumer`消费，但多个`Consumer Group`可同时消费这一消息。

![consumer_group][8]

这是`Kafka`用来实现一个`Topic`消息的广播(发送给所有`Consumer`)和单播(发送给某一个`Consumer`)的手段，一个`Topic`可以对应多个`Consumer Group`，如果需要实现广播，只要每个`Consumer`有一个独立的`Group`即可，要实现单播只需要所有的`Consumer`在同一个`Group`里，用`Consumer Group`还可以将`Consumer`进行自由的分组而不需要多次发送消息到不同的`Topic`。

**Push and pull**

作为一个消息系统，`Kafka`遵循了传统的方式，选择由`Producer`向`broker Push`消息并有`Consumer`从`broker pull`消息，`push`模式很难适应消费速率不同的消费者，因为消息发送速率是由`broker`决定的，`push`模式的目标是尽可能最快速度传递消息，但是这样容易造成`Consumer`来不及处理消息，典型的表现就是拒绝服务以及网络阻塞，而`Pull`模式则可以根据`Consumer`的消费能力以适当的速率消费消息，`pull`模式更合适，`pull`模式可简化`broker`的设计，`Consumer`可以自己控制消费方式-即可批量消费也可逐条消费，同时还能选择不同的提交方式从而实现不同的传输语义。

 


  [1]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/Segment_File.png
  [2]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/4-1.png
  [3]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/4-4.png
  [4]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/2groups.png
  [5]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/more_consumers.png
  [6]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/2groupcom/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/more_consumers.png
  [7]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/consumer_group_offset.png
  [8]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/kafka/img/Consumer_group.png