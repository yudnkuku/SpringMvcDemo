package spring.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WeappSocketClient extends WebSocketClient {

    public WeappSocketClient(URI uri) {
        super(uri);
    }

    public static void main(String[] args) throws URISyntaxException {
        WeappSocketClient client = new WeappSocketClient(new URI("ws://10.99.20.118:2333"));
        client.connect();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("websocket连接完成");
        send("123456789");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(String.format("接收到服务端消息：%s", message));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("websocket连接关闭");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("发生错误：" + ex);
    }
}
