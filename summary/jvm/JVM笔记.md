# JVM笔记

标签（空格分隔）： JVM

---
## Metaspace ##
在`HotSpot`虚拟机中，方法区可以等价于永久代，永久代中存放了以下内容：

 - 虚拟机加载的类信息
 - 常量池
 - 静态变量
 - 即时编译后的代码(`JIT`编译后的代码)

到了`Jdk 1.7`之后，**常量池已经不在永久代中分配内存，而是移到了堆中**，即常量池和对象共享堆内存。而到了`Jdk 1.8`之后，持久代被永久移除，取而代之的是`Metaspace`。

`Metaspace`和永久代最大的区别在于：`Metaspace`并不在虚拟机内存中而是使用本地内存，因此`Metaspace`具体大小理论上取决于操作系统可用内存大小。

 
## 内存溢出和内存泄露的区别 ##
1、内存溢出
内存溢出指的是程序在申请内存的时候，没有足够大的空间可以分配了

2、内存泄露
内存泄露指的是程序在申请内存之后，没有办法释放掉已经申请到的内存，它始终占用着内存，即被分配的对象可达但无用。内存泄露一般都是因为内存中有一块很大的对象，但是无法释放。

## Minor GC和Full GC的区别 ##
1、新生代`GC`(`Minor GC或者YGC`)
指的是发生在新生代的垃圾收集动作，因为大多数`java`对象存活率都不高，所以`Minor GC`非常频繁，一般回收速度也比较快

2、老年代`GC`(`Major GC或者Full GC`)
指发生在老年代或者永久代的垃圾收集动作，出现了`Major GC`，经常会伴随至少一次`Minor GC`(但不是绝对的)，`Major GC`的速度一般比`Minor GC`慢上10倍以上

`java`使用分代垃圾收集算法，即对于新生代和老年代应用的垃圾收集器不一样，例如新生代使用`Parallel New Collector`，而老年代使用`CMS`，`Minor GC`会在新生代`Eden`区没有足够的内存分配给对象时触发，而`JVM`认为`Major GC`(老年代或者永久代的垃圾回收)等价于`Full GC`，在`Minor GC`或者`Major GC`过程中都会`STW`，即用户线程挂起，只会执行`GC`线程，对于`Minor GC`，对象存活的比例低，对象回收的速度很快，因此`STW`的时间一般只有几十毫秒，而对于`Major GC`而言，由于其回收算法的不同(标记清除/`CMS`)回收过程比较耗时，从而导致`STW`的时间长

## YGC过程 ##
1、找出所有可能存活的对象，这是标记过程。
从`GC Roots`开始遍历对象，所有能够遍历到的对象都算是存活对象，打上一个标记(标记清除)

2、存活独享被标记出来后，需要把这些对象从`eden`区或者`from survivor`区复制到`to survivor`，这个过程的耗时和存活对象的大小数量有很大的关系

3、如果开启了`-XX:+PrintGcDetails`，那么在`YGC`过程中，还会记录一些数据到日志中

## 热点代码Hot Spot Code & JIT ##
部分商用虚拟机中，`Java`程序最初是通过解释器对`.class`文件进行解释执行的，当虚拟机发现某个方法或代码块运行地特别频繁的时候，就会把这些代码认定为热点代码`Hot Spot Code`（这也是我们使用的虚拟机`HotSpot`名称的由来）。为了提高热点代码的执行效率，在运行时，虚拟机将会把这些代码编译成与本地平台相关的机器码，并进行各种层次的优化，完成这个任务的编译器叫做即时编译器（`Just In Time Compiler`，即`JIT`编译器）。`JIT`编译器并不是虚拟机必需的部分，`Java`虚拟机规范并没有要求要有`JIT`编译器的存在，更没有限定或指导`JIT`编译器应该如何去实现。但是，`JIT`编译器性能的好坏、代码优化程度的高低却是衡量一款商用虚拟机优秀与否的最关键指标之一。

## 虚拟机性能检测和故障处理工具 ##

**jstat**

`jstat`使用于监视虚拟机各种运行状态信息的命令行工具，它可以显示本地或者远程(需要远程主机提供`RMI`支持)虚拟机进程中的类信息、内存、垃圾收集、`JIT`编译等运行数据，在没有`GUI`，只提供了纯文本控制台环境的服务器上，它将是运行期间定位虚拟机性能问题的首选工具。
常用选项：

|选项|作用|
|:-:|:-:|
|`-class`|监视类装载、卸载数量、总空间及类装载所耗费的事件|
|`-gc`|监视`java`堆状况，包括`Eden`区、两个`Survivor`区、老年代、永久代等的容量、已用空间、合计等信息|
|`-gccapacity`|监视内容基本与`-gc`相同，但输出主要关注`java`堆各个区域使用到的最大、最小空间|
|`-gcutil`|监视内容基本与`-gc`相同，但输出主要关注已使用的空间占总空间的百分比|
|`-gccause`|监视内容基本与`-gc`相同，但是会额外输出导致上一次`gc`产生的原因|
|`-gcnew/-gcold`|监视新生代/老年代`GC`状况|
|`-gcnewcapacity/-gcoldcapacity/-gcpermcapacity`|主要关注新生代/老年代/永久代使用到的最大、最小空间|
|`-compiler`|输出`JIT`编译器编译过的方法、耗时等信息|
|`-printcompilation`|输出已经被`JIT`编译的方法|

**jmap**

`jmap`命令用于生成堆转储快照。如果不使用`jmap`命令，要想获取`java`堆转储，可以使用`-XX:+HeapDumpOnOutOfMemoryError`参数，可以让虚拟机在`OOM`异常出现之后自动生成`dump`文件

常用选项：

|选项|作用|
|:-:|:-:|
|`-dump`|生成`java`堆转储快照|
|`-heap`显示`java`堆的详细信息|
|`-histo`|显示堆中对象统计信息，包括类、实例数量、合计等|

**jstack**

`jstack`命令用于生成虚拟机当前时刻的线程快照，线程快照就是当前虚拟机内每一条线程正在执行的方法堆栈的集合，生成线程快照的目的主要是定位线程长时间出现停顿的原因，如线程间死锁、死循环、请求外部资源导致的长时间等待等都是导致线程长时间停顿的原因。线程出现停顿的时候通过`jstack`来查看各个线程的调用堆栈，就可以知道没有响应的线程到底在后台做什么，或者在等待什么资源。


## 栈溢出 ##
`java`虚拟机规范中描述了如果线程请求的栈深度太深(换句话说方法调用的深度太深)，就会产生栈溢出了。那么，我们只要写一个无限调用自己的方法，自然就会出现方法调用的深度太深的场景

    @Test
    public void testStackOverflow() {
        StackOverflowTest test = new StackOverflowTest();
        try {
            test.stackLeak();   //java.lang.StackOverflowError
        } catch (Exception e) {
            System.out.println("Stack length : " + test.getStackLength());
            throw e;
        }
    }
    
    public class StackOverflowTest {

        private int stackLength = 1;
    
        public void stackLeak() {
            stackLength++;
            stackLeak();
        }
    
        public int getStackLength() {
            return stackLength;
        }
    }

操作系统分配给进程的内存是有限制的，比如32位的`Windows`限制为`2GB`，虚拟机提供了参数来控制`java`堆和方法区这两部分内存的最大值，剩余内存为`2GB-maxHeap-maxPerm`，程序计数器很小就忽略了，虚拟机进程本身的耗费也不算，剩下的内存就是栈的了，每个线程分配到的栈容量越大，可建立的线程数自然就越少，建立线程时就越容易把剩下的内存耗尽

## GC日志分析 ##

**三种time:user/sys/real**
`GC`日志中会出现三种类型的时间：`user/sys/real`，这三个时间实际上来自于`Unix`命令：`time ls`，这首先会输出`ls`命令的结果，输出当前目录下所有的子目录/文件，然后输出该操作的时间信息。

 - `real`：真实时间，从命令调用到结束的时间
 - `user`:进程内部用户代码消耗的时间(内核外)
 - `sys`:进程内核`CPU`耗时，用于内核内部的系统调用

`user+sys`表明了进程消耗的`CPU`实际时间，这里指的是所有`CPU`的时间总和，如果进程有多个线程，这个时间总和很可能会超过`real`时间
 
举例说明，`example1`：

    [Times: user=11.53 sys=1.38, real=1.03 secs]

这里`user+sys`时间之和远远大于`real`时间，这是由于收集日志时间的`JVM`虚拟机可能运行在多核服务器上，并且配置了多条`GC`线程的垃圾收集器，由于`GC`线程并行执行，那么`real`时间当然可能远小于总共的`CPU`时间(`user+sys`)。

`example2`:

    [Times: user=0.09 sys=0.00, real=0.09 secs]
    
对于一些单`GC`线程的垃圾搜集器(如`Serial GC`)，`real=user+sys`

## 导致长GC停顿的原因 ##

**1、高企的对象创建速率**

**2、过小的年轻代**

当年轻代大小过小时，对象会被通过担保机制进入老年代分配内存，而老年代的垃圾收集比年轻代的垃圾收集更耗时，因此适当提高年轻代的内存大小有助于减少长时间的`GC`停顿，可以通过两个方式来增加年轻代的内存大小：

 - `-Xmn`:指定年轻代内存大小
 - `-XX:NewRatio=`:指定老年代和年轻代之间的比值，例如设置`-XX:NewRatio=3`，如果堆内存大小`2GB`，那么年轻代内存大小`0.5GB`
 
**3、选择合适的`GC`算法**

`GC`算法的选择对`GC`停顿时间有显著的影响，建议使用`G1 GC`算法，因为其具备自适应能力(`auto-tuning`)，在`G1 GC`中，你可以设置最大`GC`停顿时间：`-XX:MaxGCPauseMillis=`

**4、进程交换(`Process Swapping`)**

由于缺少内存，操作系统会从内存中置换应用程序，置换操作是非常耗时的，因为他会访问硬盘设备，所以最好禁止进程交换。如果你发现自己的应用程序出现了进程交换，那么可以进行如下操作：

 - 分配更多的`RAM`
 - 减少运行在服务器上的进程数量，释放更多的内存
 - 减少应用程序的堆内存大小(不建议，可能会导致其他问题)

**5、`GC`线程不足**

如果你在`GC`日志中发现：

    [Times: user=25.56 sys=0.35, real=20.48 secs]
    
如果`real`时间不是远远小于`user`时间，这可能表明没有足够的`GC`线程，可以考虑增加`GC`线程的数量，假如`user=25s`，`GC`线程数量是`5`，那么
`real`时间大概为5s，即`real=user/GC thread count`，但是增加太多的`GC`线程会消耗大量的`CPU`资源，因此在增加`GC`线程数量之前需要进行吞吐量测试

**6、`IO`阻塞**

如果有大量的文件系统`I/O`操作，同样会造成长时间的`GC`停顿，当出现`IO`阻塞时，你会发现`real`远大于`user`

    [Times: user=0.20 sys=0.01, real=18.45 secs]

解决办法：

 - 如果是你的应用造成了大量的`IO`操作，进行优化
 - 杀死造成高`IO`操作的进程
 - 将应用程序部署到其他`IO`操作较少的服务器上

**7、`System.gc()`调用**

当调用`System.gc()`或者`Runtime.getRuntime().gc()`(`System.gc()`实际上也是调用的`Runtime.getRuntime().gc()`)，会造成`STW`(`stop the world`，将用户线程挂起)，在`STW`期间，`JVM`是被冻结的(在此期间用户线程被挂起无法执行)，`System.gc()`可以通过以下方式调用：

 - 在应用程序中显示调用`System.gc()`
 - 第三方库、框架甚至是所使用的应用服务器也会调用
 - 通过`JMX`的使用，由外部工具(例如`Visual VM`)触发
 - 应用程序如果使用`RMI`，`RMI`会定时调用`System.gc()`

需要计算是否需要显示调用`System.gc()`方法，如果没必要就移除该调用代码，另外，可以通过`-XX:+DisableExplicitGC`虚拟机参数禁止显示调用`System.gc()`

**8、较大的堆内存大小**

分配大量的堆内存(`-Xmx`)也可能造成长时间的`GC pause`，如果堆内存很高，那么更多的垃圾会在堆中积累，当`Full GC`过程中清理垃圾时会耗费更多的时间。
 
 
 
## 虚拟机参数总结 ##
这里有个小窍门记这些命令：`-XX`设置参数要带等号，例如`-XX:PermSize=10M`，`-XX`执行某些操作需要加前缀`-`，例如`-XX:+PrintGCDetails`，`-X`设置内存大小不需要等号，例如`-Xms10M -Xmx10M -Xmn10M -Xss128K -Xoss128K`，这里的`m`可以理解为`memory`缩写

**1、内存分配命令**

 - `-Xms20M/-Xmx20M`:表示`JVM`启动内存最小值/最大值为20M
 - `-Xss128K/-Xoss128K`:表示设置虚拟机栈/本地方法栈大小为128K
 - `-XX:PermSize=10M/-XX:MaxPermSize=10M`：表示`JVM`初始永久代/最大永久代的大小为10M，这个参数在`jdk 1.8`里面已经被移除
 - `-XX:NewRatio=4`:设置年轻代：老年代=1:4
 - `-XX:SurvivorRatio=8`:设置2个`Survivor`区：1个`Eden`区的大小比值是2:8，这意味着`Survivor`区占整个年轻代的1/5，这个参数默认是8
 - `-Xmn20M`:设置年轻代大小为20M
 - `-XX:PretenureSizeThreshold=3145728`:表示对象代3145728（3M）时会直接进入老年代分配内存
 - `-XX：MaxTenuringThreshold=1`:对象年龄大于1，自动进入老年代


**2、打印命令**

 - `-verbose:gc`:表示输出虚拟机中`GC`的详细情况
 - `-XX:+PrintGC`:在控制台打印`GC`信息
 - `-XX:+PrintHeapAtGC`:每次`GC`都打印堆内存情况
 - `-XX:+PrintTLAB`:表示可以看到`TLAB`的使用情况
 - `-XX:+PrintGCDetails`:打印`GC`详情
 - `-XX:+PrintGCDateStamps`:打印`GC`日期时间戳
 - `-Xloggc:<filepath>`:将`gc`详情输出到日志文件


 **3、垃圾收集器命令**
 
 - `-XX:+UseG1GC`:使用`G1`垃圾收集器

**4、其他命令**

 - `-Xnoclassgc`：关闭对`java`类的回收
 - `-XX:+TraceClassLoading`：查看类的加载信息
 - `-XX:+TraceClassUnLoading`:查看类的卸载信息
 - `-XX:+HeapDumpOnOutOfMemoryError`:让虚拟机在出现内存溢出异常时`Dump`出当前的堆内存转储快照
 - `-XX:CompileThreshold=1000`:表示某个方法如果被调用1000次以后，会被认为是热点代码，并触发即时编译 