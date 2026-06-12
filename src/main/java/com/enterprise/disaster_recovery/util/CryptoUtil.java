package com.enterprise.disaster_recovery.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;

public class CryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes();

    public static File encryptFile(File inputFile) throws Exception {
        return processFile(Cipher.ENCRYPT_MODE, inputFile, new File(inputFile.getAbsolutePath() + ".enc"));
    }

    public static File decryptFile(File inputFile) throws Exception {
        return processFile(Cipher.DECRYPT_MODE, inputFile, new File(inputFile.getAbsolutePath().replace(".enc", "")));
    }

    private static File processFile(int cipherMode, File inputFile, File outputFile) throws Exception {
        Key secretKey = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(cipherMode, secretKey);

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] inputBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);
            outputStream.write(outputBytes);
        }
        inputFile.delete();
        return outputFile;
    }
}