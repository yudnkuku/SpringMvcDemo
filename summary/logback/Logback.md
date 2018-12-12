# Logback

标签（空格分隔）： Logback

---

## 1、SLF4J ##
`The Simple Logging Facade for Java`(`SLF4J`)可以看成是各种日志框架的门面或者抽象，例如`java.util.logging`/`logback`/`log4j`，`SLF4J`允许用户在部署阶段引入自己的日志框架，你必须引入`SLF4J`相关的`slf4j-api-1.8.0-beta2.jar`。
**since 1.6.0**：如果在类路径上没有发现绑定日志框架，那么`SLF4J`会默认使用无操作实现。
**since 1.7.9**:设置`slf4j.detectLoggerNameMismatch=true`，`SLF4J`会自动检测不正确的`logger`名称。

目前有多种日志框架可供选择：

 - `slf4j-log4j12-1.8.0-beta2.jar`:`log4j`日志框架
 - `slf4j-jdk14-1.8.0-beta2.jar`：`jdk`原生的日志模块
 - `slf4j-nop-1.8.0-beta2.jar`：`NOP`，什么都不做，丢掉所有日志
 - `slf4j-simple-1.8.0-beta2.jar`：简单实现，将所有事件输出到`System.err`，只有级别高于`INFO`的日志才会被打印
 - `slf4j-jcl-1.8.0-beta2.jar`：`Jakarta Commons Logging`
 - `logback-classic-1.0.13.jar (requires logback-core-1.0.13.jar)`：`logback`日志框架

在你自己的代码中，除了有`slf4j-api.jar`包之外，还需要添加日志框架实现的`jar`包

![SLF4J日志框架绑定][1]
 
## Logback框架详解 ##
## Logger/Appender/Layout ##
这三个类是`logback`最主要的三个类
`Logger`定义日志实体，名称区分大小写，并且遵循上下级命名规则，例如`x.y`就是`x`的子`logger`，最上层的是`root logger`，所有的自定义的`logger`都继承自`root logger`，`Logger`接口提供的方法如下：

    public interface Logger {
    
      // Printing methods: 
      public void trace(String message);
      public void debug(String message);
      public void info(String message); 
      public void warn(String message); 
      public void error(String message); 
    }

**日志等级**
日志等级分为五种：`TRACE<DEBUG<INFO<WARN<ERROR`
`Logger`默认的日志级别是`DEBUG`,日志级别同样可以继承，如果子`logger`没有分配`level`，那么会直接继承自第一个定义了`level`的父`logger`，否则会使用默认的`debug`，例如：
|日志名称|分配`level`|有效`level`|
|:-:|:-:|:-:|
|`root`|`DEBUG`|`DEBUG`|
|`X`|`INFO`|`INFO`|
|`X.Y`|`none`|`INFO`|
|`X.Y.Z`|`ERROR`|`ERROR`|

**Appender&Layout**
`appender`是日志要输出的位置，例如控制台、文件、数据库等等，一个`logger`可以配置多个`appender`。
`appender`的`additivity`属性指的是该`appender`是否会将日志输出到父级`appender`，默认值是`true`
除了日志的输出位置，还需要定制日志的格式，可以使用内置的转换符来定义日志输出格式：

    %-4relative [%thread] %-5level %logger{32} - %msg%n
    打印如下：
    176  [main] DEBUG manual.architecture.HelloWorld2 - Hello world.

## Logback配置 ##
`Logback`会根据类路径中的配置文件加载配置：
1、首先寻找`logback-test.xml`文件
2、如果没找到则会寻找`logback.groovy`文件
3、如果没找到则会寻找`logback.xml`文件
4、否则会从`META-INF\services\ch.qos.logback.classic.spi.Configurator`中加载配置

一段典型的配置如下：

    <configuration>
        
      <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
          <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
      </appender>
    
      <root level="debug">
        <appender-ref ref="STDOUT" />
      </root>
    </configuration>

`configuration`元素：

![confiuration][2]

`appender`元素：

![appender元素][3]

## Appender ##
`appender`有多种：`ConsoleAppender(输出到控制台)`、`FileAppender(输出到文件)`、`RollingFileAppender(输出到滚动文件)`、`AsyncAppender(异步输出到文件)`

**1、FileAppender**
一段`FileAppender`配置：

    //需要指定appender name和class属性
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        //当前活跃日志文件路径
        <file>testFile.log</file>
        //是否接续
        <append>true</append>
        <!-- set immediateFlush to false for much higher logging throughput -->
        <immediateFlush>true</immediateFlush>
        //定义encoder，日志输出格式
        <encoder>
          <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
        </encoder>
    </appender>

通过时间戳唯一化文件名称：
        
      //定义timestamp元素
      <timestamp key="bySecond" datePattern="yyyyMMdd'T'HHmmss"/>
    
      <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        //文件路径引用bySecond
        <file>log-${bySecond}.txt</file>
        <encoder>
          <pattern>%logger{35} - %msg%n</pattern>
        </encoder>
      </appender>

**2、RollingFileAppender**
继承自`FileAppender`，指定了滚动日志文件的大小，例如将日志输出到活跃日志文件`log.txt`(由`file`元素定义)，当某个特定的条件满足时，就会将日志归档到别的文件(文件路径由`fileNamePattern`定义)。
和`RollingFileAppender`配合使用的包括：`RollingPolicy`(负责发生`rollover`时的行为)和`TriggeringPolicy`(决定发生`rollover`的时机)

**TimeBasedRollingPolicy**
使用最广泛的滚动策略，定义了基于时间的滚动策略，例如通过天或者月，`TimeBasedRollingPolicy`实现了`RollingPolicy`和`TriggeringPolicy`接口，因此可以同时指定滚动策略和滚动触发时机。

常用属性：
1、`fileNamePattern`:具有**双重意义**，一可以计算出推算出滚动周期，另外还可以指定**归档文件名称**(和`appender`的`file`属性不同，后者指定**当前的活跃日志文件**，还未归档)，例如`yyyy-MM`和`yyyy@MM`都指定了按月滚动，但是拥有不同的文件名称。
2、`maxHistory`:控制归档文件的最大数量，删除过时的文件。例如如果你指定了按月滚动，那么设置`maxHistory=6`，那么只会保留最近6个月的日志文件，超过6个月的日志文件会被删除

以下是一个配置`RollingFileAppender`的例子：

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        //当前活跃日志文件路径
        <file>logFile.log</file>
        //rollingPolicy采用TimeBasedRollingPolicy，因此只需要声明rollingPolicy元素，不需要实现triggeringPolicy
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
          <!-- 按天rollover，并指定归档文件路径-->
          <fileNamePattern>logFile.%d{yyyy-MM-dd}.%i.txt</fileNamePattern>
    
          <!-- 保留30天，每个日志文件最大100MB，所有归档日志文件最大容量是3GB -->
          <maxFileSize>100MB</maxFileSize> 
          <maxHistory>30</maxHistory>
          <totalSizeCap>3GB</totalSizeCap>
    
        </rollingPolicy>
    
        <encoder>
          <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
        </encoder>
    </appender> 

这里举个例子区别`file`和`fileNamePattern`:

    fileNamePattern="/wombat/foo.%d"
    使用按天rollover策略，即一天结束时会将当天的日志归档
    1、未设置file元素，直接使用fileNamePattern指定的文件作为活跃日志文件，例如2018年12月6号的日志会写入/wombat/foo.2018-12-06，第二天会写入/wombat/foo.2018-12-07
    2、设置了file=/wombat/foo.txt，那么2018年12月6号临时写入file指定的文件，到24点时需要将该文件重命名为/wombat/foo.2018-12-06,然后重新新建一个/wombat/foo.txt文件继续作为活跃日志文件写入第二天的日志

**FixedWindowRollingPolicy**
`FixedWindowRollingPolicy`根据固定窗口算法来重命名文件名称，`fileNamePattern`属性必须包含`%i`，来指定当前`rollover index`，该类只实现了`RollingPolicy`接口
常用属性：
1、`minIndex`:下限
2、`maxIndex`:上限
3、`fileNamePattern`:文件名称表达式，必须包含`%i`
例如第一次归档`i=1`，活跃日志名称(`file`属性指定)为`foo.log`，那么归档日志名称为`foo1.log`，第二次归档，`foo1.log`更名为`foo2.log`，`foo.log`更名为`foo1.log`，然后再新建一个`foo.log`作为活动的日志路径

配置例程：

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>test.log</file>
        //使用FixedWindowRollingPolicy策略
        <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
          <fileNamePattern>test.%i.log.zip</fileNamePattern>
          <minIndex>1</minIndex>
          <maxIndex>3</maxIndex>
        </rollingPolicy>
        
        //设置triggeringPolicy，maxFileSize表示日志文件达到5MB就触发rollover
        <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
          <maxFileSize>5MB</maxFileSize>
        </triggeringPolicy>
        <encoder>
          <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
        </encoder>
    </appender>


**触发策略Triggering Policy**
触发策略用来指导`RollingFileAppender`什么时候`rollover`

**SizeBasedTriggeringPolicy**
检查当前活跃日志文件的大小，如果超过了指定的大小(`maxFileSize`)，将会触发`rollover`
`maxFileSize`可以被指定为`KB/MB/GB`等，例如`50KB/50MB/50GB`

例程：

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>test.log</file>
        //指定rollingPolicy
        <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
          <fileNamePattern>test.%i.log.zip</fileNamePattern>
          <minIndex>1</minIndex>
          <maxIndex>3</maxIndex>
        </rollingPolicy>
        
        //指定SizeBasedTriggeringPolicy，根据活跃日志文件大小触发rollover
        <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
          <maxFileSize>5MB</maxFileSize>
        </triggeringPolicy>
        <encoder>
          <pattern>%-4relative [%thread] %-5level %logger{35} - %msg%n</pattern>
        </encoder>
    </appender>

**3、AsyncAppender**
异步日志`Appender`仅仅作为一个事件分派组件而不处理日志，因此它必须引用另外一个`appender`来处理日志。
`AsyncAppender`会将日志事件缓存到`BlockingQueue`，其创建的工作线程会从队列头部取出事件，并将它们派发到附加的`appender`，默认情况下，当队列80%满时，会丢弃掉`TRACE/DEBUG/INFO`级别的日志事件。可以设置`discardubgThreshold=0`不丢弃任何事件
常用属性：
1、`queueSize`:事件阻塞队列初始大小，默认是256
2、`discardingThreshold`:默认情况下，当阻塞队列还剩20%时，会抛弃`TRACE/DEBUG/INFO`级别的日志事件，如果不抛弃，则将此参数设置为0
3、`includeCallerData`:是否展开调用者的数据，很耗费性能，一般情况下不开启
4、`maxFlushTime`:当`LoggerContext`关闭时，`AsyncAppender`的`stop`方法会等待`maxFlushTime`直到`worker`线程完成，在这期间可以处理队列中剩余的事件，如果超时未处理完成，那么剩余的事件会被抛弃。可以查看一下源码，可以看到它实际上是通过`Thread.join(timeout)`方法来实现的。

    public void stop() {

        try {
            interruptUtil.maskInterruptFlag();
            worker.join(maxFlushTime);

5、`neverBlock`:如果是`false`(默认值)，那么当队列已满时往队列中添加事件时会阻塞，如果是`true`，则不会阻塞，会丢失一些事件
例程：

    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>myapp.log</file>
        <encoder>
          <pattern>%logger{35} - %msg%n</pattern>
        </encoder>
    </appender>
    
    //定义AsyncAppender，引用了FILE Appender，将日志事件派发给FILE Appender处理
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE" />
    </appender>
    
## Encoder&Layout ##
这两个元素都是用来声明日志布局的，其中需要转换符来设置，例如一个典型的表达式`%+转换符`：`%logger{10}`，在保证表达完整意思的前提下将`Logger name`缩减到10位打印

    logger name:mainPackage.sub.sample.Bar
    %logger{10}->m.s.s.Bar

常用转换符：
1、`logger{length}`:将`logger`缩减到`length`长度打印
2、`class{length}`：将类名称缩减到`length`长度打印
3、`contextName`:`logger context`名称打印
4、`d{pattern}/date{pattern}/d{pattern,timezone}/date{pattern,timezone}`:打印日志时间
5、`msg/m/message`:打印日志消息
6、`n`：打印换行符
7、`level`:打印日志级别
8、`relative`:打印从应用程序开始到产生日志事件经过的时间毫秒数
9、`thread`:打印当前线程名称
10、`exception{depth}/throwable{depth}`:输出和日志事件相关的异常堆栈信息，`depth`表示打印深度，有三个选项：`short/full/Any integer`，默认是`full`

**格式化修饰符**
格式化修饰符放在`%`和转换符之间，例如`%-10.20`，表示左对齐，最少10位，最大20位，如果数据不超过10位用空格补齐，如果超过了10位，从头部删除掉多余的字符。
举几个例子：

    [%10.10logger] main.foo.foo.bar.name [o.bar.name] //从头去除
    [%10.-10logger] main.foo.foo.bar.name [main.foo.f]  //从尾部去除
    
## Fileter ##
`LevelFilter`可以根据规则过滤掉日志事件。

**LevelFilter**
根据日志级别过滤事件，一般配置如下：

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">         
        //定义filter，如果日志级别是INFO则接受，否则拒绝
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
          <level>INFO</level>
          <onMatch>ACCEPT</onMatch>
          <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
          <pattern>
            %-4relative [%thread] %-5level %logger{30} - %msg%n
          </pattern>
        </encoder>
  </appender>

    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (event.getLevel().equals(level)) {
            return onMatch; //可以自定义
        } else {
            return onMismatch;  //可以自定义
        }
    }

查看`Filter`接口的`decide`方法，其返回值是`FilterReply`枚举类型，有三种情况：

    public enum FilterReply {
        DENY, NEUTRAL, ACCEPT;
    }

**ThresholdFilter**
`ThresholdFilter`可以过滤掉级别低于指定级别的日志事件

    <appender name="CONSOLE"
    class="ch.qos.logback.core.ConsoleAppender">
        <!-- deny all events with a level below INFO, that is TRACE and DEBUG -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
          //只用设置level，不用设置onMatch和onMismatch
          <level>INFO</level>
        </filter>
        <encoder>
          <pattern>
            %-4relative [%thread] %-5level %logger{30} - %msg%n
          </pattern>
        </encoder>
  </appender>

    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (event.getLevel().isGreaterOrEqual(level)) {
            return FilterReply.NEUTRAL;
        } else {
            return FilterReply.DENY;
        }
    }

  [1]: https://www.slf4j.org/images/concrete-bindings.png
  [2]: https://logback.qos.ch/manual/images/chapters/configuration/basicSyntax.png
  [3]: https://logback.qos.ch/manual/images/chapters/configuration/appenderSyntax.png