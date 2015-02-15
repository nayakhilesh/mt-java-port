package com.github.nayakhilesh;

import com.google.common.base.Function;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultAlignmentParams {

    public static Map<Quartet<Integer, Integer, Integer, Integer>, Double> getDefaultAlignmentParams(String lang1FilePath,
                                                                                                     String lang2FilePath) {

        Map<Quartet<Integer, Integer, Integer, Integer>, Double> alignmentParams = new HashMap<Quartet<Integer, Integer, Integer, Integer>, Double>();
        final Set<Pair<Integer, Integer>> sentenceLengthPairs = new HashSet<Pair<Integer, Integer>>();

        System.out.println("Initializing alignmentParams:");

        Utils.loopThroughFiles(lang1FilePath, lang2FilePath, new Function<Triplet<String, String, Integer>, Void>() {
            @Override
            public Void apply(Triplet<String, String, Integer> triplet) {
                String line1 = triplet.getValue0();
                String line2 = triplet.getValue1();
                sentenceLengthPairs.add(new Pair<Integer, Integer>(line1.split(" ").length, line2.split(" ").length));
                return null;
            }
        });

        int size = sentenceLengthPairs.size();
        int index = 1;
        for (Pair<Integer, Integer> pair : sentenceLengthPairs) {
            int l = pair.getValue0();
            int m = pair.getValue1();
            System.out.printf("\r%3d%%", Math.round((index * 100.0) / size));
            index++;
            for (int j = 0; j <= l; j++) {
                for (int i = 1; i <= m; i++) {
                    alignmentParams.put(new Quartet<Integer, Integer, Integer, Integer>(j, i, l, m), 1.0 / (l + 1));
                }
            }
        }

        System.out.println("Done Initializing alignmentParams");

        System.out.println("number of 4-tuples in alignmentParams:" + alignmentParams.size());

        return alignmentParams;
    }

}