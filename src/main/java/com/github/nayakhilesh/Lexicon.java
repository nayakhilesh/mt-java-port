package com.github.nayakhilesh;

import org.javatuples.Pair;

import java.util.*;

public class Lexicon {

    private Map<List<String>, Set<List<String>>> translatedPairs;
    private Map<Pair<List<String>, List<String>>, Integer> c2;
    private Map<List<String>, Integer> c1;

    public Lexicon() {
        this.translatedPairs = new HashMap<>();
        this.c2 = new HashMap<>();
        this.c1 = new HashMap<>();
    }

    public void add(List<Integer> lang1Alignments, List<Integer> lang2Alignments, String line1, String line2) {

        List<List<Integer>> lang2FinalAlignments = new ArrayList<>(lang2Alignments.size());
        List<List<Integer>> lang1FinalAlignments = new ArrayList<>(lang1Alignments.size());

        //find elements in intersection of alignments
        findAlignmentIntersection(lang2Alignments, lang1Alignments, lang2FinalAlignments);
        findAlignmentIntersection(lang1Alignments, lang2Alignments, lang1FinalAlignments);

        //order matters, though which is better?
        growAlignments(lang2FinalAlignments, lang1FinalAlignments, lang2Alignments, lang1Alignments);
        growAlignments(lang1FinalAlignments, lang2FinalAlignments, lang1Alignments, lang2Alignments);

        List<String> words1 = Arrays.asList(line1.split(" "));
        List<String> words2 = Arrays.asList(line2.split(" "));

        for (int startLang2 = 0; startLang2 < lang2FinalAlignments.size(); startLang2++) {

            for (int lengthLang2 = 0; lengthLang2 < lang2FinalAlignments.size() - startLang2; lengthLang2++) {

                for (int startLang1 = 0; startLang1 < lang1FinalAlignments.size(); startLang1++) {

                    for (int lengthLang1 = 0; lengthLang1 < lang1FinalAlignments.size() - startLang1; lengthLang1++) {

                        if (isConsistent(startLang2, lengthLang2,
                                startLang1, lengthLang1, lang2FinalAlignments) &&
                                isConsistent(startLang1, lengthLang1,
                                        startLang2, lengthLang2, lang1FinalAlignments)) {
                            List<String> phraseLang2 = words2.subList(startLang2, startLang2 + lengthLang2 + 1);
                            List<String> phraseLang1 = words1.subList(startLang1, startLang1 + lengthLang1 + 1);
                            //System.out.println(arrayToString(phraseLang2) + "|" + arrayToString(phraseLang1))
                            if (translatedPairs.containsKey(phraseLang1)) {
                                translatedPairs.get(phraseLang1).add(phraseLang2);
                            } else {
                                Set<List<String>> set = new HashSet<>();
                                set.add(phraseLang2);
                                translatedPairs.put(phraseLang1, set);
                            }
                            Pair<List<String>, List<String>> pair = new Pair<>(phraseLang2, phraseLang1);
                            // TODO refactor into Maps.incrementKey
                            if (c2.containsKey(pair)) {
                                c2.put(pair, c2.get(pair) + 1);
                            } else {
                                c2.put(pair, 1);
                            }
                            if (c1.containsKey(phraseLang2)) {
                                c1.put(phraseLang2, c1.get(phraseLang2) + 1);
                            } else {
                                c1.put(phraseLang2, 1);
                            }
                            //g(e,f) = log c(f,e)/c(f)
                            //e to f translation
                        }

                    }

                }

            }

        }

    }

    private boolean isConsistent(int startLang2, int lengthLang2,
                                 final int startLang1, final int lengthLang1,
                                 List<List<Integer>> lang2FinalAlignments) {

        for (int i = startLang2; i <= startLang2 + lengthLang2; i++) {
            List<Integer> valueList = lang2FinalAlignments.get(i);

            if (valueList.size() == 0) {
                return false;
            }

            for (int value : valueList) {
                if (value < startLang1 || value > (startLang1 + lengthLang1)) {
                    return false;
                }
            }
        }

        return true;
    }

    private void findAlignmentIntersection(List<Integer> lang2Alignments, List<Integer> lang1Alignments,
                                           List<List<Integer>> lang2FinalAlignments) {

        for (int lang2Index = 0; lang2Index < lang2Alignments.size(); lang2Index++) {
            int lang1Index = lang2Alignments.get(lang2Index);

            if (lang1Index > 0 && lang1Alignments.get(lang1Index - 1) == lang2Index + 1) {
                ArrayList<Integer> arrayList = new ArrayList<>();
                arrayList.add(lang1Index - 1);
                lang2FinalAlignments.add(arrayList);
            } else {
                lang2FinalAlignments.add(new ArrayList<Integer>());
            }
        }

    }

    //g(e,f) = log c(f,e)/c(f)
    //e to f translation
    public double estimate(List<String> wordsLang1, List<String> wordsLang2) {
        Pair<List<String>, List<String>> pair = new Pair<>(wordsLang2, wordsLang1);
        if (!(c2.containsKey(pair) && c1.containsKey(wordsLang2))) {
            return 0.0;
        }
        return Math.log(((double) c2.get(pair)) / ((double) c1.get(wordsLang2)));
    }

    public Set<List<String>> getTranslation(List<String> wordsLang1) {
        return translatedPairs.get(wordsLang1);
    }

    private void growAlignments(List<List<Integer>> lang2FinalAlignments,
                                List<List<Integer>> lang1FinalAlignments,
                                List<Integer> lang2Alignments, List<Integer> lang1Alignments) {

        for (int index2 = 0; index2 < lang2FinalAlignments.size(); index2++) {
            List<Integer> lang1List = lang2FinalAlignments.get(index2);

            if (lang1List.size() == 0) {

                int count = -1;
                int alignedToLang1Index = lang2Alignments.get(index2) - 1;
                if (alignedToLang1Index >= 0) {
                    count = countNeighbours(alignedToLang1Index, index2, lang2FinalAlignments, lang1FinalAlignments);
                }

                int count1 = -1;
                int row1 = 0;

                for (int index1 = 0; index1 < lang1Alignments.size(); index1++) {
                    int lang2Index = lang1Alignments.get(index1);
                    if (lang2Index - 1 == index2) {
                        int tempCount = countNeighbours(index1, index2, lang2FinalAlignments, lang1FinalAlignments);
                        if (tempCount > count1) {
                            count1 = tempCount;
                            row1 = index1;
                        }
                    }
                }

                if (!(count == -1 && count1 == -1)) {
                    if (count >= count1) {
                        lang2FinalAlignments.get(index2).add(alignedToLang1Index);
                        lang1FinalAlignments.get(alignedToLang1Index).add(index2);
                    } else {
                        lang2FinalAlignments.get(index2).add(row1);
                        lang1FinalAlignments.get(row1).add(index2);
                    }
                }

            }

        }

    }

    private int countNeighbours(int row, int col,
                                List<List<Integer>> lang2FinalAlignments, List<List<Integer>> lang1FinalAlignments) {

        int count = 0;

        for (int tempRow = row - 1; tempRow <= row + 1; tempRow++) {
            for (int tempCol = col - 1; tempCol <= col + 1; tempCol++) {
                if (tempRow >= 0 && tempRow < lang1FinalAlignments.size() &&
                        tempCol >= 0 && tempCol < lang2FinalAlignments.size() &&
                        !(tempRow == row && tempCol == col) &&
                        lang2FinalAlignments.get(tempCol).contains(tempRow)) {
                    count++;
                }
            }
        }

        return count;
    }

}