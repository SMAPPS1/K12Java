package com.bccl.dxapi.apiutility;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.*;

public class AESencrp {
    
     private static final String ALGO = "AES";
    /*private static final byte[] keyValue = 
        new byte[] { 'T', 'h', 'e', 'B', 'e', 's', 't','S', 'e', 'c', 'r','e', 't', 'K', 'e', 'y' };*/

 //    OnceUponBlueMO0n
 
    private static final byte[] keyValue = "timesgrouptimess".getBytes();
	//private static final byte[] keyValue2 = "4wydF55RpkNRZHl47MntVWwkaKZq5MVG".getBytes();
	//private static final byte[] keyValue2 = "ncx6b4lbGhxu9pi5bXB2DQdb".getBytes();
	private static final byte[] keyValue2 = "IuTzGpeo0I0Im4aR".getBytes();
	
	public static String encrypt(String Data) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(Data.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encVal);
        return encryptedValue;
    }

    public static String decrypt(String encryptedData) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedData);
        byte[] decValue = c.doFinal(decordedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }

    private static Key generateKey() throws Exception {
        Key key = new SecretKeySpec(keyValue, ALGO);
        return key;
	}

	public static String encryptPrivate(String data) throws Exception {

        Key key = new SecretKeySpec(keyValue2, ALGO);
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = c.doFinal(data.getBytes());
        String encryptedValue = new BASE64Encoder().encode(encVal);
        return encryptedValue;		
    }

}
