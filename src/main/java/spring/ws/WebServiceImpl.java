package spring.ws;

import javax.jws.WebService;

@WebService
public class WebServiceImpl implements IWebService {
    @Override
    public String sayHello(String name) {
        System.out.println("WebService sayhello to " + name);
        return "sayHello " + name;
    }

    @Override
    public String save(String username, String pwd) {
        System.out.println("WebService save username=" + username + ",pwd=" + pwd);
        return "save success";
    }
}
