package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Hashtable;
import java.util.List;

import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierPublicKey;
import security.socialistmillionaire.alice;

public class level_site_thread implements Runnable {
	
	private Socket client_socket;
	private ObjectInputStream fromClient;
	private ObjectOutputStream toClient;
	
	private level_order_site level_site_data = null;
	
	private DGKPublicKey dgk_public_key;
	private PaillierPublicKey paillier_public_key;
	
	private alice Niu = null;
	private int precision;
	private Hashtable<String, BigIntegers> encrypted_features;
	private AES crypto = null;
	
	public level_site_thread(Socket client_socket, level_order_site level_site_data, int precision, AES crypto) {
		this.client_socket = client_socket;
		this.precision = precision;
		this.crypto = crypto;
		
		try {
			toClient = new ObjectOutputStream(client_socket.getOutputStream());
			fromClient = new ObjectInputStream(client_socket.getInputStream());
			
			Object x = fromClient.readObject();
			if (x instanceof level_order_site) {
				// Traffic from Server. Level-Site alone will manage closing this.
				this.level_site_data = (level_order_site) x;
				System.out.println("Level-Site received listening on Port: " + client_socket.getLocalPort());
				System.out.println(this.level_site_data.toString());
				closeClientConnection();
			}
			else if (x instanceof Hashtable){
				System.out.println("Received Features from Client");
				encrypted_features = (Hashtable<String, BigIntegers>) x;
				// Have encrypted copy of thresholds if not done already for all nodes in level-site
				if (level_site_data != null) {
					this.level_site_data = level_site_data;
				}
			}
			else {
				System.out.println("Wrong Object Received: " + x.getClass().toString());
				closeClientConnection();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public level_order_site getLevelSiteParameters() {
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
	private boolean compare(NodeInfo ld) 
			throws HomomorphicException, ClassNotFoundException, IOException {

		BigIntegers encrypted_values = this.encrypted_features.get(ld.variable_name);
		BigInteger encrypted_client_value = null;
		
		// Convert threshold into BigInteger. Note we know threshold is always a float.
		BigInteger encrypted_thresh = null;
		String value = String.valueOf(ld.threshold);
		try {
			encrypted_thresh = new BigInteger(value);
		}
		catch (NumberFormatException e) {
			int intermediateInteger = (int) ld.threshold * (int)Math.pow(10, precision);
			encrypted_thresh = BigInteger.valueOf(intermediateInteger);
		}

        System.out.println("Comparison type: " + ld.comparisonType);
        System.out.println("plain-text value: " + encrypted_thresh);
        
        // Encrypt the thresh-hold correctly
        if ((ld.comparisonType == 1) || (ld.comparisonType == 2) || (ld.comparisonType == 4)) {
        	encrypted_thresh = PaillierCipher.encrypt(encrypted_thresh, this.paillier_public_key);
        	encrypted_client_value = encrypted_values.getIntegerValuePaillier();
            toClient.writeInt(0);
            Niu.setDGKMode(false);
        }
        else if ((ld.comparisonType == 3) || (ld.comparisonType == 5)) {
        	encrypted_thresh = DGKOperations.encrypt(encrypted_thresh, this.dgk_public_key);
        	encrypted_client_value = encrypted_values.getIntegerValueDGK();
            toClient.writeInt(1);
            Niu.setDGKMode(true);
        }
        toClient.flush();
        
        if (ld.threshold == 0) {
        	 return Niu.Protocol4(encrypted_thresh, encrypted_client_value);
        }
        else {
        	 return Niu.Protocol4(encrypted_client_value, encrypted_thresh);
        }
	}
	
	// This will run the communication with client and next level site
	public void run() {
		Object o;
		String previous_index = null;
		boolean get_previous_index = false;
		
		try {
			Niu = new alice(client_socket);
			dgk_public_key = Niu.getDGKPublicKey();
			paillier_public_key = Niu.getPaillierPublicKey();
			
			int i = this.level_site_data.getLevel();
			System.out.println("level= " + i);
			List<NodeInfo> node_level_data = this.level_site_data.get_node_data();
			
			get_previous_index = fromClient.readBoolean();
			if (get_previous_index) {
				o = fromClient.readObject();
				if (o instanceof String) {
					previous_index = (String) o;
				}
				previous_index = crypto.decrypt(previous_index);
			}
			
			// Level Data is the Node Data...
			int bound = 0;
			if (i == 0) {
				this.level_site_data.set_current_index(0);
				bound = 2;
			}
			else {
				this.level_site_data.set_current_index(Integer.parseInt(previous_index));
				bound = node_level_data.size();
			}

		    boolean equalsFound = false;
		    boolean inequalityHolds = false;
		    boolean terminalLeafFound = false;
			int node_level_index = 0;
			int n = 0;
			int next_index = 0;
			NodeInfo ls = null;
			String encrypted_next_index = null;
			
			while (node_level_index < bound && (!equalsFound) && (!terminalLeafFound)) {
				ls = node_level_data.get(node_level_index);
				System.out.println("j=" + node_level_index);
				if (ls.isLeaf()) {
					if (n == 2 * this.level_site_data.get_current_index() || n == 2 * this.level_site_data.get_current_index() + 1) {
						terminalLeafFound = true;
						System.out.println("Terminal leaf:" + ls.getVariableName());
					}
					node_level_index++;
					n += 2;
					System.out.println("Variable:" + ls.getVariableName());
				}
				else {
					if ((i==0)||((n==2 * this.level_site_data.get_current_index() || n == 2 * this.level_site_data.get_current_index() + 1))) {
						if (ls.comparisonType == 6) {
							ls.comparisonType = 3;
							boolean firstInequalityHolds = compare(ls);
							if (firstInequalityHolds) {
								inequalityHolds = true;
							}
							else {
								ls.comparisonType = 5;
								boolean secondInequalityHolds = compare(ls);
								if (secondInequalityHolds) {
									inequalityHolds = true;
								}
							}
							ls.comparisonType = 6;
						}
						else {
							inequalityHolds = compare(ls);
						}

						System.out.println("Inequality Holds:" + inequalityHolds);
						System.out.println("n:" + n + " first node index:" + 2 * this.level_site_data.get_current_index() + " second node index:" + (2 * this.level_site_data.get_current_index() + 1));
						if ((inequalityHolds) && ((n == 2 * this.level_site_data.get_current_index() || n == 2 * this.level_site_data.get_current_index() + 1))) {
							equalsFound = true;	
							this.level_site_data.set_next_index(next_index);
							System.out.println("New index:" + this.level_site_data.get_current_index());
						}
					}
					n++;
					next_index++;
					node_level_index++;
					System.out.println("Variable Name:" + ls.getVariableName() + " " + ls.comparisonType + ", " + ls.threshold);
				}
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
				encrypted_next_index = crypto.encrypt(level_site_data.get_next_index() + "");
				toClient.writeObject(encrypted_next_index);
			}
			closeClientConnection();
		}
        catch (IOException e) {
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		catch (HomomorphicException e) {
			e.printStackTrace();
		}
		finally {
			try {
				closeClientConnection();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
