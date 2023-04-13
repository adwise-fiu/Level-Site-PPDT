package weka.finito;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//For k8s deployment.
import java.lang.System;

import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Instances;

import weka.core.SerializationHelper;
import weka.finito.structs.level_order_site;
import weka.finito.structs.NodeInfo;



public final class server_site implements Runnable {

	private final String training_data;
	private final String [] level_site_ips;
	private int [] level_site_ports = null;
	private int port = -1;

    public static void main(String[] args) {
        int port = -1;
        String training_data = null;

        if(args.length < 2) {
            //TODO: Print error message.
            System.exit(1);
        }
        String data_directory = System.getenv("PPDT_DATA_DIR");
        if(data_directory == null || data_directory.isEmpty()) {
            //TODO: Print error message.
            System.exit(1);
        }

        try {
            port = Integer.parseInt(System.getenv("LEVEL_SITE_PORT"));
        } catch (NumberFormatException e) {
            //TODO: print error message.
            System.exit(1);
        }
        
        // Get data for training.
        String FilePath = args[1];
        try (BufferedReader br = new BufferedReader(new FileReader(FilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] values = line.split(",");
                String data_set = values[0];
                String features = values[1];
                //String expected_classification = values[2];
                //String features_file = new File(data_directory, features).toString();
                training_data = new File(data_directory, data_set).toString();
                System.out.println(training_data);
            }
        } catch (FileNotFoundException e) {
            //TODO: print error message.
            System.exit(1);
        } catch (IOException e) {
            //TODO: print error message.
            System.exit(1);
        }
        
        // Pass data to level sites.
        String level_domains_str = System.getenv("LEVEL_SITE_DOMAINS");
        if(level_domains_str == null || level_domains_str.isEmpty()) {
            //TODO: print error message.
            System.exit(1);
        }
        String[] level_domains = level_domains_str.split(",");
        // Create and run the server.
        server_site server = new server_site(training_data, level_domains, port);
        new Thread(server).start();
        boolean run = true;
        while(run) {
            try {
                continue;
            } catch (Exception e) {
                System.out.println("Stopping...");
                run = false;
            }
        }
        //server.stop();
    }

	// For local host testing
	public server_site(String training_data, String [] level_site_ips, int [] level_site_ports) {
		this.training_data = training_data;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = level_site_ports;
	}

	// For Cloud environment?
	public server_site(String training_data, String [] level_site_domains, int port) {
		this.training_data = training_data;
		this.level_site_ips = level_site_domains;
		this.port = port;
	}

	// Reference:
	// https://stackoverflow.com/questions/33556543/how-to-save-model-and-apply-it-on-a-test-dataset-on-java/33571811#33571811
	// Build J48 as it uses C45?
	// https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/j48/C45ModelSelection.html
	public static ClassifierTree train_decision_tree(String arff_file) throws Exception {
		Instances train = null;

		try (BufferedReader reader = new BufferedReader(new FileReader(arff_file))) {
			train = new Instances(reader);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		assert train != null;
		train.setClassIndex(train.numAttributes() - 1);

		// https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/j48/C45ModelSelection.html
		// J48 -B -C 0.25 -M 2
		// -M 2 is minimum 2, DEFAULT
		// -B this tree ONLY works for binary split is true, so pick this model...
		// -C 0.25, default confidence
		BinC45ModelSelection j48_model = new BinC45ModelSelection(2, train, true, false);
		ClassifierTree j48 = new C45PruneableClassifierTree(j48_model, true, (float) 0.25, true, true, true);

	    j48.buildClassifier(train);
	    try (PrintWriter out = new PrintWriter("dt-graph.txt")) {
	        out.println(j48.graph());
	    }
		SerializationHelper.write("ppdt-j48.model", j48);
	    return j48;
	}

	// Given a Plain-text Decision Tree, split the data up for each level site.
	public static void get_level_site_data(ClassifierTree root, List<level_order_site> all_level_sites) throws Exception {

		if (root == null) {
			return;
		}

		Queue<ClassifierTree> q = new LinkedList<>();
		q.add(root);
		int level = 0;

		while (!q.isEmpty()) {
			level_order_site Level_Order_S = new level_order_site();
			int n = q.size();

			while (n > 0) {

				ClassifierTree p = q.peek();
				q.remove();

				NodeInfo node_info = null;
				assert p != null;
				if (p.isLeaf()) {
					String variable = p.getLocalModel().dumpLabel(0, p.getTrainingData());
					node_info = new NodeInfo(true, variable);
					Level_Order_S.append_data(node_info);
				}
				else {
					float threshold = 0;
					for (int i = 0; i < p.getSons().length; i++) {
						String leftSide = p.getLocalModel().leftSide(p.getTrainingData());
						String rightSide = p.getLocalModel().rightSide(i, p.getTrainingData());

						char[] rightSideChar = rightSide.toCharArray();
						int type = 0;

						char[] rightValue = new char[0];
						if (rightSideChar[1] == '=') {
							type = 1;
							rightValue = new char[rightSideChar.length - 3];
							System.arraycopy(rightSideChar, 3, rightValue, 0, rightSideChar.length - 3);
							String rightValueStr = new String(rightValue);
							if (rightValueStr.equals("other")) {
								type = 2;
								threshold = 1;
							}
						}
						else if (rightSideChar[1] == '!') {
							type = 4;
							if (rightSideChar[2] == '=') {
								rightValue = new char[rightSideChar.length - 4];
								System.arraycopy(rightSideChar, 4, rightValue, 0, rightSideChar.length - 4);
								String rightValueStr = new String(rightValue);
								if (rightValueStr.equals("other")) {
									type = 4;
									threshold = 0;
								}
								if ((rightValueStr.equals("t"))||(rightValueStr.equals("f"))||(rightValueStr.equals("yes"))||(rightValueStr.equals("no"))) {
									type = 6;
								}
							}
						}
						else if (rightSideChar[1] == '>') {
							if (rightSideChar[2] == '=') {
								type = 2;
								rightValue = new char[rightSideChar.length - 4];
								System.arraycopy(rightSideChar, 4, rightValue, 0, rightSideChar.length - 4);
							}
							else {
								type = 3;
								rightValue = new char[rightSideChar.length - 3];
								System.arraycopy(rightSideChar, 3, rightValue, 0, rightSideChar.length - 3);
							}
						}
						else if (rightSideChar[1] == '<') {
							if (rightSideChar[2] == '=') {
								type = 4;
								rightValue = new char[rightSideChar.length - 4];
								System.arraycopy(rightSideChar, 4, rightValue, 0, rightSideChar.length - 4);
							}
							else {
								type = 5;
								rightValue = new char[rightSideChar.length - 3];
								System.arraycopy(rightSideChar, 3, rightValue, 0, rightSideChar.length - 3);
							}
						}

						String rightValueStr = new String(rightValue);

						if (!rightValueStr.equals("other")) {
							if (rightValueStr.equals("t") || rightValueStr.equals("yes")) {
								threshold = 1;
							}
							else if (rightValueStr.equals("f") || rightValueStr.equals("no")) {
								threshold = 0;
							}
							else {
								threshold = Float.parseFloat(rightValueStr);
							}
						}

						node_info = new NodeInfo(false, leftSide);
						node_info.comparisonType = type;
						node_info.threshold = threshold;
						q.add(p.getSons()[i]);
					}

					assert node_info != null;
					if (!node_info.is_leaf){

						NodeInfo additionalNode = new NodeInfo(false, node_info.getVariableName());

						if (node_info.comparisonType == 1) {
							additionalNode.comparisonType = 6;
						}
						else if (node_info.comparisonType == 2) {
							additionalNode.comparisonType = 5;
						}
						else if (node_info.comparisonType == 3) {
							additionalNode.comparisonType = 4;
						}
						else if (node_info.comparisonType == 4) {
							additionalNode.comparisonType = 3;
						}
						else if (node_info.comparisonType == 5) {
							additionalNode.comparisonType = 2;
						}
						else if (node_info.comparisonType == 6) {
							additionalNode.comparisonType = 1;
						}
						additionalNode.threshold = node_info.threshold;
						Level_Order_S.append_data(additionalNode);
					}
					Level_Order_S.append_data(node_info);
				}// else
				n--;
			} // While n > 0 (nodes > 0?)
			all_level_sites.add(Level_Order_S);
			Level_Order_S.set_level(level);
			++level;
		} // While Tree Not Empty
	}

	public void run() {
		List<level_order_site> all_level_sites = new ArrayList<>();
		ClassifierTree ppdt;
		try {
			ppdt = train_decision_tree(this.training_data);
			get_level_site_data(ppdt, all_level_sites);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		Socket level_site = null;
		ObjectOutputStream to_level_site = null;
		try {
			// Send the data to each level site, use data in-transit encryption
			for (int i = 0; i < all_level_sites.size(); i++) {
				System.out.println("i:" + i + " port:" + level_site_ports[i]);
				level_order_site current_level_site = all_level_sites.get(i);

				if (port == -1) {
					level_site = new Socket(level_site_ips[i], level_site_ports[i]);
				}
				else {
					level_site = new Socket(level_site_ips[i], port);
				}
				System.out.println(current_level_site.toString());

				to_level_site = new ObjectOutputStream(level_site.getOutputStream());
				to_level_site.writeObject(current_level_site);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				assert to_level_site != null;
				to_level_site.close();
				level_site.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
