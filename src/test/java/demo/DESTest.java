package demo;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;

public class DESTest {

//    private static final String KEY = "TTMJ_234";

    private static final String KEY = "54544d4a5f323334";

    public String encrypt(String msg) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKeySpec = new DESKeySpec(hexStr2byteArray(KEY));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(desKeySpec);
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, random);
            byte[] datasource = hexStr2byteArray(msg);
//            int length = (1 + (datasource.length - 1) / 8) * 8;
//            byte[] data = new byte[length];
//            for (int i = 0; i < datasource.length; i++) {
//                data[i] = datasource[i];
//            }
            byte[] encoded = cipher.doFinal(datasource);
//            return new Base64().encodeToString(encoded);
            return byteArray2hexStr(encoded);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] hexStr2byteArray(String hex) {
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            String tmp = hex.substring(2*i, 2*i+2);
            b[i] = (byte) Integer.parseInt(tmp, 16);
        }
        return b;
    }

    private String byteArray2hexStr(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int tmp = (b[i] & 0x00FF);
            if (tmp < 16) {
                sb.append("0").append(Integer.toHexString(tmp));
            } else {
                sb.append(Integer.toHexString(tmp));
            }
        }
        return sb.toString();
    }

    public String decrypt(String encoded) {
        return "";

    }

    public static void main(String[] args) {
        String msg = "0101010101010101";
        DESTest test = new DESTest();
        System.out.println(test.encrypt(msg));
    }

}
