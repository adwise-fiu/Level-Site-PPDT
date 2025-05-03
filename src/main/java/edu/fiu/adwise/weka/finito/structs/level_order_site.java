package edu.fiu.adwise.weka.finito.structs;

import edu.fiu.adwise.homomorphic_encryption.dgk.DGKPublicKey;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Quijano
 * This class contains all the necessary information for a Level-site to complete evaluation
 */

public final class level_order_site implements Serializable {
	@Serial
	private static final long serialVersionUID = 575566807906351024L;
    private final int level;
	public final PaillierPublicKey paillier_public_key;
	public final DGKPublicKey dgk_public_key;
    private final List<NodeInfo> node_level_data = new ArrayList<>();
	// Set to classification to a client, to let level-site d-1 know to talk to a client next with the answer
	private String next_level_site = null;
	private int next_level_site_port = -1;
	private int level_site_listening_port = -1;

	public level_order_site(int level, PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key) {
		this.level = level;
		this.paillier_public_key = paillier_public_key;
		this.dgk_public_key = dgk_public_key;
	}
	public int get_level() {
		return this.level;
	}
	public int get_listen_port() {
		return this.level_site_listening_port;
	}
	public void set_listen_port(int level_site_listening_port) {
		this.level_site_listening_port = level_site_listening_port;
	}
	public List<NodeInfo> get_node_data() {
    	return this.node_level_data;
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
    	output.append("level: ").append(get_level()).append("\n");
		output.append("Listening from previous level-site at port: ").append(get_listen_port()).append("\n");
		output.append("Next Level-Site: ").append(get_next_level_site()).append(
				":").append(get_next_level_site_port()).append("\n");

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
