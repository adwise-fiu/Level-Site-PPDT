package edu.fiu.adwise.weka.finito.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.*;

/**
 * Utility class for analyzing the depth of the classification each instance in a decision tree from the training data.
 * This analysis is useful, since we are assuming testing data will be similar to the training data.
 * It stands to reason that we'd want to know how deep the classification is, for each instance in the training data, to
 * eventually justify using our approach in evaluating a decision tree.
 */
public class depth_analysis {

    /** Logger for logging messages and errors. */
    private static final Logger logger = LogManager.getLogger(depth_analysis.class);

    /**
     * Retrieves the leaf node classification for a given instance in the decision tree.
     * If you are curious, the numbers in parentheses can obtain as follows
     * tree.getLocalModel().distribution().numIncorrect() + tree.getLocalModel().distribution().numCorrect();
     * tree.getLocalModel().distribution().numIncorrect();
     *
     * @param tree The decision tree to traverse.
     * @param feature The instance to classify.
     * @return The classification label of the leaf node.
     * @throws Exception If an error occurs during tree traversal.
     */
    public static String getLeaf(ClassifierTree tree, Instance feature) throws Exception {
        if (tree.isLeaf()) {
            return tree.getLocalModel().dumpLabel(0, tree.getTrainingData());
        } else {
            String attribute_name = tree.getLocalModel().leftSide(tree.getTrainingData()).strip();
            String rightSide = tree.getLocalModel().rightSide(0, tree.getTrainingData()).strip();
            String[] comparison = rightSide.split(" ");
            String operation = comparison[0];
            String tree_value_string = comparison[1];

            double value = Float.NEGATIVE_INFINITY;
            Attribute x = null;
            for (int i = 0; i < feature.numAttributes(); i++) {
                Attribute attr = feature.attribute(i);
                if (attr.name().equals(attribute_name)) {
                    value = feature.value(i);
                    x = attr;
                }
            }

            // if true, go leaf, otherwise go right
            if (operation.equals("=")) {
                // String comparisons only
                if (tree_value_string.equals(feature.stringValue(x))) {
                    return getLeaf(tree.getSons()[0], feature);
                } else {
                    return getLeaf(tree.getSons()[1], feature);
                }
            } else if (operation.equals("<=")) {
                // Numbers only
                if (value <= Double.parseDouble(tree_value_string)) {
                    return getLeaf(tree.getSons()[0], feature);
                } else {
                    return getLeaf(tree.getSons()[1], feature);
                }
            } else {
                System.out.println("IMPOSSIBLE!!!");
                return null;
            }
        }
    }

    /**
     * Calculates the depth of a specific leaf node in the decision tree.
     *
     * @param tree The decision tree to traverse.
     * @param leaf The classification label of the leaf node.
     * @param depth The current depth during traversal.
     * @return The depth of the specified leaf node, or -1 if not found.
     * @throws Exception If an error occurs during tree traversal.
     */
    public static int getDepth(ClassifierTree tree, String leaf, int depth) throws Exception {
        if (tree.isLeaf()) {
            String full_leaf = tree.getLocalModel().dumpLabel(0, tree.getTrainingData());
            if (full_leaf.equals(leaf)) {
                return depth;
            } else {
                return -1;
            }
        } else {
            for (int i = 0; i < tree.getSons().length; i++) {
                // Determine which type of comparison is occurring.
                if (getDepth(tree.getSons()[i], leaf, depth) != -1) {
                    return getDepth(tree.getSons()[i], leaf, depth + 1);
                }
            }
        }
        return -1;
    }

    /**
     * Main method for analyzing the depth of decision trees and writing results to a CSV file.
     *
     * @param args Command-line arguments, where the first argument is the dataset name.
     * @throws Exception If an error occurs during processing.
     */
    public static void main(String[] args) throws Exception {
        String data_set = args[0];
        String model_file = new File("data", data_set + ".model").toString();
        String arff_file = new File("data", data_set + ".arff").toString();

        ClassifierTree tree = (ClassifierTree) SerializationHelper.read(model_file);
        Instances data = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(arff_file))) {
            data = new Instances(reader);
        } catch (IOException e2) {
            logger.fatal("The Training data set was NOT found or not readable at: {}", arff_file);
            logger.fatal(e2.getStackTrace());
        }

        assert data != null;
        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }

        try (FileWriter fileWriter = new FileWriter(data_set + ".csv", true);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            for (Instance instance : data) {
                String classification = getLeaf(tree, instance);
                int depth = getDepth(tree, classification, 1);
                printWriter.println(classification + "," + depth);
            }

        } catch (IOException e) {
            logger.fatal("Unable to write to CSV file");
            logger.fatal(e.getStackTrace());
        }
    }
}