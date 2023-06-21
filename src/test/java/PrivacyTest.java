import org.junit.Before;
import org.junit.Test;
import weka.finito.AES;
import weka.finito.client;
import weka.finito.level_site_server;
import weka.finito.server_site;

import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;


public final class PrivacyTest {

	private String [] level_site_ports_string;
	private String [] level_site_ips;
	private int levels;
	private int key_size;
	private int precision;
	private String data_directory;
	private int server_port;
	private String server_ip;
	private final static String [] delete_files = {"dgk", "dgk.pub", "paillier", "paillier.pub", "classes.txt"};
	@Before
	public void read_properties() throws IOException {
		// Arguments:
		System.out.println("Running Full Local Test...");
		System.out.println("Working Directory = " + System.getProperty("user.dir"));

		Properties config = new Properties();
		try (FileReader in = new FileReader("config.properties")) {
			config.load(in);
		}
		level_site_ports_string = config.getProperty("level-site-ports").split(",");
		levels = level_site_ports_string.length;
		level_site_ips = new String[levels];
		for (int i = 0; i < levels; i++) {
			level_site_ips[i] = "127.0.0.1";
		}
		key_size = Integer.parseInt(config.getProperty("key_size"));
		precision = Integer.parseInt(config.getProperty("precision"));
		data_directory = config.getProperty("data_directory");
		server_ip = config.getProperty("server-ip");
		server_port = Integer.parseInt(config.getProperty("server-port"));
	}

	@Test
	public void test_all() throws Exception {
		String answer_path = new File(data_directory, "answers.csv").toString();
		// Parse CSV file with various tests
		try (BufferedReader br = new BufferedReader(new FileReader(answer_path))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String [] values = line.split(",");
		        String data_set = values[0];
		        String features = values[1];
		        String expected_classification = values[2];
				String full_feature_path = new File(data_directory, features).toString();
				String full_data_set_path = new File(data_directory, data_set).toString();
				System.out.println(full_data_set_path);
				String classification = test_case(full_data_set_path, full_feature_path, levels, key_size, precision,
		        		level_site_ips, level_site_ports_string, server_ip, server_port);
				System.out.println(expected_classification + " =!= " + classification);
				assertEquals(expected_classification, classification);
		    }
		}
	}

	public static String test_case(String training_data, String features_file, int levels,
								   int key_size, int precision,
			String [] level_site_ips, String [] level_site_ports_string, String server_ip, int server_port)
			throws InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException {
		
		int [] level_site_ports = new int[levels];

		// Create Level sites
    	level_site_server [] level_sites = new level_site_server[levels];
    	for (int i = 0; i < level_sites.length; i++) {
			String port_string = level_site_ports_string[i].replaceAll("[^0-9]", "");
    		level_site_ports[i] = Integer.parseInt(port_string);
    		level_sites[i] = new level_site_server(level_site_ports[i], precision,
					new AES("AppSecSpring2023"));
        	new Thread(level_sites[i]).start();
    	}

		// Create the server
		server_site cloud = new server_site(training_data, level_site_ips, level_site_ports, precision, server_port);
		Thread server = new Thread(cloud);
		server.start();

		// Create client
    	client evaluate = new client(key_size, features_file, level_site_ips, level_site_ports, precision,
				server_ip, server_port);
    	Thread client = new Thread(evaluate);
		client.start();

		// Programmatically wait until classification is done.
		server.join();
		client.join();

    	// Close the Level Sites
		for (level_site_server levelSite : level_sites) {
			levelSite.stop();
		}
		// Be sure to delete any keys you made...
		for (String file: delete_files) {
			delete_file(file);
		}

    	return evaluate.getClassification();
	}

	public static void delete_file(String file_name){
		File myObj = new File(file_name);
		if (myObj.delete()) {
			System.out.println("Deleted the file: " + myObj.getName());
		} else {
			System.out.println("Failed to delete the file.");
		}
	}
}

