package demo;

import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class MailTest {

    public static String senderAddress = "yuding7770@fiberhome.com";

    public static String recipientAddress = "yuding7770@fiberhome.com";

    public static String senderAccount = "yuding7770";

    public static String senderPwd = "ZXCVBNM123";

    public static void main(String[] args) throws Exception {
        //设置邮件服务器的参数
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.host", "smtp.fiberhome.com");
        properties.setProperty("mail.smtp.port", "465");

        //创建Session对象
        Session session = Session.getInstance(properties);
        session.setDebug(true);
        Message msg = getMimeMsg(session);

        //生成Transport对象
        Transport transport = session.getTransport();
        transport.connect(senderAccount, senderPwd);
        transport.sendMessage(msg, msg.getAllRecipients());
        //发送完关闭
        transport.close();
    }

    private static MimeMessage getMimeMsg(Session session) throws Exception {
        MimeMessage msg = new MimeMessage(session);
        //设置发送者地址
        msg.setFrom(senderAddress);
        //设置接收方地址
        msg.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(recipientAddress));
        //设置主题、内容以及发送时间
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        String subject = "技术资料上传：" + month + "/" + day;
        msg.setSubject(subject, "UTF-8");
        msg.setContent("Hello", "text/html;charset=UTF-8");
        msg.setSentDate(new Date());
        return msg;
    }
}
