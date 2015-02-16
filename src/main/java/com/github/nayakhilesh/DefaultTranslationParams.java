package com.github.nayakhilesh;


import com.google.common.base.Function;
import org.javatuples.Triplet;

import java.util.*;

import static com.github.nayakhilesh.Utils.NULL;
import static com.github.nayakhilesh.Utils.loopThroughFiles;

public class DefaultTranslationParams {

    public static Map<String, Map<String, Double>> getDefaultTranslationParams(String lang1FilePath,
                                                                               String lang2FilePath) {

        Map<String, Set<String>> n = getN(lang1FilePath, lang2FilePath);
        return getUniformlyDistributedTranslationParams(lang1FilePath, lang2FilePath, n);
    }

    private static Map<String, Set<String>> getN(String lang1FilePath, String lang2FilePath) {

        final Map<String, Set<String>> n = new HashMap<>();

        System.out.println("Initializing 'n':");

        loopThroughFiles(lang1FilePath, lang2FilePath, new Function<Triplet<String, String, Integer>, Void>() {

                    @Override
                    public Void apply(Triplet<String, String, Integer> Triplet) {
                        String line1 = Triplet.getValue0();
                        String line2 = Triplet.getValue1();

                        List<String> words1 = Arrays.asList(line1.split(" "));
                        words1.add(0, NULL);

                        for (String word1 : words1) {
                            if (n.containsKey(word1)) {
                                n.get(word1).addAll(Arrays.asList(line2.split(" ")));
                            } else {
                                n.put(word1, new HashSet<>(Arrays.asList(line2.split(" "))));
                            }
                        }
                        return null;
                    }

                }
        );

        System.out.println("Done Initializing 'n'");

        return n;
    }

    private static Map<String, Map<String, Double>> getUniformlyDistributedTranslationParams(String lang1FilePath, String lang2FilePath,
                                                                                             final Map<String, Set<String>> n) {


        final Map<String, Map<String, Double>> translationParams = new HashMap<>();
        final TranslationParamEstimator transParamEst = new TranslationParamEstimator();

        System.out.println("Initializing translationParams:");

        loopThroughFiles(lang1FilePath, lang2FilePath, new Function<Triplet<String, String, Integer>, Void>() {

                    @Override
                    public Void apply(Triplet<String, String, Integer> Triplet) {
                        String line1 = Triplet.getValue0();
                        String line2 = Triplet.getValue1();

                        List<String> words1 = Arrays.asList(line1.split(" "));
                        words1.add(0, NULL);

                        for (String word1 : words1) {
                            for (String word2 : line2.split(" ")) {
                                if (translationParams.containsKey(word1)) {
                                    translationParams.get(word1).put(word2, transParamEst.estimate(word1, n));
                                } else {
                                    Map<String, Double> map = new HashMap<>();
                                    map.put(word2, transParamEst.estimate(word1, n));
                                    translationParams.put(word1, map);
                                }
                            }
                        }
                        return null;
                    }

                }
        );

        System.out.println("Done Initializing translationParams");

        System.out.println("number of lang1 words in translationParams:" + translationParams.size());
        int num = 0;
        for (Map<String, Double> map : translationParams.values()) {
            num += map.size();
        }
        System.out.println("number of lang2|lang1 combinations in translationParams:" + num);

        return translationParams;
    }

    private static class TranslationParamEstimator {

        private Map<String, Double> cache = new HashMap<>();

        public double estimate(String word1,
                               Map<String, Set<String>> n) {
            if (cache.containsKey(word1)) {
                return cache.get(word1);
            } else {
                double value = 1.0 / n.get(word1).size();
                cache.put(word1, value);
                return value;
            }
        }
    }

}