package demo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class NIOClient {

    private SocketChannel socketChannel;

    private Selector selector;

    private static final int CLIENT_PORT = 8099;

    private static final Charset CHARSET = Charset.forName("GBK");

    public NIOClient() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open(
                new InetSocketAddress("127.0.0.1", CLIENT_PORT));
        socketChannel.configureBlocking(false);
        //注册读和连接监听
        int ops = SelectionKey.OP_READ | SelectionKey.OP_CONNECT;
        socketChannel.register(selector, ops);
        System.out.println("Welcome to nio client");
    }

    public static void main(String[] args) throws Exception {
        new NIOClient().sendMsg();
    }

    private void sendMsg() throws IOException, InterruptedException {
        Scanner in = new Scanner(System.in);
        System.out.println("#Client:input the message you want to send!");
        Thread thread = new ClientThread();
        thread.start();
        Thread.sleep(500);

        while(in.hasNextLine()) {
            String msg = in.nextLine();
            socketChannel.write(CHARSET.encode(msg));
            System.out.println("#SendToServer:" + msg);
        }
    }

    private class ClientThread extends Thread {
        @Override
        public void run() {
            while(true) {
                try {
                    while(selector.select() > 0) {
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                        while(keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            if(key.isConnectable()) {
                                System.out.println("#Client:connect to server successfully");
                            }
                            if(key.isReadable()) {
                                try {
                                    revMsg(key);
                                } catch(IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            keyIterator.remove();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void revMsg(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuffer content = new StringBuffer();
        while(sc != null && sc.read(buffer) > 0) {
            buffer.flip();
            content.append(CHARSET.decode(buffer));
        }
        System.out.println("#ReceiveFromServer:\"" + content.toString() + "\"");
    }
}
