package edu.fiu.adwise.weka.finito.utils;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;

public final class LabelEncoder implements Serializable {
    private final HashMap<String, BigInteger> labelToIndex;
    private final HashMap<BigInteger, String> indexToLabel;
    private int currentIndex;

    public LabelEncoder() {
        labelToIndex = new HashMap<>();
        indexToLabel = new HashMap<>();
        currentIndex = 0;
    }

    public BigInteger encode(String label) {
        if (!labelToIndex.containsKey(label)) {
            labelToIndex.put(label, BigInteger.valueOf(currentIndex));
            indexToLabel.put(BigInteger.valueOf(currentIndex), label);
            currentIndex++;
        }
        return labelToIndex.get(label);
    }

    public String decode(BigInteger index) {
        if (!indexToLabel.containsKey(index)) {
            throw new IllegalArgumentException("Index not found in LabelEncoder");
        }
        return indexToLabel.get(index);
    }

    public int size() {
        return currentIndex;
    }
}