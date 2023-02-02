package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierPublicKey;

/**
 * @author Andrew Quijano
 * This class contains all the necessary information for a Level-site to complete evaluation
 */

public class level_order_site implements Serializable {
	
	private static final long serialVersionUID = 575566807906351024L;

	private int index = 0;
    private int next_index;
    private int level = 0;
    
    private List<NodeInfo> node_level_data = new ArrayList<NodeInfo>();
    
    public List<NodeInfo> get_node_data() {
    	return this.node_level_data;
    }
    
    public int getLevel() {
    	return this.level;
    }
    
    public void set_current_index(int index) {
    	this.index = index;
    }
    
    public void set_next_index(int next_index) {
    	this.next_index = next_index;
    }
    
    public int get_next_index() {
    	return this.next_index;
    }
    
    public int get_current_index() {
    	return this.index;
    }
    
    public void set_level(int level) {
    	this.level = level;
    }
    
    public void encrypt(DGKPublicKey dgk_public_key, PaillierPublicKey paillier_public_key) throws HomomorphicException {
    	for (NodeInfo n: node_level_data) {
    		n.encrypt_thresholds(dgk_public_key, paillier_public_key);
    	}	
    }
    
    public boolean are_values_encrypted() {
    	NodeInfo node = node_level_data.get(0);
    	return node.dgk_encrypted != null && node.paillier_encrypted != null;
    }
    
    public void append_data(NodeInfo info) {
    	node_level_data.add(info);
    }
    
    public String toString() {
    	StringBuilder output = new StringBuilder();
    	int num = 0;
    	output.append("level: ");
    	output.append(this.level);
    	output.append('\n');
    	for (NodeInfo i: node_level_data) {
    		output.append("Index ");
    		output.append(num);
    		output.append('\n');
    		output.append(i.toString());
    		output.append('\n');
    		num++;
    	}
    	return output.toString();
    }
    
    private void readObject(ObjectInputStream aInputStream) 
    		throws ClassNotFoundException, IOException {
        aInputStream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream aOutputStream) 
    		throws IOException {
    	aOutputStream.defaultWriteObject();
    }
}
