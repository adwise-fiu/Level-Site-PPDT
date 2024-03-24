package weka.finito;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.lang.System;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import security.dgk.DGKKeyPairGenerator;
import security.dgk.DGKPrivateKey;
import security.dgk.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierKeyPairGenerator;
import security.paillier.PaillierPrivateKey;
import security.paillier.PaillierPublicKey;
import security.socialistmillionaire.bob_joye;
import weka.finito.structs.features;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static weka.finito.utils.shared.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class client implements Runnable {
	private static final Logger logger = LogManager.getLogger(client.class);
	private final SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
	private final String classes_file = "classes.txt";
	private final String features_file;
	private final int key_size;
	private final int precision;
	private final String [] level_site_ips;
	private final int [] level_site_ports;
	private final int port;
	private String classification = null;
	private KeyPair dgk;
	private KeyPair paillier;
	private features feature = null;
	private boolean classification_complete = false;
	private String [] classes;
	private DGKPublicKey dgk_public_key;
	private PaillierPublicKey paillier_public_key;
	private DGKPrivateKey dgk_private_key;
	private PaillierPrivateKey paillier_private_key;
	private final HashMap<String, String> hashed_classification = new HashMap<>();
	private final String server_ip;
	private final int server_port;

    //For k8s deployment.
    public static void main(String[] args) {
		setup_tls();
		
        // Declare variables needed.
        int key_size = -1;
        int precision = -1;
		int port = -1;
        String level_site_string;
		String server_ip;

        // Read in our environment variables.
        level_site_string = System.getenv("LEVEL_SITE_DOMAINS");
        if(level_site_string == null || level_site_string.isEmpty()) {
            logger.fatal("No level site domains provided.");
            System.exit(1);
        }
        String[] level_domains = level_site_string.split(",");

        try {
            port = Integer.parseInt(System.getenv("PORT_NUM"));
        } catch (NumberFormatException e) {
            logger.fatal("No port provided for the Level Sites.");
            System.exit(1);
        }

        try {
            precision = Integer.parseInt(System.getenv("PRECISION"));
        } catch (NumberFormatException e) {
            logger.fatal("No Precision value provided.");
            System.exit(1);
        }

        try {
            key_size = Integer.parseInt(System.getenv("PPDT_KEY_SIZE"));
        } catch (NumberFormatException e) {
            logger.fatal("No crypto key provided value provided.");
            System.exit(1);
        }

		server_ip = System.getenv("SERVER");
		if(server_ip == null || server_ip.isEmpty()) {
			logger.fatal("No server site domain provided.");
			System.exit(1);
		}

		client test = null;
		if (args.length == 1) {
			// Test with level-sites
			test = new client(key_size, args[0], level_domains, port, precision, server_ip, port);
		}
		else if (args.length == 2) {
			// Test with just a server-site directly
			if (args[1].equalsIgnoreCase("--server")) {
				test = new client(key_size, args[0], precision, server_ip, port);
			}
			else {
				test = new client(key_size, args[0], level_domains, port, precision, server_ip, port);
			}
		}
		else {
			logger.fatal("Missing Testing Data set as an argument parameter");
			System.exit(1);
		}
		test.run();
    }

	// For local host testing with GitHub Actions
	public client(int key_size, String features_file, String [] level_site_ips, int [] level_site_ports,
				  int precision, String server_ip, int server_port) {
		this.key_size = key_size;
		this.features_file = features_file;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = level_site_ports;
		this.precision = precision;
		this.port = -1;
		this.server_ip = server_ip;
		this.server_port = server_port;
	}

	// Testing using Kubernetes
	public client(int key_size, String features_file, String [] level_site_ips, int port,
				  int precision, String server_ip, int server_port) {
		this.key_size = key_size;
		this.features_file = features_file;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = null;
		this.precision = precision;
		this.port = port;
		this.server_ip = server_ip;
		this.server_port = server_port;
	}

	// Testing using only a single server, no level-sites
	public client(int key_size, String features_file,
				  int precision, String server_ip, int server_port) {
		this.key_size = key_size;
		this.features_file = features_file;
		this.level_site_ips = null;
		this.level_site_ports = null;
		this.port = -1;
		this.precision = precision;
		this.server_ip = server_ip;
		this.server_port = server_port;
	}

	// Get Classification after Evaluation
	public String getClassification() {
		return this.classification;
	}

	public void generate_keys() {
		// Generate Key Pairs
		DGKKeyPairGenerator p = new DGKKeyPairGenerator();
		p.initialize(key_size, null);
		dgk = p.generateKeyPair();

		PaillierKeyPairGenerator pa = new PaillierKeyPairGenerator();
		pa.initialize(key_size, null);
		paillier = pa.generateKeyPair();

		dgk_public_key = (DGKPublicKey) dgk.getPublic();
		paillier_public_key = (PaillierPublicKey) paillier.getPublic();
		dgk_private_key = (DGKPrivateKey) dgk.getPrivate();
		paillier_private_key = (PaillierPrivateKey) paillier.getPrivate();
	}

	private boolean need_keys() {
		try {
			dgk_public_key = DGKPublicKey.readKey("dgk.pub");
			paillier_public_key = PaillierPublicKey.readKey("paillier.pub");
			dgk_private_key = DGKPrivateKey.readKey("dgk");
			paillier_private_key = PaillierPrivateKey.readKey("paillier");
			dgk = new KeyPair(dgk_public_key, dgk_private_key);
			paillier = new KeyPair(paillier_public_key, paillier_private_key);
			classes = read_classes();
			for (String aClass : classes) {
				hashed_classification.put(hash(aClass), aClass);
			}
			return false;
		}
		catch (NoSuchAlgorithmException | IOException | ClassNotFoundException e) {
			return true;
		}
    }

	private String [] read_classes() {
		// Remember the classes of DT as well
		StringBuilder content = new StringBuilder();
		String line;

		try (BufferedReader reader =
					 new BufferedReader(new FileReader(classes_file))) {
			while ((line = reader.readLine()) != null) {
				content.append(line);
				content.append(System.lineSeparator());
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return content.toString().split(System.lineSeparator());
	}

	// Used for set-up
	private void setup_with_server_site(PaillierPublicKey paillier, DGKPublicKey dgk)
			throws IOException, ClassNotFoundException {
		logger.info("Connecting to " + server_ip + ":" + server_port + " for set-up");
		try (SSLSocket server_site = (SSLSocket) factory.createSocket(server_ip, server_port)) {
			// Step: 3
			server_site.setEnabledProtocols(protocols);
			server_site.setEnabledCipherSuites(cipher_suites);

			// Step: 4 {optional}
			server_site.startHandshake();

			ObjectOutputStream to_server_site = new ObjectOutputStream(server_site.getOutputStream());
			ValidatingObjectInputStream from_server_site = get_ois(server_site);

			// Receive a message from the client to get their keys
			to_server_site.writeObject(paillier);
			to_server_site.writeObject(dgk);
			to_server_site.flush();

			// Get leaves from Server-site
			Object o = from_server_site.readObject();
			classes = (String []) o;
		}
		logger.info("Completed set-up with server");
	}

	// Evaluation
	private features read_features(String path,
								   PaillierPublicKey paillier_public_key,
								   DGKPublicKey dgk_public_key, int precision)
					throws IOException, HomomorphicException {

        return new features(path, precision, paillier_public_key, dgk_public_key);
	}

	private void evaluate_with_server_site(Socket server_site)
			throws IOException, HomomorphicException, ClassNotFoundException {
		// Communicate with each Level-Site
		Object o;
		bob_joye client;

		// Send the Public Keys using Alice and Bob
		client = new bob_joye(paillier, dgk, null);
		client.set_socket(server_site);

		// Send the encrypted data to Level-Site
		ObjectOutputStream oos = new ObjectOutputStream(server_site.getOutputStream());
		oos.writeObject(this.feature);
		oos.flush();

		// Yup, I need the while loop here because all level-sites are at server
		int comparison_type;
		while (true) {
			comparison_type = client.readInt();
			if (comparison_type == -1) {
				this.classification_complete = true;
				break;
			}
			else if (comparison_type == 0) {
				logger.info("Comparing two Paillier Values");
				client.setDGKMode(false);
				client.Protocol2();
			}
			else if (comparison_type == 1) {
				logger.info("Comparing two DGK Values");
				client.setDGKMode(true);
				client.Protocol2();
			}
			else if (comparison_type == 2) {
				logger.info("Comparing two DGK Values, Encrypted Equals!");
				client.setDGKMode(true);
				client.encrypted_equals();
			}
		}

		o = client.readObject();
		if (o instanceof String) {
			classification = (String) o;
			classification = hashed_classification.get(classification);
		}
	}

	// Function used to Evaluate for each level-site
	private void communicate_with_level_site(Socket level_site)
			throws IOException, ClassNotFoundException, HomomorphicException {
		// Communicate with each Level-Site
		Object o;
		bob_joye client;

		// Create I/O stream and send features
		// this.feature.set_next_index(next_index);
		ObjectOutputStream to_level_site = new ObjectOutputStream(level_site.getOutputStream());
		ObjectInputStream from_level_site = get_ois(level_site);
		to_level_site.writeObject(this.feature);
		to_level_site.flush();

		// Send the Public Keys using Alice and Bob
		client = new bob_joye(paillier, dgk, null);
		client.set_socket(level_site);

		// Get the comparison
		// I am not sure why I need this loop, but you will only need 1 comparison.
		int comparison_type;
		while (true) {
			comparison_type = from_level_site.readInt();
			logger.info(String.format("Using comparison type %d", comparison_type));
			if (comparison_type == -1) {
				this.classification_complete = true;
				break;
			}
			else if (comparison_type == 0) {
				logger.info("Comparing two Paillier Values");
				client.setDGKMode(false);
				client.Protocol2();
			}
			else if (comparison_type == 1) {
				logger.info("Comparing two DGK Values");
				client.setDGKMode(true);
				client.Protocol2();
			}
			else if (comparison_type == 2) {
				logger.info("Comparing two DGK Values, Encrypted Equals!");
				client.setDGKMode(true);
				client.encrypted_equals();
			}
		}

		// Get boolean from level-site:
		// true - get leaf value
		// false - get encrypted AES index for next round
		classification_complete = client.readBoolean();
		if (classification_complete) {
			o = client.readObject();
			if (o instanceof String) {
				classification = (String) o;
				classification = hashed_classification.get(classification);
			}
		}
		else {
			o = from_level_site.readObject();
			if (o instanceof features) {
				this.feature = (features) o;
			}
		}
	}

	// Function used to Train (if needed) and Evaluate
	public void run() {

		// Step: 1
		boolean talk_to_server_site = this.need_keys();

		try {
			// Don't regenerate keys if you are just using a different VALUES file
			if (talk_to_server_site) {
				logger.info("Need to generate keys...");
				generate_keys();
			}
			else {
				logger.info("I already read the keys from a file made from a previous run...");
			}

			feature = read_features(features_file, paillier_public_key, dgk_public_key, precision);

			// Client needs to give server-site public key (to give to level-sites)
			// Client needs to know all possible classes...
			if (talk_to_server_site) {
				// Don't send keys to server-site to ask for classes now it is assumed level-sites are up
				setup_with_server_site(paillier_public_key, dgk_public_key);
				for (String aClass : classes) {
					hashed_classification.put(hash(aClass), aClass);
				}
				// Make sure level-sites got everything...
				Thread.sleep(2000);
			}
			else {
				logger.info("Not contacting server-site. Seems you just want to test on the" +
						" same PPDT but different VALUES");
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		int connection_port;
		long start_time = System.nanoTime();

		// If you are just evaluating directly with the server-site
		if (level_site_ips == null) {
			try(SSLSocket server_site = (SSLSocket) factory.createSocket(server_ip, server_port)) {
				// Step: 3
				server_site.setEnabledProtocols(protocols);
				server_site.setEnabledCipherSuites(cipher_suites);

				// Step: 4 {optional}
				server_site.startHandshake();

				logger.info("Client connected to sever-site with PPDT");
				evaluate_with_server_site(server_site);
				long end_time = System.nanoTime();
				logger.info("The Classification is: " + classification);
				double run_time = (double) (end_time - start_time);
				run_time = run_time/1000000;
				logger.info(String.format("It took %f ms to classify\n", run_time));
				finish_evaluation();
			}
			catch (HomomorphicException | IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
			return;
		}

		try {
			for (int i = 0; i < level_site_ips.length; i++) {
				if (classification_complete) {
					break;
				}
				if (port == -1) {
					assert level_site_ports != null;
					connection_port = level_site_ports[i];
					logger.info("Local Test: " + level_site_ips[i] + ":" + level_site_ports[i]);
				}
				else {
					connection_port = port;
				}

				try(SSLSocket level_site = (SSLSocket) factory.createSocket(level_site_ips[i], connection_port)) {
					// Step: 3
					level_site.setEnabledProtocols(protocols);
					level_site.setEnabledCipherSuites(cipher_suites);

					// Step: 4 {optional}
					level_site.startHandshake();
					logger.info("Client connected to level " + i);
					communicate_with_level_site(level_site);
				}
			}
            long end_time = System.nanoTime();
			logger.info("The Classification is: " + classification);
			double run_time = (double) (end_time - start_time);
			run_time = run_time/1000000;
            logger.info(String.format("It took %f ms to classify\n", run_time));
			finish_evaluation();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void finish_evaluation() throws IOException {
		// At the end, write your keys...
		dgk_public_key.writeKey("dgk.pub");
		paillier_public_key.writeKey("paillier.pub");
		dgk_private_key.writeKey("dgk");
		paillier_private_key.writeKey("paillier");

		// Remember the classes as well too...
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(classes_file, true))) {
			for (String aClass: classes) {
				writer.write(aClass);
				writer.write("\n");
			}
		}
	}
}
