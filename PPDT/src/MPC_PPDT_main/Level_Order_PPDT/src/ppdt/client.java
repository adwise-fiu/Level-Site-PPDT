package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Enumeration;
import java.util.Hashtable;

import security.DGK.DGKKeyPairGenerator;
import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierKeyPairGenerator;
import security.paillier.PaillierPublicKey;
import security.socialistmillionaire.bob;

public class client {

	static ObjectInputStream from_level_site_d = null;
	static ObjectOutputStream to_level_site_d = null;
	static ObjectInputStream from_level_site_zero = null;
	static ObjectOutputStream to_level_site_zero = null;
	
	private static PaillierPublicKey paillier_public_key;
	private static DGKPublicKey dgk_public_key;
	
	public static Hashtable<String, BigInteger> read_features(String path) throws IOException {

	      try (BufferedReader br = new BufferedReader(new FileReader(path))) {
	    	  String line;
			  Hashtable<String, BigInteger> values = new Hashtable<String, BigInteger>();
			  while ((line = br.readLine()) != null) {
			    String key, value;
			    String[] splitted = line.split("\\t");
			    key = splitted[0];
			    value = splitted[1];
			    if (value.equals("t") || (value.equals("yes"))) {
			    	value = "1";
			    }
			    if (value.equals("f") || (value.equals("no"))) {
			    	value = "0";
			    }
			    if (value.equals("other")) {
			    	value = "1";
			    }
			    values.put(key, new BigInteger(value));
			  }
			  return values;
		}
	}
	
	private static void evaluate(bob client, Hashtable<String, BigInteger> features)
			throws IOException, ClassNotFoundException, HomomorphicException {

		int comparison_type = -1;
		BigInteger thresh_hold = null;
		
		// TODO: Avoid leaking comparison type, send boolean from level-site
		// Hashtable is sent to client, encrypt the feature value
		while(true) {
			comparison_type = from_level_site_zero.readInt();
			if (comparison_type != -1) {
				break;
			}
			// Get variable name
			String variable_name = (String) from_level_site_zero.readObject();
			thresh_hold = features.get(variable_name);
			
	        if ((comparison_type == 1) || (comparison_type == 2) || (comparison_type == 4)) {
	        	thresh_hold = PaillierCipher.encrypt(thresh_hold, paillier_public_key);
	            client.setDGKMode(false);
	        }
	        else if ((comparison_type == 3) || (comparison_type == 5)) {
	        	thresh_hold = DGKOperations.encrypt(thresh_hold, dgk_public_key);
	            client.setDGKMode(true);
	        }
	        to_level_site_zero.writeObject(thresh_hold);
	        client.Protocol4();
		}
		// Get the encrypted index for next level-site and kick it off
		
	}
	
	public static void main(String [] args) throws HomomorphicException, IllegalArgumentException, UnknownHostException, IOException, ClassNotFoundException {
		// ARGUMENTS
		int KEY_SIZE = 1024;
		String server_ip = "127.0.0.1"; // Level-Site 0
		String file_name = "../data/hypothyroid.values";
		ServerSocket receive = new ServerSocket(9000);

		// Generate Key Pair
		DGKKeyPairGenerator p = new DGKKeyPairGenerator();
		p.initialize(KEY_SIZE, null);
		KeyPair dgk = p.generateKeyPair();

		PaillierKeyPairGenerator pa = new PaillierKeyPairGenerator();
		p.initialize(KEY_SIZE, null);
		KeyPair paillier = pa.generateKeyPair();
		
		// Send Key to Server, the Server will give the key to each level site.
		// The shortcut would be use to initialization of alice and bob...
		// Socket level_site_zero = new Socket(server_ip, 9254);
		// bob andrew = new bob(level_site_zero, paillier, dgk);
		
		// Read the Features
		Hashtable<String, BigInteger> feature = read_features(file_name);
		
		System.exit(0);
		
		// Communicate with each Level-Site 0
		try {
			// for loop
			Socket level_site_zero = new Socket(server_ip, 9000);
			from_level_site_zero = new ObjectInputStream(level_site_zero.getInputStream());
			to_level_site_zero = new ObjectOutputStream(level_site_zero.getOutputStream());
			
			// Send the Public Keys using Alice and Bob
			bob client = new bob(level_site_zero, dgk, paillier);
			paillier_public_key = client.getPaillierPublicKey();
			dgk_public_key = client.getDGKPublicKey();
			
			// Send the encrypted data to Level-Site 0
			evaluate(client, feature);
			
			// Get encrypted index, NOTE all Level-sites
			// Share SAME AES Key, do NOT give to Client the Key
			
		}
		catch (IOException e) {
			
		}
		finally {
			from_level_site_zero.close();
			to_level_site_zero.close();
		}
		
		// Receive data from Level-site d
		Socket level_site_d = receive.accept();
		from_level_site_d = new ObjectInputStream(level_site_d.getInputStream());
		to_level_site_d = new ObjectOutputStream(level_site_d.getOutputStream());
		// Object x = from_level_site_d.readObject();
	}
}
