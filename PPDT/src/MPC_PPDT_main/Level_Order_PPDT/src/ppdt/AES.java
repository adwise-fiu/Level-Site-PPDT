package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AES {
	private SecretKey key;
	private byte [] key_bytes;
	private int iterations = 1000;
	private int key_length = 16;
	private byte [] salt = new byte[16];
	private SecureRandom random = new SecureRandom();
	
	public AES(String password) {
		random.nextBytes(this.salt);
		setKey(password);
	}
	
	public AES(String password, int iterations, int key_length) {
		this.iterations = iterations;
		this.key_length = key_length;
		random.nextBytes(this.salt);
		setKey(password);
	}
	
	// TODO: Horribly insecure! Needs better algorithms!
	public void setKey(String myKey)
	{
        MessageDigest sha = null;
        try {
        	key_bytes = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key_bytes = sha.digest(key_bytes);
            key_bytes = Arrays.copyOf(key_bytes, 16);
            this.key = new SecretKeySpec(key_bytes, "AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public String encrypt(String strToEncrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, this.key);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		}
		catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	public String decrypt(String strToDecrypt) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, this.key);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		}
		catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		return null;
	}
	
	public static void main (String [] args) {
		AES t = new AES("helloworld");
		String x = t.encrypt("evil");
		System.out.println(x);
		System.out.println(t.decrypt(x));
	}
}