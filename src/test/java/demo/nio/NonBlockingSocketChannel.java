package demo.nio;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class NonBlockingSocketChannel {

    private static final int PORT = 1234;

    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ServerSocket ss = ssc.socket();
        ss.bind(new InetSocketAddress(PORT));
        System.out.println("正在等待客户端连接。。。。时间为：" + System.currentTimeMillis());
        while(true) {
            SocketChannel sc = ssc.accept();
            if(sc == null) {
                Thread.sleep(1000);
            } else {
                System.out.println("客户端已有数据到来，IP地址为：" + sc.socket().getRemoteSocketAddress()
                                + "时间为：" + System.currentTimeMillis());
                ByteBuffer buffer = ByteBuffer.allocate(100);
                sc.read(buffer);
                buffer.flip();
                while(buffer.hasRemaining()) {
                    System.out.print((char) buffer.get());
                }
                buffer.clear();
                sc.close();
                System.exit(0);
            }
        }
    }
}
