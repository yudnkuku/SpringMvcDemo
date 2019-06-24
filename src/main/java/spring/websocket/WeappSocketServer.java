package spring.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Collection;

public class WeappSocketServer extends WebSocketServer {

    private static final int PORT = 2333;

    public WeappSocketServer(InetSocketAddress address) {
        super(address);
    }

    public WeappSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    public static void main(String[] args) {
        WeappSocketServer server = new WeappSocketServer(PORT);
        server.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String input = reader.readLine();
                server.broadcastMsg(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        String address = webSocket.getRemoteSocketAddress().getAddress().getHostAddress();
        String msg = String.format("(%s)加入", address);
//        broadcastMsg(msg);
        printMsg(msg);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        String address = webSocket.getRemoteSocketAddress().getAddress().getHostAddress();
        String msg = String.format("(%s)离开", address);
//        broadcastMsg(msg);
        printMsg(msg);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        broadcastMsg(s);
        printMsg(s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        if (null != webSocket) {
            if (!webSocket.isClosed()) {
                webSocket.close(0);
            }
        }
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        printMsg("websocket服务端启动");
    }

    private void broadcastMsg(String msg) {
        Collection<WebSocket> clients = getConnections();
        synchronized (clients) {
            for (WebSocket webSocket : clients) {
                webSocket.send(msg);
            }
        }
    }

    private static void printMsg(String msg) {
        System.out.println(String.format("[%d]%s", System.currentTimeMillis(), msg));
    }
}
