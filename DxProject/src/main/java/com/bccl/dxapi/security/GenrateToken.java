package com.bccl.dxapi.security;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class GenrateToken {

	static Logger log = Logger.getLogger(GenrateToken.class.getName());
	
	public static String issueToken(String pan, String deviceid) {
		long expireTime = System.currentTimeMillis() + 15 * 60 * 1000;
		long secretkey = (long) Math.floor(Math.random() * 9000000000L) + 1000000000L;
		String token = pan + ":" + expireTime + ":" + deviceid + ":" + secretkey;
		String encrypttoken = encrypt(token);
		return encrypttoken;
	}

	public static String createToken(javax.servlet.http.HttpServletRequest httpRequest) {
		String pan = (String) httpRequest.getAttribute("pan");
		String deviceid = (String) httpRequest.getAttribute("pan");
		long secretkey = (long) Math.floor(Math.random() * 9000000000L) + 1000000000L;
		String token = pan + ":" + deviceid + ":" + secretkey;
		String encrypttoken = encrypt(token);
		return encrypttoken;
	}

	public static String encrypt(String value) {
		try {
			String key = "bcctoi1521bl1234"; // 128 bit key
			String initVector = "RandomInitVector"; // 16 bytes IV
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			byte[] encrypted = cipher.doFinal(value.getBytes());
			String enval = new String(Base64.encodeBase64(encrypted));
			return enval;
		} catch (Exception ex) {
			log.info("Exception in encrypt : ",ex.fillInStackTrace());
		}

		return null;
	}

	public static String decrypt(byte[] encrypted) {
		try {

			String key = "bcctoi1521bl1234"; // 128 bit key
			String initVector = "RandomInitVector"; // 16 bytes IV
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(original);
		} catch (Exception ex) {
			log.info("Exception in decrypt : ",ex.fillInStackTrace());
		}
		return null;
	}

	public static String encryptJSON(String value, String pan) {
		try {
			String bitkey = pan.substring(0, 8) + "bccl1234";
			String key = bitkey; // 128 bit key
			String initVector = bitkey; // 16 bytes IV
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			byte[] encrypted = cipher.doFinal(value.getBytes());
			String enval = new String(Base64.encodeBase64(encrypted));
			return enval;
		} catch (Exception ex) {
			log.info("Exception in encryptJSON : ",ex.fillInStackTrace());
		}

		return null;
	}

	public static String decryptJson(byte[] encrypted, String pan) {
		try {
			String bitkey = pan.substring(0, 8) + "bccl1234";
			String key = bitkey; // 128 bit key
			String initVector = bitkey; // 16 bytes IV
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(original);
		} catch (Exception ex) {
			log.info("Exception in encryptJSON : ",ex.fillInStackTrace());
		}
		return null;
	}

	public static String encode(String url) {
		try {
			String encodeURL = URLEncoder.encode(url, "UTF-8");
			return encodeURL;
		} catch (UnsupportedEncodingException e) {
			log.info("Exception in encode : ",e.fillInStackTrace());
			return "Issue while encoding" + e.getMessage();
		}
	}

	public static String decode(String url) {
		try {
			String prevURL = "";
			String decodeURL = url;
			decodeURL = URLDecoder.decode(decodeURL, "UTF-8");
			return decodeURL;
		} catch (UnsupportedEncodingException e) {
			log.info("Exception in decode : ",e.fillInStackTrace());
			return "Issue while decoding" + e.getMessage();
		}
	}

	public static String random(int size) {

		StringBuilder generatedToken = new StringBuilder();
		try {
			SecureRandom number = SecureRandom.getInstance("SHA1PRNG");
			for (int i = 0; i < size; i++) {
				generatedToken.append(number.nextInt(6));
			}
		} catch (NoSuchAlgorithmException e) {
			log.info("Exception in random : ",e.fillInStackTrace());
		}
		return generatedToken.toString();
	}

}
