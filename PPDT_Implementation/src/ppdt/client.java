package ppdt;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import ppdt.structs.DataInfo;
import security.DGK.DGKKeyPairGenerator;
import security.misc.HomomorphicException;
import security.paillier.PaillierKeyPairGenerator;
import security.socialistmillionaire.bob;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;


public class client {
	

	public void followPath(Client client, List<Level_Order_Site> Level_Nodes) throws HomomorphicException {

		boolean equalsFound=false;
		boolean inequalityHolds = false;
		boolean terminalLeafFound = false;

		try {
			for (int i=0; i<Level_Nodes.size(); i++){
				System.out.println("Level i:"+i+" size:"+Level_Nodes.get(i).LevelData.size());
			}
			for (int i = 0; i < Level_Nodes.size() && (!terminalLeafFound); i++) {
				System.out.println("i=" + i);
				Level_Order_Site Level_Order_S = Level_Nodes.get(i);
				int bound=0;
				if (i == 0) {
					Level_Order_S.index=0;
					bound=2;
				} else {
					bound = Level_Order_S.LevelData.size();
				}

				equalsFound = false;
				int j=0;
				int n=0;
				int k=0;

				while (j < bound && (!equalsFound) && (!terminalLeafFound)) {
					DataInfo ls = (DataInfo) Level_Order_S.LevelData.get(j);
					System.out.println("j=" + j);
					if (ls.isLeaf == false) {
						if ((i==0)||((n==2 * Level_Order_S.index || n == 2 * Level_Order_S.index + 1))) {
							if (ls.comparisonType == 6) {
								ls.comparisonType = 3;
								boolean firstInequalityHolds = OfflineAuction.runOfflineAuction(ls, client, precision, paillier, dgk);
								ls.comparisonType = 5;
								boolean secondInequalityHolds = OfflineAuction.runOfflineAuction(ls, client, precision, paillier, dgk);
								if (firstInequalityHolds || secondInequalityHolds) {
									inequalityHolds = true;
								}
							} else {
								inequalityHolds = OfflineAuction.runOfflineAuction(ls, client, precision, paillier, dgk);
							}

							System.out.println("Inequality Holds:" + inequalityHolds);
							System.out.println("n:" + n + " first node index:" + 2 * Level_Order_S.index + " second node index:" + (2 * Level_Order_S.index + 1));
							if ((inequalityHolds) && ((n == 2 * Level_Order_S.index || n == 2 * Level_Order_S.index + 1))) {
								equalsFound = true;

								Level_Nodes.get(i).nextIndex = k;
								TransmitValueSecurely.transmit_value_securely(Level_Nodes.get(i), Level_Nodes.get(i + 1));
								System.out.println("New index:" + Level_Nodes.get(i + 1).index);

							}
						}
						n++;
						k++;
						j++;
						System.out.println("Variable Name:" + ls.variableName + " variable value:" + client.values.get(ls.variableName));
						System.out.println("Variable Name:" + ls.variableName + " " + ls.comparisonType + ", " + ls.threshold);
					} else {
						if (n == 2 * Level_Order_S.index || n == 2 * Level_Order_S.index + 1) {
							terminalLeafFound = true;
							System.out.println("Terminal leaf:" + ls.variableName);
						}
						j++;
						n+=2;
						System.out.println("Variable:" + ls.variableName);
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}

	}

	public Hashtable<String, String> LevelOrderTreeTraversalGetValues(ClassifierTree root, 
			List<Level_Order_Site> LevelNodes, StringBuffer text, Hashtable<String, String> values) throws Exception {

		if (root==null){
			return null;
		}

		String line;
		Enumeration<String> e = values.keys();
		while (e.hasMoreElements()){
			String key = e.nextElement();
			text.append(key+","+values.get(key)+"\n");
		}
		text.append("\n\n");

		Queue<ClassifierTree> q = new LinkedList<>();
		q.add(root);
		while (!q.isEmpty()) {
			Level_Order_Site Level_Order_S = new Level_Order_Site();
			Level_Order_S.LevelData = new ArrayList<DataInfo>();

			int n = q.size();
			while (n > 0) {

				ClassifierTree p = q.peek();
				q.remove();
				if (p.isLeaf()) {
					text.append(": " + p.getLocalModel().dumpLabel(0, p.getTrainingData()) + "\n");
					DataInfo levelOrderS = new DataInfo();
					levelOrderS.isLeaf=true;
					levelOrderS.variableName = p.getLocalModel().dumpLabel(0, p.getTrainingData());
					Level_Order_S.LevelData.add(levelOrderS);

				} else {
					float threshold=0;
					for (int i = 0; i < p.getSons().length; i++) {
						text.append(p.getLocalModel().leftSide(p.getTrainingData()) +
								p.getLocalModel().rightSide(i, p.getTrainingData()) + "\n");
						String leftSide = p.getLocalModel().leftSide(p.getTrainingData());
						System.out.println("Left side:"+leftSide);
						String rightSide = p.getLocalModel().rightSide(i, p.getTrainingData());

						char[] rightSideChar = rightSide.toCharArray();
						int type = 0;


						char[] rightValue = new char[0];
						if (rightSideChar[1] == '=') {
							type = 1;

							rightValue = new char[rightSideChar.length - 3];
							for (int k = 3; k < rightSideChar.length; k++)
								rightValue[k - 3] = rightSideChar[k];
							String rightValueStr = new String(rightValue);
							if (rightValueStr.equals("other")){
								type = 2;
								threshold=1;
								Enumeration<String> ex = values.keys();
								while (ex.hasMoreElements()){
									String key = ex.nextElement();
									if (values.get(key).equals("other")){
										values.put(key,"1");
									}
								}
							}
						} else if (rightSideChar[1]=='!'){

							type=4;
							if (rightSideChar[2]=='='){
								rightValue = new char[rightSideChar.length - 4];
								for (int k = 4; k < rightSideChar.length; k++)
									rightValue[k - 4] = rightSideChar[k];
								String rightValueStr = new String(rightValue);
								if (rightValueStr.equals("other")){
									type = 4;
									threshold=0;
								}

								Enumeration<String> ex = values.keys();
								while (ex.hasMoreElements()){
									String key = ex.nextElement();
									if (values.get(key).equals("other")){
										values.put(key,"1");
									}
								}
								if ((rightValueStr.equals("t"))||(rightValueStr.equals("f")))
									type = 6;
							}


						}
						else if (rightSideChar[1] == '>') {
							if (rightSideChar[2] == '=') {
								type = 2;
								rightValue = new char[rightSideChar.length - 4];
								for (int k = 4; k < rightSideChar.length; k++)
									rightValue[k - 4] = rightSideChar[k];
							} else {
								type = 3;
								rightValue = new char[rightSideChar.length - 3];
								for (int k = 3; k < rightSideChar.length; k++)
									rightValue[k - 3] = rightSideChar[k];
							}
						} else if (rightSideChar[1] == '<') {
							if (rightSideChar[2] == '=') {
								type = 4;
								rightValue = new char[rightSideChar.length - 4];
								for (int k = 4; k < rightSideChar.length; k++)
									rightValue[k - 4] = rightSideChar[k];
							} else {
								type = 5;
								rightValue = new char[rightSideChar.length - 3];
								for (int k = 3; k < rightSideChar.length; k++)
									rightValue[k - 3] = rightSideChar[k];
							}
						}

						String rightValueStr = new String(rightValue);
						System.out.println(" " + type + ". " + rightValueStr);

						if (!rightValueStr.equals("other")) {
							if (rightValueStr.equals("t")||rightValueStr.equals("yes")) {
								threshold = 1;
							} else if (rightValueStr.equals("f")||rightValueStr.equals("no")) {
								threshold = 0;

							}

							else {
								threshold = Float.parseFloat(rightValueStr);
							}
						}

						DataInfo myInfo = new DataInfo();
						myInfo.variableName=leftSide;
						myInfo.comparisonType = type;
						myInfo.threshold = threshold;
						myInfo.isLeaf = false;
						Level_Order_S.LevelData.add(myInfo);

						q.add(p.getSons()[i]);
					}
				}
				n--;
			}
			LevelNodes.add(Level_Order_S);
			text.append("\n");
		}
		return values;
	}


	public static Hashtable<String, String> read_feature(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		Hashtable<String, String> values = new Hashtable<String, String>();
		String line;
		
		while((line = br.readLine()) != null) {
			String key, value;
			String[] splitted = line.split("\\t");
			key = splitted[0];
			value = splitted[1];
			if (value.equals("t") ||(value.equals("yes"))) {
				value = "1"; 
			}
			if (value.equals("f") || (value.equals("no"))) {
				value = "0";
			}
			values.put(key, value);
		}
		br.close();
		return values;
	}

	public static void main(String [] args) throws HomomorphicException, IllegalArgumentException, UnknownHostException, IOException {
		// ARGUMENTS
		int KEY_SIZE = 1024;
		String server_ip = "127.0.0.1"; // Level-Site 0
		String file_name = "evil.txt";
		ServerSocket receive = null;
		
		// Generate Key Pair
		DGKKeyPairGenerator p = new DGKKeyPairGenerator();
		p.initialize(KEY_SIZE, null);
		KeyPair dgk = p.generateKeyPair();

		PaillierKeyPairGenerator pa = new PaillierKeyPairGenerator();
		p.initialize(KEY_SIZE, null);
		KeyPair paillier = pa.generateKeyPair();
		
		// Send Key to Server, the Server will give the key to each level site.
		// The shortcut would be use to initialization of alice and bob...
		Socket level_site_zero = new Socket(server_ip, 9254);
		bob andrew = new bob(level_site_zero, paillier, dgk);
		
		// Read the Features
		Hashtable<String, String> feature = read_feature(file_name);
		
		// Send ENCRYPTED data to Level-Site 0

		// Receive data from Level-site d
		Socket level_site_d = receive.accept();

	}
}
