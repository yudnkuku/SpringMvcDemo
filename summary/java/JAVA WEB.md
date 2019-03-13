# JAVA WEB  

标签（空格分隔）： JAVA

---

## web项目部署到根目录下 ##
将`pro.war`包放到`$TOMCAT_HOME/webapps/`目录下，并改名为`ROOT.war`，然后将`$TOMCAT_HOME/webapps`下的`ROOT`文件夹删掉，重启`tomcat`，会在`webapps`目录下重新生成新的`ROOT`目录，其中实际上就是`war`包解压后的内容，这样可以直接通过`ip:port`访问项目，而不需要加上项目路径，避免了首页访问静态文件出现`404`错误。

之前也试过常见的方式，直接将`war`包放置到`webapps`目录下，重启`tomcat`，查看`$TOMCAT_HOME/logs/localhost.log`日志，一直报错，错误信息如下：

        [org.springframework.web.util.WebAppRootListener]
     java.lang.IllegalStateException: Web app root system property already set to different value: 'jtis.web.root' = [D:\apache-tomcat-8.5.32\webapps\ROOT\] instead of [D:\apache-tomcat-8.5.32\webapps\JtisOAServer\] - Choose unique values for the 'webAppRootKey' context-param in your web.xml files!
    	at org.springframework.web.util.WebUtils.setWebAppRootSystemProperty(WebUtils.java:161)
    	at org.springframework.web.util.WebAppRootListener.contextInitialized(WebAppRootListener...
    	
根据异常信息进入`WebAppRootListener.contextInitialized`方法：

    public void contextInitialized(ServletContextEvent event) {
		WebUtils.setWebAppRootSystemProperty(event.getServletContext());
	}
	
该方法会监听应用上下文(`ServletContext`)初始化过程，并将`webAppRootKey`对应的初始化参数作为系统属性设置为项目的根路径，在项目的`web.xml`中一般会这样定义：

    <listener>
        <listener-class>org.springframework.web.util.WebAppRootListener</listener-class>
    </listener>
    <context-param>
        <param-name>webAppRootKey</param-name>
        <param-value>my.project.root</param-value>
    </context-param>

那么`WebAppRootListener`的`contextInitialized`方法会将`my.project.root`设置为系统属性的`key`，`value`则为项目的根路径，例如`$TOMCAT_HOME/webapps/my.project.root/`，具体可以看源码：
    
    //接着调用WebUtils.setWebAppRootSystemProperty
    public static void setWebAppRootSystemProperty(ServletContext servletContext) throws IllegalStateException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		//获取项目根路径
		String root = servletContext.getRealPath("/");
		if (root == null) {
			throw new IllegalStateException(
				"Cannot set web app root system property when WAR file is not expanded");
		}
		//获取web.xml中定义的webAppRootKey参数值
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		//没有定义webAppRootKey则取默认值webapp.root
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		//系统属性中是否已经存在该key对应值
		String oldValue = System.getProperty(key);
		//系统属性已存在，且该值不等于root，那么会报异常，上面部署时日志报错就是这里
		if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
			throw new IllegalStateException(
				"Web app root system property already set to different value: '" +
				key + "' = [" + oldValue + "] instead of [" + root + "] - " +
				"Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
		}
		//将key,root设置为一对系统属性
		System.setProperty(key, root);
		servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
	}

在上下文环境销毁时会调用`contextDestroyed`回调，删除这些系统属性：

    @Override
	public void contextDestroyed(ServletContextEvent event) {
		WebUtils.removeWebAppRootSystemProperty(event.getServletContext());
	}
	
	public static void removeWebAppRootSystemProperty(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		System.getProperties().remove(key);
	}

一般而言，`WebAppRootListener`在`web.xml`文件中定义时一般放在`ContextLoaderListener`之前，对于`Tomcat`容器，不同的`web`应用是会共享系统属性的，因此对于每个应用都应该设置一个**唯一**的`webAppRootKey`初始化参数(`context-param`)。

看一下`WebAppRootListener`的继承关系，它实现了`ServletContextListener`接口，`ServletContextListener`接口用于监听`ServletContext`的生命周期改变：

    public interface ServletContextListener extends EventListener { 
        //初始化回调，会在所有的filter和servlet初始化之前调用
        public void contextInitialized(ServletContextEvent sce);
        
        //销毁回调，所有的filter和servlet在此之前已经全部销毁
        public void contextDestroyed(ServletContextEvent sce);
    }
    
## Session和Cookie ##

参考链接：[知乎-任云肖的回答][1]

## 跨域CORS ##
**功能概述**

跨域资源共享标准新增了一组`HTTP`源站通过浏览器有权限访问哪些资源。另外规范要求，对那些可能对**服务器产生副作用**(是不是可以认为就是`POST`请求)的`HTTP`请求方法(特别是`GET`以外的`HTTP`请求，或者搭配某些`MIME`类型的`POST`请求)，浏览器必须首先使用`OPTIONS`方法发起一个预检请求(`preflight request`)，从而获知服务器是否允许该跨域请求。服务器确认允许后，才发起实际的`HTTP`请求，在预检请求的返回中，服务器端也可以通知客户端，是否需要携带身份凭证(包括`Cookies`和`HTTP`认证相关数据)。

**简单请求**

某些请求不会触发`CORS`预检请求，这样的请求成为简单请求，满足所有下述条件，则该请求视为简单请求：

 - 使用下列方法之一：`GET/HEAD/POST`
 - `Fetch`规范定义了`CORS`安全的首部字段集合，不得人为设置该集合之外的其他首部字段：`Accept/Accept-Language/Content-Language/Content-Type`，`Content-Type`需要额外的限制
 - `Content-Type`的值仅限于下列三者之一：`text/plain`、`multipart/form-data`、`application/x-www/form-urlencoded`
 - 请求中的任意`XMLHttpRequestUpload`对象均没有注册任何事件监听器，`XMLHttpRequestUpload`对象可以使用`XMLHttpRequest.upload`属性访问
 - 请求中没有使用`ReadableStream`对象

比如站点`http://foo.example`的网页应用想要访问`http://bar.other`的资源，看下请求和响应报文：
    
    #请求报文
    GET /resources/public-data/ HTTP/1.1
    Host: bar.other
    User-Agent: Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.1b3pre) Gecko/20081130 Minefield/3.1b3pre
    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
    Accept-Language: en-us,en;q=0.5
    Accept-Encoding: gzip,deflate
    Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
    Connection: keep-alive
    Referer: http://foo.example/examples/access-control/simpleXSInvocation.html
    Origin: http://foo.example #和HOST不同，说明是跨域请求
    
    #响应报文
    HTTP/1.1 200 OK
    Date: Mon, 01 Dec 2008 00:23:53 GMT
    Server: Apache/2.0.61 
    Access-Control-Allow-Origin: * #允许任何站点跨域
    Keep-Alive: timeout=2, max=100
    Connection: Keep-Alive
    Transfer-Encoding: chunked
    Content-Type: application/xml
    
    [XML Data]

 
**预检请求**

需预检的请求要求必须首先使用`OPTIONS`方法发起一个预检请求(`preflight request`)到服务器，以获知服务器是否允许该实际请求，预检请求的使用，可以避免跨域请求对服务器的用户数据产生未预期的影响。

当请求满足下述任一条件时，即应首先发送预检请求：

 - 使用了下面任一`HTTP`方法：`PUT/DELETE/CONNECT/OPTIONS/TRACE/PATCH`
 - 人为设置了`CORS`安全的首部字段之外的其他首部字段：`Accept/Accept-Language/Content-Language/Content-Type`
 - `Content-Type`的值不属于下列之一：`application/x-www-form-urlencoded`、`multipart/form-data`、`text/plain`
 - 请求中的`XMLHttpRequestUpload`对象注册了任意多个事件监听器
 - 请求中使用了`ReadableStream`对象

例如通过脚本发送一个需要执行预检请求的`HTTP`请求：

    var invocation = new XMLHttpRequest();
    var url = 'http://bar.other/resources/post-here/';
    var body = '<?xml version="1.0"?><person><name>Arun</name></person>';
        
    function callOtherDomain(){
      if(invocation)
        {
          invocation.open('POST', url, true);
          invocation.setRequestHeader('X-PINGOTHER', 'pingpong');
          invocation.setRequestHeader('Content-Type', 'application/xml');
          invocation.onreadystatechange = handler;
          invocation.send(body); 
        }
    }

 上述代码使用`POST`请求发送一个`xml`文档，该请求包含了一个自定义的请求头部字段`X-PINGOTHER:pingpong`，另外该请求的`Content-Type=application/xml`，因此该请求需要首先发起预检请求。
 
 ![preflight_request][2]
 
 预检请求报文和响应报文：
 
     #预检请求OPTIONS报文
     1.OPTIONS /resources/post-here/ HTTP/1.1
     2.Host: bar.other
     3.User-Agent: Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.1b3pre) Gecko/20081130 Minefield/3.1b3pre
     4.Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
     5.Accept-Language: en-us,en;q=0.5
     6.Accept-Encoding: gzip,deflate
     7.Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
     8.Connection: keep-alive
     9.Origin: http://foo.example
    10.Access-Control-Request-Method: POST #增加请求头
    11.Access-Control-Request-Headers: #增加请求头 X-PINGOTHER, Content-Type
    
    #预检请求响应报文
    14.HTTP/1.1 200 OK
    15.Date: Mon, 01 Dec 2008 01:15:39 GMT
    16.Server: Apache/2.0.61 (Unix)
    17.Access-Control-Allow-Origin: http://foo.example
    18.Access-Control-Allow-Methods: POST, GET, OPTIONS
    19.Access-Control-Allow-Headers: X-PINGOTHER, Content-Type
    20.Access-Control-Max-Age: 86400
    21.Vary: Accept-Encoding, Origin
    22.Content-Encoding: gzip
    23.Content-Length: 0
    24.Keep-Alive: timeout=2, max=100
    25.Connection: Keep-Alive
    26.Content-Type: text/plain

预检请求完成之后，发送实际请求：
    
    #实际的POST请求
    POST /resources/post-here/ HTTP/1.1
    Host: bar.other
    User-Agent: Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.1b3pre) Gecko/20081130 Minefield/3.1b3pre
    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
    Accept-Language: en-us,en;q=0.5
    Accept-Encoding: gzip,deflate
    Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
    Connection: keep-alive
    X-PINGOTHER: pingpong
    Content-Type: text/xml; charset=UTF-8
    Referer: http://foo.example/examples/preflightInvocation.html
    Content-Length: 55
    Origin: http://foo.example
    Pragma: no-cache
    Cache-Control: no-cache
    
    <?xml version="1.0"?><person><name>Arun</name></person>
    
    #POST请求响应报文
    HTTP/1.1 200 OK
    Date: Mon, 01 Dec 2008 01:15:40 GMT
    Server: Apache/2.0.61 (Unix)
    Access-Control-Allow-Origin: http://foo.example
    Vary: Accept-Encoding, Origin
    Content-Encoding: gzip
    Content-Length: 235
    Keep-Alive: timeout=2, max=99
    Connection: Keep-Alive
    Content-Type: text/plain
    
    [Some GZIP'd payload]

可以看到预检请求中同时携带了两个首部字段：

    Access-Control-Request-Method:POST
    Access-Control-Request-Headers:X-PINGOTHER,Content-Type

`Access-Control-Request-Method`告知服务器，实际请求将使用`POST`方法，`Access-Control-Request-Headers`告知服务器，实际请求将携带两个自定义请求首部字段:`X-PINGOTHER`和`Content-Type`，服务器将据此决定，该实际请求是否被允许。

从预检请求的响应可以看出，服务器将接受后续的实际请求，响应如下所示：

    Access-Control-Allow-Origin: http://foo.example
    Access-Control-Allow-Methods: POST, GET, OPTIONS
    Access-Control-Allow-Headers: X-PINGOTHER, Content-Type
    Access-Control-Max-Age: 86400

`Access-Control-Allow-Methods`表明服务器允许客户端使用`POST/GET/OPTIONS`方法发起请求。

`Access-Control-Allow-Headers`表明服务器允许请求中携带字段`X-PINGOTHER`与`Content-Type`

`Access-Control-Max-Age`表明该响应的有效时间未86400秒，也就是24小时，在有效时间内，浏览器无需为同一请求再次发起预检请求

**预检请求与重定向**

大多数浏览器不支持针对于预检请求的重定向，如果一个预检请求发生了重定向，浏览器将报告错误。

**附带身份凭证的请求**

`Fetch`和`CORS`的一个有趣特性是，可以基于`HTTP cookies`和`HTTP`认证信息发送身份凭证，一般而言，对于跨域`XMLHttpRequest`或者是`Fetch`请求，浏览器不会发送身份凭证信息，如果要发送凭证信息，需要设置`XMLHttpRequest`的某个特殊标志位。

例如`http://foo.example`的某脚本向`http://bar.other`发起一个`GET`请求，并设置`Cookies`：

    var invocation = new XMLHttpRequest();
    var url = 'http://bar.other/resources/credentialed-content/';
        
    function callOtherDomain(){
      if(invocation) {
        invocation.open('GET', url, true);
        invocation.withCredentials = true;  #携带cookies
        invocation.onreadystatechange = handler;
        invocation.send(); 
      }
    }

因为这是一个简单的`GET`请求，所以浏览器不会对其发起预检请求，但是如果服务端的响应中未携带`Access-Controle-Allow-Credentials：true`，浏览器将不会把响应内容返回给请求的发送者。

`Spring MVC`框架允许你配置`CORS`(`Cross-Origin Resource Sharing`)，因为一些安全因素，浏览器禁止`AJAX`请求当前域外资源，例如你可能在一个页面和`evil.com`有银行账户，`evil.com`的脚本不能发起`AJAX`请求从另一个页面的银行账户取钱。

`CORS`是被多数浏览器实现的`W3C`标准，这使得你可以指定哪些跨域请求能够被授权。

`CORS`区分了`preflight`、简单请求和实际请求，`Spring MVC`中的`HandlerMapping`实现提供了`CORS`的内置支持，在成功的将请求映射到`handler`之后，`HandlerMapping`实现会检测特定请求和`handler`的`CORS`配置，并进一步操作，`Preflight`请求会直接处理，简单和实际的`CORS`请求会被拦截、验证并设置`CORS`响应头
    
    #GET请求报文，携带cookies
    GET /resources/access-control-with-credentials/ HTTP/1.1
    Host: bar.other
    User-Agent: Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.1b3pre) Gecko/20081130 Minefield/3.1b3pre
    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
    Accept-Language: en-us,en;q=0.5
    Accept-Encoding: gzip,deflate
    Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
    Connection: keep-alive
    Referer: http://foo.example/examples/credential.html
    Origin: http://foo.example
    Cookie: pageAccess=2 #携带cookies
    
    #实际响应报文
    HTTP/1.1 200 OK
    Date: Mon, 01 Dec 2008 01:34:52 GMT
    Server: Apache/2.0.61 (Unix) PHP/4.4.7 mod_ssl/2.0.61 OpenSSL/0.9.7e mod_fastcgi/2.4.2 DAV/2 SVN/1.4.2
    X-Powered-By: PHP/5.2.6
    Access-Control-Allow-Origin: http://foo.example
    Access-Control-Allow-Credentials: true #允许跨域携带cookies，浏览器收到响应后会将响应返回给请求的发送者
    Cache-Control: no-cache
    Pragma: no-cache
    Set-Cookie: pageAccess=3; expires=Wed, 31-Dec-2008 01:34:53 GMT
    Vary: Accept-Encoding, Origin
    Content-Encoding: gzip
    Content-Length: 106
    Keep-Alive: timeout=2, max=100
    Connection: Keep-Alive
    Content-Type: text/plain
    
    
    [text/plain payload]

对于附带身份认证的请求，服务器不得设置`Access-Control-Allow-Origin`的值为`*`，这是因为请求的头部中携带了`Cookie`信息，如果`Access-Controle-Allow-Origin`的值为`*`，请求将会失败，而将其设置为某个特定的域名（例如`http://foo.example`），则请求将执行
为了能够通过跨域请求(`Origin`请求头和请求的`host`不同)，你需要显式的声明`CORS`配置，如果没有匹配的`CORS`配置，那么`preflight`请求将会被拒绝，`CORS`相关的响应头不会被加入简单、实际的`CORS`请求，因此浏览器会拒绝这些响应。

**HTTP响应头部字段**

 - `Access-Control-Allow-Origin`:指定了允许访问该资源的外域`URI`，对于**不需要携带身份凭证的请求**，服务器可以指定该字段的值为通配符，表示允许来自所有域的请求
 - `Access-Control-Expose-Headers`:服务器把允许浏览器访问的头放入白名单，例如`Access-Control-Expose-Headers:X-My-Custom-Header,X-Another-Custom-Header`允许浏览器通过`getResponseHeader`访问`X-My-Custom-Header`和`X-Another-Custom-Header`响应头
 - `Access-Control-Max-Age`：指定了预检请求的结果能够被缓存多久，单位秒
 - `Access-Control-Allow-Credentials`：指定了当浏览器的`credentials`设置为`true`时是否允许浏览器读取`response`的内容。当用在预检请求的响应中时，它指定了实际请求是否可以使用`credentials`。请注意：简单`GET`请求不会被预检，如果对此类请求的响应中不包含该字段，这个响应将被忽略掉，并且浏览器也不会将响应内容返回给网页
 - `Allow-Control-Allow-Methods`:用于**预检请求的响应**，其指明了实际请求所允许使用的`HTTP`方法
 - `Access-Control-Allow-Headers`：用于**预检请求的响应**，其指明了实际请求中允许携带的首部字段

**HTTP请求首部字段**

 - `Origin`：表明预检请求或者实际请求的源站，不管是否为跨域请求，`Origin`字段总是被发送
 - `Access-Control-Request-Method`：用于`预检请求`，其作用是将实际请求所携带的首部字段告诉服务器
 - `Access-Control-Request-Headers`：用于`预检请求`，其作用是将实际请求所携带的首部字段告诉服务器


**Spring MVC CORS配置**

每一个`HandlerMapping`可以单独配置基于`URL`模板的`CorsConfiguration`映射，在多数情况中，应用会使用`MVC Java`配置或者`XML`名称域来声明这些映射关系。

你可以将`HandlerMapping`级别的全局`CORS`配置和更加细粒度、`handler`级别的`CORS`配置结合，局部配置一般会覆盖全局配置。

**@CrossOrigin**

`@CrossOrigin`注解使得控制器方法能够处理跨域请求，如下所示：

    @RestController
    @RequestMapping("/account")
    public class AccountController {
    
        @CrossOrigin
        @GetMapping("/{id}")
        public Account retrieve(@PathVariable Long id) {
            // ...
        }
    
        @DeleteMapping("/{id}")
        public void remove(@PathVariable Long id) {
            // ...
        }
    }

默认情况下，`@CrossOrigin`允许：

 - 所有的源(`origin`)
 - 所有的请求头
 - 控制器方法映射的所有请求方法

`allowCredentials`属性表明是否允许携带跨域`cookie`。

`@CrossOrigin`也支持类级注解，如下所示：

    @CrossOrigin(origins = "http://domain2.com", maxAge = 3600)
    @RestController
    @RequestMapping("/account")
    public class AccountController {
    
        @GetMapping("/{id}")
        public Account retrieve(@PathVariable Long id) {
            // ...
        }
    
        @DeleteMapping("/{id}")
        public void remove(@PathVariable Long id) {
            // ...
        }
    }


在类和方法上可以同时使用`@CrossOrigin`注解：

    @CrossOrigin(maxAge = 3600)
    @RestController
    @RequestMapping("/account")
    public class AccountController {
    
        @CrossOrigin("http://domain2.com")
        @GetMapping("/{id}")
        public Account retrieve(@PathVariable Long id) {
            // ...
        }
    
        @DeleteMapping("/{id}")
        public void remove(@PathVariable Long id) {
            // ...
        }
    }

**Java配置方式**

使用`CorsRegistry`回调实现`CORS`的`java`配置。

    @Configuration
    @EnableWebMvc
    public class WebConfig implements WebMvcConfigurer {
    
        @Override
        public void addCorsMappings(CorsRegistry registry) {
    
            registry.addMapping("/api/**")
                .allowedOrigins("http://domain2.com")
                .allowedMethods("PUT", "DELETE")
                .allowedHeaders("header1", "header2", "header3")
                .exposedHeaders("header1", "header2")
                .allowCredentials(true).maxAge(3600);
    
            // Add more mappings...
        }
    }

**XML配置**

使用`<mvc:cors>`元素：

    <mvc:cors>
    
        <mvc:mapping path="/api/**"
            allowed-origins="http://domain1.com, http://domain2.com"
            allowed-methods="GET, PUT"
            allowed-headers="header1, header2, header3"
            exposed-headers="header1, header2" allow-credentials="true"
            max-age="123" />
    
        <mvc:mapping path="/resources/**"
            allowed-origins="http://domain1.com" />
    
    </mvc:cors>


  [1]: https://www.zhihu.com/question/19786827/answer/151015728
  [2]: https://github.com/yudnkuku/SpringMvcDemo/blob/master/summary/java/img/preflight_request.png