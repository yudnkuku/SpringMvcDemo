package demo;

import java.io.Serializable;

public class SerializableObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String str0;

    private transient String str1;

    private static String str2 = "abc";

    public SerializableObject(String str0, String str1) {
        this.str0 = str0;
        this.str1 = str1;
    }

    public String getStr0() {
        return str0;
    }

    public String getStr1() {
        return str1;
    }
}
