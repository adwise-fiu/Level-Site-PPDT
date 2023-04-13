package weka.finito;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Hashtable;

//For k8s deployment
import java.lang.System;

import security.DGK.DGKKeyPairGenerator;
import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierKeyPairGenerator;
import security.paillier.PaillierPublicKey;
import security.socialistmillionaire.bob;
import weka.finito.structs.BigIntegers;

public class client implements Runnable {
	private final String features_file;
	private final int key_size;
	private final int precision;

	private final String [] level_site_ips;
	private final int [] level_site_ports;
	private final int port;

	private String classification = null;

    //For k8s deployment.
    public static void main(String[] args) {
        // Declare variables needed.
        int key_size = -1;
        int precision = -1;
        String level_site_string = null;
        int port = -1;
        int success = 0;
        int failure = 0;

        // Check argv size. We need our data.
        if(args.length < 2) {
            System.out.println("Incorrect number of arguments.");
            System.exit(1);
        }

        // Read in our environment variables.
        level_site_string = System.getenv("LEVEL_SITE_DOMAINS");
        if(level_site_string == null || level_site_string.isEmpty()) {
            System.out.println("No level site domains provided.");
            System.exit(1);
        }
        String[] level_domains = level_site_string.split(",");

        try {
            port = Integer.parseInt(System.getenv("LEVEL_SITE_PORT"));
        } catch (NumberFormatException e) {
            System.out.println("No port provided for the Level Sites.");
            System.exit(1);
        }
        int[] ports = new int[level_domains.length];
        for(int i = 0; i < level_domains.length; i++) {
            ports[i] = port;
        }

        try {
            precision = Integer.parseInt(System.getenv("PRECISION"));
        } catch (NumberFormatException e) {
            System.out.println("No Precision value provided.");
            System.exit(1);
        }

        try {
            key_size = Integer.parseInt(System.getenv("PPDT_KEY_SIZE"));
        } catch (NumberFormatException e) {
            System.out.println("No crypto key provided value provided.");
            System.exit(1);
        }

        String data_directory = System.getenv("PPDT_DATA_DIR");
        if(data_directory == null || data_directory.isEmpty()) {
            System.out.println("Data directory not provided.");
            System.exit(1);
        }

        String file_path = args[1];
        try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] values = line.split(",");
                String data_set = values[0];
                String features = values[1];
                String expected_classification = values[2];
                String features_file = new File(data_directory, features).toString();
                client evaluate = new client(key_size, features_file, level_domains, ports, precision);
                new Thread(evaluate).start();
                Thread.sleep(1000L * level_domains.length);
                String classification  = evaluate.getClassification();
                //training_data = new File(data_directory, data_set).toString();
                System.out.println(classification);
                if(expected_classification == classification) {
                    success++;
                } else {
                    failure++;
                }
            }
            String print_string = String.format("Successes: %d, Failures %d", success, failure);
            System.out.println(print_string);
        } catch (FileNotFoundException e) {
            System.out.println("Data Files not found.");
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Issues with reading IO.");
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Interrupt.");
            System.exit(1);
        }
        System.exit(0);
    }

	// For local host testing
	public client(int key_size, String features_file, String [] level_site_ips, int [] level_site_ports, int precision) {
		this.key_size = key_size;
		this.features_file = features_file;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = level_site_ports;
		this.precision = precision;
		this.port = -1;
	}

	public final String getClassification() {
		return this.classification;
	}

	public final Hashtable<String, BigIntegers> read_features(String path,
														PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key, int precision)
					throws IOException, HomomorphicException {

		BigInteger integerValuePaillier;
		BigInteger integerValueDGK;
		int intermediateInteger;
		Hashtable<String, BigIntegers> values = new Hashtable<>();
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			String line;

			while ((line = br.readLine()) != null) {
				String key, value;
				String[] split = line.split("\\t");
				key = split[0];
				value = split[1];
				if (value.equals("t") || (value.equals("yes"))) {
					value = "1";
				}
				if (value.equals("f") || (value.equals("no"))) {
					value = "0";
				}
				if (value.equals("other")) {
					value = "1";
				}
				System.out.println("Initial value:"+value);
				intermediateInteger = (int) (Double.parseDouble(value) * Math.pow(10, precision));
				System.out.println("Value to be compared with:"+intermediateInteger);
				integerValuePaillier = PaillierCipher.encrypt(intermediateInteger, paillier_public_key);
				integerValueDGK = DGKOperations.encrypt(intermediateInteger, dgk_public_key);
				values.put(key, new BigIntegers(integerValuePaillier, integerValueDGK));
			}
			return values;
		}
	}

	public final void run() {
        long start_time = System.nanoTime();
		// Generate Key Pairs
		DGKKeyPairGenerator p = new DGKKeyPairGenerator();
		p.initialize(key_size, null);
		KeyPair dgk = p.generateKeyPair();
		DGKPublicKey dgk_public_key = (DGKPublicKey) dgk.getPublic();

		PaillierKeyPairGenerator pa = new PaillierKeyPairGenerator();
		p.initialize(key_size, null);
		KeyPair paillier = pa.generateKeyPair();
		PaillierPublicKey paillier_public_key = (PaillierPublicKey) paillier.getPublic();

		// Read the Features
		Hashtable<String, BigIntegers> feature = null;
		try {
			feature = read_features(features_file, paillier_public_key, dgk_public_key, precision);
		}
		catch (IOException | HomomorphicException e1) {
			e1.printStackTrace();
		}

		// Communicate with each Level-Site
		Socket level_site;
		String next_index = null;
		String iv = null;
		Object o;
		boolean classification_complete;
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

				ObjectOutputStream to_level_site = new ObjectOutputStream(level_site.getOutputStream());
				ObjectInputStream from_level_site = new ObjectInputStream(level_site.getInputStream());
				System.out.println("Client connected to level " + i);

				// Send the encrypted data to Level-Site
				to_level_site.writeObject(feature);
				to_level_site.flush();

				// Send the Public Keys using Alice and Bob
				client = new bob(level_site, paillier, dgk);

				// Send bool:
				// 1- true, there is an encrypted index coming
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
				int comparison_type;
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
						break;
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
            long end_time = System.nanoTime();
			System.out.println("The Classification is: " + classification);
            System.out.printf("It took %f ms to classify\n", (end_time - start_time)/1000000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
