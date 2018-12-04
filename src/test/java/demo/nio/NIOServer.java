package demo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {

    private ServerSocketChannel serverSocketChannel;

    private Selector selector;

    private static final int SERVER_PORT = 8099;

    private static final Charset CHARSET = Charset.forName("GBK");

    public NIOServer() throws Exception {
        //创建Selector对象
        selector = Selector.open();
        //打开服务器端通道
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", SERVER_PORT));
        //设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //注册就绪监听
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("#Server:Welcome to nio server");
    }

    public static void main(String[] args) throws Exception {
        new NIOServer().service();
    }

    private void service() throws Exception {
        //通过阻塞式的select()方法选择就绪的通道
        while(selector.select() > 0) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while(keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if(key.isAcceptable()) {
                    SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                    if(sc != null) {
                        System.out.println("#Server：connect to client:"
                                + sc.socket().getInetAddress() + "--"
                                + sc.socket().getPort());
                        //设置为非阻塞模式
                        sc.configureBlocking(false);
                        //注册读监听
                        sc.register(selector, SelectionKey.OP_READ);
                    }
                }

                if(key.isReadable()) {
                    readMsg(key);
                }
                keyIterator.remove();
            }
        }
    }

    private void readMsg(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuffer content = new StringBuffer();
        try {
            while(sc.read(buffer) > 0) {
                buffer.flip();
                content.append(CHARSET.decode(buffer));
            }
            System.out.println("#ReceiveFromClient:" + content.toString());
            key.interestOps(SelectionKey.OP_READ);
            buffer.clear();
        } catch(IOException e) {
            key.cancel();
            if(key.channel() != null) {
                key.channel().close();
            }
        }
        sendEcho(key, content.toString());
    }

    private void sendEcho(SelectionKey key, String content) throws IOException {
        if(content.length() > 0) {
            SocketChannel sc = (SocketChannel) key.channel();
            String echo = "#SendToClient:\"" + content + ",from server" + "\"";
            sc.write(CHARSET.encode(echo));
            System.out.println(echo);
        }
    }
}
