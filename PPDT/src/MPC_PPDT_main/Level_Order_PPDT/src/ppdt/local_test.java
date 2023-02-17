package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class local_test {
		
	public static void main(String [] args) throws Exception {
		
		// Arguments:
		System.out.println("Running Full Local Test...");
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		
		if (args.length == 0) {
			System.out.println("Running basic pre-CI test...");
			basic_test();
			return;
		}
		
		Properties config = new Properties();
		try (FileReader in = new FileReader("../data/config.properties")) {
		    config.load(in);
		}
		String [] level_site_ports_string = config.getProperty("level-site-ports").split(",");
		String [] level_site_ips = config.getProperty("level-site-ips").split(",");
		int levels = Integer.parseInt(config.getProperty("levels"));
		int key_size = Integer.parseInt(config.getProperty("key_size"));
		int precision = Integer.parseInt(config.getProperty("precision"));
		
		// Parse CSV file with various tests
		try (BufferedReader br = new BufferedReader(new FileReader("../data/answers.csv"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String [] values = line.split(",");
		        String data_set = values[0];
		        String features = values[1];
		        String expected_classification = values[2];
		        // TODO: Need to append what data directory is to the name of values/dataset?
		        String classification = test(data_set, features, levels, key_size, precision, 
		        		level_site_ips, level_site_ports_string);
		        if (expected_classification.equals(classification)) {
		        	// PASS
		        }
		    }
		}
	}
	
	public static void basic_test() throws FileNotFoundException, IOException, InterruptedException {
		Properties config = new Properties();
		try (FileReader in = new FileReader("../data/config.properties")) {
		    config.load(in);
		}
		String [] level_site_ports_string = config.getProperty("level-site-ports").split(",");
		String [] level_site_ips = config.getProperty("level-site-ips").split(",");
		int levels = Integer.parseInt(config.getProperty("levels"));
		int key_size = Integer.parseInt(config.getProperty("key_size"));
		int precision = Integer.parseInt(config.getProperty("precision"));
		
		String features_file = config.getProperty("features");
		String training_data = config.getProperty("training");
		int [] level_site_ports = new int[levels];
		
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
    	
    	Thread.sleep(1000 * 60);
    	
    	// Close the Level Sites
    	for (int i = 0; i < level_sites.length; i++) {
    		level_sites[i].stop();
    	}			
	}
	
	public static String test(String training_data, String features_file, int levels, int key_size, int precision,
			String [] level_site_ips, String [] level_site_ports_string) 
			throws InterruptedException, FileNotFoundException, IOException {
		
		int [] level_site_ports = new int[levels];
		
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
    	
    	Thread.sleep(1000 * 60);
    	
    	// Close the Level Sites
    	for (int i = 0; i < level_sites.length; i++) {
    		level_sites[i].stop();
    	}
    	return evaluate.getClassification();
	}
}
