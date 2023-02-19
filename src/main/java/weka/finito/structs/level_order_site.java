package weka.finito.structs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Quijano
 * This class contains all the necessary information for a Level-site to complete evaluation
 */

public class level_order_site implements Serializable {

	private static final long serialVersionUID = 575566807906351024L;
	private int index = 0;
    private int next_index;
    private int level = 0;
    
    private final List<NodeInfo> node_level_data = new ArrayList<>();
    
    public List<NodeInfo> get_node_data() {
    	return this.node_level_data;
    }
    
    public int getLevel() {
    	return this.level;
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
    
    public void set_level(int level) {
    	this.level = level;
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
