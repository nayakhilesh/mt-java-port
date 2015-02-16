package com.github.nayakhilesh;


import com.google.common.base.Function;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;

import java.util.*;

public class IbmModel2 {

    private Map<String, Map<String, Double>> translationParams;
    private Map<Quartet<Integer, Integer, Integer, Integer>, Double> alignmentParams;

    public IbmModel2(String lang1FilePath, String lang2FilePath,
                     int numIterations, Map<String, Map<String, Double>> initialTranslationParams) {

        this.translationParams = initialTranslationParams;
        this.alignmentParams = DefaultAlignmentParams.getDefaultAlignmentParams(lang1FilePath, lang2FilePath);

        long startEm = System.currentTimeMillis();
        List<Pair<String, String>> temp = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : translationParams.entrySet()) {
            String word1 = entry.getKey();
            Map<String, Double> map = entry.getValue();
            for (String word2 : map.keySet()) {
                temp.add(new Pair<>(word1, word2));
            }
        }

        Set<Quartet<Integer, Integer, Integer, Integer>> tempAlign = alignmentParams.keySet();

        System.out.println("also number of lang2|lang1 combinations in translationParams:" + temp.size());
        System.out.println("also number of 4-tuples in alignmentParams:" + tempAlign.size());

        for (int iter = 0; iter < numIterations; iter++) {

            System.out.println("Starting iteration #" + iter);

            final Map<String, Double> c1 = new HashMap<>();
            final Map<Pair<String, String>, Double> c2 = new HashMap<>();
            final Map<Triplet<Integer, Integer, Integer>, Double> c3 = new HashMap<>();
            final Map<Quartet<Integer, Integer, Integer, Integer>, Double> c4 = new HashMap<>();

            Utils.loopThroughFiles(lang1FilePath, lang2FilePath, new Function<Triplet<String, String, Integer>, Void>() {
                @Override
                public Void apply(Triplet<String, String, Integer> triplet) {

                    String line1 = triplet.getValue0();
                    String line2 = triplet.getValue1();

                    List<String> words1 = Arrays.asList(line1.split(" "));
                    int size1 = words1.size();
                    List<String> nullPrefixedWords1 = new ArrayList<>();
                    nullPrefixedWords1.add(Utils.NULL);
                    nullPrefixedWords1.addAll(words1);

                    List<String> words2 = Arrays.asList(line2.split(" "));
                    int size2 = words2.size();

                    for (int index2 = 0; index2 < words2.size(); index2++) {
                        String word2 = words2.get(index2);

                        double denom = 0.0;
                        for (int index1 = 0; index1 < nullPrefixedWords1.size(); index1++) {
                            String word1 = nullPrefixedWords1.get(index1);
                            denom += alignmentParams.get(new Quartet<>(index1, index2 + 1, size1, size2)) *
                                    translationParams.get(word1).get(word2);
                        }
                        for (int index1 = 0; index1 < nullPrefixedWords1.size(); index1++) {
                            String word1 = nullPrefixedWords1.get(index1);
                            Quartet<Integer, Integer, Integer, Integer> quartet = new Quartet<>(index1, index2 + 1, size1, size2);
                            double delta = (alignmentParams.get(quartet) *
                                    translationParams.get(word1).get(word2)) / denom;
                            Pair<String, String> pair = new Pair<>(word1, word2);
                            if (c2.containsKey(pair)) {
                                c2.put(pair, c2.get(pair) + delta);
                            } else {
                                c2.put(pair, delta);
                            }
                            if (c1.containsKey(word1)) {
                                c1.put(word1, c1.get(word1) + delta);
                            } else {
                                c1.put(word1, delta);
                            }
                            if (c4.containsKey(quartet)) {
                                c4.put(quartet, c4.get(quartet) + delta);
                            } else {
                                c4.put(quartet, delta);
                            }
                            Triplet<Integer, Integer, Integer> triplet1 = new Triplet<>(index2 + 1, size1, size2);
                            if (c3.containsKey(triplet1)) {
                                c3.put(triplet1, c3.get(triplet1) + delta);
                            } else {
                                c3.put(triplet1, delta);
                            }
                        }
                    }

                    return null;
                }
            });

            for (Pair<String, String> pair : temp) {
                String word1 = pair.getValue0();
                String word2 = pair.getValue1();
                translationParams.get(word1).put(word2, c2.get(pair) / c1.get(word1));
            }

            for (Quartet<Integer, Integer, Integer, Integer> quartet : tempAlign) {
                int index2 = quartet.getValue1();
                int size1 = quartet.getValue2();
                int size2 = quartet.getValue3();
                Triplet<Integer, Integer, Integer> triplet = new Triplet<>(index2, size1, size2);
                alignmentParams.put(quartet, c4.get(quartet) / c3.get(triplet));
            }

            System.out.println("Finished iteration #" + iter);
        }

        System.out.println("number of lang1 words in translationParams:" + translationParams.size());
        int sum = 0;
        for (Map<String, Double> map : translationParams.values()) {
            sum += map.size();
        }
        System.out.println("number of lang2|lang1 combinations in translationParams:" + sum);
        System.out.println("number of 4-tuples in alignmentParams:" + alignmentParams.size());

        long endEm = System.currentTimeMillis();

        System.out.println("EM time=" + (endEm - startEm) / 1000.0 + "s");
    }

    public List<Integer> extractAlignments(String line1, String line2) {

        List<String> words2 = Arrays.asList(line2.split(" "));
        int size2 = words2.size();

        List<String> words1 = Arrays.asList(line1.split(" "));
        int size1 = words1.size();

        List<String> nullPrefixedWords1 = new ArrayList<>();
        nullPrefixedWords1.add(Utils.NULL);
        nullPrefixedWords1.addAll(words1);

        List<Integer> list = new ArrayList<>();
        for (int index2 = 0; index2 < words2.size(); index2++) {
            String word2 = words2.get(index2);

            int maxIndex = 0;
            double maxValue = Double.MIN_VALUE;
            for (int index1 = 0; index1 < nullPrefixedWords1.size(); index1++) {
                String word1 = nullPrefixedWords1.get(index1);
                Quartet<Integer, Integer, Integer, Integer> quartet = new Quartet<>(index1, index2 + 1, size1, size2);
                double value = alignmentParams.get(quartet) * translationParams.get(word1).get(word2);
                if (value > maxValue) {
                    maxIndex = index1;
                    maxValue = value;
                }
            }
            list.add(maxIndex);
        }

        return list;
    }

}