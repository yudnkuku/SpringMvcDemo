Spring MVC and java demo
2018技术书总结：
1、周志明--深入理解JVM虚拟机
2、方腾飞--并发编程最佳实践
3、MySQL入门
4、Spring源码分析
5、Spring In Action
6、Spring Boot
7、深入分析Java Web技术内幕
8、Netty权威指南

成果：
1、学习MyBatis及其源码
2、看了一些JDK源码，特别是集合、juc包、同步相关，对并发的理解更深刻，熟悉AQS机制，包括使用它实现的ReentrantLock
3、对线程的理解更加深刻，掌握了线程通信机制(wait/notify)、线程中断机制
4、熟悉线程池及其原理
5、熟悉多线程情况下存在的一些问题并了解解决办法
    (1)通过synchronized关键字实现内置锁(悲观锁，适合读操作较多的场景，保证操作的正确性)
    (2)通过Lock/Condition来实现更加灵活的加锁和释放锁，掌握其常用的编程模型
    (3)一些引申的问题：
        例如volatile关键字(只能保证可见性，无法保证原子性)、双重检查初始化(DCL，使用了volatile关键字禁止重排序)、
        HashMap rehash后出现的死循环问题(导致CPU占用100%，1.7版本)、
        死锁问题(Dead Lock，线程A拿到锁L等待锁M，线程B拿到锁M等待锁L，互相死锁，通过加入版本号解决或者使线程获取锁的顺序一致)、
        研读了ThreadLocal源码，熟悉其使用场景(在线程内部保存变量)、
    
                                     
6、掌握了Spring Mvc框架，熟悉一些常用配置
7、熟悉SLF4J，掌握了logback框架
8、掌握maven构建工具、熟悉GIT使用
9、接触了NIO编程，熟悉JDK提供的nio编程api，并熟悉了netty框架，研读了部分源码，
在github上fork了jenkov用jdk原生nio api设计的非阻塞服务器，并做了详细的解读分析

下一步工作：
1、巩固已经熟悉或掌握的知识，学而时习之
2、看新书，设计模式、算法、java优化等

