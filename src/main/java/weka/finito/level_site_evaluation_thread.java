package weka.finito;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.System;

import security.misc.HomomorphicException;
import security.socialistmillionaire.alice_joye;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.features;
import weka.finito.structs.level_order_site;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static weka.finito.utils.shared.*;

public class level_site_evaluation_thread implements Runnable {
	private static final Logger logger = LogManager.getLogger(level_site_evaluation_thread.class);
	private static final SSLSocketFactory socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
	protected static SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
	private SSLSocket client_socket;
	private SSLSocket next_level_site_socket;
	private SSLSocket previous_level_site_socket;
	private final level_order_site level_site_data;
	private features encrypted_features;
	private ObjectOutputStream next_level_site;
	private ObjectInputStream previous_site;
	private SSLServerSocket previous_level_site_listener;

	// This thread is only for level-site 0
	public level_site_evaluation_thread(SSLSocket client_socket, level_order_site level_site_data,
										features encrypted_features, ObjectOutputStream next_level_site) {
		// Have encrypted copy of thresholds if not done already for all nodes in level-site
		this.client_socket = client_socket;
		this.level_site_data = level_site_data;
		this.encrypted_features = encrypted_features;
		this.next_level_site = next_level_site;
	}

	// For all other levels
	public level_site_evaluation_thread(level_order_site level_site_data) throws IOException {
		this.level_site_data = level_site_data;
		init();
	}

	protected void init() throws IOException {
		if (level_site_data.get_level() != 0) {
			previous_level_site_listener = createServerSocket(this.level_site_data.get_listen_port());
			previous_level_site_socket = (SSLSocket) previous_level_site_listener.accept();
			previous_level_site_socket.setKeepAlive(true);
			previous_site = get_ois(previous_level_site_socket);
		}

		// All the level-sites should do this step though, except level-site d
		// Create a persistent connection to next level-site and oos to send the next stuff down
		if (level_site_data.get_next_level_site() != null) {
			next_level_site_socket = createSocket(level_site_data.get_next_level_site(),
					level_site_data.get_next_level_site_port());
			next_level_site_socket.setKeepAlive(true);
			next_level_site = new ObjectOutputStream(next_level_site_socket.getOutputStream());
		}
	}

	public final void evaluate() throws IOException, HomomorphicException, ClassNotFoundException {
		long start_time = System.nanoTime();
		alice_joye niu = new alice_joye();

		niu.set_socket(client_socket);
		niu.setDGKPublicKey(this.level_site_data.dgk_public_key);
		niu.setPaillierPublicKey(this.level_site_data.paillier_public_key);

		// Null, keep going down the tree,
		// Not null, you got the correct leaf node of your DT!
		// encrypted_features will have index updated within traverse_level
		NodeInfo reply = traverse_level(level_site_data, encrypted_features, niu);
		niu.writeInt(-1);

		if (reply != null) {
			// Tell the client the value
			niu.writeBoolean(true);
			niu.writeObject(reply.getVariableName());
		}
		else {
			niu.writeBoolean(false);
			next_level_site.writeObject(encrypted_features);
			next_level_site.flush();
		}
		long stop_time = System.nanoTime();
		double run_time = (double) (stop_time - start_time);
		run_time = run_time / 1000000;
		logger.info(String.format("Total Level-Site run-time took %f ms\n", run_time));
	}

	// This will run the communication with client and next level site
	public final void run() {
		logger.info("Showing level-site");
		logger.info(level_site_data.toString());
		Object o;

        try {
			if (level_site_data.get_level() != 0) {
				evaluate();
				/*

				// Loop read object and evaluate. On interrupt, close server socket
				while (!Thread.interrupted()) {
					// Previous level-site sends data for comparison
					o = previous_site.readObject();
					if (o instanceof features) {
						encrypted_features = (features) o;
					} else {
						throw new RuntimeException("I received an object that should be features!");
					}
					// Create connection to the client
					client_socket = createSocket(
							encrypted_features.get_client_ip(),
							encrypted_features.get_client_port());
					evaluate();
				}
				// Close everything
				closeConnection(previous_level_site_listener);
				closeConnection(previous_level_site_socket);
				closeConnection(next_level_site_socket);
				 */
			}
			else {
				evaluate();
			}
		}
        catch (Exception e) {
			logger.error("Exception found", e);
		}
		finally {
			try {
				closeConnection(client_socket);
			}
			catch (IOException e) {
				logger.info("IO Exception in closing Level-Site Connection in Evaluation", e);
			}
		}
	}

	public static SSLServerSocket createServerSocket(int serverPort) {
		SSLServerSocket serverSocket;
		try {
			// Step: 1
			serverSocket = (SSLServerSocket) factory.createServerSocket(serverPort);
			serverSocket.setEnabledProtocols(protocols);
			serverSocket.setEnabledCipherSuites(cipher_suites);
		}
		catch (IOException e) {
			throw new RuntimeException("Cannot open port " + serverPort, e);
		}
		return serverSocket;
	}

	public static SSLSocket createSocket(String hostname, int port) {
		SSLSocket client_socket;
		try {
			// Step: 1
			client_socket = (SSLSocket) socket_factory.createSocket(hostname, port);
			client_socket.setEnabledProtocols(protocols);
			client_socket.setEnabledCipherSuites(cipher_suites);
		}
		catch (IOException e) {
			throw new RuntimeException("Cannot open port " + port, e);
		}
		return client_socket;
	}
}
