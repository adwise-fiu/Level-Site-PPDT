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
		Hashtable<String, BigIntegers> values = new Hashtable<String, BigIntegers>();
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;

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


				intermediateInteger = (int) Double.parseDouble(value) * (int) Math.pow(10, precision);
				integerValuePaillier = PaillierCipher.encrypt(intermediateInteger, paillier_public_key);
				integerValueDGK = DGKOperations.encrypt(intermediateInteger, dgk_public_key);
				values.put(key, new BigIntegers(integerValuePaillier, integerValueDGK));


			}
		}
		return values;
	}

	
	public void run() {
		// Generate Key Pairs
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
		String next_index = null;
		String classification = null;
		String iv = null;
		Object o = null;
		boolean classification_complete = false;
		bob client;
		
		System.out.println("Read features...");
		
		try {
			for (int i = 0; i < level_site_ips.length; i++) {
				if (port == -1) {
					System.out.println("Local Test:" + level_site_ips[i] + " " + level_site_ports[i]);
					level_site = new Socket(level_site_ips[i], level_site_ports[i]);
				}
				else {
					level_site = new Socket(level_site_ips[i], port);
				}
				
				to_level_site = new ObjectOutputStream(level_site.getOutputStream());
				from_level_site = new ObjectInputStream(level_site.getInputStream());
				System.out.println("Client connected to level " + i);
				
				// Send the encrypted data to Level-Site
				to_level_site.writeObject(feature);
				to_level_site.flush();
				
				// Send the Public Keys using Alice and Bob
				client = new bob(level_site, paillier, dgk);
				
				// Send bool:
				// 1- true, there is a encrypted index coming
				// 2- false, there is NO encrypted index coming
				if (next_index == null) {
					to_level_site.writeBoolean(false);
				}
				else {
					to_level_site.writeBoolean(true);
					to_level_site.writeObject(next_index);
					to_level_site.writeObject(iv);
				}
				to_level_site.flush();
				
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
					else if (comparison_type == 1) {
			            client.setDGKMode(true);
			        }
			        client.Protocol4();
				}
				
				// Get boolean from level-site:
				// true - get leaf value
				// false - get encrypted AES index for next round
				classification_complete = from_level_site.readBoolean();
				o = from_level_site.readObject();
				if (classification_complete) {
					if (o instanceof String) {
						classification = (String) o;
					}
				}
				else {
					if (o instanceof String) {
						next_index = (String) o;
					}
					o = from_level_site.readObject();
					if (o instanceof String) {
						iv = (String) o;
					}
				}
			}
			System.out.println("The Classification is: " + classification);
		}
		catch (IOException e) {
			
		}
		catch (Exception e) {
			
		}
	}
}
