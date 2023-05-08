package weka.finito.structs;

import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierPublicKey;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Andrew Quijano
 * This class contains all the information about a specific node in the DT
 */

public final class NodeInfo implements Serializable, Comparable<NodeInfo> {

	private static final long serialVersionUID = -3569139531917752891L;
	public final boolean is_leaf;
	public final String variable_name;
	public final int comparisonType;
    public final float threshold;

	private BigInteger paillier;
	private BigInteger dgk;

    public NodeInfo(boolean is_leaf, String variable_name, int comparisonType) {
    	this.is_leaf = is_leaf;
    	this.variable_name = variable_name;
		this.comparisonType = comparisonType;
		this.threshold = 0;
    }

	public void encrypt(float threshold, int precision,
						PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key)
			throws HomomorphicException {
		BigInteger encrypted_thresh;
		int intermediateInteger = (int) (threshold * Math.pow(10, precision));
		encrypted_thresh = BigInteger.valueOf(intermediateInteger);
		this.setPaillier(PaillierCipher.encrypt(encrypted_thresh, paillier_public_key));
		this.setDGK(DGKOperations.encrypt(encrypted_thresh, dgk_public_key));
	}

	public void setDGK(BigInteger dgk){
		this.dgk = dgk;
	}

	public BigInteger getDGK() {
		return this.dgk;
	}

	public void setPaillier(BigInteger paillier) {
		this.paillier = paillier;
	}

	public BigInteger getPaillier() {
		return this.paillier;
	}

    
    public boolean isLeaf() {
    	return this.is_leaf;
    }
    
    public String getVariableName() {
    	return this.variable_name;
    }
    
    public String toString() {
    	StringBuilder output;
		output = new StringBuilder();
		output.append("var_name: ").append(this.variable_name).append('\n');
    	output.append("Leaf: ");
    	output.append(this.is_leaf);
    	output.append('\n');
    	output.append("comparison_type: ");
    	output.append(comparisonType);
    	output.append('\n');
    	output.append("threshold: ");
    	output.append(threshold);
    	output.append('\n');
    	return output.toString();
    }

	public int compareTo(NodeInfo o) {
		boolean leaf_match = this.is_leaf == o.isLeaf();
		boolean variable_match = this.variable_name.equals(o.variable_name);
		boolean comparison_match = this.comparisonType == o.comparisonType;
		boolean threshold_match = this.threshold == o.threshold;
		if (leaf_match && variable_match && comparison_match && threshold_match) {
			return 0;
		}
		else {
			return this.variable_name.compareTo(o.variable_name);
		}
	}
}
