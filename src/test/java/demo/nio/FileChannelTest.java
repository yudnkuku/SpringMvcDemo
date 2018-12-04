package demo.nio;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelTest {

    public static void main(String[] args) throws Exception{
        String path = "d:/test.txt";
        FileInputStream fis = new FileInputStream(path);
        FileChannel fc = fis.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(100);
        int readBytes = fc.read(buffer);
        while(readBytes != -1) {
            buffer.flip();
            while(buffer.hasRemaining()) {
                System.out.print((char) buffer.get());
            }
            buffer.clear();
            readBytes = fc.read(buffer);
        }
        fc.close();
    }

    @Test
    public void testWrite() throws Exception{
        File file = new File("d:/test.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel fc = raf.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(100);
        String str = "spring mvc";
        buffer.put(str.getBytes());
        buffer.flip();
        fc.write(buffer);
        buffer.clear();
        fc.close();
    }
}
