package edu.fiu.adwise.weka.finito.utils;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import edu.fiu.adwise.homomorphic_encryption.misc.HomomorphicException;
import edu.fiu.adwise.homomorphic_encryption.socialistmillionaire.alice;

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

import edu.fiu.adwise.weka.finito.structs.BigIntegers;
import edu.fiu.adwise.weka.finito.structs.NodeInfo;
import edu.fiu.adwise.weka.finito.structs.features;
import edu.fiu.adwise.weka.finito.structs.level_order_site;

/**
 * Utility class providing shared methods for encryption, TLS setup, and network communication.
 * This class includes methods for hashing strings, setting up TLS properties, traversing nodes within a level of a decision tree.
 * I should note, it is in a shared function as this is used for both testing the level-site approach and everything in one server.
 */
public class shared {

    /** Logger for logging messages and errors. */
    private static final Logger logger = LogManager.getLogger(shared.class);

    /** Supported TLS protocols. */
    public static final String[] protocols = new String[]{ "TLSv1.2", "TLSv1.3" };

    /**
     * Converts a string into a positive BigInteger using UTF-8 encoding.
     * Need to enforce it to be positive to fit constraints of homomorphic encryption;
     * since it is 255 bits or so, I can only use Paillier.
     * @param text The input string to hash.
     * @return A positive BigInteger representation of the string.
     */
    public static BigInteger hash_to_big_integer(String text) {
        byte[] hash = text.getBytes(StandardCharsets.UTF_8);
        return new BigInteger(1, hash);
    }

    /**
     * Configures the TLS environment using system properties.
     */
    public static void setup_tls() {
        String keystore = System.getenv("KEYSTORE") != null ? System.getenv("KEYSTORE") : "";
        String password = System.getenv("PASSWORD") != null ? System.getenv("PASSWORD") : "";
        Properties systemProps = System.getProperties();
        systemProps.put("javax.net.ssl.keyStorePassword", password);
        systemProps.put("javax.net.ssl.keyStore", keystore);
        systemProps.put("javax.net.ssl.trustStore", keystore);
        systemProps.put("javax.net.ssl.trustStorePassword", password);
        System.setProperties(systemProps);
    }

    /**
     * Traverses a level-order site to find the appropriate node based on encrypted features.
     * Essentially, traverse every node in the level of the tree, the index will inform you which node to visit.
     * If the node is a classification, return the node.
     * Otherwise, you need to compare the encrypted features with the node's threshold, and update the index for the next level-site.
     * @param level_site_data The level-order site data.
     * @param encrypted_features The encrypted features to compare.
     * @param niu The Alice protocol instance for comparison.
     * @return The matching NodeInfo object.
     * @throws HomomorphicException If an error occurs during homomorphic operations.
     * @throws IOException If an I/O error occurs.
     * @throws ClassNotFoundException If a class is not found during deserialization.
     */
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
        // Level-sites are made of leaves and split the inequality into two nodes,
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

    /**
     * A wrapper for encrypted integer comparison protocol.
     * This will handle which encryption scheme and comparison is to be used.
     *
     * @param ld The NodeInfo containing the threshold.
     * @param comparisonType The type of comparison to perform.
     * @param encrypted_features The encrypted features to compare.
     * @param Niu The Alice protocol instance for comparison.
     * @return True if the comparison holds, false otherwise.
     * @throws ClassNotFoundException If a class is not found during deserialization.
     * @throws HomomorphicException If an error occurs during homomorphic operations.
     * @throws IOException If an I/O error occurs.
     */
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
        logger.info("Using comparison type {}", comparisonType);

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
            // Also, factors in type 6, just need it to the negated result
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

    /**
     * Closes the connection by closing the provided streams and socket.
     *
     * @param oos The ObjectOutputStream to close.
     * @param ois The ObjectInputStream to close.
     * @param client_socket The client socket to close.
     * @throws IOException If an I/O error occurs.
     */
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

    /**
     * Closes the connection for a given client socket.
     *
     * @param client_socket The client socket to close.
     * @throws IOException If an I/O error occurs.
     */
    public static void closeConnection(Socket client_socket) throws IOException {
        closeConnection(null, null, client_socket);
    }

    /**
     * Creates a ValidatingObjectInputStream for a given socket.
     * This restricts the types of objects that can be deserialized to prevent security issues.
     * We restrict the types to only classes needed for the decision tree protocol implemented.
     *
     * @param socket The socket to read from.
     * @return A ValidatingObjectInputStream instance.
     * @throws IOException If an I/O error occurs.
     */
    public static ValidatingObjectInputStream get_ois(Socket socket) throws IOException {
        ValidatingObjectInputStream ois = new ValidatingObjectInputStream(socket.getInputStream());
        ois.accept(
                edu.fiu.adwise.weka.finito.structs.NodeInfo.class,
                edu.fiu.adwise.weka.finito.structs.level_order_site.class,
                edu.fiu.adwise.weka.finito.structs.BigIntegers.class,
                edu.fiu.adwise.weka.finito.structs.features.class,
                edu.fiu.adwise.weka.finito.utils.LabelEncoder.class,
                java.util.HashMap.class,
                java.util.ArrayList.class,
                java.lang.String.class,
                edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey.class,
                edu.fiu.adwise.homomorphic_encryption.dgk.DGKPublicKey.class,
                java.lang.Number.class,
                java.math.BigInteger.class,
                java.lang.Long.class
        );
        ois.accept("[B");
        ois.accept("[L*");
        return ois;
    }

    /**
     * Closes the connection for a given server socket.
     *
     * @param server_socket The server socket to close.
     * @throws IOException If an I/O error occurs.
     */
    public static void closeConnection(ServerSocket server_socket) throws IOException {
        if (server_socket != null) {
            server_socket.close();
        }
    }
}