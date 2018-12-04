package demo;

import com.thoughtworks.xstream.XStream;
import spring.entity.User;

import java.util.Date;

public class XStreamTest {

    public static void main(String[] args) {
        User user = new User("yuding","123","yudnkuku@163.com",new Date());
        XStream xStream = new XStream();
        xStream.alias("User",User.class);
        xStream.aliasField("name",User.class,"username");
        xStream.aliasField("pwd",User.class,"password");
        xStream.aliasField("e-mail",User.class,"email");
        xStream.aliasField("bd",User.class,"birthday");
        String xmlStr = xStream.toXML(user);
        System.out.println(xmlStr);
    }
}
