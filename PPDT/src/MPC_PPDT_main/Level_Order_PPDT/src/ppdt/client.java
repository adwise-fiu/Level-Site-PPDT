package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Hashtable;

import security.DGK.DGKKeyPairGenerator;
import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierKeyPairGenerator;
import security.paillier.PaillierPublicKey;
import security.socialistmillionaire.bob;

public class client implements Runnable {
	private String features_file;
	private int key_size = -1;
	private DGKPublicKey dgk_public_key = null;
	private PaillierPublicKey paillier_public_key = null;
	private int precision;
	
	private String [] level_site_ips;
	private int [] level_site_ports;
	private int port = -1;
	
	private ObjectInputStream from_level_site;
	private ObjectOutputStream to_level_site;

	// For local host testing
	public client(int key_size, String features_file, String [] level_site_ips, int [] level_site_ports, int precision) {
		this.key_size = key_size;
		this.features_file = features_file;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = level_site_ports;
		this.precision = precision;
	}
	
	// For cloud environment
	public client(int key_size, String features_file, String [] level_site_ips, int port, int precision) {
		this.key_size = key_size;
		this.features_file = features_file;
		this.level_site_ips = level_site_ips;
		this.port = port;
		this.precision = precision;
	}

	public Hashtable<String, BigIntegers> read_features(String path, 
			PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key, int precision) 
					throws IOException, HomomorphicException {

		BigInteger integerValue;
		BigInteger integerValuePaillier;
		BigInteger integerValueDGK;
		int intermediateInteger;

		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;
			Hashtable<String, BigIntegers> values = new Hashtable<String, BigIntegers>();
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

				try {
					integerValue = new BigInteger(value);
					integerValuePaillier = PaillierCipher.encrypt(integerValue, paillier_public_key);
					integerValueDGK = DGKOperations.encrypt(integerValue, dgk_public_key);
					values.put(key, new BigIntegers(integerValuePaillier, integerValueDGK));
				} 
				catch (NumberFormatException nfe){
					intermediateInteger = (int) Double.parseDouble(value)*(int)Math.pow(10, precision);
					integerValuePaillier = PaillierCipher.encrypt(intermediateInteger, paillier_public_key);
					integerValueDGK = DGKOperations.encrypt(intermediateInteger, dgk_public_key);
					values.put(key, new BigIntegers(integerValuePaillier, integerValueDGK));
				}
			}
			return values;
		}
	}

	
	public void run() {
		// Generate Key Pair
		DGKKeyPairGenerator p = new DGKKeyPairGenerator();
		p.initialize(key_size, null);
		KeyPair dgk = p.generateKeyPair();
		dgk_public_key = (DGKPublicKey) dgk.getPublic();

		PaillierKeyPairGenerator pa = new PaillierKeyPairGenerator();
		p.initialize(key_size, null);
		KeyPair paillier = pa.generateKeyPair();
		paillier_public_key = (PaillierPublicKey) paillier.getPublic();
		
		// Read the Features
		Hashtable<String, BigIntegers> feature = null;
		try {
			feature = read_features(features_file, paillier_public_key, dgk_public_key, precision);
		} 
		catch (IOException | HomomorphicException e1) {
			e1.printStackTrace();
		}
		
		// Communicate with each Level-Site	
		Socket level_site = null;
		try {
			for (int i = 0; i < level_site_ips.length; i++) {
				if (port == -1) {
					level_site = new Socket(level_site_ips[i], level_site_ports[i]);
				}
				else {
					level_site = new Socket(level_site_ips[i], port);
				}
				
				from_level_site = new ObjectInputStream(level_site.getInputStream());
				to_level_site = new ObjectOutputStream(level_site.getOutputStream());
				
				// Send the Public Keys using Alice and Bob
				bob client = new bob(level_site, dgk, paillier);

				// Send the encrypted data to Level-Site
				to_level_site.writeObject(feature);
				
				// Send bool:
				// 1- true, there is a encrypted index coming
				// 2- false, there is NO encrypted index coming
				
				// Work with the comparison
				int comparison_type = -1;
				while(true) {
					comparison_type = from_level_site.readInt();
					if (comparison_type == -1) {
						break;
					}
					else if (comparison_type == 0) {
						client.setDGKMode(false);
					}
					else if (comparison_type == 1){
			            client.setDGKMode(true);
			        }
			        client.Protocol4();
				}
				
				// Get boolean from level-site:
				// true - get leaf value
				// false - get encrypted AES index for next round
				
				from_level_site.close();
				to_level_site.close();
			}
		}
		catch (IOException e) {
			
		}
		catch (Exception e) {
			
		}
	}
}
