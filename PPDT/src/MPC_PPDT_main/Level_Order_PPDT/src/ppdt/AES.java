package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AES {
	private SecretKey key;
	private int iterations = 65536;
	private int key_length = 256;
	private byte [] salt = "123456789".getBytes();
	private SecureRandom random = new SecureRandom();
	
	private byte [] iv_bytes = new byte[16];
	private IvParameterSpec ivspec = null;  
	private String iv_string = null;
	
	// TODO: Kubernetes, secret salt and password?
	public AES(String password) {
		try {
			getKeyFromPassword(password);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}
	
	public String getIV() {
		return this.iv_string;
	}
	
	public void getKeyFromPassword(String password)
		    throws NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), this.salt, this.iterations, this.key_length);
		this.key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
	}
	
	public String encrypt(String strToEncrypt) 
			throws NoSuchAlgorithmException, NoSuchPaddingException, 
			IllegalBlockSizeException, BadPaddingException, InvalidKeyException, 
			UnsupportedEncodingException, InvalidAlgorithmParameterException {
		
		// Generate new IV
		random.nextBytes(iv_bytes);
		ivspec = new IvParameterSpec(iv_bytes);
		iv_string = Base64.getEncoder().encodeToString(iv_bytes);
		
		// Run Encryption
	    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");  
	    cipher.init(Cipher.ENCRYPT_MODE, this.key, ivspec);
		byte [] output = cipher.doFinal(strToEncrypt.getBytes("UTF-8"));
		return Base64.getEncoder().encodeToString(output);
	}

	public String decrypt(String strToDecrypt, String iv) 
			throws NoSuchAlgorithmException, NoSuchPaddingException, 
			IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		// Get IV
		iv_string = iv;
		iv_bytes = Base64.getDecoder().decode(iv);
		ivspec = new IvParameterSpec(iv_bytes);
		
		// Decrypt the value
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");  
	    cipher.init(Cipher.DECRYPT_MODE, this.key, ivspec);
		byte [] output = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
		return new String(output);
	}
}