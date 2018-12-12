# Spring MVC源码解析

标签（空格分隔）： 源码解析

---

## 类型转换相关源码 ##
我们在进行请求映射时，请求字符串到处理函数参数的转换通常由框架内置的转换服务实现
`Spring MVC`和类型转换相关的一些接口、类在包`org.springframework.core.convert`中，从`GenericConversionService`概述一些一些主要的接口：

 - `ConversionService`:提供类型转换的服务接口
 - `ConverterRegistry`:提供转换器`Converter`的注册
 - `GenericConversionService`:实现了以上两个接口
 - `FormattingConversionService`:继承了`GenericConversionService`，同时实现了`FormatterRegistry`接口，可以注册`Formatter`
 - `GenericConverter`：两个或者多个类之间的转换通用接口，这个接口和`Converter`接口没有关系，然而在`GenericConversionService`中实际参与转换的是`GenericConverter`实例，因此会看到它在内部定义了一个`ConverterAdapter`类来进行适配。实际上真正的转换任务还是交给了`Converter`实例完成
    
        
        public interface GenericConverter {
            //获取可以转换的类型对
            Set<ConvertiblePair> getConvertibleTypes();
            //转换方法
            Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType);
            //内部类，源类目标类转换对
            final class ConvertiblePair {

		        private final Class<?> sourceType;

		        private final Class<?> targetType;
		    }
        }

   
`GenericConversionService`源码分析：
    
    //Converters是内部类
    private final Converters converters = new Converters();
    
    //注册Converter实例
    public void addConverter(Converter<?, ?> converter) {
		ResolvableType[] typeInfo = getRequiredTypeInfo(converter.getClass(), Converter.class);
		if (typeInfo == null && converter instanceof DecoratingProxy) {
			typeInfo = getRequiredTypeInfo(((DecoratingProxy) converter).getDecoratedClass(), Converter.class);
		}
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to determine source type <S> and target type <T> for your " +
					"Converter [" + converter.getClass().getName() + "]; does the class parameterize those types?");
		}
		//将Converter实例封装成ConverterAdapter，即将Converter适配成GenericConverter
		addConverter(new ConverterAdapter(converter, typeInfo[0], typeInfo[1]));
	}
	
    //添加converter
    public void addConverter(GenericConverter converter) {
    		this.converters.add(converter);
    		invalidateCache();
        }

    //根据source/target类型获取GenericConverter实例方法
    protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
		//先从缓存中获取
		GenericConverter converter = this.converterCache.get(key);
		if (converter != null) {
			return (converter != NO_MATCH ? converter : null);
		}
        //缓存中没有则在调用Converters的find(sourceType,targetType)方法
		converter = this.converters.find(sourceType, targetType);
		if (converter == null) {
		    //如果converter为空，则取默认值
			converter = getDefaultConverter(sourceType, targetType);
		}

		if (converter != null) {
		    //不为空添加进缓存，便于下次直接从缓存中取值
			this.converterCache.put(key, converter);
			return converter;
		}

		this.converterCache.put(key, NO_MATCH);
		return null;
	}
	
    //将sourceType类型的source转换为targetType
    public Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		if (sourceType == null) {
			Assert.isTrue(source == null, "Source must be [null] if source type == [null]");
			return handleResult(null, targetType, convertNullSource(null, targetType));
		}
		if (source != null && !sourceType.getObjectType().isInstance(source)) {
			throw new IllegalArgumentException("Source to convert from must be an instance of [" +
					sourceType + "]; instead it was a [" + source.getClass().getName() + "]");
		}
		//获取转换器，getConverter方法从缓存中获取转换器，这里会根据不同的sourceType和targetType获取对应的Converter实例
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter != null) {
		    //使用转换器转换类型
			Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
			return handleResult(sourceType, targetType, result);
		}
		return handleConverterNotFound(source, sourceType, targetType);
	}


大部分类型转换都会走上述代码流程，对于一些有格式化注解的参数，代码流程会有点不一样，例如如下代码：

    @GetMapping("formattedCollection")
	public String formattedCollection(@RequestParam @DateTimeFormat(iso=ISO.DATE) Collection<Date> values) {
		return "Converted formatted collection " + values;
	}

如果没有注解`@DateTimeFormat`，则会直接使用`StringToCollectionConverter`进行类型转换，然而注解了`@DateTimeFormat`之后，代码流程发生了变化，这里需要介绍一下`FormattingConversionService`
    
    //继承自GenericConversionService，实现了FormatterRegistry,FormatterRegistry继承自ConverterRegistry，因此可以同时注册Formatter和Converter
    public class FormattingConversionService extends GenericConversionService
		implements FormatterRegistry, EmbeddedValueResolverAware {
		    
		    //注册转换器Converter
		    public void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory) {
		Class<? extends Annotation> annotationType = getAnnotationType(annotationFormatterFactory);
		if (this.embeddedValueResolver != null && annotationFormatterFactory instanceof EmbeddedValueResolverAware) {
			((EmbeddedValueResolverAware) annotationFormatterFactory).setEmbeddedValueResolver(this.embeddedValueResolver);
		}
		Set<Class<?>> fieldTypes = annotationFormatterFactory.getFieldTypes();
		for (Class<?> fieldType : fieldTypes) {
		    //对每个注解参数构建AnnotationPrinterConverter和AnnotationParserConverter，并注册到GenericConversionService中的converters中保存
			addConverter(new AnnotationPrinterConverter(annotationType, annotationFormatterFactory, fieldType));
			addConverter(new AnnotationParserConverter(annotationType, annotationFormatterFactory, fieldType));
		}
	}
		    
		    
		    //内部类，将注解类转换为String
		    private class AnnotationParserConverter implements ConditionalGenericConverter {
		    
    		    private final Class<? extends Annotation> annotationType;
    
        		@SuppressWarnings("rawtypes")
        		private final AnnotationFormatterFactory annotationFormatterFactory;
        
        		private final Class<?> fieldType;
        		
    		    //实现的convert方法
    		    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        			Annotation ann = targetType.getAnnotation(this.annotationType);
        			if (ann == null) {
        				throw new IllegalStateException(
        						"Expected [" + this.annotationType.getName() + "] to be present on " + targetType);
        			}
        			AnnotationConverterKey converterKey = new AnnotationConverterKey(ann, targetType.getObjectType());
        			//从缓存中获取converter
        			GenericConverter converter = cachedParsers.get(converterKey);
        			if (converter == null) {
        			    //从annotationFormatterFactory获取Parser
        				Parser<?> parser = this.annotationFormatterFactory.getParser(
        						converterKey.getAnnotation(), converterKey.getFieldType());
        				//构建ParserConverter实例进行类型转换，注意构造时传入了外部类的引用
        				converter = new ParserConverter(this.fieldType, parser, FormattingConversionService.this);
        				cachedParsers.put(converterKey, converter);
        			}
        			//转换
        			return converter.convert(source, sourceType, targetType);
        		}
		    }
		    //内部类，将String转换为注解类
		    private class AnnotationPrinterConverter implements ConditionalGenericConverter {}
		    //内部类
		    private class ParserConverter implements ConditionalGenericConverter {
		    
		            //转换方法
		            public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String text = (String) source;
			if (!StringUtils.hasText(text)) {
				return null;
			}
			Object result;
			try {
			    //调用内部的parser实例进行类型转换
				result = this.parser.parse(text, LocaleContextHolder.getLocale());
			}
			catch (IllegalArgumentException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
			TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
			if (!resultType.isAssignableTo(targetType)) {
			    //如果转换结果类型不是targetType类型的父类，接着使用内部的conversionService进行转换，这个service是由构造函数传进来的，即再一次调用了外部类的convert方法
				result = this.conversionService.convert(result, resultType, targetType);
			}
			return result;
		}
		    }
		    //内部类
		    private class PrinterConverter implements ConditionalGenericConverter {}
		}

如何用`xml`方式注册`Converter`和`Formatter`：

        <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
        xmlns:mvc="http://www.springframework.org/schema/mvc"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="
            http://www.springframework.org/schema/beans
            http://www.springframework.org/schema/beans/spring-beans.xsd
            http://www.springframework.org/schema/mvc
            http://www.springframework.org/schema/mvc/spring-mvc.xsd">
    
        <mvc:annotation-driven conversion-service="conversionService"/>
    
        <bean id="conversionService"
                //使用FormattingConversionServiceFactoryBean来注册converter和formatter
                class="org.springframework.format.support.FormattingConversionServiceFactoryBean">
            <property name="converters">
                <set>
                    <bean class="org.example.MyConverter"/>
                </set>
            </property>
            <property name="formatters">
                <set>
                    <bean class="org.example.MyFormatter"/>
                    <bean class="org.example.MyAnnotationFormatterFactory"/>
                </set>
            </property>
            <property name="formatterRegistrars">
                <set>
                    <bean class="org.example.MyFormatterRegistrar"/>
                </set>
            </property>
        </bean>
    
    </beans>

## 参数解析接口 ##

    org.springframework.web.method.support.HandlerMethodArgumentResolver

其主要方法：
    
    //判断方法参数是否可以被解析器解析
    boolean supportsParameter(MethodParameter parameter);
    //解析参数
    Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception;

`springmvc`框架为该接口提供了一些默认的实现，可以解析诸如`@PathVariable`、`@RequestParam`、`@ModelAttribute`等注解的参数，将请求提供的参数转换为`java`类型再绑定到`handler`方法参数上，具体可以查看该接口的实现类，当然我们可以自定义实现自己的参数解析器。

首先定义一个自定义注解：

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface RequestAttribute {
    	String value();
    }

再定义一个参数解析器实现上述接口：

    public class CustomArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
	    //如果方法参数注解有RequestAttribute，则可以用此解析器解析
		return parameter.getParameterAnnotation(RequestAttribute.class) != null;
	}

	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
			throws Exception {
        
		RequestAttribute attr = parameter.getParameterAnnotation(RequestAttribute.class);
		//返回attr.value()名称对应的属性值
		return webRequest.getAttribute(attr.value(), WebRequest.SCOPE_REQUEST);
	}
	
    }

控制器代码：

    @RestController
    public class CustomArgumentController {
    
    //使用@ModelAttribute注解void方法，此方法在所有Handler方法之前调用
	@ModelAttribute
	void beforeInvokingHandlerMethod(HttpServletRequest request) {
	    //设置请求属性'foo'='bar'
		request.setAttribute("foo", "bar");
	}
	
	//使用@RequestAttribute注解
	@GetMapping("/data/custom")
	public String custom(@RequestAttribute("foo") String foo) {
		return "Got 'foo' request attribute value '" + foo + "'";
	}

    }

最后注册该解析器，这里使用注解方式：

    @Configuration
    @ComponentScan()
    @EnableWebMvc
    public class WebMvcConfig implements WebMvcConfigurer {
        @Override
	    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		    resolvers.add(new CustomArgumentResolver());
    	}
    }

`xml`方式：

    <mvc:annotation-driven>
        <mvc:argument-resolvers>
            <bean class="spring.config.CustomArgumentResolver"/>
        </mvc:argument-resolvers>
    </mvc-annotation-driven>

参数解析完后如果有必要需要进行类型转换，具体源码在`AbstractNamedValueMethodArgumentResolver`，实现了`HandlerMethodArgumentResolver`接口的抽象类，很多内置的方法参数解析器都继承了此类

    public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        //获取参数的名称值信息，如果注解没有写name，会自动将参数名称设置为默认名称
		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();
        
        //解析可能包含的placeholder
		Object resolvedName = resolveStringValue(namedValueInfo.name);
		if (resolvedName == null) {
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}
        //解析名称对应的值
		Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				arg = resolveStringValue(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, nestedParameter, webRequest);
			}
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		}
		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			arg = resolveStringValue(namedValueInfo.defaultValue);
		}

		if (binderFactory != null) {
			WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
			try {
			    //转换逻辑
				arg = binder.convertIfNecessary(arg, parameter.getParameterType(), parameter);
			}
			catch (ConversionNotSupportedException ex) {
				throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());
			}
			catch (TypeMismatchException ex) {
				throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
						namedValueInfo.name, parameter, ex.getCause());

			}
		}

		handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);

		return arg;
	}
    
## UrlBasedViewResolver视图解析器 ##
实现了`ViewResolver`接口，能将逻辑视图名称解析为视图`URL`，可以通过指定`viewClass`属性指定支持的视图类型
也可以通过指定`prefix`和`suffix`参数和逻辑视图名称拼接成视图`URL`,如下：

    prefix="/WEB-INF/", suffix=".jsp", viewName="hello" -> /WEB-INF/hello.jsp

如果逻辑视图名称包含前缀`redirect:`或者`forward:`，那么会进一步进行处理，具体看`createView`方法
对于`InternaleResourceViewResolver`应该放在视图解析器链的最后，应为它会解析所有视图名称，而不考虑其对应的视图资源是否存在

    @Override
	protected View createView(String viewName, Locale locale) throws Exception {
		// If this resolver is not supposed to handle the given view,
		// return null to pass on to the next resolver in the chain.
		if (!canHandle(viewName, locale)) {
			return null;
		}
		// Check for special "redirect:" prefix.
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
		    //如果视图名称前缀是"redirect:"，截取前缀后面的字符串作为redirectUrl
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			//构造RedirectView实例
			RedirectView view = new RedirectView(redirectUrl, isRedirectContextRelative(), isRedirectHttp10Compatible());
			String[] hosts = getRedirectHosts();
			if (hosts != null) {
				view.setHosts(hosts);
			}
			return applyLifecycleMethods(viewName, view);
		}
		// Check for special "forward:" prefix.
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
		    //如果视图名称前缀是"forward:"，截取前缀后面的字符串
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			//构造InternalResourceView实例
			return new InternalResourceView(forwardUrl);
		}
		// Else fall back to superclass implementation: calling loadView.
		return super.createView(viewName, locale);
	}

在渲染视图的时候会调用`RedirectView`的`renderMergedOutputModel`方法：
    
    //此方法
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
        //根据模型和请求构建跳转url
		String targetUrl = createTargetUrl(model, request);
		targetUrl = updateTargetUrl(targetUrl, model, request, response);

		// Save flash attributes
		RequestContextUtils.saveOutputFlashMap(targetUrl, request, response);

		// 跳转
		sendRedirect(request, response, targetUrl, this.http10Compatible);
	}
	
	//构建跳转Url
	protected final String createTargetUrl(Map<String, Object> model, HttpServletRequest request)
			throws UnsupportedEncodingException {

		// Prepare target URL.
		StringBuilder targetUrl = new StringBuilder();
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
    
		if (this.contextRelative && getUrl().startsWith("/")) {
		    //如果contextRelative属性为true且url前缀是"/"，则在targetUrl前面添加请求上下文路径contextPath
			// Do not apply context path to relative URLs.
			targetUrl.append(request.getContextPath());
		}
		//追加url
		targetUrl.append(getUrl());

		String enc = this.encodingScheme;
		if (enc == null) {
			enc = request.getCharacterEncoding();
		}
		if (enc == null) {
			enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}

		if (this.expandUriTemplateVariables && StringUtils.hasText(targetUrl)) {
		    //替换uri中的占位符变量，例如视图名称中包含"hello/{foo}"，而模型有"foo"对应的属性"bar"，那么则替换视图名称"hello/bar"
			Map<String, String> variables = getCurrentRequestUriVariables(request);
			targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, enc);
		}
		if (isPropagateQueryProperties()) {
		    //追加查询参数
		 	appendCurrentQueryParams(targetUrl, request);
		}
		if (this.exposeModelAttributes) {
		    //追加查询属性
			appendQueryProperties(targetUrl, model, enc);
		}

		return targetUrl.toString();
	}
	
	//跳转方法
	protected void sendRedirect(HttpServletRequest request, HttpServletResponse response,
			String targetUrl, boolean http10Compatible) throws IOException {

		String encodedURL = (isRemoteHost(targetUrl) ? targetUrl : response.encodeRedirectURL(targetUrl));
		if (http10Compatible) {
			HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
			if (this.statusCode != null) {
				response.setStatus(this.statusCode.value());
				response.setHeader("Location", encodedURL);
			}
			else if (attributeStatusCode != null) {
				response.setStatus(attributeStatusCode.value());
				response.setHeader("Location", encodedURL);
			}
			else {
				// 302跳转
				response.sendRedirect(encodedURL);
			}
		}
		else {
			HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
			response.setStatus(statusCode.value());
			response.setHeader("Location", encodedURL);
		}
	}
	
	这里需要注意下response.sendRedirect(String location)方法：
	1、如果location没有"/"前缀，则跳转路径相对于servletPath，例如请求全路径是"http://localhost:port/ctx/controllerPath/handlerPath"，而控制器返回视图是"redirect:test"，那么302跳转的路径是"http://localhost:port/ctx/controllerPath/test"，转由"controllerPath/test"的控制器处理，以上"ctx"是设置的应用上下文路径contextPath，"controllerPath"是控制器的RequestMapping value值，"handlerPath"是Handler的RequestMapping value值。举个例子：
	
            	@Controller
            	@RequestMapping("controllerPath")
            	public class ControllerTest() {
            	    @RequestMapping("handlerPath")
            	    public String handler() {
            	        return "redirect:test";
            	    }
            	}
    
    2、如果location有一个"/"前缀，那么302跳转至"http:localhost:prot/ctx/test"