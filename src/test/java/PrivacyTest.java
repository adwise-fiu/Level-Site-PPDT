import org.junit.Before;
import org.junit.Test;
import weka.finito.AES;
import weka.finito.client;
import weka.finito.level_site_server;
import weka.finito.server_site;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;


public final class PrivacyTest {

	private String [] level_site_ports_string;
	private String [] level_site_ips;
	private int levels;
	private int key_size;
	private int precision;
	private String data_directory;

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
	}

	@Test
	public  void test_all() throws Exception {
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
		        		level_site_ips, level_site_ports_string);
		        assertEquals(expected_classification, classification);
		    }
		}
	}

	public static String test_case(String training_data, String features_file, int levels, int key_size, int precision,
			String [] level_site_ips, String [] level_site_ports_string) 
			throws InterruptedException {
		
		int [] level_site_ports = new int[levels];

		// Create Level sites
    	level_site_server [] level_sites = new level_site_server[levels];
    	for (int i = 0; i < level_sites.length; i++) {
    		level_site_ports[i] = Integer.parseInt(level_site_ports_string[i]);
    		level_sites[i] = new level_site_server(level_site_ports[i], precision, true,
					new AES("AppSecSpring2023"));
        	new Thread(level_sites[i]).start();
    	}

		// Create the server
		server_site cloud = new server_site(training_data, level_site_ips, level_site_ports);
		Thread server = new Thread(cloud);
		server.start();
		server.join();

		// Create client
    	client evaluate = new client(key_size, features_file, level_site_ips, level_site_ports, precision);
    	Thread client = new Thread(evaluate);
		client.start();

		// Programmatically wait until classification is done.
		client.join();

    	// Close the Level Sites
		for (level_site_server levelSite : level_sites) {
			levelSite.stop();
		}
    	return evaluate.getClassification();
	}
}

