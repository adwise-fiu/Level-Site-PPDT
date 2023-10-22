package weka.finito.utils;

import security.misc.HomomorphicException;
import security.socialistmillionaire.alice;
import weka.finito.structs.BigIntegers;
import weka.finito.structs.NodeInfo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Properties;

public class shared {

    public static final String[] protocols = new String[]{"TLSv1.3"};
    public static final String[] cipher_suites = new String[]{"TLS_AES_128_GCM_SHA256"};


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
            return Niu.Protocol4(encrypted_thresh, encrypted_client_value);
        }
        else {
            return Niu.Protocol4(encrypted_client_value, encrypted_thresh);
        }
    }
}
