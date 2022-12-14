package ppdt.structs;

import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Andrew Quijano
 * This class contains all the necessary information for a Level-site to complete evaluation
 */

public class level_order_site implements Serializable {
	
	private static final long serialVersionUID = 575566807906351024L;

	private int index;
    private int next_index;
    private String next_ip_address;   //Point to next IP to send data to
    private Socket client_connection; // Socket to client-site, used to build alice/bob
    
    private List<NodeInfo> node_level_data = new ArrayList<NodeInfo>();
    
    public void set_current_index(int index) {
    	this.index = index;
    }
    
    public void set_next_index(int next_index) {
    	this.next_index = next_index;
    }
    
    public void append_data(NodeInfo info) {
    	node_level_data.add(info);
    }
    
    public String toString() {
    	StringBuilder output = new StringBuilder();
    	for (NodeInfo i: node_level_data) {
    		output.append(i.toString());
    		output.append('\n');
    	}
    	return output.toString();
    }
}
