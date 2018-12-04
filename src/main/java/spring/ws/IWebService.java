package spring.ws;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface IWebService {

    @WebMethod
    String sayHello(String name);

    @WebMethod
    String save(String username,String pwd);
}
