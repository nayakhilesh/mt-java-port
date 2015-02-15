package com.github.nayakhilesh;

import com.google.common.base.Function;
import org.javatuples.Triplet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Utils {

    /*
type TranslationParameters = collection.immutable.Map[String, collection.mutable.Map[String, Double]]
type AlignmentParameters = collection.immutable.Map[(Int, Int, Int, Int), Double]

type MutableTranslationParameters = collection.mutable.Map[String, collection.mutable.Map[String, Double]]
type MutableAlignmentParameters = collection.mutable.Map[(Int, Int, Int, Int), Double]
*/

    public static final String NULL = "NULL";

    public static void loopThroughFiles(String file1Path, String file2Path, Function<Triplet<String, String, Integer>, Void> funcToPerform) {

        try {
            BufferedReader br1 = new BufferedReader(new FileReader(file1Path));
            BufferedReader br2 = new BufferedReader(new FileReader(file2Path));

            String line1, line2;
            int lineNumber = 1;
            while ((line1 = br1.readLine()) != null && (line2 = br2.readLine()) != null) {
                if (lineNumber % 200 == 0) {
                    System.out.println("line#:" + lineNumber);
                }
                if (!line1.trim().isEmpty() && !line2.trim().isEmpty()) {
                    funcToPerform.apply(new Triplet<String, String, Integer>(line1, line2, lineNumber));
                }
                lineNumber++;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

}
