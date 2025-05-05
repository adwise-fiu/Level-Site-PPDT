package edu.fiu.adwise.weka.finito;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.System;

import edu.fiu.adwise.homomorphic_encryption.misc.HomomorphicException;
import edu.fiu.adwise.homomorphic_encryption.socialistmillionaire.alice_joye;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.fiu.adwise.weka.finito.structs.NodeInfo;
import edu.fiu.adwise.weka.finito.structs.level_order_site;
import edu.fiu.adwise.weka.finito.structs.features;
import static edu.fiu.adwise.weka.finito.client.createSocket;
import static edu.fiu.adwise.weka.finito.utils.shared.*;

/**
 * Represents a thread for evaluating a level site in a distributed decision tree system.
 * Handles communication with the client and other level sites, processes encrypted features,
 * and evaluates decision tree nodes.
 */
public class level_site_evaluation_thread implements Runnable {
	/** Logger for logging evaluation thread activities. */
	private static final Logger logger = LogManager.getLogger(level_site_evaluation_thread.class);

	/** Socket connection to the client. */
	private Socket client_socket;

	/** Socket connection to the next level site. */
	private Socket next_level_site_socket;

	/** Socket connection to the previous level site. */
	private Socket previous_level_site_socket;

	/** Data and configuration for the current level site. */
	private final level_order_site level_site_data;

	/** Encrypted features received for evaluation. */
	private features encrypted_features;

	/** Output stream for sending data to the next level site. */
	private ObjectOutputStream next_level_site = null;

	/** Input stream for receiving data from the previous level site. */
	private ObjectInputStream previous_site = null;

	/** Listener for connections from the previous level site. */
	private ServerSocket previous_level_site_listener;

	/**
	 * Constructor for level-site 0.
	 * Initializes the thread with client socket, level site data, encrypted features, and next level site output stream.
	 *
	 * @param client_socket The socket connection to the client.
	 * @param level_site_data The data and configuration for the current level site.
	 * @param encrypted_features The encrypted features received for evaluation.
	 * @param next_level_site The output stream to the next level site.
	 */
	public level_site_evaluation_thread(Socket client_socket, level_order_site level_site_data,
										features encrypted_features, ObjectOutputStream next_level_site) {
		this.client_socket = client_socket;
		this.level_site_data = level_site_data;
		this.encrypted_features = encrypted_features;
		this.next_level_site = next_level_site;
	}

	/**
	 * Constructor for all other levels.
	 * Initializes the thread with level site data and a listener for the previous level site.
	 *
	 * @param level_site_data The data and configuration for the current level site.
	 * @param previous_level_site_listener The listener for connections from the previous level site.
	 * @throws IOException If an I/O error occurs while initializing the listener.
	 */
	public level_site_evaluation_thread(level_order_site level_site_data,
										ServerSocket previous_level_site_listener) throws IOException {
		this.level_site_data = level_site_data;
		this.previous_level_site_listener = previous_level_site_listener;
	}

	/**
	 * Initializes connections for the evaluation thread.
	 * Sets up connections to the previous and next level sites if not already established.
	 *
	 * @throws IOException If an I/O error occurs while setting up connections.
	 */
	protected void init() throws IOException {
		// All level-sites except 0 (already checked in constructor) must do this.
		if (previous_site == null) {
			logger.debug("Evaluation thread will now do the listening");
			if (previous_level_site_listener != null) {
				previous_level_site_socket = previous_level_site_listener.accept();
				previous_level_site_socket.setKeepAlive(true);
				previous_site = get_ois(previous_level_site_socket);
			}
		} else {
			logger.debug("Connection to previous level-site already exists!");
		}

		// All the level-sites should do this step, though, except level-site d
		// Create a persistent connection to the next level-site and oos to send the next stuff down
		if (next_level_site == null) {
			if (level_site_data.get_next_level_site() != null) {
				next_level_site_socket = createSocket(level_site_data.get_next_level_site(),
						level_site_data.get_next_level_site_port());
				next_level_site_socket.setKeepAlive(true);
				next_level_site = new ObjectOutputStream(next_level_site_socket.getOutputStream());
			}
		} else {
			logger.debug("Connection to the next level-site already exists");
		}
	}

	/**
	 * Evaluates the decision tree node for the current level site.
	 * Communicates with the client and forwards data to the next level site if necessary.
	 *
	 * @throws IOException If an I/O error occurs during communication.
	 * @throws HomomorphicException If an error occurs during homomorphic encryption operations.
	 * @throws ClassNotFoundException If a class required for deserialization is not found.
	 */
	private void evaluate() throws IOException, HomomorphicException, ClassNotFoundException {
		long start_time = System.nanoTime();
		alice_joye niu = new alice_joye();

		niu.set_socket(client_socket);
		niu.setDGKPublicKey(this.level_site_data.dgk_public_key);
		niu.setPaillierPublicKey(this.level_site_data.paillier_public_key);

		NodeInfo reply = traverse_level(level_site_data, encrypted_features, niu);
		niu.writeInt(-1);

		// Null, keep going down the tree,
		// Not null, you got the correct leaf node of your DT!
		// encrypted_features will have an index updated within traverse_level
		if (reply != null) {
			// Tell the client the classification
			niu.writeBoolean(true);
			niu.writeObject(reply.getVariableName());
		} else {
			niu.writeBoolean(false);
			next_level_site.writeObject(encrypted_features);
			next_level_site.flush();
		}
		long stop_time = System.nanoTime();
		double run_time = (double) (stop_time - start_time) / 1000000;
		logger.info(String.format("Total Level-Site run-time took %f ms\n", run_time));
	}

	/**
	 * Runs the evaluation thread.
	 * Handles communication with the client and other level sites, processes encrypted features,
	 * and evaluates decision tree nodes.
	 */
	public final void run() {
		logger.debug("Showing level-site");
		logger.debug(level_site_data.toString());
		Object o;

		try {
			init();
			if (level_site_data.get_level() != 0) {
				logger.info("Level-site {} is now waiting for evaluations", level_site_data.get_level());
				while (!Thread.interrupted()) {
					o = previous_site.readObject();
					logger.info("Level-site {} got an object", level_site_data.get_level());
					if (o instanceof features) {
						encrypted_features = (features) o;
					} else {
						throw new RuntimeException("Level-site " + level_site_data.get_level()
								+ "received an object that should be features!");
					}
					logger.info("Level-site {} got encrypted features!", level_site_data.get_level());
					client_socket = createSocket(
							encrypted_features.get_client_ip(),
							encrypted_features.get_client_port());
					logger.info("Level-site {} connected to the client: {}:{}", level_site_data.get_level(),
							encrypted_features.get_client_ip(), encrypted_features.get_client_port());
					evaluate();
				}
				closeConnection(previous_level_site_listener);
				closeConnection(previous_level_site_socket);
				closeConnection(next_level_site_socket);
			} else {
				// I already got client socket and features, so evaluate the thread now and close
				evaluate();
			}
		} catch (EOFException e) {
			// This is fine, will occur when stuck on readObject() and interrupted.
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				closeConnection(client_socket);
			} catch (IOException e) {
				logger.info("IO Exception in closing Level-Site Connection in Evaluation", e);
			}
		}
	}
}
