package edu.fiu.adwise.weka.finito.structs;

import edu.fiu.adwise.homomorphic_encryption.dgk.DGKPublicKey;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a level-order site in a distributed system, managing encryption keys,
 * node data, and communication details for the current level.
 */
public final class level_order_site implements Serializable {
	/** Serial version UID for ensuring compatibility during serialization. */
	@Serial
	private static final long serialVersionUID = 575566807906351024L;

	/** The level of the site in the hierarchy. */
	private final int level;

	/** The Paillier public key for encryption. */
	public final PaillierPublicKey paillier_public_key;

	/** The DGK public key for encryption. */
	public final DGKPublicKey dgk_public_key;

	/** The list of node data for the current level. */
	private final List<NodeInfo> node_level_data = new ArrayList<>();

	/** The next level site IP address. */
	private String next_level_site = null;

	/** The port of the next level site. */
	private int next_level_site_port = -1;

	/** The listening port for the current level site. */
	private int level_site_listening_port = -1;

	/**
	 * Constructs a `level_order_site` object with the specified level and encryption keys.
	 *
	 * @param level The level of the site.
	 * @param paillier_public_key The Paillier public key for encryption.
	 * @param dgk_public_key The DGK public key for encryption.
	 */
	public level_order_site(int level, PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key) {
		this.level = level;
		this.paillier_public_key = paillier_public_key;
		this.dgk_public_key = dgk_public_key;
	}

	/**
	 * Retrieves the level of the site.
	 *
	 * @return The level of the site.
	 */
	public int get_level() {
		return this.level;
	}

	/**
	 * Retrieves the listening port for the current level site.
	 *
	 * @return The listening port.
	 */
	public int get_listen_port() {
		return this.level_site_listening_port;
	}

	/**
	 * Sets the listening port for the current level site.
	 *
	 * @param level_site_listening_port The listening port to set.
	 */
	public void set_listen_port(int level_site_listening_port) {
		this.level_site_listening_port = level_site_listening_port;
	}

	/**
	 * Retrieves the node data for the current level.
	 *
	 * @return The list of node data.
	 */
	public List<NodeInfo> get_node_data() {
		return this.node_level_data;
	}

	/**
	 * Sets the next level site address.
	 *
	 * @param next_level_site The next level site address to set.
	 */
	public void set_next_level_site(String next_level_site) {
		this.next_level_site = next_level_site;
	}

	/**
	 * Retrieves the next level site address.
	 *
	 * @return The next level site address.
	 */
	public String get_next_level_site() {
		return this.next_level_site;
	}

	/**
	 * Sets the port of the next level site.
	 *
	 * @param next_level_site_port The port of the next level site to set.
	 */
	public void set_next_level_site_port(int next_level_site_port) {
		this.next_level_site_port = next_level_site_port;
	}

	/**
	 * Retrieves the port of the next level site.
	 *
	 * @return The port of the next level site.
	 */
	public int get_next_level_site_port() {
		return this.next_level_site_port;
	}

	/**
	 * Appends node data to the current level.
	 *
	 * @param info The node data to append.
	 */
	public void append_data(NodeInfo info) {
		node_level_data.add(info);
	}

	/**
	 * Returns a string representation of the level-order site.
	 *
	 * @return A string representation of the site.
	 */
	public String toString() {
		StringBuilder output = new StringBuilder();
		int num = 0;
		output.append("level: ").append(get_level()).append("\n");
		output.append("Listening from previous level-site at port: ").append(get_listen_port()).append("\n");
		output.append("Next Level-Site: ").append(get_next_level_site()).append(":").append(get_next_level_site_port()).append("\n");

		for (NodeInfo i : node_level_data) {
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