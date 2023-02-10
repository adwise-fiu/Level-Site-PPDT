package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Instances;


public class server_site implements Runnable {
	
	private String training_data;
	private String [] level_site_ips;
	private int [] level_site_ports = null;
	private int port = -1;
	
	private ObjectOutputStream to_level_site = null;
	private ObjectInputStream from_level_site = null;
	
	// For local host testing
	public server_site(String training_data, String [] level_site_ips, int [] level_site_ports) {
		this.training_data = training_data;
		this.level_site_ips = level_site_ips;
		this.level_site_ports = level_site_ports;
	}
	
	// For Cloud environment?
	public server_site(String training_data, String [] level_site_ips, int port) {
		this.training_data = training_data;
		this.level_site_ips = level_site_ips;
		this.port = port;
	}
	
	// Reference: 
	// https://stackoverflow.com/questions/33556543/how-to-save-model-and-apply-it-on-a-test-dataset-on-java/33571811#33571811
	// Build J48 as it uses C45?
	// https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/j48/C45ModelSelection.html
	public static ClassifierTree train_decision_tree(String arff_file) throws Exception {
		BufferedReader reader = null;
		Instances train = null;
		
		try {
			reader = new BufferedReader(new FileReader(arff_file));
			train = new Instances(reader);
			reader.close();
		}
		catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
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
							for (int k = 3; k < rightSideChar.length; k++) {
								rightValue[k - 3] = rightSideChar[k];
							}
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
								for (int k = 4; k < rightSideChar.length; k++) {
									rightValue[k - 4] = rightSideChar[k];
								}
								String rightValueStr = new String(rightValue);
								if (rightValueStr.equals("other")) {
									type = 4;
									threshold = 0;
								}

								if ((rightValueStr.equals("t"))||(rightValueStr.equals("f"))) {
									type = 6;
								}
							}
						}
						else if (rightSideChar[1] == '>') {
							if (rightSideChar[2] == '=') {
								type = 2;
								rightValue = new char[rightSideChar.length - 4];
								for (int k = 4; k < rightSideChar.length; k++)
									rightValue[k - 4] = rightSideChar[k];
							}
							else {
								type = 3;
								rightValue = new char[rightSideChar.length - 3];
								for (int k = 3; k < rightSideChar.length; k++)
									rightValue[k - 3] = rightSideChar[k];
							}
						}
						else if (rightSideChar[1] == '<') {
							if (rightSideChar[2] == '=') {
								type = 4;
								rightValue = new char[rightSideChar.length - 4];
								for (int k = 4; k < rightSideChar.length; k++) {
									rightValue[k - 4] = rightSideChar[k];
								}
							}
							else {
								type = 5;
								rightValue = new char[rightSideChar.length - 3];
								for (int k = 3; k < rightSideChar.length; k++) {
									rightValue[k - 3] = rightSideChar[k];
								}
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
					Level_Order_S.append_data(node_info);
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
				}// else
				n--;
			} // While n > 0 (nodes > 0?)
			all_level_sites.add(Level_Order_S);
			Level_Order_S.set_level(level);
			++level;
		} // While Tree Not Empty
	}

	public void run() {
		ClassifierTree ppdt;
		try {
			ppdt = train_decision_tree(this.training_data);
			List<level_order_site> all_level_sites = new ArrayList<level_order_site>();
			get_level_site_data(ppdt, all_level_sites);

			Socket level_site = null;
			// Send the data to each level site, use data in-transit encryption
			for (int i = 0; i < level_site_ips.length; i++) {
				level_order_site current_level_site = all_level_sites.get(i);
				if (port == -1) {
					level_site = new Socket(level_site_ips[i], level_site_ports[i]);
				}
				else {
					level_site = new Socket(level_site_ips[i], port);
				}
				
				to_level_site = new ObjectOutputStream(level_site.getOutputStream());
				from_level_site = new ObjectInputStream(level_site.getInputStream());
				to_level_site.writeObject(current_level_site);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
