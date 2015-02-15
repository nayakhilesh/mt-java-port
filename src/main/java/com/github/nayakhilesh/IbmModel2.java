package com.github.nayakhilesh;

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

import scala.Array.canBuildFrom
import scala.compat.Platform
import scala.io.Source

import com.github.nayakhilesh;.Utils._
import scala.collection.immutable

object IbmModel2 {

  def apply(lang1FilePath: String, lang2FilePath: String, numIterations: Int,
            initialTranslationParams: MutableTranslationParameters = null,
            initialAlignmentParams: MutableAlignmentParameters = null) =
    new IbmModel2(computeParams(lang1FilePath, lang2FilePath, numIterations, initialTranslationParams, initialAlignmentParams))

  def apply(filePath: String) =
    new IbmModel2(readParams(filePath))

  private[this] def computeParams(lang1FilePath: String, lang2FilePath: String, numIterations: Int,
                                  initialTranslationParams: MutableTranslationParameters,
                                  initialAlignmentParams: MutableAlignmentParameters) = {

    val translationParams =
      if (initialTranslationParams == null)
        DefaultTranslationParams.getDefaultTranslationParams(lang1FilePath, lang2FilePath)
      else
        initialTranslationParams

    val alignmentParams =
      if (initialAlignmentParams == null)
        DefaultAlignmentParams.getDefaultAlignmentParams(lang1FilePath, lang2FilePath)
      else
        initialAlignmentParams

    val startEm = System.currentTimeMillis();

    val temp = translationParams.toSeq.flatMap {
      case (word1, map) =>
        map.foldLeft(List[(String, String)]()) {
          case (list, (word2, _)) => (word1, word2) +: list
        }
    }

    val tempAlign = alignmentParams.keys

    System.out.println("also number of lang2|lang1 combinations in translationParams:" + temp.size)
    System.out.println("also number of 4-tuples in alignmentParams:" + tempAlign.size)

    1 to numIterations foreach {
      iter =>
        System.out.println("Starting iteration #" + iter)

        val c1 = collection.mutable.Map[String, Double]()
        val c2 = collection.mutable.Map[(String, String), Double]()
        val c3 = collection.mutable.Map[(Int, Int, Int), Double]()
        val c4 = collection.mutable.Map[(Int, Int, Int, Int), Double]()

        loopThroughFiles(lang1FilePath, lang2FilePath) {
          (line1, line2, index) =>

            val arr1 = line1 split " "
            val size1 = arr1.size
            val nullPrefixedArr1 = NULL +: arr1

            val arr2 = line2 split " "
            val size2 = arr2.size

            arr2.iterator.zipWithIndex foreach {
              case (word2, index2) =>

                val denom = nullPrefixedArr1.iterator.zipWithIndex.foldLeft(0.0) {
                  case (acc, (word1, index1)) =>
                    acc + (alignmentParams((index1, index2 + 1, size1, size2)) * translationParams(word1)(word2))
                }
                nullPrefixedArr1.iterator.zipWithIndex foreach {
                  case (word1, index1) =>
                    val delta = (alignmentParams((index1, index2 + 1, size1, size2)) * translationParams(word1)(word2)) / denom
                    c2((word1, word2)) = c2.getOrElse((word1, word2), 0.0) + delta
                    c1(word1) = c1.getOrElse(word1, 0.0) + delta
                    c4((index1, index2 + 1, size1, size2)) = c4.getOrElse((index1, index2 + 1, size1, size2), 0.0) + delta
                    c3((index2 + 1, size1, size2)) = c3.getOrElse((index2 + 1, size1, size2), 0.0) + delta
                }

            }

        }

        temp foreach {
          case (word1, word2) =>
            translationParams(word1)(word2) = c2((word1, word2)) / c1(word1)
        }

        tempAlign foreach {
          case (index1, index2, size1, size2) =>
            alignmentParams((index1, index2, size1, size2)) = c4((index1, index2, size1, size2)) / c3((index2, size1, size2))
        }

        System.out.println("Finished iteration #" + iter)
    }

    System.out.println("number of lang1 words in translationParams:" + translationParams.size)
    System.out.println("number of lang2|lang1 combinations in translationParams:" +
      translationParams.foldLeft(0) {
        case (acc, (_, map)) => acc + map.size
      })

    System.out.println("number of 4-tuples in alignmentParams:" + alignmentParams.size)

    val endEm = System.currentTimeMillis();

    System.out.println("EM time=" + (endEm - startEm) / 1000.0 + "s")

    (translationParams.toMap, alignmentParams.toMap)
  }

}

class IbmModel2(private[this] val params: (TranslationParameters, AlignmentParameters)) extends IbmModelLike {

  val translationParams = params._1
  val alignmentParams = params._2

  override def extractAlignments(line1: String, line2: String): immutable.Seq[Int] = {

    val arr2 = line2 split " "
    val size2 = arr2.size

    val mappedArray = arr2.iterator.zipWithIndex map {
      case (word2, index2) =>

        val arr1 = line1 split " "
        val size1 = arr1.size

        val (_, maxIndex) = (NULL +: arr1).iterator.zipWithIndex.maxBy {
          case (word1, index1) => alignmentParams((index1, index2 + 1, size1, size2)) * translationParams(word1)(word2)
        }

        maxIndex
    }

    mappedArray.toVector
  }

  override def writeParams(outputFilePath: String) {

    System.out.println("Writing params to file")

    val out = new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(new File(outputFilePath)), "UTF8"))

    translationParams.foreach {
      case (word1, map) =>
        map.foreach {
          case (word2, prob) => out.write(word1 + " " + word2 + " " + prob + "\n")
        }
    }

    out.write("\n")

    alignmentParams.foreach {
      case ((index2, index1, size1, size2), prob) =>
        out.write(index2 + " " + index1 + " " + size1 + " " + size2 + " " + prob + "\n")
    }

    out.flush()
    out.close()

    System.out.println("Done Writing params to file")

  }

}