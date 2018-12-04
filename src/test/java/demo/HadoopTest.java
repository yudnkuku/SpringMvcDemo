package demo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.TestDFSIO;
import org.apache.hadoop.io.IOUtils;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URL;

public class HadoopTest {

    static {
        URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
    }

    @Test
    public void readFile() {
        InputStream in = null;
        String input = "hdfs://127.0.0.1:9000/hello.txt";
        try {
            in = new URL(input).openStream();
            IOUtils.copyBytes(in, System.out, 4096, false);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(in);
        }
    }

    @Test
    public void getFileFromFileSystem() {
        String uri = "hdfs://127.0.0.1:9000/hello.txt";
        Configuration conf = new Configuration();
        InputStream in = null;
        try {
            FileSystem fs = FileSystem.get(URI.create(uri), conf);
            in = fs.open(new Path(uri));
            IOUtils.copyBytes(in, System.out, 4096, false);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(in);
        }
    }

    @Test
    public void copyFile() throws Exception {
        String localSrc  ="D:\\hello.txt";
        String dist = "hdfs://127.0.0.1:9000/hello.txt";
        InputStream in = new BufferedInputStream(new FileInputStream(localSrc));
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(dist), conf);
        OutputStream out = fs.append(new Path(dist));
        IOUtils.copyBytes(in, out, 1024, true);
    }

    @Test
    public void testPerformance() {
        String[] args = new String[]{"-write", "-nrFiles", "10", "-size", "10MB"};
        TestDFSIO.main(args);
    }
}
