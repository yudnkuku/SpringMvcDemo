package spring.config;

import com.google.gson.Gson;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import spring.bean.Person;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

public class PersonHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return (parameter.getParameterType().equals(Person.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
            throws Exception {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(servletRequest);
        MediaType mediaType = inputMessage.getHeaders().getContentType();
        Charset charset = (mediaType.getCharset() != null) ? mediaType.getCharset() : Charset.forName("UTF-8");
        Reader reader = new InputStreamReader(inputMessage.getBody(), charset);
        Gson gson = new Gson();
        Class<Person> type = (Class<Person>) parameter.getParameterType();
        return gson.fromJson(reader, type);
    }
}
