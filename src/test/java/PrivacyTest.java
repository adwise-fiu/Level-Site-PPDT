import org.junit.Before;
import org.junit.Test;
import weka.finito.client;
import weka.finito.level_site_server;
import weka.finito.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static weka.finito.utils.shared.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PrivacyTest {
	private static final Logger logger = LogManager.getLogger(PrivacyTest.class);
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
		setup_tls();

		// Arguments:
		logger.info("Running Full Local Test...");
		logger.info("Working Directory = " + System.getProperty("user.dir"));

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

	public static String test_server_case(String training_data, String features_file,
										int key_size, int precision, String server_ip, int server_port)
			throws InterruptedException {

		// Create the server
		server cloud = new server(training_data, precision, server_port);
		Thread server = new Thread(cloud);
		server.start();

		// Create client
		client evaluate = new client(key_size, features_file, precision, server_ip, server_port);
		Thread client = new Thread(evaluate);
		client.start();

		// Programmatically wait until classification is done.
		server.join();
		client.join();

		// Be sure to delete any keys you made...
		for (String file: delete_files) {
			delete_file(file);
		}
		return evaluate.getClassification();
	}

	// Use this to test with level-sites
	@Test
	public void test_all_level_sites() throws Exception {
		String answer_path = new File(data_directory, "answers.csv").toString();
		run_test(answer_path, true);
	}

	public void run_test(String answer_path, boolean use_level_sites) throws Exception {
		// Parse CSV file with various tests
		try (BufferedReader br = new BufferedReader(new FileReader(answer_path))) {
			String line;
			String classification;
			while ((line = br.readLine()) != null) {
				String [] values = line.split(",");
				String data_set = values[0];
				String features = values[1];
				String expected_classification = values[2];
				String full_feature_path = new File(data_directory, features).toString();
				String full_data_set_path = new File(data_directory, data_set).toString();
				logger.info(full_data_set_path);
				logger.info("Feature Vector Path: " + full_feature_path);
				if (use_level_sites) {
					classification = test_level_site(full_data_set_path, full_feature_path, levels, key_size, precision,
							level_site_ips, level_site_ports_string, server_ip, server_port);
				}
				else {
					classification = test_server_case(full_data_set_path, full_feature_path, key_size, precision,
							server_ip, server_port);
				}

				logger.info(expected_classification + " =!= " + classification);
				assertEquals(expected_classification, classification);
			}
		}
	}

	@Test
	public void test_single_site() throws Exception {
		String answer_path;

		answer_path = new File(data_directory, "answers.csv").toString();
		run_test(answer_path, false);
		
		// Because of the depth of these trees from Liu et al. we will use a test and not for level-site
		answer_path = new File(data_directory, "answers_liu.csv").toString();
		run_test(answer_path, false);
	}

	public static String test_level_site(String training_data, String features_file, int levels,
								   int key_size, int precision,
			String [] level_site_ips, String [] level_site_ports_string, String server_ip, int server_port)
			throws InterruptedException {
		
		int [] level_site_ports = new int[levels];

		// Create Level sites
    	level_site_server [] level_sites = new level_site_server[levels];
    	for (int i = 0; i < level_sites.length; i++) {
			String port_string = level_site_ports_string[i].replaceAll("[^0-9]", "");
    		level_site_ports[i] = Integer.parseInt(port_string);
    		level_sites[i] = new level_site_server(level_site_ports[i]);
        	new Thread(level_sites[i]).start();
    	}

		// Create the server
		server cloud = new server(training_data, level_site_ips, level_site_ports, precision, server_port);
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
			logger.info("Deleted the file: " + myObj.getName());
		} else {
			logger.info("Failed to delete the file: " + myObj.getName());
		}
	}
}

