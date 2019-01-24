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