package edu.fiu.adwise.weka.finito.structs;

import edu.fiu.adwise.homomorphic_encryption.dgk.DGKOperations;
import edu.fiu.adwise.homomorphic_encryption.dgk.DGKPublicKey;
import edu.fiu.adwise.homomorphic_encryption.misc.HomomorphicException;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierCipher;
import edu.fiu.adwise.homomorphic_encryption.paillier.PaillierPublicKey;

import java.io.*;
import java.math.BigInteger;
import java.util.HashMap;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import edu.fiu.adwise.weka.finito.utils.LabelEncoder;

/**
 * A class representing features with encrypted thresholds for homomorphic encryption.
 * This class is immutable and serializable.
 */
public final class features implements Serializable {
    // private static final Logger logger = LogManager.getLogger(features.class);
    /** Serial version UID for ensuring compatibility during serialization. */
    @Serial
    private static final long serialVersionUID = 6000706455545108960L;

    /** The IP address of the client. */
    private String client_ip;

    /** The port number of the client. */
    private int client_port;

    /** The next index to be processed. */
    private int next_index;

    /** The current index being processed */
    private int current_index;

    /** A map containing encrypted thresholds for features. */
    private final HashMap<String, BigIntegers> thresholds;

    /** A string representation of the thresholds for debugging purposes. */
    private final String thresh_hold_map;

    /**
     * Constructs a `features` object by reading and encrypting threshold values from a file.
     *
     * @param path               The file path containing feature thresholds.
     * @param precision          The precision for threshold values.
     * @param paillier_public_key The Paillier public key for encryption.
     * @param dgk_public_key     The DGK public key for encryption.
     * @param encoder            The label encoder for encoding non-numeric values.
     * @throws HomomorphicException If an error occurs during encryption.
     * @throws IOException          If an error occurs while reading the file.
     */
    public features(String path, int precision, PaillierPublicKey paillier_public_key,
                    DGKPublicKey dgk_public_key, LabelEncoder encoder)
            throws HomomorphicException, IOException {
        this.client_ip = "";
        this.next_index = 0;
        this.current_index = 0;
        this.thresholds = read_values(path, precision, paillier_public_key, dgk_public_key, encoder);
        this.thresh_hold_map = save_thresholds(path, precision, encoder);
    }

    /**
     * Retrieves the encrypted thresholds for a given feature.
     *
     * @param feature The feature name.
     * @return The encrypted thresholds as a `BigIntegers` object.
     */
    public BigIntegers get_thresholds(String feature) {
        return this.thresholds.get(feature);
    }

    /**
     * Retrieves the client IP address.
     *
     * @return The client IP address as a string.
     */
    public String get_client_ip() {
        return this.client_ip;
    }

    /**
     * Sets the client IP address.
     *
     * @param client_ip The client IP address to set.
     */
    public void set_client_ip(String client_ip) {
        this.client_ip = client_ip;
    }

    /**
     * Retrieves the client port number.
     *
     * @return The client port number.
     */
    public int get_client_port() {
        return this.client_port;
    }

    /**
     * Sets the client port number.
     *
     * @param client_port The client port number to set.
     */
    public void set_client_port(int client_port) {
        this.client_port = client_port;
    }

    /**
     * Retrieves the next index value.
     *
     * @return The next index value.
     */
    public int get_next_index() {
        return this.next_index;
    }

    /**
     * Sets the next index value.
     *
     * @param next_index The next index value to set.
     */
    public void set_next_index(int next_index) {
        this.next_index = next_index;
    }

    /**
     * Retrieves the current index value.
     *
     * @return The current index value.
     */
    public int get_current_index() {
        return this.current_index;
    }

    /**
     * Sets the current index value.
     *
     * @param current_index The current index value to set.
     */
    public void set_current_index(int current_index) {
        this.current_index = current_index;
    }

    /**
     * Saves the thresholds to a string representation for debugging purposes.
     *
     * @param path      The file path containing feature thresholds.
     * @param precision The precision for threshold values.
     * @param encoder   The label encoder for encoding non-numeric values.
     * @return A string representation of the thresholds.
     * @throws IOException If an error occurs while reading the file.
     */
    public static String save_thresholds(String path, int precision, LabelEncoder encoder) throws IOException {
        double double_value;
        BigInteger temp;
        StringBuilder debug = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                String key, value;
                String[] split = line.split("\\t");
                key = split[0];
                value = split[1];

                // I need to refer to the label encoder after training to know what I am doing...
                try {
                    double_value = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    double_value = encoder.encode(value).doubleValue();
                }
                temp = NodeInfo.set_precision(double_value, precision);
                debug.append(key).append(" -> ").append(temp).append("\n");
            }
        }
        return debug.toString();
    }


    /**
     * Reads and encrypts threshold values from a file.
     *
     * @param path               The file path containing feature thresholds.
     * @param precision          The precision for threshold values.
     * @param paillier_public_key The Paillier public key for encryption.
     * @param dgk_public_key     The DGK public key for encryption.
     * @param encoder            The label encoder for encoding non-numeric values.
     * @return A map of feature names to their encrypted thresholds.
     * @throws IOException          If an error occurs while reading the file.
     * @throws HomomorphicException If an error occurs during encryption.
     */
    public static HashMap<String, BigIntegers> read_values(String path,
                                                           int precision,
                                                           PaillierPublicKey paillier_public_key,
                                                           DGKPublicKey dgk_public_key,
                                                           LabelEncoder encoder)
            throws IOException, HomomorphicException {

        BigInteger integerValuePaillier;
        BigInteger integerValueDGK;
        BigInteger temp;
        HashMap<String, BigIntegers> values = new HashMap<>();
        double double_value;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                String key, value;
                String[] split = line.split("\\t");
                key = split[0];
                value = split[1];

                // I need to refer to the label encoder after training to know what I am doing...
                try {
                    double_value = Double.parseDouble(value);
                }
                catch (NumberFormatException e) {
                    double_value = encoder.encode(value).doubleValue();
                }
                temp = NodeInfo.set_precision(double_value, precision);
                integerValuePaillier = PaillierCipher.encrypt(temp, paillier_public_key);
                integerValueDGK = DGKOperations.encrypt(temp, dgk_public_key);
                values.put(key, new BigIntegers(integerValuePaillier, integerValueDGK));
            }
        }
        return values;
    }

    /**
     * Returns a string representation of the thresholds.
     *
     * @return A string representation of the thresholds.
     */
    public String toString() {
        return thresh_hold_map;
    }
}
