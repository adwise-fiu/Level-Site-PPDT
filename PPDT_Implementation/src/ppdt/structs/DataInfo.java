package ppdt.structs;

/**
 * 
 * @author Andrew Quijano
 * This class contains all the information about a specific node in the DT
 */
public class DataInfo {

	public final boolean is_leaf;
	public final String variable_name;
	public int comparisonType;
    public float threshold;
    
    public DataInfo(boolean is_leaf, String variable_name) {
    	this.is_leaf = is_leaf;
    	this.variable_name = variable_name;
    }
    
    public String toString() {
    	StringBuilder output = new StringBuilder();
    	output.append(this.variable_name);
    	output.append("Leaf: ");
    	output.append(this.is_leaf);
    	output.append(comparisonType);
    	output.append(threshold);
    	return output.toString();
    }
}
