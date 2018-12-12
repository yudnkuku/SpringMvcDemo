# Maven

标签（空格分隔）： Maven

---

这篇文章用一些比较简洁的语言总结`maven`学习心得
## 什么是maven ##
根据官网的说法，`maven`的核心就是一个框架执行框架(`plugin execution framework`)，其所有的工作都是有插件完成的，由插件寻找特定的目标去执行

## 什么是生命周期(build lifecycle) ##
有三个内置的生命周期(`build lifecycle`)：默认(`default`)、清理(`clean`)和站点(`site`),这些构建声明周期由一系列不同的构建相位组成，每个构建相位代表了生命周期的不同阶段
默认生命周期由如下构建相位组成(`build phase`)如下表：
|生命周期名称|描述|
|:-:|:-:|
|`validate`|验证项目是否正确，所有必要的信息可用|
|`compile`|编译项目的源代码|
|`test`|测试|
|`package`|打包|
|`verify`|校验检查|
|`install`|安装依赖到本地|
|`deploy`|部署|
这些构建相位按照顺序执行完成默认的生命周期，这意味着当默认生命周期使用时，`maven`会首先验证工程，然后编译源码、测试、打包、执行集成测试、核查集成测试、安装包到本地仓库、最后部署到远程仓库
例如，执行命令：

    mvn install

那么，在`install`之前的所有相位都会被执行(依次执行`validate`/`compile`/`package`..)，因此只需要指定最后一个`phase`即可。在构建环境中，使用如下命令清理构建并将实例部署到共享仓库

    mvn clean deploy
    
## 什么是构建相位(build phase) ##
尽管构建相位是构建生命周期中的某一步，但是执行的规程却不一样，这些通过和构建相位绑定不同的插件目标(`plugin goal`)来实现
插件目标表示指定的任务(比构建相位粒度更细)，这些任务负责工程的构建和管理，每个目标可以绑定到0个或 多个构建相位，没有绑定相位的目标可以在构建周期之外直接执行，说白了执行构建相位实际上就是执行与之绑定的一些构建目标，执行顺序和其调用的顺序一致，例如执行如下命令：

    mvn clean dependency:copy-dependencies package
    
首先会执行`clean`相位(意味着`clean`之前所有相位和它本身将依次执行)，然后执行`dependency:copy-dependencies`插件目标(格式`plugin:goal`)，最后在执行`package`相位(顺序执行之前所有相位)。
如果一个构建目标绑定到了一个或者多个构建相位，每个相位过程中都会执行该目标

## maven构建相位和goal的绑定 ##
`maven`命令行格式通常为：

    mvn [plugin-name]:[goal-name]

通常要指定`plugin`名称和`goal`名称，常用插件列表如下：[maven内置插件][1]
而`maven`提供的插件将一些`goal`默认绑定了`phase`，因此可以直接使用`mvn phase-name`来替换上述的`mvn plugin-name:goal-name`，当然这些绑定也是可以配置的，可以自定义绑定`goal`到`phase`。
假如你使用`dispaly:time`目标向命令行`echo`当前时间，并且想在`process-test-resources`相位运行以表明测试开始时间，配置如下：

    <plugin>
       <groupId>com.mycompany.example</groupId>
       <artifactId>display-maven-plugin</artifactId>
       <version>1.0</version>
       <executions>
         <execution>
           <phase>process-test-resources</phase>
           <goals>
             <goal>time</goal>
           </goals>
         </execution>
       </executions>
     </plugin>
`POM`文件中的`packaging`元素可以指定很多属性值，例如`jar/war/ear/pom`，如果没有指定默认值是`jar`，每个打包方式都包含了一系列构建目标绑定到特定的构建相位，以`jar`为例：
|`build phase`|`plugin:goal`|
|:-:|:-:|
|`process-resources`|`resources:resources`|
|`compile`|`compiler:compile`|
|`process-test-resources`|`resources:testResources`|
|`test-compile`|`compiler:testCompile`|
|`test`|`surefire:test`|
|`package`|`jar:jar`|
|`install`|`install:install`|
|`deploy`|`deploy:deploy`|

另外可以通过插件来绑定相位，插件是向`maven`提供构建目标的实例，每个插件可以有一个或者多个目标，每个目标代表了该插件的一个功能，例如`Complier`插件有两个目标：`compile`和`testCompile`，前者编译源码，后者编译测试代码。
通过配置的构建目标会添加到已经默认绑定到构建相位上的构建目标，如果一个相位绑定了多个目标，那么先执行默认绑定的目标，再执行`POM`中配置的目标，例如如下代码将`modello:jaa`绑定到`generate:sources`相位上(`modello:java`目标用来生成`java`源码)，因此如下配置：

     <plugin>
       <groupId>org.codehaus.modello</groupId>
       <artifactId>modello-maven-plugin</artifactId>
       <version>1.8.1</version>
       <executions>
         <execution>
           <configuration>
             <models>
               <model>src/main/mdo/maven.mdo</model>
             </models>
             <version>4.0.0</version>
           </configuration>
           <goals>
             <goal>java</goal>
           </goals>
         </execution>
       </executions>
     </plugin>

## maven工程标准目录布局 ##
具体目录参考如下：
|目录|描述|
|:-:|:-:|
|`src/main/java`|源码目录|
|`src/main/resources`|资源目录|
|`src/main/filters`|资源过滤器目录|
|`src/test/webapp`|`web`应用源码|
|`src/test/java`|测试源码|
|`src/test/resources`|测试资源文件|

## 依赖作用域 ##
总共有6种作用域：

 - `compile`：默认作用域，此作用域依赖在工程的所有类路径中均可用，并且可以向依赖工程传递
 - `provided`：表明你希望`JDK`或者容器在运行时提供依赖，例如，当构建`J2EE`的`web`工程时，你可以把`Servlet API`和相关的`Java EE API`设置成`provided`，因为这些类`web`容器提供
 - `runtime`：表明该依赖编译不需要、运行时需要
 - `test`：表明噶依赖仅在`test compilation`和`execution`相位时才需要

## 生命周期参考 ##
下面列出了`default`、`clean`和`site`三个生命周期的所有构建相位
`Clean Lifecycle`：
|构建相位|描述|
|:-:|:-:|
|`pre-clean`|在清理工程之前执行的过程|
|`clean`|清理之前构建生成的文件|
|`post-clean`|工程清理之后需要执行的过程|

`Default Lifecycle`:
这个太多，详细请参考[Default Lifecycle][2]

|构建相位|描述|
|:-:|:-:|
|`validate`|验证|
|`process-resources`|复制和处理资源到目标路径中，为打包做准备|
|`test-compile`|将测试代码编译至目标路径|
|`test`|使用合适的单元测试框架测试|
|`prepare-package`|在真正打包之前执行的必要操作|
|`package`|打包，例如`jar`|
|`verify`|验证|
|`install`|安装到本地仓库|
|`deploy`|在集成或者发布环境中完成，将最终的包复制到远程仓库供其他人共享|

`Site Lifecycle`:

|构建相位|描述|
|:-:|:-:|
|`pre-site`|在实际工程地址生成之前执行的必要过程|
|`site`|生成工程的地址文档|
|`post-site`|生成后执行的过程|
|`site-deploy`|将生成的地址文档部署到指定的`web server`|

## 内建的生命周期绑定 ##
`Clean Lifecycle Bindings`:
|构建相位|构建目标|
|:-:|:-:|
|`clean`|`clean:clean`|

`Default Lifecycle Bindings`:
|构建相位|构建目标|
|:-:|:-:|
|`process-resources`|`resources:resources`|
|`compile`|`compiler:compile`|
|`test-compile`|`compiler:testCompile`|
|`test`|`surefire:test`|
|`package`|`jar:jar/war:war/或者其他类型`|
|`install`|`install:install`|
|`deploy`|`deploy:deploy`|

`Site Lifecycle Bindings`:
|构建相位|构建目标|
|:-:|:-:|
|`site`|`site:site`|
|`site-deploy`|`site:deploy`|
参考：[maven官方文档][3]


  [1]: https://maven.apache.org/plugins/index.html
  [2]: http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
  [3]: http://maven.apache.org/guides/getting-started/index.html