package com.github.nayakhilesh;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TrigramLanguageModel {

    // constants
    public static final String BEFORE_SYMBOL = "*";
    private static final String AFTER_SYMBOL = "STOP";
    private static final double VALIDATION_DATA_FRACTION = 0.05;
    private static final double EPSILON = 10E-6;
    private static final int NUM_PARTITIONS = 4; //determined by method phi

    // state
    private List<Double> lambda1;
    private List<Double> lambda2;
    private List<Double> lambda3;
    private int totalNumWords;
    private Map<Triplet<String, String, String>, Integer> c3;
    private Map<Pair<String, String>, Integer> c2;
    private Map<String, Integer> c1;

    // uses Linear Interpolation with history partitions
    public TrigramLanguageModel() {
        c1 = new HashMap<String, Integer>();
        c2 = new HashMap<Pair<String, String>, Integer>();
        c3 = new HashMap<Triplet<String, String, String>, Integer>();
        totalNumWords = 0;
        lambda1 = new ArrayList<Double>(NUM_PARTITIONS);
        lambda2 = new ArrayList<Double>(NUM_PARTITIONS);
        lambda3 = new ArrayList<Double>(NUM_PARTITIONS);
    }

    private int countLines(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        int lines = 0;
        while (reader.readLine() != null) {
            lines++;
        }
        reader.close();
        return lines;
    }

    public void initialize(String filePath) throws IOException {

        int numLines = countLines(filePath);
        int partitionPoint = (int) (numLines * (1.0 - VALIDATION_DATA_FRACTION));

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;

        // training data
        int lineNumber = 1;
        while ((line = br.readLine()) != null && lineNumber <= partitionPoint) {
            if (lineNumber % 200 == 0) {
                System.out.println("line#:" + lineNumber);
            }

            List<String> words = Arrays.asList(line.split(" "));
            totalNumWords += words.size();
            words.add(0, BEFORE_SYMBOL);
            words.add(0, BEFORE_SYMBOL);
            words.add(AFTER_SYMBOL);

            // sliding window
            for (int i = 0, j = 1, k = 2; k < words.size(); i++, j++, k++) {
                String word1 = words.get(i);
                String word2 = words.get(j);
                String word3 = words.get(k);

                Triplet<String, String, String> Triplet = new Triplet<String, String, String>(word1, word2, word3);
                if (c3.containsKey(Triplet)) {
                    c3.put(Triplet, c3.get(Triplet) + 1);
                } else {
                    c3.put(Triplet, 1);
                }

                Pair<String, String> pair = new Pair<String, String>(word2, word3);
                if (c2.containsKey(pair)) {
                    c2.put(pair, c2.get(pair) + 1);
                } else {
                    c2.put(pair, 1);
                }

                if (c1.containsKey(word3)) {
                    c1.put(word3, c1.get(word3) + 1);
                } else {
                    c1.put(word3, 1);
                }
            }

            lineNumber++;
        }

        Map<Triplet<String, String, String>, Integer> cPrime = new HashMap<Triplet<String, String, String>, Integer>();

        // validation data
        lineNumber = 1;
        while ((line = br.readLine()) != null) {
            if (lineNumber % 200 == 0) {
                System.out.println("line#:" + lineNumber);
            }

            List<String> words = Arrays.asList(line.split(" "));
            totalNumWords += words.size();
            words.add(0, BEFORE_SYMBOL);
            words.add(0, BEFORE_SYMBOL);
            words.add(AFTER_SYMBOL);

            // sliding window
            for (int i = 0, j = 1, k = 2; k < words.size(); i++, j++, k++) {
                String word1 = words.get(i);
                String word2 = words.get(j);
                String word3 = words.get(k);

                Triplet<String, String, String> Triplet = new Triplet<String, String, String>(word1, word2, word3);
                if (cPrime.containsKey(Triplet)) {
                    cPrime.put(Triplet, cPrime.get(Triplet) + 1);
                } else {
                    cPrime.put(Triplet, 1);
                }
            }

        }

        for (int i = 0; i < NUM_PARTITIONS; i++) {
            Triplet<Double, Double, Double> lambdasBucketI = emAlgorithm(cPrime, i);
            lambda1.add(lambdasBucketI.getValue0());
            lambda2.add(lambdasBucketI.getValue1());
            lambda3.add(lambdasBucketI.getValue2());
        }

    }

    private Triplet<Double, Double, Double> emAlgorithm(Map<Triplet<String, String, String>, Integer> cPrime, int bucket) {

        Random random = new Random();
        double lambda1 = random.nextDouble();
        double lambda2 = random.nextDouble();
        double lambda3 = 1 - (lambda1 + lambda2);

        double prevLambda1, prevLambda2, prevLambda3;

        //EM here
        long startEm = System.currentTimeMillis();

        do {

            double count1 = 0.0;
            double count2 = 0.0;
            double count3 = 0.0;

            prevLambda1 = lambda1;
            prevLambda2 = lambda2;
            prevLambda3 = lambda3;

            for (Map.Entry<Triplet<String, String, String>, Integer> entry : cPrime.entrySet()) {

                Triplet<String, String, String> words = entry.getKey();
                String word1 = words.getValue0();
                String word2 = words.getValue1();
                String word3 = words.getValue2();
                if (phi(word1, word2) == bucket) {
                    int numOccurrences = entry.getValue();
                    double part1 = lambda1 * qML(word1, word2, word3);
                    double part2 = lambda2 * qML(word2, word3);
                    double part3 = lambda3 * qML(word3);
                    double denominator = part1 + part2 + part3;
                    if (denominator != 0) {
                        count1 += ((numOccurrences * part1) / denominator);
                        count2 += ((numOccurrences * part2) / denominator);
                        count3 += ((numOccurrences * part3) / denominator);
                    }
                }

            }

            double sum = count1 + count2 + count3;
            lambda1 = count1 / sum;
            lambda2 = count2 / sum;
            lambda3 = count3 / sum;

        } while (!(doublesAreEqual(lambda1, prevLambda1) &&
                doublesAreEqual(lambda2, prevLambda2) &&
                doublesAreEqual(lambda3, prevLambda3)));

        long endEm = System.currentTimeMillis();

        System.out.println("TrigramLanguageModel EM time=" + (endEm - startEm) / 1000.0 + "s");

        return new Triplet<Double, Double, Double>(lambda1, lambda2, lambda3);
    }

    private int phi(String word1, String word2) {
        Pair<String, String> pair = new Pair<String, String>(word1, word2);
        if (c2.containsKey(pair)) {
            switch (c2.get(pair)) {
                case 1:
                case 2:
                    return 1;
                case 3:
                case 4:
                case 5:
                    return 2;
                default:
                    return 3;
            }

        }
        return 0;
    }

    public double estimate(List<String> words) {

        List<String> newWords = new ArrayList<String>();
        newWords.addAll(words);
        newWords.add(0, BEFORE_SYMBOL);
        newWords.add(0, BEFORE_SYMBOL);
        newWords.add(AFTER_SYMBOL);

        double sum = 0.0;
        // sliding window
        for (int i = 0, j = 1, k = 2; k < newWords.size(); i++, j++, k++) {
            String word1 = newWords.get(i);
            String word2 = newWords.get(j);
            String word3 = newWords.get(k);

            sum += Math.log(q(word1, word2, word3));
        }
        return sum;
    }

    private double q(String word1, String word2, String word3) {
        int bucket = phi(word1, word2);
        return (lambda1.get(bucket) * qML(word1, word2, word3)) + (lambda2.get(bucket) * qML(word2, word3)) +
                (lambda3.get(bucket) * qML(word1));
    }

    private boolean doublesAreEqual(double d1, double d2) {
        return Math.abs(d1 - d2) < EPSILON;
    }

    private double qML(String word1, String word2, String word3) {
        Pair<String, String> pair = new Pair<String, String>(word1, word2);
        if (!c2.containsKey(pair)) {
            return 0.0;
        } else {
            Triplet<String, String, String> Triplet = new Triplet<String, String, String>(word1, word2, word3);
            if (c3.containsKey(Triplet)) {
                return ((double) c3.get(Triplet)) / ((double) c2.get(pair));
            } else {
                return 0.0;
            }
        }
    }

    private double qML(String word1, String word2) {
        if (!c1.containsKey(word1)) {
            return 0.0;
        } else {
            Pair<String, String> pair = new Pair<String, String>(word1, word2);
            if (c2.containsKey(pair)) {
                return ((double) c2.get(pair)) / ((double) c1.get(word1));
            } else {
                return 0.0;
            }
        }
    }

    private double qML(String word1) {
        if (totalNumWords == 0) {
            return 0.0;
        } else {
            if (c1.containsKey(word1)) {
                return ((double) c1.get(word1)) / ((double) totalNumWords);
            } else {
                return 0.0;
            }
        }
    }

}