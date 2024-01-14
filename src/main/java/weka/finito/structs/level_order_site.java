package weka.finito.structs;

import security.dgk.DGKPublicKey;
import security.paillier.PaillierPublicKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Quijano
 * This class contains all the necessary information for a Level-site to complete evaluation
 */

public final class level_order_site implements Serializable {

	private static final long serialVersionUID = 575566807906351024L;
	private int index = 0;
    private int next_index;
    private final int level;

	public final PaillierPublicKey paillier_public_key;
	public final DGKPublicKey dgk_public_key;
    
    private final List<NodeInfo> node_level_data = new ArrayList<>();

	// Set to value to client, to let level-site d-1 know to talk to client next with answer
	private String next_level_site = "client";

	private int next_level_site_port = -1;

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

	public void set_next_level_site(String next_level_site) {
		this.next_level_site = next_level_site;
	}

	public String get_next_level_site() {
		return this.next_level_site;
	}

	public void set_next_level_site_port(int next_level_site_port) {
		this.next_level_site_port = next_level_site_port;
	}

	public int get_next_level_site_port() {
		return this.next_level_site_port;
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
}
