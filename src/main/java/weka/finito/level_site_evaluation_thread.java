package weka.finito;

import java.io.ObjectOutputStream;
import java.lang.System;

import security.socialistmillionaire.alice_joye;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.features;
import weka.finito.structs.level_order_site;

import java.io.IOException;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static weka.finito.utils.shared.*;

public class level_site_evaluation_thread implements Runnable {
	private static final Logger logger = LogManager.getLogger(level_site_evaluation_thread.class);
	private final Socket client_socket;
	private final level_order_site level_site_data;
	private final features encrypted_features;
	private final ObjectOutputStream oos;

	// This thread is ONLY to handle evaluations
	public level_site_evaluation_thread(Socket client_socket, level_order_site level_site_data,
										features encrypted_features, ObjectOutputStream oos) {
		// Have encrypted copy of thresholds if not done already for all nodes in level-site
		this.client_socket = client_socket;
		this.level_site_data = level_site_data;
		this.encrypted_features = encrypted_features;
		this.oos = oos;
	}

	// This will run the communication with client and next level site
	public final void run() {
		long start_time = System.nanoTime();
		alice_joye niu = new alice_joye();
        try {
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
				oos.writeObject(encrypted_features);
				oos.flush();
			}
			long stop_time = System.nanoTime();
			double run_time = (double) (stop_time - start_time);
			run_time = run_time / 1000000;
			logger.info(String.format("Total Level-Site run-time took %f ms\n", run_time));
		}
        catch (Exception e) {
			logger.error(e.getStackTrace());
		}
		finally {
			try {
				closeConnection(client_socket);
			} catch (IOException e) {
				logger.info("IO Exception in closing Level-Site Connection in Evaluation");
			}
		}
	}
}
