package demo;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AES128Test {

    private static final String KEY = "1234567891234567";

    public static void main(String[] args) throws Exception {
        String msg = "Hello World";
        String key = "1234567891234567";
        String encoded = encrypt(msg, key);
        if (encoded != null) {
            System.out.println(msg + "加密后：" + encoded);
        }
        String decrypted = decrypt(encoded, key);
        if (null != decrypted) {
            System.out.println("解密后：" + decrypted);
        }
    }

    private static String encrypt(String msg, String key) throws Exception {
        if (null == msg || "" == msg) {
            return null;
        }
        if (key.length() != 16) {
            System.out.println("加密key的长度不是16位");
            return null;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128, new SecureRandom(key.getBytes()));
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] encodedFormat = secretKey.getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(encodedFormat, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encrypted = cipher.doFinal(msg.getBytes("utf-8"));
        return new Base64().encodeToString(encrypted);
    }

    private static String decrypt(String encoded, String key) throws Exception {
        if (null == encoded || "" == encoded) {
            return null;
        }
        if (key.length() != 16) {
            System.out.println("解密key的长度不是16");
            return null;
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128, new SecureRandom(key.getBytes()));
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] encodedFormat = secretKey.getEncoded();
        SecretKeySpec secretKeySpec = new SecretKeySpec(encodedFormat, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] decryptedBase64 = new Base64().decode(encoded);
        byte[] decrypted = cipher.doFinal(decryptedBase64);
        return new String(decrypted, "utf-8");
    }

    @Test
    public void test() {
        try {
            String content = "ff01";
            String password = "fiberhomesodngrp";
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            byte[] byteContent = hexStr2byteArray(content);
            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
            byte[] result = cipher.doFinal(byteContent);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
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
}
