package weka.finito.utils;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import security.misc.HomomorphicException;
import security.socialistmillionaire.alice;
import weka.finito.structs.BigIntegers;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.features;
import weka.finito.structs.level_order_site;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




public class shared {
    private static final Logger logger = LogManager.getLogger(shared.class);
    public static final String[] protocols = new String[]{ "TLSv1.2", "TLSv1.3"};

    // Need to enforce it to be positive, since it is 255 bits or so, I can only use Paillier
    public static BigInteger hash_to_big_integer(String text) {
        byte [] hash = text.getBytes(StandardCharsets.UTF_8);
        return new BigInteger(1, hash);
    }

    public static void setup_tls() {
        // If you get a null pointer, you forgot to populate environment variables...
        String keystore = System.getenv("KEYSTORE");
        String password = System.getenv("PASSWORD");
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.ssl.keyStorePassword", password);
        systemProps.put("javax.net.ssl.keyStore", keystore);
        systemProps.put("javax.net.ssl.trustStore", keystore);
        systemProps.put("javax.net.ssl.trustStorePassword", password);

        // Enable verbose SSL handshake debugging
        // systemProps.put("javax.net.debug", "ssl,handshake,data");
        System.setProperties(systemProps);
    }

    public static NodeInfo traverse_level(level_order_site level_site_data,
                                         features encrypted_features,
                                         alice niu)
            throws HomomorphicException, IOException, ClassNotFoundException {

        List<NodeInfo> node_level_data = level_site_data.get_node_data();
        boolean terminalLeafFound = false;
        boolean equalsFound = false;
        boolean inequalityHolds;
        int node_level_index = 0;
        int n = 0;
        int next_index = 0;
        NodeInfo ls;
        NodeInfo to_return = null;

        // The n index tells you when you are in scope in regard to level-site
        // Level-sites are made of leaves, and split the inequality into two nodes,
        // so you have a '<=' and '>' node and '=' and '!=' in pairs
        logger.debug("In-scope index should be: {} and {}",
                2 * encrypted_features.get_current_index(), 2 * encrypted_features.get_current_index() + 1);

        while ((!equalsFound) && (!terminalLeafFound)) {
            ls = node_level_data.get(node_level_index);
            logger.debug("j={}", node_level_index);
            logger.debug("n={}", n);
            if (ls.isLeaf()) {
                if (n == 2 * encrypted_features.get_current_index()
                        || n == 2 * encrypted_features.get_current_index() + 1) {
                    logger.debug("Found the leaf at node={} to be used", n);
                    terminalLeafFound = true;
                    to_return = ls;
                }
                n += 2;
            }
            else {
                if ((n == 2 * encrypted_features.get_current_index()
                        || n == 2 * encrypted_features.get_current_index() + 1)) {

                    logger.debug("At node={}, I need to compare", n);
                    logger.debug("I am comparing at node {}", ls);
                    equalsFound = true;

                    // Niu.Protocol2(encrypted_thresh, encrypted_client_value);
                    // encrypted_thresh >= encrypted_client_value
                    // encrypted_client_value <= encrypted_thresh
                    inequalityHolds = compare(ls, ls.comparisonType, encrypted_features, niu);
                    if (inequalityHolds) {
                        if (ls.comparisonType == 4) {
                            logger.debug("[DT-Threshold] <= [VALUES] is TRUE");
                        }
                        else if (ls.comparisonType == 3) {
                            logger.debug("[DT-Threshold] > [VALUES] is TRUE");
                        }
                        else if (ls.comparisonType == 1) {
                            logger.debug("[DT-Threshold] == [VALUES] is TRUE");
                        }
                        else if (ls.comparisonType == 6) {
                            logger.debug("[DT-Threshold] != [VALUES] is TRUE");
                        }
                        encrypted_features.set_next_index(next_index);
                    }
                    else {
                        if (ls.comparisonType == 4) {
                            logger.debug("[DT-Threshold] <= [VALUES] is FALSE");
                        }
                        else if (ls.comparisonType == 3) {
                            logger.debug("[DT-Threshold] > [VALUES] is FALSE");
                        }
                        else if (ls.comparisonType == 1) {
                            logger.debug("[DT-Threshold] == [VALUES] is FALSE");
                        }
                        else if (ls.comparisonType == 6) {
                            logger.debug("[DT-Threshold] != [VALUES] is FALSE");
                        }
                        encrypted_features.set_next_index(next_index + 1);
                    }
                    logger.debug("New index: {}", encrypted_features.get_next_index());
                }
                n++;
                next_index++;
            }
            node_level_index++;
        } // while
        // Update Index and send it down to the next level-site
        encrypted_features.set_current_index(encrypted_features.get_next_index());
        return to_return;
    }

    // Used by level-site and server-site to compare with a client
    public static boolean compare(NodeInfo ld, int comparisonType,
                                  features encrypted_features, alice Niu)
            throws ClassNotFoundException, HomomorphicException, IOException {

        boolean answer;

        BigIntegers encrypted_values = encrypted_features.get_thresholds(ld.variable_name);
        if (encrypted_values == null) {
            throw new RuntimeException(String.format("Seems like the feature %s is not known", ld.variable_name));
        }
        else {
            logger.debug("Parsing the Attribute: {}", ld.variable_name);
        }
        BigInteger encrypted_client_value = null;
        BigInteger encrypted_thresh = null;
        logger.info(String.format("Using comparison type %d", comparisonType));

        // Encrypt the thresh-hold correct
        // Note only types 1, 3, 4, 6 have been known to exist
        if ((comparisonType == 2) || (comparisonType == 5)) {
            encrypted_thresh = ld.getPaillier();
            encrypted_client_value = encrypted_values.integerValuePaillier();
            Niu.writeInt(0);
            Niu.setDGKMode(false);
        }
        else if ((comparisonType == 3) || (comparisonType == 4)) {
            encrypted_thresh = ld.getDGK();
            encrypted_client_value = encrypted_values.integerValueDGK();
            Niu.writeInt(1);
            Niu.setDGKMode(true);
        }
        else if (comparisonType == 1 || comparisonType == 6) {
            encrypted_thresh = ld.getDGK();
            encrypted_client_value = encrypted_values.integerValueDGK();
            Niu.writeInt(2);
            Niu.setDGKMode(true);
        }

        assert encrypted_client_value != null;
        long start_time = System.nanoTime();
        if (comparisonType == 1) {
            logger.info("Using encrypted equals check");
            answer = Niu.encrypted_equals(encrypted_thresh, encrypted_client_value);
        }
        else if (comparisonType == 6) {
            // Also factors in type 6, just need it to the negated result
            logger.info("Using encrypted inequality check");
            answer = Niu.encrypted_equals(encrypted_thresh, encrypted_client_value);
            answer = !answer;
        }
        // only seen type 4 in the wild
        else if ((comparisonType == 4) || (comparisonType == 5)) {
            // Remember, X >= Y is the same as Y <= X
            // encrypted_thresh >= client_value
            // client_value <= encrypted_thresh
            answer = Niu.Protocol2(encrypted_thresh, encrypted_client_value);
        }
        // only seen type 3 in the wild
        else {
            // client_value >= encrypted_thresh
            // encrypted_thresh <= client_value
            answer = Niu.Protocol2(encrypted_client_value, encrypted_thresh);
        }
        long stop_time = System.nanoTime();
        double run_time = (double) (stop_time - start_time);
        run_time = run_time / 1000000;
        logger.info(String.format("Comparison took %f ms\n", run_time));
        return answer;
    }

    public static void closeConnection(ObjectOutputStream oos, 
                    ObjectInputStream ois, Socket client_socket) throws IOException {
        if (oos != null) {
            oos.close();
        }
		if (ois != null) {
            ois.close();
        }
		if (client_socket != null && client_socket.isConnected()) {
			client_socket.close();
		}
	}

    public static void closeConnection(Socket client_socket) throws IOException {
        closeConnection(null, null, client_socket);
    }

    public static ValidatingObjectInputStream get_ois(Socket socket) throws IOException {
        ValidatingObjectInputStream ois = new ValidatingObjectInputStream(socket.getInputStream());
        ois.accept(
                weka.finito.structs.NodeInfo.class,
                weka.finito.structs.level_order_site.class,
                weka.finito.structs.BigIntegers.class,
                weka.finito.structs.features.class,
                weka.finito.utils.LabelEncoder.class,

                java.util.HashMap.class,
                java.util.ArrayList.class,
                java.lang.String.class,
                security.paillier.PaillierPublicKey.class,
                security.dgk.DGKPublicKey.class,

                java.lang.Number.class,
                java.math.BigInteger.class,
                java.lang.Long.class
        );
        ois.accept("[B");
        ois.accept("[L*");
        return ois;
    }

    public static void closeConnection(ServerSocket server_socket) throws IOException {
        if (server_socket != null) {
            server_socket.close();
        }
    }
}
