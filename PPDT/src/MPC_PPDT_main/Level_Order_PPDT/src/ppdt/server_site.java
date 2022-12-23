package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import MPC_PPDT_main.Level_Order_PPDT.src.ppdt.NodeInfo;
import MPC_PPDT_main.Level_Order_PPDT.src.ppdt.level_order_site;

import weka.classifiers.trees.j48.C45ModelSelection;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Instances;
import weka.core.SerializationHelper;


public class server_site {
	
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
		
		// https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/j48/C45ModelSelection.html
	    C45ModelSelection j48_model = new C45ModelSelection(0, train, false, false);
	    ClassifierTree j48 = new ClassifierTree(j48_model);
	    train.setClassIndex(train.numAttributes() - 1);
	    
	    // train.setClassIndex(0);
	    System.out.println(train.classAttribute());
	    j48.buildTree(train, true);

	    //SerializationHelper.write("j48.model", j48);
	    return j48;
	}
	
	// Given a Plain-text Decision Tree, split the data up for each level site.
	public static void get_level_site_data(ClassifierTree root, List<level_order_site> all_level_sites) throws Exception {

		if (root == null) {
			return;
		}

		Queue<ClassifierTree> q = new LinkedList<>();
		q.add(root);
		
		while (!q.isEmpty()) {
			level_order_site Level_Order_S = new level_order_site();
			int n = q.size();
			
			while (n > 0) {

				ClassifierTree p = q.peek();
				q.remove();
				
				System.out.println(p.getLocalModel().dumpModel(p.getTrainingData()));
				
				System.out.println("L: " + p.getLocalModel().leftSide(p.getTrainingData()));
				System.out.println("R0: " + p.getLocalModel().rightSide(0, p.getTrainingData()));
				
				System.out.println("See sons");
				for (int i = 0; i < p.getSons().length; i++) {
					System.out.println(p.getSons()[i].getLocalModel().dumpModel(p.getTrainingData()));
				}
				
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
								threshold=1;
							}
						}
						else if (rightSideChar[1]=='!') {
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
							if (rightValueStr.equals("t")||rightValueStr.equals("yes")) {
								threshold = 1;
							} 
							else if (rightValueStr.equals("f")||rightValueStr.equals("no")) {
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
				}// else
				n--;
			} // While n > 0
			System.out.println("ESCAPE WHILE");
			all_level_sites.add(Level_Order_S);
			
			System.out.println("Checking current level site data list...");
			int level = 0;
			for (level_order_site l: all_level_sites) {
				System.out.println("-------------level: " + level);
				System.out.println(l.toString());
				level++;
			}
			
		} // While Tree Not Empty
	}

	public static void main(String [] args) throws Exception {
		// Assuming a 192.168.1.X Network, I can support 251 levels?
		// I will assume 192.168.1.1 is Server
		// I will assume 192.168.1.2 is Client
		// Level Site 0 is 192.168.1.3
		// Level Site 1 is 192.168.1.2, ...
		// Level Site d is 192.168.1.8 (depth)

		// Each level site needs the 
		// 1- public keys (will get from Part 4)
		// 2- IP of where to send result to (or Client)
		// 3- Socket of previous Level-Site to talk to...
		// 4- Socket of Client-site for using Alice/Bob

		// TODO: 
		// Spyrios can you please check that the creation of level sites looks good?
		// Also, I want to know how you think this approach looks so far?

		// Arguments:
		// System.out.println("Working Directory = " + System.getProperty("user.dir"));
		
		// Runs at: MPC-PPDT\PPDT
		String file = "../data/hypothyroid.arff";
		ClassifierTree ppdt = train_decision_tree(file);

		List<level_order_site> all_level_sites = new ArrayList<level_order_site>();
		get_level_site_data(ppdt, all_level_sites);
		
		// Send the data to each level site
		for (int i = 0; i < all_level_sites.size(); i++) {
			level_order_site current_level_site = all_level_sites.get(i);
			String level_site_ip = "192.168.1" + String.valueOf(i + 100);
			//Socket level_site_socket = new Socket(level_site_ip, 9254);
		}
	}
}
