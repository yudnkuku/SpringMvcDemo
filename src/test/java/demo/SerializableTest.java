package demo;

import java.io.*;

public class SerializableTest {

    public static void main(String[] agrs) throws Exception {
        File file = new File("D:" + File.separator + "s.text");
        OutputStream os = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(new SerializableObject("str0","srt1"));
        oos.close();

        InputStream is = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(is);
        SerializableObject obj = (SerializableObject) ois.readObject();
        System.out.println(obj.getStr0());
        System.out.println(obj.getStr1());
        ois.close();
    }
}
