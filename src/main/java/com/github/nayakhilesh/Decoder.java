package com.github.nayakhilesh;

import com.google.common.base.Joiner;
import lombok.Value;
import org.javatuples.Pair;

import java.util.*;

public class Decoder {

    private static final Comparator<Beam.State> STATE_COMPARATOR = new Comparator<Beam.State>() {
        @Override
        public int compare(Beam.State o1, Beam.State o2) {
            if (o1.score > o2.score) {
                return 1;
            } else if (o1.score < o2.score) {
                return -1;
            }
            return 0;
        }
    };

    private static final Comparator<Phrase> PHRASE_COMPARATOR = new Comparator<Phrase>() {
        @Override
        public int compare(Phrase o1, Phrase o2) {
            return o1.lang1Start - o2.lang1Start;
        }
    };

    private Lexicon lexicon;
    private TrigramLanguageModel languageModel;
    private int distortionLimit;
    private double distortionPenalty;
    private double beamWidth;

    public Decoder(Lexicon lexicon, TrigramLanguageModel languageModel,
                   int distortionLimit, double distortionPenalty, double beamWidth) {

        this.lexicon = lexicon;
        this.languageModel = languageModel;
        this.distortionLimit = distortionLimit;
        this.distortionPenalty = distortionPenalty;
        this.beamWidth = beamWidth;

        System.out.println("Created decoder with distortionLimit=" + distortionLimit +
                ", distortionPenalty=" + distortionPenalty + ", beamWidth=" + beamWidth);
    }

    public String decode(String line) {

        List<String> words = Arrays.asList(line.split(" "));
        int n = words.size();
        List<Beam> beams = new ArrayList<>();
        for (int i = 0; i < n + 1; i++) {
            beams.add(new Beam(beamWidth));
        }

        Beam.State q0 = new Beam.State(TrigramLanguageModel.BEFORE_SYMBOL, TrigramLanguageModel.BEFORE_SYMBOL,
                new BitSet(n), 0, 0);
        beams.get(0).addState(q0);

        // back-pointers
        Map<Beam.State, Pair<Beam.State, Phrase>> bp = new HashMap<>();

        for (int i = 0; i < n; i++) {
            for (Beam.State q : beams.get(i).getStates()) {
                for (Phrase p : ph(q, words)) {
                    Beam.State q1 = next(q, p, words);
                    int j = q1.bitString.cardinality();
                    add(beams.get(j), q1, q, p, bp);
                }
            }
        }

        for (int i = beams.size() - 1; i >= 0; i--) {

            Beam beam = beams.get(i);
            if (beam.getStates().size() > 0) {

                Beam.State maxState = Collections.max(beam.getStates(), STATE_COMPARATOR);

                Set<Phrase> phrases = new TreeSet<>(PHRASE_COMPARATOR);
                Beam.State currentState = maxState;
                Pair<Beam.State, Phrase> pair = bp.get(currentState);
                while (pair != null) {
                    currentState = pair.getValue0();
                    Phrase p = pair.getValue1();
                    phrases.add(p);
                    pair = bp.get(currentState);
                }

                StringBuilder translatedLine = new StringBuilder();

                int index = 0;
                while (index < words.size()) {

                    Phrase phrase = null;
                    for (Phrase p : phrases) {
                        if (p.lang1Start <= index + 1 &&
                                index + 1 <= p.lang1End) {
                            phrase = p;
                            break;
                        }
                    }

                    if (phrase != null) {
                        translatedLine.append(Joiner.on(" ").join(phrase.lang2Words)).append(" ");
                        phrases.remove(phrase);
                        index = phrase.lang1End;
                    } else {
                        translatedLine.append(words.get(index)).append(" ");
                        index++;
                    }

                }

                // remove trailing space
                if (translatedLine.length() > 0) {
                    translatedLine.setLength(translatedLine.length() - 1);
                }

                return translatedLine.toString().replace('*', ' ');
            }

        }

        return "No translation found";
    }

    private Set<Phrase> ph(Beam.State q, List<String> wordsLang1) {

        Set<Phrase> phrases = new HashSet<>();
        //(r + 1) - d <= s <= (r + 1) + d
        for (int start = Math.max(q.prevEnd + 1 - distortionLimit, 0);
             start <= Math.min(q.prevEnd + 1 + distortionLimit, wordsLang1.size() - 1); start++) {
            int end = start;
            boolean overlap = false;
            while (end < wordsLang1.size() && !overlap) {
                int nextSet = q.bitString.nextSetBit(start);
                if (nextSet == -1 || nextSet > end) {
                    Set<List<String>> lang2WordsSet = lexicon.getTranslation(wordsLang1.subList(start, end + 1));
                    if (lang2WordsSet != null) {
                        for (List<String> lang2Words : lang2WordsSet) {
                            phrases.add(new Phrase(start + 1, end + 1, lang2Words));
                        }
                    }
                } else {
                    overlap = true;
                }
                end++;
            }
        }

        return phrases;
    }

    private void add(Beam beam, Beam.State q1, Beam.State q, Phrase p,
                     Map<Beam.State, Pair<Beam.State, Phrase>> bp) {

        Beam.State existing = beam.getState(q1);
        if (existing == null) {
            beam.addState(q1);
            bp.put(q1, new Pair<>(q, p));
        } else if (q1.score > existing.score) {
            beam.removeState(existing);
            beam.addState(q1);
            bp.put(q1, new Pair<>(q, p));
        }
        if (q1.score > beam.max) {
            beam.max = q1.score;
            beam.purge();
        }
    }

    private Beam.State next(Beam.State q, Phrase p, List<String> wordsLang1) {
        int numWords = p.lang2Words.size();
        if (numWords == 0 || p.lang1Start > p.lang1End) {
            throw new IllegalArgumentException();
        }

        String newWord1 = numWords == 1 ? q.word2 : p.lang2Words.get(numWords - 2);
        String newWord2 = p.lang2Words.get(p.lang2Words.size() - 1);
        BitSet newBitString = (BitSet) q.bitString.clone();
        newBitString.set(p.lang1Start - 1, p.lang1End);
        int newPrevEnd = p.lang1End;
        double newScore = q.score + lexicon.estimate(wordsLang1.subList(p.lang1Start - 1, p.lang1End), p.lang2Words) +
                languageModel.estimate(p.lang2Words) + (distortionPenalty * Math.abs(q.prevEnd + 1 - p.lang1Start));

        return new Beam.State(newWord1, newWord2, newBitString, newPrevEnd, newScore);
    }

    private static class Beam {

        @Value
        private static class State {

            private String word1;
            private String word2;
            private BitSet bitString;
            private int prevEnd;
            private double score;

        }

        private double width;
        private double max;
        private Map<State, State> map;

        public Beam(double width) {
            this.width = width;
            this.max = -1.0;
            this.map = new HashMap<>();
        }

        public void addState(State state) {
            map.put(state, state);
        }

        public void removeState(State state) {
            map.remove(state);
        }

        public State getState(State state) {
            return map.get(state);
        }

        public Set<State> getStates() {
            return map.keySet();
        }

        public void purge() {
            for (Iterator<State> iter = map.keySet().iterator(); iter.hasNext(); ) {
                State state = iter.next();
                if (state.getScore() < max - width) {
                    iter.remove();
                }
            }
        }

    }

    //start and end are 1 indexed
    //i.e. start..end (inclusive) in lang1 is translated as lang2Words
    @Value
    private static class Phrase {

        private int lang1Start;
        private int lang1End;
        private List<String> lang2Words;

    }

}

