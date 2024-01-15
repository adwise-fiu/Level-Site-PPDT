package weka.finito;

import java.lang.System;

import security.socialistmillionaire.alice_joye;
import weka.finito.structs.BigIntegers;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.level_order_site;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static weka.finito.utils.shared.*;

public class level_site_evaluation_thread implements Runnable {

	private final Socket client_socket;
	private final level_order_site level_site_data;
	private final HashMap<String, BigIntegers> encrypted_features = new HashMap<>();

	// This thread is ONLY to handle evaluations
	public level_site_evaluation_thread(Socket client_socket, level_order_site level_site_data, HashMap x) {
		// Have encrypted copy of thresholds if not done already for all nodes in level-site
		this.level_site_data = level_site_data;
		this.client_socket = client_socket;

		for (Map.Entry<?, ?> entry: ((HashMap<?, ?>) x).entrySet()) {
			if (entry.getKey() instanceof String && entry.getValue() instanceof BigIntegers) {
				encrypted_features.put((String) entry.getKey(), (BigIntegers) entry.getValue());
			}
		}
	}

	// This will run the communication with client and next level site
	public final void run() {
		long start_time = System.nanoTime();
		alice_joye niu = new alice_joye();
        ObjectInputStream ois = null;
		ObjectOutputStream oos = null;
        try {
			niu.set_socket(client_socket);
			niu.setDGKPublicKey(this.level_site_data.dgk_public_key);
			niu.setPaillierPublicKey(this.level_site_data.paillier_public_key);

			level_site_data.set_current_index(niu.readInt());

            // Null, keep going down the tree,
			// Not null, you got the correct leaf node of your DT!
			NodeInfo reply = traverse_level(level_site_data, encrypted_features, niu);
			niu.writeInt(-1);

			if (reply != null) {
				// Tell the client the value
				niu.writeBoolean(true);
				niu.writeObject(reply.getVariableName());
			}
			else {
				niu.writeBoolean(false);
				niu.writeInt(level_site_data.get_next_index());
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
				closeConnection(oos, ois, client_socket);
			} catch (IOException e) {
				System.out.println("IO Exception in closing Level-Site Connection in Evaluation");
			}
		}
	}
}
