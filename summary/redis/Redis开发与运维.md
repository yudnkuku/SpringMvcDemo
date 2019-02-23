# Redis开发与运维

标签（空格分隔）： 读书笔记

---

Chapter 1 初识Redis
------

`redis`提供了很多命令行工具：

1 启动`redis`服务端

    redis-server /path/to/config
    
2 启动`redis`客户端

    redis-cli #启动redis客户端
    redis-cli shutdown #停止redis服务
    
## Chapter 2 API的理解和使用 ##
常用命令：

    keys * #输出所有键
    dbsize #输出键总数
    exists key #key是否存在
    del key [key...] #删除键
    expire key seconds #设置过期时间
    ttl key #返回key剩余过期时间 -1表示没有设置过期时间 -2表示键不存在
    type key #key的数据类型
    
**数据结构和编码**

`type`命令只会输出键的数据结构类型，例如`string/list/set/hset/zset`，但是每种数据类型在内部都会有自己的编码实现，例如`list`结构就有`ziplist`和`linkedlist`两种编码，`ziplist`比较省内存，查看编码：

    object encoding key
    
**单线程架构**

`Redis`使用了单线程架构和`I/O`复用技术来实现高性能的内存数据库服务。通常来说单线程的处理能力比多线程差，但是`Redis`的单线程模型能够达到每秒万级别的处理能力，其原因归纳为三点：

 - 纯内存访问，`Redis`将所有数据放入内存中，响应速度很快
 - 非阻塞`I/O`，`Redis`使用`epoll`作为`I/O`多路复用技术的实现，再加上`Redis`自身的事件处理模型将`epoll`中的连接/读写/关闭都转换为事件，不再网络`I/O`上浪费过多的时间
 - 单线程避免了线程切换和竞态产生的消耗

**字符串**

常用命令：

    set/get key value 
    setex key seconds value #设置键和过期时间
    setnx key value #当键不存在时才能设置，可用于实现分布式锁
    set key value xx #当键存在时更新值
    mset/mget key value [key value...] #批量设置/获取
    incr/decr key #加/减1
    incrby/decrby key val #加/减
    append key value #追加value
    strlen key #返回键值长度
    
内部编码：

字符串的内部编码有三种：

 - `int`:8个字节的长整形
 - `embstr`:小于等于39字节的字符串
 - `raw`:大于39字节的字符串

**哈希-HSET**

常用命令：

    hset key field value 
    hget key field
    hdel key field [field...]
    hlen key
    hmget key field [field...] #批量获取
    hmset key field value [field value...] #批量设置
    hexists key field 
    hkeys key
    hvals key
    hgetall key
    
内部编码：

哈希类型的内部编码方式有两种：

 - `ziplist`:当哈希类型元素个数小于`hash-max-ziplist-entries`(默认512)，并且每个元素的值都小于`hash-max-ziplist-value`(默认64)时，会使用`ziplist`作为内部编码实现，节省内存
 - `hashtable`:反之则会使用`hashtable`,其读写时间复杂度为`O(1)`

**列表-LIST**

常用命令：

    rpush/lpush key value [value...]
    rpop/lpop key 
    linsert key before|after privot value #在privot前后插入元素
    lrange key start end #获取指定范围元素，最左是0，最右是-1，例如lrange key 0 -1就是获取所有元素
    lindex key index #查找index元素
    ltrim key start end #按照范围修剪表
    lset key index newValue #设置值
    blpop/brpop key [key...] timeout #阻塞式弹出元素，阻塞一直到list中有元素为止或者到达超时时间timeout，如果timeout=0，将一直阻塞下去
    
    
内部编码：

列表内部编码有两种：

 - `ziplist`:当列表元素个数小于`list-max-ziplist-entries`(默认512)，且列表中每个元素的长度小于`list-max-ziplist-value`(默认64字节)，会使用`ziplist`来作为内部编码实现，省内存
 - `linkedlist`:反之则会使用`linkedlist`作为实现

使用场景：

    lpush + lpop = Stack
    lpush + rpop = Queue
    lpush + ltrim = Collection(有限集合)
    lpush + brpop = Message Queue(阻塞队列)
    
**集合-SET**

常用命令：

    sadd key value [value...]
    srem key value [value...]
    scard key #计算元素个数
    sismember key value 
    srandmember key #随机返回一个元素
    spop key #随机弹出一个元素
    smembers key 
    sinter/sunion/sdiff key [key] #求交集/并集/差集
    sinterstore/sunionstore/sdiffstore key [key...] #求交集/并集/差集并存储到指定键中
    
内部编码：

集合类型的内部编码有两种：

 - `intset`:当集合中所有元素都是整形，且元素个数小于`set-max-inset-entries`(默认512)时，会使用`intset`来作为内部编码实现，省内存
 - `hashtable`:否则使用`hashtable`作为内部编码实现

**有序集合-ZSET**

常用命令：

    zadd key score member [score member...] #增加成员，分数在前，成员在后
    zcard key 
    zscore key member 
    zrank/zrevrank key member #排序
    zrem key member [member...]
    zincrby key increment member #增加分数，也是分数在前
    zrange/zrevrange key start end [withscores] #返回指定排名区间的元素
    zrangebyscore/zrevrangebyscore key min max [withscores] [limin offset count] #返回指定区间分数的元素，min/max支持开区间(左括号)和-inf(负无穷)/+inf(正无穷)
    zcount key min max 
    zremrangebyrank key start end
    zremrangebyscore key min end
    zinterstore/zunionstore destination numkeys key [key...] [weights [weight...]] [aggregate sum|min|max] #求交集/并集，例如zinterstore des 2 key1 key2 weights 1 0.5 aggregate max
    
内部编码：

有序集合的内部编码有两种：

 - `ziplist`:当有序集合元素个数小于`zset-max-ziplist-entries`(默认时128)，且每个元素的长度小于`zset-max-ziplist-value`(默认时64字节)时，会使用`ziplist`作为内部编码实现，节省内存
 - `skiplist`:否则使用`skiplist`作为内部编码实现

**键管理命令**
  

    expire key seconds
    expireat key timestamp 
    ttl key #注意set命令也会清除过期是时间
    persist key #取消过期时间
    scan #渐进式遍历
    
如果`Redis`包含了大量的键，执行`keys`命令很可能会造成`Redis`阻塞，所以一般建议不要在生产环境下使用`keys`命令，但有时候确实有遍历键的需求，`Redis`存储键值对实际使用的是`hashtable`数据结构，那么可以使用`scan`命令来渐进式遍历：

    scan cursor [match pattern] [count number] 
    

 - `cursocr`是必需参数，每次遍历结束都会返回下一次遍历的游标
 - `match pattern`:模式匹配
 - `count number`:指每次要遍历的键个数，默认是10，可以适当增大

**数据库管理**

    select dbIndex #默认16个db,当前db默认是0
    flushdb/

 
    

 
    
    

 
    

 
    

 
    
    

 
