package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.math.BigInteger;

import security.DGK.DGKOperations;
import security.DGK.DGKPublicKey;
import security.misc.HomomorphicException;
import security.paillier.PaillierCipher;
import security.paillier.PaillierPublicKey;

/**
 * 
 * @author Andrew Quijano
 * This class contains all the information about a specific node in the DT
 */
public class NodeInfo {

	public final boolean is_leaf;
	public final String variable_name;
	public int comparisonType;
    public float threshold;
    public BigInteger dgk_encrypted = null;
    public BigInteger paillier_encrypted = null;
    
    public NodeInfo(boolean is_leaf, String variable_name) {
    	this.is_leaf = is_leaf;
    	this.variable_name = variable_name;
    }
    
    public boolean isLeaf() {
    	return this.is_leaf;
    }
    
    public String getVariableName() {
    	return this.variable_name;
    }
    
    public void encrypt_thresholds(DGKPublicKey dgk_public, PaillierPublicKey paillier_public) throws HomomorphicException {
    	this.dgk_encrypted = DGKOperations.encrypt((long) threshold, dgk_public);
    	this.paillier_encrypted = PaillierCipher.encrypt(new BigInteger("" + threshold), paillier_public);
    }
    
    public BigInteger getDGK() {
    	return dgk_encrypted;
    }
    
    public BigInteger getPaillier() {
    	return paillier_encrypted;
    }
    
    public String toString() {
    	StringBuilder output = new StringBuilder();
    	output.append("var_name: " + this.variable_name + '\n');
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
}
