package com.github.nayakhilesh;

import java.util.Map;

public class IbmModel1 {

    private Map<String, Map<String, Double>> translationParams;

   public IbmModel1(String lang1FilePath, String lang2FilePath, int numIterations) {

        translationParams = DefaultTranslationParams.getDefaultTranslationParams(lang1FilePath, lang2FilePath);

        long startEm = System.currentTimeMillis();

        val temp = translationParams.toSeq.flatMap {
        case (word1, map) =>
        map.foldLeft(List[(String, String)]()) {
        case (list, (word2, _)) => (word1, word2) +: list
        }
        }

        System.out.println("also number of lang2|lang1 combinations in translationParams:" + temp.size)

        1 to numIterations foreach {
        iter =>
        System.out.println("Starting iteration #" + iter)

        val c1 = collection.mutable.Map[String, Double]()
        val c2 = collection.mutable.Map[(String, String), Double]()

        loopThroughFiles(lang1FilePath, lang2FilePath) {
        (line1, line2, index) =>
        line2 split " " foreach {
        word2 =>

        val denom = (NULL +: (line1 split " ")).foldLeft(0.0)((acc, word1) => acc + translationParams(word1)(word2))
        NULL +: (line1 split " ") foreach {
        word1 =>
        val delta = translationParams(word1)(word2) / denom
        c2((word1, word2)) = c2.getOrElse((word1, word2), 0.0) + delta
        c1(word1) = c1.getOrElse(word1, 0.0) + delta
        }

        }
        }

        temp foreach {
        case (word1, word2) =>
        translationParams(word1)(word2) = c2((word1, word2)) / c1(word1)
        }

        System.out.println("Finished iteration #" + iter)
        }

        System.out.println("number of lang1 words in translationParams:" + translationParams.size)
        System.out.println("number of lang2|lang1 combinations in translationParams:" +
        translationParams.foldLeft(0) {
        case (acc, (_, map)) => acc + map.size
        })

        long endEm = System.currentTimeMillis();

        System.out.println("EM time=" + (endEm - startEm) / 1000.0 + "s");

        translationParams.toMap
        }


        }

  public List<Integer> extractAlignments(String line1, String line2) {
/*
    List<Integer> list = new ArrayList<Integer>();
    for (String s : line2.split(" ")) {

        }
        */
    line2 split " " foreach {
      word2 =>

        val (_, maxIndex) = ((NULL +: (line1 split " ")).iterator.zipWithIndex).maxBy {
          case (word1, index1) => translationParams(word1)(word2)
        }
        list += maxIndex

    }

    list.toVector
  }

}