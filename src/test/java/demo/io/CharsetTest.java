package demo.io;

import org.junit.Test;

import java.nio.charset.Charset;

public class CharsetTest {

    @Test
    public void charsetTest() {
        System.setProperty("file.encoding","UTF-16");
        System.out.println(Charset.defaultCharset().name());
    }
}
