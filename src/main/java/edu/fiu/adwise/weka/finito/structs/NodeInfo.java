package edu.fiu.adwise.weka.finito.structs;

import edu.fiu.adwise.homomorphic_encryption.dgk.DGKOperations;
import edu.fiu.adwise.homomorphic_encryption.dgk.DGKPublicKey;
import edu.fiu.adwise.homomorphic_encryption.misc.HomomorphicException;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierCipher;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * Represents information about a node in a decision tree, including encryption details and thresholds.
 */
public final class NodeInfo implements Serializable {
	/** Serial version UID for ensuring compatibility during serialization. */
	@Serial
	private static final long serialVersionUID = -3569139531917752891L;

	/** Indicates whether the node is a leaf. */
	public final boolean is_leaf;

	/** The variable name associated with the node. This is the attribute */
	public final String variable_name;

	/** The comparison type for the node, e.g., equals or less than or equal to */
	public final int comparisonType;

	/** The threshold value for the node. */
	public double threshold;

	/** The Paillier-encrypted threshold. */
	private BigInteger paillier;

	/** The DGK-encrypted threshold. */
	private BigInteger dgk;

	/** The real leaf value for leaf nodes. This is the classification. */
	private final String real_leaf;

	/**
	 * Constructs a `NodeInfo` object for a non-leaf node.
	 *
	 * @param is_leaf Indicates if the node is a leaf.
	 * @param variable_name The variable name associated with the node.
	 * @param comparisonType The comparison type for the node.
	 */
	public NodeInfo(boolean is_leaf, String variable_name, int comparisonType) {
		this.is_leaf = is_leaf;
		this.variable_name = variable_name;
		this.comparisonType = comparisonType;
		this.threshold = 0;
		this.real_leaf = "";
	}

	/**
	 * Constructs a `NodeInfo` object for a leaf node.
	 *
	 * @param is_leaf Indicates if the node is a leaf.
	 * @param variable_name The variable name associated with the node.
	 * @param comparisonType The comparison type for the node.
	 * @param real_leaf The real leaf value for the node.
	 */
	public NodeInfo(boolean is_leaf, String variable_name, int comparisonType, String real_leaf) {
		this.is_leaf = is_leaf;
		this.variable_name = variable_name;
		this.comparisonType = comparisonType;
		this.threshold = 0;
		this.real_leaf = real_leaf;
	}

	/**
	 * Encrypts the threshold value using the provided public keys.
	 *
	 * @param temp_thresh The threshold value to encrypt.
	 * @param paillier_public_key The Paillier public key for encryption.
	 * @param dgk_public_key The DGK public key for encryption.
	 * @throws HomomorphicException If an error occurs during encryption.
	 */
	public void encrypt(BigInteger temp_thresh,
						PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key)
			throws HomomorphicException {

		if (paillier_public_key != null) {
			this.setPaillier(PaillierCipher.encrypt(temp_thresh, paillier_public_key));
		}
		if (dgk_public_key != null) {
			this.setDGK(DGKOperations.encrypt(temp_thresh, dgk_public_key));
		}
		this.threshold = temp_thresh.doubleValue();
	}

	/**
	 * Sets the precision of a threshold value.
	 * This means that the threshold value is multiplied by 10 raised to the power of the precision.
	 *
	 * @param threshold The threshold value.
	 * @param precision The precision to set.
	 * @return The threshold value with the specified precision as a `BigInteger`.
	 */
	public static BigInteger set_precision(double threshold, int precision) {
		int intermediateInteger = (int) (threshold * Math.pow(10, precision));
		return BigInteger.valueOf(intermediateInteger);
	}

	/**
	 * Sets the DGK-encrypted value.
	 *
	 * @param dgk The DGK-encrypted value to set.
	 */
	public void setDGK(BigInteger dgk) {
		this.dgk = dgk;
	}

	/**
	 * Retrieves the DGK-encrypted value.
	 *
	 * @return The DGK-encrypted value.
	 */
	public BigInteger getDGK() {
		return this.dgk;
	}

	/**
	 * Sets the Paillier-encrypted value.
	 *
	 * @param paillier The Paillier-encrypted value to set.
	 */
	public void setPaillier(BigInteger paillier) {
		this.paillier = paillier;
	}

	/**
	 * Retrieves the Paillier-encrypted value.
	 *
	 * @return The Paillier-encrypted value.
	 */
	public BigInteger getPaillier() {
		return this.paillier;
	}

	/**
	 * Checks if the node is a leaf.
	 *
	 * @return `true` if the node is a leaf, otherwise `false`.
	 */
	public boolean isLeaf() {
		return this.is_leaf;
	}

	/**
	 * Retrieves the variable name associated with the node.
	 *
	 * @return The variable name.
	 */
	public String getVariableName() {
		return this.variable_name;
	}

	/**
	 * Returns a string representation of the node.
	 *
	 * @return A string representation of the node.
	 */
	public String toString() {
		StringBuilder output = new StringBuilder();
		if (is_leaf) {
			output.append("Encrypted value\n");
		} else {
			output.append("attribute name: ").append(this.variable_name).append('\n');
		}
		output.append("Leaf: ").append(this.is_leaf).append('\n');
		output.append("comparison_type: ").append(comparisonType).append('\n');
		if (is_leaf) {
			output.append("Leaf: ").append(real_leaf);
		} else {
			output.append("threshold: ").append(threshold);
		}
		output.append('\n');
		return output.toString();
	}
}