package edu.fiu.adwise.weka.finito.utils;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * A utility class for encoding and decoding labels to and from unique indices.
 * This is useful for machine learning tasks where categorical labels need to be converted to numerical indices.
 * Then the indices can be used in algorithms that require numerical input.
 * In this case, we use encrypted equality check to compare the labels.
 */
public final class LabelEncoder implements Serializable {
    /** A map from labels to their corresponding indices. */
    private final HashMap<String, BigInteger> labelToIndex;

    /** A map from indices to their corresponding labels. */
    private final HashMap<BigInteger, String> indexToLabel;

    /** The current index to assign to the next new label. */
    private int currentIndex;

    /** Constructs a new `LabelEncoder` with empty mappings. */
    public LabelEncoder() {
        labelToIndex = new HashMap<>();
        indexToLabel = new HashMap<>();
        currentIndex = 0;
    }

    /**
     * Encodes a label into a unique index. If the label is not already encoded, it assigns a new index.
     *
     * @param label The label to encode.
     * @return The unique index corresponding to the label.
     */
    public BigInteger encode(String label) {
        if (!labelToIndex.containsKey(label)) {
            labelToIndex.put(label, BigInteger.valueOf(currentIndex));
            indexToLabel.put(BigInteger.valueOf(currentIndex), label);
            currentIndex++;
        }
        return labelToIndex.get(label);
    }

    /**
     * Decodes an index back into its corresponding label.
     *
     * @param index The index to decode.
     * @return The label corresponding to the index.
     * @throws IllegalArgumentException If the index is not found in the encoder.
     */
    public String decode(BigInteger index) {
        if (!indexToLabel.containsKey(index)) {
            throw new IllegalArgumentException("Index not found in LabelEncoder");
        }
        return indexToLabel.get(index);
    }

    /**
     * Retrieves the number of labels currently encoded.
     *
     * @return The number of encoded labels.
     */
    public int size() {
        return currentIndex;
    }
}