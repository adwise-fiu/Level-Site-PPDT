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
	private Hashtable<String, BigIntegers> encrypted_features;
	private int precision;
	
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
				
				// Have encrypted copy of thresholds if not done already for all nodes in level-site
				if (level_site_data != null) {
					this.level_site_data = level_site_data;
				}
				this.encrypted_features = (Hashtable<String, BigIntegers>) x;
			}
			else {
				return;
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
	// https://github.com/spyrosthalkidis/Weka_Code_Finito/blob/main/Weka/weka-trunk-master/weka/src/main/java/weka/classifiers/trees/j48/OfflineAuction.java
	private boolean compare(NodeInfo ld) 
			throws HomomorphicException, ClassNotFoundException, IOException {

		// Obtain the thresh-hold values
        double secondDouble = ld.threshold;
        BigInteger secondInt = new BigInteger(String.valueOf(ld.threshold));
        BigInteger plain_b;
        BigInteger b = null;
        BigIntegers encrypted_client_values = encrypted_features.get(ld.variable_name);
        BigInteger encrypted_client_value = null;
        
        int threshold = (int) ld.threshold;
        if (ld.comparisonType != 1) {
            //double newDouble2 = (secondDouble * 10.0 * Math.pow(10, precision));
            //secondInt = (long) newDouble2;
            //secondInt = secondInt/10;
            plain_b = secondInt;
        }
        else {
            plain_b = new BigInteger("" + threshold);
        }
        System.out.println("Comparison type:" + ld.comparisonType);
        System.out.println("b:" + plain_b);
        
        // Encrypt the value for the value in Decision Tree
        if ((ld.comparisonType == 1) || (ld.comparisonType == 2) || (ld.comparisonType == 4)) {
        	encrypted_client_value = encrypted_client_values.getIntegerValuePaillier();
        	
        	b = PaillierCipher.encrypt(plain_b, this.paillier_public_key);
            toClient.writeInt(0);
            Niu.setDGKMode(false);
        }
        else if ((ld.comparisonType == 3) || (ld.comparisonType == 5)) {
        	encrypted_client_value = encrypted_client_values.getIntegerValueDGK();
        	
        	b = DGKOperations.encrypt(plain_b, this.dgk_public_key);
            toClient.writeInt(1);
            Niu.setDGKMode(true);
        }
        
        // Get the value from the client you will compare against
        if (threshold == 0) {
            return Niu.Protocol4(b, encrypted_client_value);
        }
        else {
        	return Niu.Protocol4(encrypted_client_value, b);
        }
	}
	
	// This will run the communication with client and current level site
	// https://github.com/spyrosthalkidis/Weka_Code_Finito/blob/main/Weka/weka-trunk-master/weka/src/main/java/weka/classifiers/trees/j48/ClassifierTree.java#L764-L822
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

			NodeInfo ls = null;
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
					if ((i == 0)||((n == 2 * this.level_site_data.get_current_index() || n == 2 * this.level_site_data.get_current_index() + 1))) {
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
							
							//TransmitValueSecurely.transmit_value_securely(Level_Nodes.get(i), Level_Nodes.get(i + 1));
							System.out.println("New index:" + this.level_site_data.get_current_index());
						}
					}
					n++;
					next_index++;
					node_level_index++;
					System.out.println("Variable Name:" + ls.getVariableName() + " " + ls.comparisonType + ", " + ls.threshold);
				}
			}
			
			// Place -1 to break Protocol4 loop at the client...
			toClient.writeInt(-1);
			
			// TODO: Encrypt and send to client with shared AES Key of Level-sites
			if (terminalLeafFound) {
				// Let the Client know you have the classification, return that!
				toClient.writeObject(ls.getVariableName());
			}
			else {
				// Give the client the AES encrypted index
				// Note that ONLY the level sites have the AES Key
				// Question is, why does the next level site need the next_index? do I update line 154 with the index?
				level_site_data.get_next_index();
			}
			
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
