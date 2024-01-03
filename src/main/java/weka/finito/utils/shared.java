package weka.finito.utils;

import security.misc.HomomorphicException;
import security.socialistmillionaire.alice;
import weka.finito.structs.BigIntegers;
import weka.finito.structs.NodeInfo;
import weka.finito.structs.level_order_site;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public class shared {

    public static final String[] protocols = new String[]{ "TLSv1.2", "TLSv1.3"};
    public static final String[] cipher_suites = new String[] {
            "TLS_AES_128_GCM_SHA256",

            // Done with bleeding edge, back to TLS v1.2 and below
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",

            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",

            // RSA key transport sucks, but they are needed as a fallback.
            // For example, microsoft.com fails under all versions of TLS
            // if they are not included. If only TLS 1.0 is available in
            // the client, then google.com will fail too. TLS v1.3 is
            // trying to deprecate them, so it will be interesting to see
            // what happens.
            "TLS_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_128_CBC_SHA"
    };


    // Used by server-site to hash leaves and client-site to find the leaf
    public static String hash(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
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
        System.setProperties(systemProps);
    }

    public static NodeInfo traverse_level(level_order_site level_site_data,
                                         Hashtable<String, BigIntegers> encrypted_features,
                                         ObjectOutputStream toClient, alice niu)
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

        while ((!equalsFound) && (!terminalLeafFound)) {
            ls = node_level_data.get(node_level_index);
            System.out.println("j=" + node_level_index);
            if (ls.isLeaf()) {
                if (n == 2 * level_site_data.get_current_index()
                        || n == 2 * level_site_data.get_current_index() + 1) {
                    terminalLeafFound = true;
                    to_return = ls;
                }
                n += 2;
            }
            else {
                if ((n == 2 * level_site_data.get_current_index()
                        || n == 2 * level_site_data.get_current_index() + 1)) {

                    if (ls.comparisonType == 6) {
                        inequalityHolds = compare(ls, 1,
                                encrypted_features, toClient, niu);
                        inequalityHolds = !inequalityHolds;
                    }
                    else {
                        inequalityHolds = compare(ls, ls.comparisonType,
                                encrypted_features, toClient, niu);
                    }

                    if (inequalityHolds) {
                        equalsFound = true;
                        level_site_data.set_next_index(next_index);
                        System.out.println("New index: " + level_site_data.get_next_index());
                    }
                }
                n++;
                next_index++;
            }
            node_level_index++;
        }
        return to_return;
    }

    // Used by level-site and server-site to compare with a client
    public static boolean compare(NodeInfo ld, int comparisonType,
                            Hashtable<String, BigIntegers> encrypted_features,
                            ObjectOutputStream toClient, alice Niu)
            throws ClassNotFoundException, HomomorphicException, IOException {

        long start_time = System.nanoTime();

        BigIntegers encrypted_values = encrypted_features.get(ld.variable_name);
        BigInteger encrypted_client_value = null;
        BigInteger encrypted_thresh = null;

        // Encrypt the thresh-hold correctly
        if ((comparisonType == 1) || (comparisonType == 2) || (comparisonType == 4)) {
            encrypted_thresh = ld.getPaillier();
            encrypted_client_value = encrypted_values.getIntegerValuePaillier();
            toClient.writeInt(0);
            Niu.setDGKMode(false);
        }
        else if ((comparisonType == 3) || (comparisonType == 5)) {
            encrypted_thresh = ld.getDGK();
            encrypted_client_value = encrypted_values.getIntegerValueDGK();
            toClient.writeInt(1);
            Niu.setDGKMode(true);
        }
        toClient.flush();
        assert encrypted_client_value != null;
        long stop_time = System.nanoTime();

        double run_time = (double) (stop_time - start_time);
        run_time = run_time / 1000000;
        System.out.printf("Comparison took %f ms\n", run_time);
        if (((comparisonType == 1) && (ld.threshold == 0))
                || (comparisonType == 4) || (comparisonType == 5)) {
            return Niu.Protocol2(encrypted_thresh, encrypted_client_value);
        }
        else {
            return Niu.Protocol2(encrypted_client_value, encrypted_thresh);
        }
    }
}
