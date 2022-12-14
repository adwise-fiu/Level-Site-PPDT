package ppdt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Hashtable;

import security.DGK.DGKKeyPairGenerator;
import security.misc.HomomorphicException;
import security.paillier.PaillierKeyPairGenerator;

public class client {

	public static Hashtable<String, String> read_feature(String path) throws IOException {
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
			// WARNING: SEE LevelOrderTreeTraversalGetValues
			// I DONT KNOW HOW THE "other" edge case is handled, but IT SHOULD BE MANAGED HERE!
			values.put(key, value);
		}
		br.close();
		return values;
	}

	public static void main(String [] args) throws HomomorphicException, IllegalArgumentException, UnknownHostException, IOException {
		// ARGUMENTS
		int KEY_SIZE = 1024;
		String server_ip = "127.0.0.1"; // Level-Site 0
		String file_name = "evil.txt";
		ServerSocket receive = null;
		
		// Generate Key Pair
		DGKKeyPairGenerator p = new DGKKeyPairGenerator();
		p.initialize(KEY_SIZE, null);
		KeyPair dgk = p.generateKeyPair();

		PaillierKeyPairGenerator pa = new PaillierKeyPairGenerator();
		p.initialize(KEY_SIZE, null);
		KeyPair paillier = pa.generateKeyPair();
		
		// Send Key to Server, the Server will give the key to each level site.
		// The shortcut would be use to initialization of alice and bob...
		//Socket level_site_zero = new Socket(server_ip, 9254);
		//bob andrew = new bob(level_site_zero, paillier, dgk);
		
		// Read the Features
		Hashtable<String, String> feature = read_feature(file_name);
		
		// Send ENCRYPTED data to Level-Site 0

		// Receive data from Level-site d
		//Socket level_site_d = receive.accept();

	}
}
