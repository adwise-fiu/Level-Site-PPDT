package ppdt.components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Enumeration;
import java.util.Hashtable;

import security.DGK.DGKKeyPairGenerator;
import security.misc.HomomorphicException;
import security.paillier.PaillierKeyPairGenerator;

public class client {

	public static Hashtable<String, String> read_features(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		Hashtable<String, String> values = new Hashtable<String, String>();
		String line;
		
		while((line = br.readLine()) != null) {
			String key, value;
			String[] splitted = line.split("\\t");
			key = splitted[0];
			value = splitted[1];
			if (value.equals("t") ||(value.equals("yes"))) {
				value = "1"; 
			}
			if (value.equals("f") || (value.equals("no"))) {
				value = "0";
			}
			values.put(key, value);
		}
		
		// WARNING: SEE LevelOrderTreeTraversalGetValues
		// I DONT KNOW HOW THE "other" edge case is handled, but IT SHOULD BE MANAGED HERE!
		Enumeration<String> ex = values.keys();
        while (ex.hasMoreElements()){
            String key = ex.nextElement();
            if (values.get(key).equals("other")) {
            	values.put(key, "1");
            }
        }
		br.close();
		return values;
	}

	public static void main(String [] args) throws HomomorphicException, IllegalArgumentException, UnknownHostException, IOException {
		// ARGUMENTS
		int KEY_SIZE = 1024;
		String server_ip = "127.0.0.1"; // Level-Site 0
		String file_name = "../data/hypothyroid.arff";
		ServerSocket receive = new ServerSocket(9000);
		ObjectInputStream from_level_site_d = null;
		ObjectOutputStream to_level_site_d = null;
		ObjectInputStream from_level_site_zero = null;
		ObjectOutputStream to_level_site_zero = null;
		
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
		Hashtable<String, String> feature = read_features(file_name);
		
		// Communicate with Level-Site 0
		try {
			Socket level_site_zero = new Socket(server_ip, 9000);
			from_level_site_zero = new ObjectInputStream(level_site_zero.getInputStream());
			to_level_site_zero = new ObjectOutputStream(level_site_zero.getOutputStream());
			
			// Send the Public Keys using Alice and Bob
			
			// Send the encrypted data to Level-Site 0
			
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
