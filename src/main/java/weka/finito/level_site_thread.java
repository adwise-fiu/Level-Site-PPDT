package weka.finito;

import java.lang.System;

import security.socialistmillionaire.alice;
import weka.finito.structs.BigIntegers;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.level_order_site;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static weka.finito.utils.shared.compare;
import static weka.finito.utils.shared.traverse_level;

public class level_site_thread implements Runnable {

	private final Socket client_socket;
	private ObjectInputStream fromClient;
	private ObjectOutputStream toClient;

	private level_order_site level_site_data = null;

	private final Hashtable<String, BigIntegers> encrypted_features = new Hashtable<>();
	private final AES crypto;

	public level_site_thread(Socket client_socket, level_order_site level_site_data, AES crypto) {
		this.client_socket = client_socket;
		this.crypto = crypto;
		String clientIpAddress = client_socket.getInetAddress().getHostAddress();
		System.out.println("Level-Site got connection from client: " + clientIpAddress);

		Object x;
		try {
			toClient = new ObjectOutputStream(this.client_socket.getOutputStream());
			fromClient = new ObjectInputStream(this.client_socket.getInputStream());

			x = fromClient.readObject();
			if (x instanceof level_order_site) {
				// Traffic from Server. Level-Site alone will manage closing this.
				this.level_site_data = (level_order_site) x;
				// System.out.println("Level-Site received training data on Port: " + client_socket.getLocalPort());
				this.toClient.writeBoolean(true);
				closeClientConnection();
			} else if (x instanceof Hashtable) {
				for (Map.Entry<?, ?> entry: ((Hashtable<?, ?>) x).entrySet()){
					if (entry.getKey() instanceof String && entry.getValue() instanceof BigIntegers) {
						encrypted_features.put((String) entry.getKey(), (BigIntegers) entry.getValue());
					}
				}
				// Have encrypted copy of thresholds if not done already for all nodes in level-site
				this.level_site_data = level_site_data;
			} else {
				System.out.println("Wrong Object Received: " + x.getClass());
				closeClientConnection();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final level_order_site getLevelSiteParameters() {
		return this.level_site_data;
	}

	private void closeClientConnection() throws IOException {
		toClient.close();
		fromClient.close();
		if (this.client_socket != null && this.client_socket.isConnected()) {
			this.client_socket.close();
		}
	}

	// This will run the communication with client and next level site
	public final void run() {
		Object o;
		String previous_index = null;
		String iv = null;
		boolean get_previous_index;
		long start_time = System.nanoTime();

		try {
			alice niu = new alice(client_socket);
			if (this.level_site_data == null) {
				toClient.writeInt(-2);
				closeClientConnection();
				return;
			}

			niu.setDGKPublicKey(this.level_site_data.dgk_public_key);
			niu.setPaillierPublicKey(this.level_site_data.paillier_public_key);

			get_previous_index = fromClient.readBoolean();
			if (get_previous_index) {
				o = fromClient.readObject();
				if (o instanceof String) {
					previous_index = (String) o;
				}
				o = fromClient.readObject();
				if (o instanceof String) {
					iv = (String) o;
				}
				previous_index = crypto.decrypt(previous_index, iv);
			}

			// Level Data is the Node Data...
			// it is set to 0 by default...
			if (previous_index != null) {
				this.level_site_data.set_current_index(Integer.parseInt(previous_index));
			}

			// Null, keep going down the tree,
			// Not-null, you got the correct leaf node of your DT!
			NodeInfo reply = traverse_level(level_site_data, encrypted_features, toClient, niu);

			String encrypted_next_index;

			// Place -1 to break Protocol4 loop
			toClient.writeInt(-1);
			toClient.flush();

			if (reply != null) {
				// Tell the client the value
				toClient.writeBoolean(true);
				toClient.writeObject(reply.getVariableName());
			}
			else {
				toClient.writeBoolean(false);
				// encrypt with AES, send to client which will send to next level-site
				encrypted_next_index = crypto.encrypt(String.valueOf(this.level_site_data.get_next_index()));
				iv = crypto.getIV();
				toClient.writeObject(encrypted_next_index);
				toClient.writeObject(iv);
			}
			long stop_time = System.nanoTime();
			double run_time = (double) (stop_time - start_time);
			run_time = run_time / 1000000;
			System.out.printf("Total Level-Site run-time took %f ms\n", run_time);
		}
        catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				closeClientConnection();
			} catch (IOException e) {
				System.out.println("IO Exception in closing Level-Site Connection in Evaluation");
			}
		}
	}
}
