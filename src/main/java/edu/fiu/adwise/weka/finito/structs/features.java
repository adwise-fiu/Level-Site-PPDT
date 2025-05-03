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


public final class features implements Serializable {
    // private static final Logger logger = LogManager.getLogger(features.class);
    @Serial
    private static final long serialVersionUID = 6000706455545108960L;
    private String client_ip;
    private int client_port;
    private int next_index;
    private int current_index;
    private final HashMap<String, BigIntegers> thresholds;
    private final String thresh_hold_map;

    public features(String path, int precision, PaillierPublicKey paillier_public_key,
                    DGKPublicKey dgk_public_key, LabelEncoder encoder)
            throws HomomorphicException, IOException {
        this.client_ip = "";
        this.next_index = 0;
        this.current_index = 0;
        this.thresholds = read_values(path, precision, paillier_public_key, dgk_public_key, encoder);
        this.thresh_hold_map = save_thresholds(path, precision, encoder);
    }

    public BigIntegers get_thresholds(String feature) {
        return this.thresholds.get(feature);
    }

    public String get_client_ip() {
        return this.client_ip;
    }

    public void set_client_ip(String client_ip) {
        this.client_ip = client_ip;
    }

    public int get_client_port() {
        return this.client_port;
    }

    public void set_client_port(int client_port) {
        this.client_port = client_port;
    }

    public int get_next_index() {
        return this.next_index;
    }

    public void set_next_index(int next_index) {
        this.next_index = next_index;
    }

    public int get_current_index() {
        return this.current_index;
    }

    public void set_current_index(int current_index) {
        this.current_index = current_index;
    }

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

    public String toString() {
        return thresh_hold_map;
    }
}
