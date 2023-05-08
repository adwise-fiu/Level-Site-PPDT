package weka.finito;

import java.lang.System;

import security.misc.HomomorphicException;
import security.socialistmillionaire.alice;
import weka.finito.structs.BigIntegers;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.level_order_site;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Hashtable;
import java.util.List;

public class level_site_thread implements Runnable {

	private final Socket client_socket;
	private ObjectInputStream fromClient;
	private ObjectOutputStream toClient;

	private level_order_site level_site_data = null;

	private alice Niu = null;

	private Hashtable<String, BigIntegers> encrypted_features;
	private final AES crypto;

	public level_site_thread(Socket client_socket, level_order_site level_site_data, AES crypto) {
		this.client_socket = client_socket;
		this.crypto = crypto;

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
				encrypted_features = (Hashtable<String, BigIntegers>) x;
				// Have encrypted copy of thresholds if not done already for all nodes in level-site
				this.level_site_data = level_site_data;
			} else {
				System.out.println("Wrong Object Received: " + x.getClass().toString());
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

	// a - from CLIENT, should already be encrypted...
	private boolean compare(NodeInfo ld, int comparisonType)
			throws HomomorphicException, ClassNotFoundException, IOException {

        long start_time = System.nanoTime();

		BigIntegers encrypted_values = this.encrypted_features.get(ld.variable_name);
		BigInteger encrypted_client_value = null;
		BigInteger encrypted_thresh = null;

        // Encrypt the thresh-hold correctly
        if ((comparisonType == 1) || (comparisonType == 2) || (comparisonType == 4)) {
			encrypted_thresh = ld.getPaillier();
			encrypted_client_value = encrypted_values.getIntegerValuePaillier();
            toClient.writeInt(0);
            Niu.setDGKMode(false);
        }
        else if ((comparisonType == 3) || (comparisonType == 5)) {
			encrypted_thresh = ld.getDGK();
			encrypted_client_value = encrypted_values.getIntegerValueDGK();
            toClient.writeInt(1);
            Niu.setDGKMode(true);
        }
        toClient.flush();
		assert encrypted_client_value != null;
        long stop_time = System.nanoTime();

		double run_time = (double) (stop_time - start_time);
		run_time = run_time / 1000000;
		System.out.printf("Comparison took %f ms\n", run_time);
		if (((comparisonType == 1) && (ld.threshold == 0))
				|| (comparisonType == 4) || (comparisonType == 5)) {
			return Niu.Protocol4(encrypted_thresh, encrypted_client_value);
        }
        else {
			return Niu.Protocol4(encrypted_client_value, encrypted_thresh);
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
			Niu = new alice(client_socket);
			if (this.level_site_data == null) {
				toClient.writeInt(-2);
				closeClientConnection();
				return;
			}

			List<NodeInfo> node_level_data = this.level_site_data.get_node_data();
			Niu.setDGKPublicKey(this.level_site_data.dgk_public_key);
			Niu.setPaillierPublicKey(this.level_site_data.paillier_public_key);

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
			if (previous_index != null) {
				this.level_site_data.set_current_index(Integer.parseInt(previous_index));
			}

			boolean equalsFound = false;
			boolean inequalityHolds = false;
			boolean terminalLeafFound = false;
			int node_level_index = 0;
			int n = 0;
			int next_index = 0;
			NodeInfo ls = null;
			String encrypted_next_index;

			while ((!equalsFound) && (!terminalLeafFound)) {
				ls = node_level_data.get(node_level_index);
				System.out.println("j=" + node_level_index);
				if (ls.isLeaf()) {
					if (n == 2 * this.level_site_data.get_current_index() || n == 2 * this.level_site_data.get_current_index() + 1) {
						terminalLeafFound = true;
						System.out.println("Terminal leaf:" + ls.getVariableName());
					}
					n += 2;
				}
				else {
					if ((n==2 * this.level_site_data.get_current_index() || n == 2 * this.level_site_data.get_current_index() + 1)) {
						if (ls.comparisonType == 6) {
							boolean firstInequalityHolds = compare(ls, 3);
							if (firstInequalityHolds) {
								inequalityHolds = true;
							} else {
								boolean secondInequalityHolds = compare(ls, 5);
								if (secondInequalityHolds) {
									inequalityHolds = true;
								}
							}
						} else {
							inequalityHolds = compare(ls, ls.comparisonType);
						}

						if (inequalityHolds) {
							equalsFound = true;
							this.level_site_data.set_next_index(next_index);
							System.out.println("New index:" + this.level_site_data.get_current_index());
						}
					}
					n++;
					next_index++;
				}
				node_level_index++;
			}

			// Place -1 to break Protocol4 loop
			toClient.writeInt(-1);
			toClient.flush();

			if (terminalLeafFound) {
				// Tell the client the value
				toClient.writeBoolean(true);
				toClient.writeObject(ls.getVariableName());
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
			System.out.printf("Total run-time took %f ms\n", run_time);
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
