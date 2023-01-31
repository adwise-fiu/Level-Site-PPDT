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
	
	private level_order_site level_site_data;
	
	private DGKPublicKey dgk_public_key;
	private PaillierPublicKey paillier_public_key;
	
	private alice Niu = null;
	private int precision;
	private Hashtable<String, BigIntegers> encrypted_features;
	
	public level_site_thread(Socket client_socket, level_order_site level_site_data, int precision) {
		this.client_socket = client_socket;
		this.precision = precision;
		
		try {
			fromClient = new ObjectInputStream(client_socket.getInputStream());
			toClient = new ObjectOutputStream(client_socket.getOutputStream());
			
			Object x = fromClient.readObject();
			if (x instanceof level_order_site) {
				this.level_site_data = (level_order_site) x;
				closeClientConnection();
			}
			else if (x instanceof Hashtable){
				Niu = new alice(client_socket);
				dgk_public_key = Niu.getDGKPublicKey();
				paillier_public_key = Niu.getPaillierPublicKey();
				
				encrypted_features = (Hashtable<String, BigIntegers>) x;
				// Have encrypted copy of thresholds if not done already for all nodes in level-site
				if (level_site_data != null) {
					this.level_site_data = level_site_data;
				}
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
        
        if (ld.threshold == 0) {
        	 return Niu.Protocol4(encrypted_thresh, encrypted_client_value);
        }
        else {
        	 return Niu.Protocol4(encrypted_client_value, encrypted_thresh);
        }  
	}
	
	// This will run the communication with client and next level site
	public void run() {
		try {
			int i = this.level_site_data.getLevel();
			System.out.println("i=" + i);
			List<NodeInfo> node_level_data = this.level_site_data.get_node_data();
			
			// Level Data is the Node Data...
			int bound = 0;
			if (i == 0) {
				this.level_site_data.set_current_index(0);
				bound = 2;
			} 
			else {
				bound = node_level_data.size();
			}

		    boolean equalsFound = false;
		    boolean inequalityHolds = false;
		    boolean terminalLeafFound = false;
			int node_level_index = 0;
			int n = 0;
			int next_index = 0;

			while (node_level_index < bound && (!equalsFound) && (!terminalLeafFound)) {
				NodeInfo ls = node_level_data.get(node_level_index);
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
							ls.comparisonType = 5;
							boolean secondInequalityHolds = compare(ls);
							if (firstInequalityHolds || secondInequalityHolds) {
								inequalityHolds = true;
							}
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
			
			// TODO: Encrypt and send to client with shared AES Key of Level-sites
			level_site_data.get_next_index();
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
