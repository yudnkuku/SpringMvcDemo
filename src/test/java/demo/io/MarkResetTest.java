package demo.io;

import org.junit.Test;

import java.io.*;

public class MarkResetTest {

    @Test
    public void markResetTest() throws IOException {
        String content = "yudingjava!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        //mark supported
        if (!inputStream.markSupported()) {
            System.out.println("Mark method not supported");
        }
        int ch;
        boolean marked = false;
        while((ch = inputStream.read()) != -1) {    //read() pos++
            System.out.print((char) ch);
            if (((char) ch) == 'g' && !marked) {
                inputStream.mark(content.length()); //mark()方法标记mark=pos pos此时指向g后面的一个字符
                marked = true;
            }
            if (((char) ch) == '!' && marked) {
                inputStream.reset();    //reset()会重新设置pos=mark 即mark()方法标记的位置
                marked = false;
            }
        }
    }


    @Test
    public void charsetTest() throws Exception {
        String file = "d:/test.txt";
        String charset = "UTF-16";
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter osw = new OutputStreamWriter(fos,charset);
        try {
            osw.write("这是要保存的字符串。");
        } finally {
            osw.close();
        }

        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis,charset);
        StringBuffer stringBuffer = new StringBuffer();
        char[] buf = new char[64];
        int count = 0;
        try {
            while((count = (isr.read(buf))) != -1) {
                stringBuffer.append(buf,0,count);
            }
        } finally {
            isr.close();
        }
        System.out.println(stringBuffer.toString());
    }

    @Test
    public void classTest() {
        String path = this.getClass().getClassLoader().getResource("").toString();
        System.out.println(path);
        System.out.println(ClassLoader.getSystemClassLoader());
    }
}
