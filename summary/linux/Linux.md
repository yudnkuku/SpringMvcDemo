# Linux

标签（空格分隔）： Linux

---

## 压缩包安装jdk ##
1、解压压缩包到`java`目录

    mkdir -p /usr/local/java
    tar -vzxf jdk.tar.gz -C /usr/lcaol/java/ ->解压文件并移动到/usr/local/java目录下
    
2、添加环境变量，编辑配置文件
    
    vim /etc/profile
    
在`/etc/profile`文件末尾添加如下代码：

    export JAVA_HOME=/usr/local/java/jdk1.8.0_161
    export CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/
    export PATH=$PATH:$JAVA_HOME/bin
    
3、保存退出，然后重新加载配置文件

    source /etc/profile
    
4、测试安装

    java -version
    
## 安装MySQL ##
1、`yum`上`mysql`的资源有问题，所以不能仅仅用`yum`，还需要使用其他命令安装`mysql`社区版安装包

新建`mysql`文件夹：`mkdir /temp`

进入文件夹:`cd /temp`

下载文件安装包：`wget http://repo.mysql.com/mysql-community-release-el7-5.noarch.rpm`

解压：`rpm -ivh mysql-community-release-el7-5.noarch.rpm`

安装：`yum install mysql mysql-server mysql-devel -y`

2、启动服务

    systemctl start mysql.service
    netstat -anp | grep 3306
    
3、设置密码

通过`yum`安装的`mysql`的管理员账户是没有面的，这里通过命令设置其密码为`root`

    mysqladmin -u root password root
    mysql -u root -p root
    show databases;
    
4、设置大小写不敏感

`Linux MySQL`是大写小敏感的，为了设置成非大小写敏感，可以修改`/etc/my.cnf`，添加如下代码到`[mysqld]`下:`lower_case_table_names=1`


重启`mysql`服务: `service mysqld restart`
    
5、解决中文乱码问题

修改`/etc/my.cnf`文件，添加如下代码：

    [mysqld]
    port=3306
    socket=/var/lib/mysql/mysql.sock
    character-set-server=utf8
    
    [client]
    port=3306
    socket=/var/lib/mysql/mysql.sock
    default-character-set=utf8
    
    [mysql]
    no-auto-rehash
    
保存并重启服务:`service mysqld restart`
    
6、授权远程客户端登录

先登录`mysql`:`mysql -u root -p root`

添加用户：`grant all privileges on *.* to root@'%' identified by 'root'`
    
刷新生效：`flush privileges`

这两条代码执行完，本地的`navicat`便可以使用此用户名和密码远程登录`mysql`

## tomcat配置 ##
1、`yum`上没有`tomcat`的源，下载后将下载压缩包拖进`/temp`目录下(可以借助`WinSCP`或者`MobaXterm`工具)

2、安装

解压压缩包：`tar -xzf apache-tomcat-8.5.34.tar.gz`

移动到`/usr/local/tomcat8`目录下：`mv apache-tomcat-8.5.34 /usr/local/tomcat8`(这步可不要)

3、启动验证

启动：`/usr/local/tomcat8/bin/startup.sh`

验证端口是否占用：`netstat -anp | grep 8080`
## 常用命令 ##

**tar**

参数：

    -c : 建立一个压缩文件
    -x : 解开一个压缩文件
    -t : 查看压缩文件
    上面的三个参数仅能存在一个
    -z : 是否用gzip解压缩
    -j : 是否用bzip2解压缩
    -v : 解压缩过程显示文件
    -f : 使用档名，在f之后必须立即接档名，不要再加额外参数
    
例子：

    tar -cf archive.tar foo bar #从foo bar创建archive.tar压缩文件
    tar -tvf archive.tar #列出archive.tar中的所有文件
    tar -xf acchive.tar #解压archive.tar文件
    
**tail/head**

例子：

    head -6 readme.txt #显示文件前6行
    tail -25 mail.txt #显示文件最后25行
    tail -f /usr/local/tomcat/logs/localhost.log #动态输出log文件内容，达到监视的效果
    

