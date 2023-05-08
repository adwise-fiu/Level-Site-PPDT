package weka.finito.structs;

import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierPublicKey;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Quijano
 * This class contains all the necessary information for a Level-site to complete evaluation
 */

public final class level_order_site implements Serializable, Comparable<level_order_site> {

	private static final long serialVersionUID = 575566807906351024L;
	private int index = 0;
    private int next_index;
    private final int level;

	public final PaillierPublicKey paillier_public_key;
	public final DGKPublicKey dgk_public_key;
    
    private final List<NodeInfo> node_level_data = new ArrayList<>();

	public level_order_site(int level, PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key) {
		this.level = level;
		this.paillier_public_key = paillier_public_key;
		this.dgk_public_key = dgk_public_key;
	}

	public List<NodeInfo> get_node_data() {
    	return this.node_level_data;
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

	public int compareTo(level_order_site o) {
		if (this.node_level_data.size() == o.node_level_data.size()) {
			for (int i = 0; i < node_level_data.size(); i++) {
				NodeInfo left = this.node_level_data.get(i);
				NodeInfo right = o.node_level_data.get(i);
				int comparison = left.compareTo(right);
				if (comparison != 0) {
					return comparison;
				}
			}
			return 0;
		}
		else if (this.node_level_data.size() > o.node_level_data.size()) {
			return 1;
		}
		else {
			return -1;
		}
	}
}
