package weka.finito.structs;

import java.io.Serializable;

/**
 * @author Andrew Quijano
 * This class contains all the information about a specific node in the DT
 */

public final class NodeInfo implements Serializable, Comparable<NodeInfo> {

	private static final long serialVersionUID = -3569139531917752891L;
	public boolean is_leaf;
	public String variable_name;
	public int comparisonType;
    public float threshold;

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
