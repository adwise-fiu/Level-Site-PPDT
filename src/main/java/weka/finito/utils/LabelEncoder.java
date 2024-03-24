package weka.finito.utils;
import java.io.Serializable;
import java.util.HashMap;

public final class LabelEncoder implements Serializable {
    private HashMap<String, Integer> labelToIndex;
    private HashMap<Integer, String> indexToLabel;
    private int currentIndex;

    public LabelEncoder() {
        labelToIndex = new HashMap<>();
        indexToLabel = new HashMap<>();
        currentIndex = 0;
    }

    public int encode(String label) {
        if (!labelToIndex.containsKey(label)) {
            labelToIndex.put(label, currentIndex);
            indexToLabel.put(currentIndex, label);
            currentIndex++;
        }
        return labelToIndex.get(label);
    }

    public String decode(int index) {
        if (!indexToLabel.containsKey(index)) {
            throw new IllegalArgumentException("Index not found in LabelEncoder");
        }
        return indexToLabel.get(index);
    }

    public int size() {
        return currentIndex;
    }
}