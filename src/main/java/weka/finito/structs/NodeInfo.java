package weka.finito.structs;

import security.dgk.DGKOperations;
import security.dgk.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierPublicKey;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * @author Andrew Quijano
 * This class contains all the information about a specific node in the DT
 */

public final class NodeInfo implements Serializable {
	@Serial
	private static final long serialVersionUID = -3569139531917752891L;
	public final boolean is_leaf;
	public final String variable_name;
	public final int comparisonType;
    public double threshold;
	private BigInteger paillier;
	private BigInteger dgk;
	private final String real_leaf;

    public NodeInfo(boolean is_leaf, String variable_name, int comparisonType) {
    	this.is_leaf = is_leaf;
    	this.variable_name = variable_name;
		this.comparisonType = comparisonType;
		this.threshold = 0;
		this.real_leaf = "";
    }

	public NodeInfo(boolean is_leaf, String variable_name, int comparisonType, String real_leaf) {
		this.is_leaf = is_leaf;
		this.variable_name = variable_name;
		this.comparisonType = comparisonType;
		this.threshold = 0;
		this.real_leaf = real_leaf;
	}

	public void encrypt(BigInteger temp_thresh,
						PaillierPublicKey paillier_public_key, DGKPublicKey dgk_public_key)
			throws HomomorphicException {

		if (paillier_public_key != null) {
			this.setPaillier(PaillierCipher.encrypt(temp_thresh, paillier_public_key));
		}
		if (dgk_public_key != null) {
			this.setDGK(DGKOperations.encrypt(temp_thresh, dgk_public_key));
		}
		// TODO: Be sure to comment this out, just used for debugging to more easily identify nodes
		this.threshold = temp_thresh.doubleValue();
	}

	public static BigInteger set_precision(double threshold, int precision) {
		int intermediateInteger = (int) (threshold * Math.pow(10, precision));
        return BigInteger.valueOf(intermediateInteger);
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
		if (is_leaf) {
			output.append("Encrypted value\n");
		}
		else {
			output.append("attribute name: ").append(this.variable_name).append('\n');
		}
    	output.append("Leaf: ");
    	output.append(this.is_leaf);
    	output.append('\n');
    	output.append("comparison_type: ");
    	output.append(comparisonType);
    	output.append('\n');
		if (is_leaf) {
			output.append("Leaf: ");
			output.append(real_leaf);
		}
    	else {
			output.append("threshold: ");
			output.append(threshold);
		}
    	output.append('\n');
    	return output.toString();
    }
}
