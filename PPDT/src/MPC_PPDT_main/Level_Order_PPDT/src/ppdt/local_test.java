package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.FileReader;
import java.util.Properties;

public class local_test {
		
	public static void main(String [] args) throws Exception {
		
		// Arguments:
		System.out.println("Running Full Local Test...");
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		
		Properties config = new Properties();
		try (FileReader in = new FileReader("../data/config.properties")) {
		    config.load(in);
		}
		String [] level_site_ports_string = config.getProperty("level-site-ports").split(",");
		String [] level_site_ips = config.getProperty("level-site-ips").split(",");
		int levels = Integer.parseInt(config.getProperty("levels"));
		String features_file = config.getProperty("features");
		String training_data = config.getProperty("training");
		int key_size = Integer.parseInt(config.getProperty("key_size"));
		int [] level_site_ports = new int[levels];
		int precision = Integer.parseInt(config.getProperty("precision"));
		
		// Create Level sites
    	level_site_server [] level_sites = new level_site_server[levels];
    	for (int i = 0; i < level_sites.length; i++) {
    		level_site_ports[i] = Integer.parseInt(level_site_ports_string[i]);
    		level_sites[i] = new level_site_server(level_site_ports[i], precision);
        	new Thread(level_sites[i]).start();
    	}
    	
    	Thread.sleep(1000 * 7);

		// Create the server
		server_site cloud = new server_site(training_data, level_site_ips, level_site_ports);
    	new Thread(cloud).start();
    	
    	Thread.sleep(1000 * 7);
    	
		// Create client
    	client evaluate = new client(key_size, features_file, level_site_ips, level_site_ports, precision);
    	new Thread(evaluate).start();
    	
    	Thread.sleep(1000 * 7);
    	
    	// Close the Level Sites
    	/*
    	for (int i = 0; i < level_sites.length; i++) {
    		level_sites[i].stop();
    	}
    	*/
	}
}
