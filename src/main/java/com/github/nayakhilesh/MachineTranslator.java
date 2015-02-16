package com.github.nayakhilesh;

import com.google.common.base.Function;
import org.javatuples.Triplet;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class MachineTranslator {

    private Decoder decoder;

    public MachineTranslator(Properties properties,
                             String lang1FilePath,
                             String lang2FilePath) throws IOException {

        Lexicon lexicon = buildLexicon(lang1FilePath, lang2FilePath);
        TrigramLanguageModel lang2Model = new TrigramLanguageModel();
        lang2Model.initialize(lang2FilePath);

        int distortionLimit = Integer.parseInt(properties.getProperty("decoder.distortion-limit"));
        double distortionPenalty = Double.parseDouble(properties.getProperty("decoder.distortion-penalty"));
        double beamWidth = Double.parseDouble(properties.getProperty("decoder.beam-width"));

        this.decoder = new Decoder(lexicon, lang2Model, distortionLimit, distortionPenalty, beamWidth);
    }

    public String translate(String sentence) {
        return decoder.decode(sentence);
    }

    private Lexicon buildLexicon(String lang1FilePath, String lang2FilePath) {

        //p(f|e)
        //computes prob that word f in lang2 aligns to word e in lang1
        final IbmModel2 ibm2Lang1 = initializeIbmModel2(lang1FilePath, lang2FilePath);

        //p(e|f)
        //computes prob that word e in lang1 aligns to word f in lang2
        final IbmModel2 ibm2Lang2 = initializeIbmModel2(lang2FilePath, lang1FilePath);

        final Lexicon lexicon = new Lexicon();
        System.out.println("Building lexicon...");
        long startLexicon = System.currentTimeMillis();
        Utils.loopThroughFiles(lang1FilePath, lang2FilePath, new Function<Triplet<String, String, Integer>, Void>() {
            @Override
            public Void apply(Triplet<String, String, Integer> input) {

                String line1 = input.getValue0();
                String line2 = input.getValue1();

                //each position has the index of the lang1 word it aligns to (starting from 0 <=> NULL)
                List<Integer> lang2Alignments = ibm2Lang1.extractAlignments(line1, line2);

                //each position has the index of the lang2 word it aligns to (starting from 0 <=> NULL)
                List<Integer> lang1Alignments = ibm2Lang2.extractAlignments(line2, line1);

                lexicon.add(lang1Alignments, lang2Alignments, line1, line2);
                return null;
            }
        });

        long endLexicon = System.currentTimeMillis();

        System.out.println("Building lexicon time=" + (endLexicon - startLexicon) / 1000.0 + "s");

        return lexicon;
    }

    public IbmModel2 initializeIbmModel2(String lang1FilePath, String lang2FilePath) {

        IbmModel1 ibm1 = new IbmModel1(lang1FilePath, lang2FilePath, 5);
        return new IbmModel2(lang1FilePath, lang2FilePath, 5, ibm1.getTranslationParams());
    }

}

