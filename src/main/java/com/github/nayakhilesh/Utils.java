package com.github.nayakhilesh;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import org.javatuples.Triplet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Utils {

    public static final String NULL = "NULL";

    public static void loopThroughFiles(String file1Path, String file2Path, Function<Triplet<String, String, Integer>, Void> funcToPerform) {

        try (BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream(file1Path), Charsets.UTF_8));
             BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream(file2Path), Charsets.UTF_8))) {

            String line1, line2;
            int lineNumber = 1;
            while ((line1 = br1.readLine()) != null && (line2 = br2.readLine()) != null) {
                if (lineNumber % 200 == 0) {
                    System.out.println("line#:" + lineNumber);
                }
                if (!line1.trim().isEmpty() && !line2.trim().isEmpty()) {
                    funcToPerform.apply(new Triplet<>(line1, line2, lineNumber));
                }
                lineNumber++;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    public static List<String> splitAndPrefix(String line, String... prefixes) {
        List<String> words = new ArrayList<>();
        words.addAll(Arrays.asList(prefixes));
        words.addAll(Arrays.asList(line.split(" ")));
        return words;
    }

    public static <A> void mapPutOrAdd(Map<A, Double> map, A key, double value) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + value);
        } else {
            map.put(key, value);
        }
    }

    public static <A> void mapIncrementKey(Map<A, Integer> map, A key) {
        if (map.containsKey(key)) {
            map.put(key, map.get(key) + 1);
        } else {
            map.put(key, 1);
        }
    }

}
