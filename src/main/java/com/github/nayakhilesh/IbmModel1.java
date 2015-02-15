package com.github.nayakhilesh;

import com.google.common.base.Function;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;

public class IbmModel1 {

    private Map<String, Map<String, Double>> translationParams;

    public IbmModel1(String lang1FilePath, String lang2FilePath, int numIterations) {

        translationParams = DefaultTranslationParams.getDefaultTranslationParams(lang1FilePath, lang2FilePath);

        long startEm = System.currentTimeMillis();

        List<Pair<String, String>> temp = new ArrayList<Pair<String, String>>();
        for (Map.Entry<String, Map<String, Double>> entry : translationParams.entrySet()) {
            String word1 = entry.getKey();
            Map<String, Double> map = entry.getValue();
            for (String word2 : map.keySet()) {
                temp.add(new Pair<String, String>(word1, word2));
            }
        }

        System.out.println("also number of lang2|lang1 combinations in translationParams:" + temp.size());

        for (int iter = 0; iter < numIterations; iter++) {

            System.out.println("Starting iteration #" + iter);

            final Map<String, Double> c1 = new HashMap<String, Double>();
            final Map<Pair<String, String>, Double> c2 = new HashMap<Pair<String, String>, Double>();

            Utils.loopThroughFiles(lang1FilePath, lang2FilePath, new Function<Triplet<String, String, Integer>, Void>() {
                @Override
                public Void apply(Triplet<String, String, Integer> triplet) {
                    String line1 = triplet.getValue0();
                    String line2 = triplet.getValue1();

                    for (String word2 : line2.split(" ")) {

                        List<String> words1 = Arrays.asList(line1.split(" "));
                        words1.add(0, Utils.NULL);

                        double denom = 0.0;
                        for (String word1 : words1) {
                            denom += translationParams.get(word1).get(word2);
                        }
                        for (String word1 : words1) {
                            double delta = translationParams.get(word1).get(word2) / denom;
                            Pair<String, String> pair = new Pair<String, String>(word1, word2);
                            if (c2.containsKey(pair)) {
                                c2.put(pair, c2.get(pair) + delta);
                            } else {
                                c2.put(pair, 0.0);
                            }
                            if (c1.containsKey(word1)) {
                                c1.put(word1, c1.get(word1) + delta);
                            } else {
                                c1.put(word1, 0.0);
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

            System.out.println("Finished iteration #" + iter);
        }

        System.out.println("number of lang1 words in translationParams:" + translationParams.size());
        int sum = 0;
        for (Map<String, Double> map : translationParams.values()) {
            sum += map.size();
        }
        System.out.println("number of lang2|lang1 combinations in translationParams:" + sum);

        long endEm = System.currentTimeMillis();

        System.out.println("EM time=" + (endEm - startEm) / 1000.0 + "s");

    }

    public List<Integer> extractAlignments(String line1, String line2) {

        List<String> words1 = Arrays.asList(line1.split(" "));
        words1.add(0, Utils.NULL);
        List<Integer> list = new ArrayList<Integer>();
        for (String word2 : line2.split(" ")) {

            int maxIndex = 0;
            double maxValue = Double.MIN_VALUE;
            for (int index1 = 0; index1 < words1.size(); index1++) {
                String word1 = words1.get(index1);
                double value = translationParams.get(word1).get(word2);
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