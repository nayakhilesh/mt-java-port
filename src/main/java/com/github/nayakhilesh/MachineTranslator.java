package com.github.nayakhilesh;;

import java.io.IOException;
import java.util.Properties;

public class MachineTranslator {

    private Decoder decoder;

    public MachineTranslator(Properties properties,
                             String lang1FilePath,
                             String lang2FilePath) throws IOException {

        Lexicon lexicon = buildLexicon(properties, lang1FilePath, lang2FilePath);
        TrigramLanguageModel lang2Model = new TrigramLanguageModel();
        lang2Model.initialize(lang2FilePath);

        int distortionLimit = Integer.valueOf(properties.getProperty("decoder.distortion-limit"));
        double distortionPenalty = Double.valueOf(properties.getProperty("decoder.distortion-penalty"));
        double beamWidth = Double.valueOf(properties.getProperty("decoder.beam-width"));

        this.decoder = new Decoder(lexicon, lang2Model, distortionLimit, distortionPenalty, beamWidth);
    }

    public String translate(String sentence) {
        return decoder.decode(sentence);        
    }

  private Lexicon buildLexicon(Properties properties, String lang1FilePath, String lang2FilePath) {

    //p(f|e)
    //computes prob that word f in lang2 aligns to word e in lang1
    IbmModel2 ibm2Lang1 = initializeIbmModel2(lang1FilePath, lang2FilePath)

    //p(e|f)
    //computes prob that word e in lang1 aligns to word f in lang2
    IbmModel2 ibm2Lang2 = initializeIbmModel2(lang2FilePath, lang1FilePath)

    Lexicon lexicon = new Lexicon();
    System.out.println("Building lexicon...");
    long startLexicon = System.currentTimeMillis();
    loopThroughFiles(lang1FilePath, lang2FilePath, true) {
      (line1, line2, index) =>

      //each position has the index of the lang1 word it aligns to (starting from 0 <=> NULL)
        val lang2Alignments = ibm2Lang1.extractAlignments(line1, line2)

        //each position has the index of the lang2 word it aligns to (starting from 0 <=> NULL)
        val lang1Alignments = ibm2Lang2.extractAlignments(line2, line1)

        lexicon.add(lang1Alignments, lang2Alignments, line1, line2)
    }
    long endLexicon = System.currentTimeMillis();

    System.out.println("Building lexicon time=" + (endLexicon - startLexicon) / 1000.0 + "s");

    lexicon
  }

  public IbmModel2 initializeIbmModel2(String lang1FilePath, String lang2FilePath) {

      IbmModel1 ibm1 = new IbmModel1(lang1FilePath, lang2FilePath, 5)
      val mutableIbm1TranslationParams = collection.mutable.Map(ibm1.translationParams.toSeq: _*)
    val ibm2 =
      IbmModel2(lang1FilePath, lang2FilePath, 5, mutableIbm1TranslationParams)

    return ibm2
  }

}

