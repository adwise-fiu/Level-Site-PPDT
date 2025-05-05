package edu.fiu.adwise.weka.finito;

import java.io.File;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.lang.System;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Instances;
import weka.core.SerializationHelper;

import edu.fiu.adwise.homomorphic_encryption.dgk.DGKPublicKey;
import edu.fiu.adwise.homomorphic_encryption.misc.HomomorphicException;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierCipher;
import edu.fiu.adwise.homomorphic_encryption.socialistmillionaire.alice_joye;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.fiu.adwise.weka.finito.structs.features;
import edu.fiu.adwise.weka.finito.structs.level_order_site;
import edu.fiu.adwise.weka.finito.structs.NodeInfo;
import static edu.fiu.adwise.weka.finito.client.createServerSocket;
import static edu.fiu.adwise.weka.finito.client.createSocket;
import static edu.fiu.adwise.weka.finito.utils.shared.*;
import edu.fiu.adwise.weka.finito.utils.LabelEncoder;

/**
 * The `server` class handles the training and evaluation of decision trees
 * for privacy-preserving distributed decision tree (PPDT) classification.
 */
public final class server implements Runnable {

	/** Logger for the server class. */
	private static final Logger logger = LogManager.getLogger(server.class);

	/** Operating system name. */
	private static final String os = System.getProperty("os.name").toLowerCase();

	/** Path to the training data file. */
	private final String training_data;

	/** IP addresses of level sites. */
	private final String[] level_site_ips;

	/** Ports for level sites. */
	private int[] level_site_ports = null;

	/** Port for server communication. */
	private int port = -1;

	/** Paillier public key for encryption. */
	private PaillierPublicKey paillier_public;

	/** DGK public key for encryption. */
	private DGKPublicKey dgk_public;

	/** Precision for floating-point operations. */
	private final int precision;

	/** Decision tree model. */
	private ClassifierTree ppdt = null;

	/** List of all level sites. */
	private final List<level_order_site> all_level_sites = new ArrayList<>();

	/** Server port for communication. */
	private final int server_port;

	/** Number of evaluations to perform. */
	private int evaluations = 1;

	/** Label encoder for categorical data. */
	private final LabelEncoder label_encoder = new LabelEncoder();

	/**
	 * Main method to initialize and run the server.
	 *
	 * @param args Command-line arguments. Expects the path to the training data file.
	 */
    public static void main(String[] args) {
		setup_tls();

        int port = 0;
		int precision = 0;
		String training_data = null;

		// Get data for training.
		if (args.length == 1) {
			training_data = args[0];
		}
		else {
			logger.error("Missing Training Data set as an argument parameter");
			System.exit(1);
		}

        try {
            port = Integer.parseInt(System.getenv("PORT_NUM"));
        } catch (NumberFormatException e) {
			logger.error("No level site port provided");
            System.exit(1);
        }

		try {
			precision = Integer.parseInt(System.getenv("PRECISION"));
		} catch (NumberFormatException e) {
			logger.error("Precision is not defined.");
			System.exit(1);
		}
        
        // Pass data to level sites.
        String level_domains_str = System.getenv("LEVEL_SITE_DOMAINS");
        if(level_domains_str == null || level_domains_str.isEmpty()) {
			logger.error("No level site domains provided");
            System.exit(1);
        }
        String[] level_domains = level_domains_str.split(",");

		// Create and run the server.
        logger.info("Server Initialized and started running");
		server server = new server(training_data, level_domains, port, precision, port);
		server.run();
	}

	/**
	 * Constructor for cloud environment testing.
	 *
	 * @param training_data Path to the training data file.
	 * @param level_site_domains IP addresses of level sites.
	 * @param port Port for communication.
	 * @param precision Precision for floating-point operations.
	 * @param server_port Server port for communication.
	 */
	public server(String training_data, String [] level_site_domains, int port, int precision, int server_port) {
		this.training_data = training_data;
		this.level_site_ips = level_site_domains;
		this.port = port;
		this.precision = precision;
		this.server_port = server_port;
		// I will likely want more than 1 test with server-site, after level-sites are ready!
		this.evaluations = 1000;
	}

	/**
	 * Constructor for local host testing with level sites.
	 *
	 * @param training_data Path to the training data file.
	 * @param level_site_ips IP addresses of level sites.
	 * @param level_site_ports Ports for level sites.
	 * @param precision Precision for floating-point operations.
	 * @param server_port Server port for communication.
	 */
	public server(String training_data, String [] level_site_ips, int [] level_site_ports, int precision,
				  int server_port) {
		this.training_data = training_data;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = level_site_ports;
		this.precision = precision;
		this.server_port = server_port;
	}

	/**
	 * Constructor for local host testing without level sites.
	 *
	 * @param training_data Path to the training data file.
	 * @param precision Precision for floating-point operations.
	 * @param server_port Server port for communication.
	 */
	public server(String training_data, int precision, int server_port) {
		this.training_data = training_data;
		this.level_site_ips = null;
		this.precision = precision;
		this.server_port = server_port;
	}

	/**
	 * Runs the server site to handle direct evaluation requests from clients.
	 * Listens on the specified port and processes client requests until the
	 * specified number of evaluations is completed.
	 *
	 * @param port The port on which the server will listen for client connections.
	 * @throws IOException If an I/O error occurs while handling sockets.
	 * @throws HomomorphicException If an error occurs during homomorphic encryption operations.
	 * @throws ClassNotFoundException If a class required for deserialization is not found.
	 */
	private void run_server_site(int port) throws IOException, HomomorphicException, ClassNotFoundException {
		int count = 0;
		try (ServerSocket serverSocket = createServerSocket(port)) {
			logger.info("Server will be waiting for direct evaluation from client");
			while (count < evaluations) {
				try (Socket client_site = serverSocket.accept()) {
					evaluate_with_client_directly(client_site);
				}
				++count;
			}
		}
	}

	/**
	 * Evaluates a client request directly on the server without using level-sites.
	 * Processes encrypted features sent by the client and traverses the decision tree
	 * to find the appropriate leaf node.
	 * This is essentially the same as running all level-sites on one server, but
	 * you will lose timing attack protection, see the paper
	 * @param client_site The socket connection to the client.
	 * @throws IOException If an I/O error occurs during communication.
	 * @throws HomomorphicException If an error occurs during homomorphic encryption operations.
	 * @throws ClassNotFoundException If a class required for deserialization is not found.
	 */
	private void evaluate_with_client_directly(Socket client_site)
			throws IOException, HomomorphicException, ClassNotFoundException {

		Object client_input;
		features input = null;
		alice_joye Niu = new alice_joye();
		Niu.set_socket(client_site);
		Niu.setPaillierPublicKey(paillier_public);
		Niu.setDGKPublicKey(dgk_public);

		// Get encrypted features
		ValidatingObjectInputStream ois = get_ois(client_site);
		client_input = ois.readObject();
		if (client_input instanceof features) {
			input = (features) client_input;
		}
		assert input != null;
		long start_time = System.nanoTime();

		// Traverse DT until you hit a leaf, the client has to track the index...
		for (level_order_site level_site_data : all_level_sites) {
			// Handle at a level...
			NodeInfo leaf = traverse_level(level_site_data, input, Niu);

			// You found a leaf! No more traversing needed!
			if (leaf != null) {
				// Tell the client the value
				Niu.writeInt(-1);
				Niu.writeObject(leaf.getVariableName());
				long stop_time = System.nanoTime();
				double run_time = (double) (stop_time - start_time);
				run_time = run_time / 1000000;
				logger.info(String.format("Total Server-Site run-time took %f ms\n", run_time));
				break;
			}
		}
	}

	/**
	 * Handles communication with the client to exchange public keys and train the decision tree.
	 * Also trains level-sites if required and sends the label encoder back to the client.
	 *
	 * @throws Exception If an error occurs during communication or training.
	 */
	private void client_communication() throws Exception {
		ServerSocket serverSocket = createServerSocket(server_port);
        logger.info("Server ready to get public keys from client on port: {}", server_port);

		try (Socket client_site = serverSocket.accept()) {

			ObjectOutputStream to_client_site = new ObjectOutputStream(client_site.getOutputStream());
			ValidatingObjectInputStream from_client_site = get_ois(client_site);

			// Receive a message from the client to get their keys
			Object o = from_client_site.readObject();
			this.paillier_public = (PaillierPublicKey) o;

			o = from_client_site.readObject();
			this.dgk_public = (DGKPublicKey) o;
			logger.info("Server collected keys from client");

			// Train level-sites
			get_level_site_data(ppdt, all_level_sites);
			logger.info("Server trained DT");

			if (this.level_site_ips != null) {
				train_level_sites();
			}
			logger.info("Server just trained all the level-sites");

			// Also, I know the labels used for PPDT; the client must know
			to_client_site.writeObject(label_encoder);
			logger.info("Server sent the leaves and label encoder back to the client");
		}
		serverSocket.close();
	}

	/**
	 * Checks if the operating system is Unix-based.
	 *
	 * @return True if the operating system is Unix-based, false otherwise.
	 */
	private static boolean isUnix() {
		return (server.os.contains("nix") || server.os.contains("nux") || server.os.contains("aix"));
	}

	/**
	 * Generates a visual representation of the decision tree in DOT and PNG formats.
	 *
	 * @param j48 The decision tree to be visualized.
	 * @param base_name The base name for the output files.
	 * @throws Exception If an error occurs during file generation or processing.
	 */
	private static void printTree(ClassifierTree j48, String base_name)
			throws Exception {
		File output_dot_file = new File("output", base_name + ".dot");
		File output_image_file = new File("output", base_name + ".png");

		try (PrintWriter out = new PrintWriter(output_dot_file)) {
			out.println(j48.graph());
		}
		if (isUnix()) {
			String[] c = {"dot", "-Tpng", output_dot_file.toString(), "-o", output_image_file.toString()};
			try {
				Process p = Runtime.getRuntime().exec(c);
				if(!p.waitFor(5, TimeUnit.SECONDS)) {
					p.destroy();
				}
			}
			catch (IOException e) {
				logger.error("Can't generate image, so skip for now...");
			}
		}
	}

	/**
     * Trains a decision tree using the provided ARFF file and saves the model.
     * If the file is already a model file, it loads and visualizes the tree.
     * Reference:
     * <a href="https://stackoverflow.com/questions/33556543/how-to-save-model-and-apply-it-on-a-test-dataset-on-java/33571811#33571811">save_weka_model</a>
     * <a href="https://git.cms.waikato.ac.nz/weka/weka/-/blob/main/trunk/weka/src/main/java/weka/classifiers/trees/J48.java#L164">I reversed engineered this line of code</a>
     *
     * @param arff_file The path to the ARFF or model file.
     * @return The trained decision tree.
     * @throws Exception If an error occurs during training or file processing.
     */
	private static ClassifierTree train_decision_tree(String arff_file)
			throws Exception {
		File training_file = new File(arff_file);
		String base_name = training_file.getName().split("\\.")[0];
		File output_model_file = new File("output", base_name + ".model");

		File dir = new File("output");
		if (!dir.exists()) {
			if(!dir.mkdirs()) {
				System.err.println("Error Creating output directory to store models and images!");
				System.exit(1);
			}
		}

		if (arff_file.endsWith(".model")) {
			ClassifierTree j48 = (ClassifierTree) SerializationHelper.read(arff_file);
			printTree(j48, base_name);
			return j48;
		}

		// If this is an .arff file
		Instances train = null;

		try (BufferedReader reader = new BufferedReader(new FileReader(arff_file))) {
			train = new Instances(reader);
		} catch (IOException e2) {
			logger.error(e2.getStackTrace());
		}
		assert train != null;
		train.setClassIndex(train.numAttributes() - 1);

		// https://git.cms.waikato.ac.nz/weka/weka/-/blob/main/trunk/weka/src/main/java/weka/classifiers/trees/j48/BinC45ModelSelection.java
		// J48 -B -C 0.25 -M 2
		// -M 2 is minimum 2, DEFAULT
		// -B this tree ONLY works for binary split is true, so pick this model...
		// -C 0.25, default confidence
		BinC45ModelSelection j48_model = new BinC45ModelSelection(2, train, true, false);
		ClassifierTree j48 = new C45PruneableClassifierTree(j48_model, true, (float) 0.25, true, true, true);

	    j48.buildClassifier(train);
		SerializationHelper.write(output_model_file.toString(), j48);
		printTree(j48, base_name);
	    return j48;
	}

	/**
	 * Splits a plain-text decision tree into data for each level site.
	 * This method traverses the decision tree and organizes its nodes into levels,
	 * encrypting the data for secure processing at each level site.
	 *
	 * @param root The root of the decision tree to be split.
	 * @param all_level_sites A list to store the data for each level site.
	 * @throws Exception If an error occurs during encryption or data processing.
	 */
	private void get_level_site_data(ClassifierTree root, List<level_order_site> all_level_sites)
			throws Exception {

		if (root == null) {
			return;
		}

		Queue<ClassifierTree> q = new LinkedList<>();
		q.add(root);
		int level = 0;
		BigInteger temp_thresh = null;

		while (!q.isEmpty()) {
			level_order_site Level_Order_S = new level_order_site(level, paillier_public, dgk_public);
			int n = q.size();

			while (n > 0) {

				ClassifierTree p = q.peek();
				q.remove();

				NodeInfo node_info = null;
				assert p != null;
				if (p.isLeaf()) {
					String variable = p.getLocalModel().dumpLabel(0, p.getTrainingData());
					BigInteger encryption = PaillierCipher.encrypt(hash_to_big_integer(variable), paillier_public);
					node_info = new NodeInfo(true, encryption.toString(), 0, variable);
					Level_Order_S.append_data(node_info);
				}
				else {
					double threshold;

					for (int i = 0; i < p.getSons().length; i++) {
						// Determine which type of comparison is occurring.
						String leftSide = p.getLocalModel().leftSide(p.getTrainingData());
						String rightSide = p.getLocalModel().rightSide(i, p.getTrainingData());

						// For Type 1/6, it seems a label encoder might be helpful.
						// Maybe use hashing?
						// The client and server have to agree ahead of time...
						char[] rightSideChar = rightSide.toCharArray();
						int type = 0;
						char[] rightValue = new char[0];

						// Type 1: =
						// Don't think you can get anything except two booleans or string with '='
						if (rightSideChar[1] == '=') {
							type = 1;
							rightValue = new char[rightSideChar.length - 3];
							System.arraycopy(rightSideChar, 3, rightValue, 0, rightSideChar.length - 3);
						}
						// Type 6: !=
						// Don't think you can get anything except two booleans or string with '!='
						else if (rightSideChar[1] == '!') {
							type = 6;
							rightValue = new char[rightSideChar.length - 4];
							System.arraycopy(rightSideChar, 4, rightValue, 0, rightSideChar.length - 4);
						}
						// Type 2 or 3, > or >=
						else if (rightSideChar[1] == '>') {
							if (rightSideChar[2] == '=') {
								// Type 2: >=, not seen when building these trees
								type = 2;
								rightValue = new char[rightSideChar.length - 4];
								System.arraycopy(rightSideChar, 4, rightValue, 0, rightSideChar.length - 4);
							}
							else {
								// In practice, I only see type 3, which is great!
								// Type 3: >
								type = 3;
								rightValue = new char[rightSideChar.length - 3];
								System.arraycopy(rightSideChar, 3, rightValue, 0, rightSideChar.length - 3);
							}
						}
						// Type 4 or 5, < or <=
						else if (rightSideChar[1] == '<') {
							// In practice, I only see type 4, which is great!
							// Type 4: <=
							if (rightSideChar[2] == '=') {
								type = 4;
								rightValue = new char[rightSideChar.length - 4];
								System.arraycopy(rightSideChar, 4, rightValue, 0, rightSideChar.length - 4);
							}
							// Type 5: <, not seen when building these trees
							else {
								type = 5;
								rightValue = new char[rightSideChar.length - 3];
								System.arraycopy(rightSideChar, 3, rightValue, 0, rightSideChar.length - 3);
							}
						}

						// Get and encrypt the threshold for level-site usage
						String threshold_string = new String(rightValue);

						try {
							threshold = Float.parseFloat(threshold_string);
						}
						catch (NumberFormatException e) {
							// Use Label Encoder, only type 1 and 6 though
							threshold = label_encoder.encode(threshold_string).doubleValue();
						}
						temp_thresh = NodeInfo.set_precision(threshold, precision);
						node_info = new NodeInfo(false, leftSide, type);
						node_info.encrypt(temp_thresh, paillier_public, dgk_public);
						q.add(p.getSons()[i]);
					}

					assert node_info != null;
					if (!node_info.is_leaf) {
						NodeInfo additionalNode = null;
						if (node_info.comparisonType == 1) {
							additionalNode = new NodeInfo(false, node_info.getVariableName(), 6);
						}
						else if (node_info.comparisonType == 2) {
							additionalNode = new NodeInfo(false, node_info.getVariableName(), 5);
						}
						else if (node_info.comparisonType == 3) {
							additionalNode = new NodeInfo(false, node_info.getVariableName(), 4);
						}
						else if (node_info.comparisonType == 4) {
							additionalNode = new NodeInfo(false, node_info.getVariableName(), 3);
						}
						else if (node_info.comparisonType == 5) {
							additionalNode = new NodeInfo(false, node_info.getVariableName(), 2);
						}
						else if (node_info.comparisonType == 6) {
							additionalNode = new NodeInfo(false, node_info.getVariableName(), 1);
						}
						assert additionalNode != null;
						additionalNode.encrypt(temp_thresh, paillier_public, dgk_public);
						Level_Order_S.append_data(additionalNode);
					}
					Level_Order_S.append_data(node_info);
				}// else
				n--;
			} // While n > 0 (nodes > 0)
			all_level_sites.add(Level_Order_S);
			++level;
		} // While a tree is not empty
	}

	/**
	 * Executes the main server logic. Trains the decision tree if necessary,
	 * communicates with the client to retrieve public keys, and evaluates the tree
	 * either with or without level sites.
	 */
	public void run() {

		try {
			// Train the DT if you have to.
			if (ppdt == null) {
				ppdt = train_decision_tree(this.training_data);
				// Get Public Keys from Client AND train level-sites
				client_communication();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		// If we are testing without level-sites do this...
		if (this.level_site_ips != null) {
			// If running on a cluster, might as well train be able to run server-site too.
			// If running locally, this.evaluations is set to 1 by default for local testing.
			if (this.evaluations != 1) {
				logger.info("It seems server is being tested in K8s environment!");
				try {
					run_server_site(this.server_port);
				}
				catch (IOException | HomomorphicException | ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			try {
				run_server_site(this.server_port);
			}
			catch (IOException | HomomorphicException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Trains the level sites by sending encrypted data to each site.
	 * Ensures that the number of level sites matches the requirements of the decision tree.
	 * Uses secure communication for data transmission.
	 *
	 * @throws RuntimeException If there are insufficient level sites or an I/O error occurs.
	 */
	private void train_level_sites() {
		ObjectOutputStream to_level_site;
		ValidatingObjectInputStream from_level_site;
		int connection_port;

		// There should be at least 1 IP Address for each level site
        assert this.level_site_ips != null;
        if(this.level_site_ips.length < all_level_sites.size()) {
			String error = String.format("Please create more level-sites for the " +
					"decision tree trained from %s, create %d level-sites", training_data, all_level_sites.size());
			throw new RuntimeException(error);
		}

		// Send the data to each level site, use data in-transit encryption
		// I think it is SAFER if I create level-sites from d and go up, so all accepts are ready...
		for (int i = all_level_sites.size() - 1; i >= 0; i--) {
			level_order_site current_level_site = all_level_sites.get(i);

			if (port == -1) {
				connection_port = this.level_site_ports[i];
			}
			else {
				connection_port = this.port;
			}

			// level-site d
			if (i == all_level_sites.size() - 1) {
				if (port == -1) {
					current_level_site.set_listen_port(level_site_ports[i]);
				}
				else {
					current_level_site.set_listen_port(connection_port);
				}
			}
			else if (i == 0) {
				current_level_site.set_next_level_site(level_site_ips[(i + 1) % level_site_ips.length]);
				if (port == -1) {
					current_level_site.set_next_level_site_port(level_site_ports[(i + 1) % level_site_ports.length]);
				}
				else {
					current_level_site.set_next_level_site_port(connection_port);
				}
			}
			else {
				current_level_site.set_next_level_site(level_site_ips[(i + 1) % level_site_ips.length]);
				if (port == -1) {
					current_level_site.set_next_level_site_port(level_site_ports[(i + 1) % level_site_ports.length]);
					current_level_site.set_listen_port(level_site_ports[i]);
				}
				else {
					current_level_site.set_next_level_site_port(connection_port);
					current_level_site.set_listen_port(connection_port);
				}
			}

			try(Socket level_site = createSocket(level_site_ips[i], connection_port)) {
                logger.info("training level-site {} on port:{}", i, connection_port);
				to_level_site = new ObjectOutputStream(level_site.getOutputStream());
				from_level_site = get_ois(level_site);
				to_level_site.writeObject(current_level_site);
				to_level_site.flush();
				if(from_level_site.readBoolean()) {
                    logger.info("Training Successful on port:{}", connection_port);
				}
				else {
                    logger.error("Training NOT Successful on port:{}", connection_port);
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
