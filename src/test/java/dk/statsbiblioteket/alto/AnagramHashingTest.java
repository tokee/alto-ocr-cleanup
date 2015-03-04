/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.alto;

import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 *
 */
public class AnagramHashingTest extends TestCase {
    private static Log log = LogFactory.getLog(AnagramHashingTest.class);

    public static final File[] INPUT_FOLDERS = new File[] {
            new File("alto_sample_32/"),
//            new File("/home/te/projects/alto-ocr-cleanup/1795_small"),
            new File("/home/te/projects/alto-ocr-cleanup/1795"),
            new File("/home/te/projects/data/ninestars_alto_1795"),
            new File("/home/te/projects/data/ninestars_alto"),
            new File("/mnt/bulk/quack3/tilbud2_src/Ninestars_optionB3"),
            new File("alto_sample_tiny/")
    };

    private File getInputFolder() {
        for (File input: INPUT_FOLDERS) {
            // Directly available?
            if (input.exists()) {
                return input;
            }
            // Maybe on the class path?
            URL url = Thread.currentThread().getContextClassLoader().getResource(input.toString());
            if (url != null) {
                return new File(url.getFile());
            }
            log.debug("Sample '" + input.getAbsolutePath() + "' did not exist");
        }
        throw new IllegalArgumentException("None of the possible input folders existed. "
                                           + "Please provide at least one folder containing .alto.xml-files");
    }

    public void disabledTestGenerateSampleWords() throws IOException {
        final int CHUNK = 100;

        List<String> words = new ArrayList<String>(100000);
        AnagramHashing te = new AnagramHashing();
        List<File> altos = new ArrayList<File>();
        te.getALTOs(altos, getInputFolder());
        for (File alto: altos) {
            log.debug("Processing ALTO " + alto);
            for (String ts: te.splitAndPrune(te.getStrings(alto))) {
                words.add(ts);
            }
        }
        Collections.shuffle(words);

        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while (pos < words.size()) {
            sb.setLength(0);
            sb.append("<String CONTENT=\"");
            int end = Math.min(words.size(), pos + CHUNK);
            for (int i = pos ; i < end ; i++) {
                if (i > pos) {
                    sb.append(' ');
                }
                sb.append(words.get(i).replace("&", "&amp;").replace("'", "&apos;").replace("\"", "&quot;").
                        replace("<", "&lt;").replace(">", "&gt;"));
            }
            sb.append("\"/>");
            System.out.println(sb.toString());
            pos = end;
        }
    }

    public void testDump() throws IOException {
        final File INPUT = new File("/home/te/projects/alto-ocr-cleanup/raw.txt");
        final int NGRAM_MAX = 2;
        final int MIN_LENGTH = 4;
        final int MAX_LINES = 100000;

        if (!INPUT.exists()) {
            return;
        }
        FileInputStream fis = new FileInputStream(INPUT);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        AnagramHashing te = new AnagramHashing();
        Set<String> uniq = new LinkedHashSet<String>();
        Alphabet totalAlphabet = new Alphabet();
        totalAlphabet.add(0);
        String line;
        List<String> ll = new ArrayList<String>(1);
        ll.add("Dummy");
        int count = 0;
        while (count < MAX_LINES && (line = br.readLine()) != null) {
            if (count++ % 10000 == 0) {
                System.out.println(count + ": " + line);
            }
            ll.set(0, line);
            for (String ts: te.splitAndPrune(ll)) {
                if (ts.length() >= MIN_LENGTH) {
                    long h = AnagramUtil.hash(ts);
                    te.anagramDict.addPrimary(h, ts);
                    uniq.add(ts);
                }
                AnagramHashing.addToAlphabet(totalAlphabet, ts, NGRAM_MAX);
                totalAlphabet.removeDuplicates(); // Prune regularly to prevent insane growth
            }
        }
        outputAnagram(NGRAM_MAX, MIN_LENGTH, te, uniq, totalAlphabet, false);
        fis.close();
    }

    public void testMajor() throws IOException {
        final int NGRAM_MAX = 2;
        final int MIN_LENGTH = 4;

        AnagramHashing te = new AnagramHashing();
        List<File> altos = new ArrayList<File>();
        te.getALTOs(altos, getInputFolder());
        Set<String> uniq = new LinkedHashSet<String>();
        Alphabet totalAlphabet = new Alphabet();
        totalAlphabet.add(0);
        for (File alto: altos) {
            log.debug("Processing ALTO " + alto);
            for (String ts: te.splitAndPrune(te.getStrings(alto))) {
                log.trace("- " + ts);
                if (ts.length() >= MIN_LENGTH) {
                    long h = AnagramUtil.hash(ts);
                    te.anagramDict.addPrimary(h, ts);
                    uniq.add(ts);
                }
                AnagramHashing.addToAlphabet(totalAlphabet, ts, NGRAM_MAX);
                totalAlphabet.removeDuplicates(); // Prune regularly to prevent insane growth
            }
        }
        outputAnagram(NGRAM_MAX, MIN_LENGTH, te, uniq, totalAlphabet, true);
    }

    private void outputAnagram(int NGRAM_MAX, int MIN_LENGTH, AnagramHashing te, Set<String> uniq, Alphabet totalAlphabet, boolean dumpSingle) {
        // Create an entry for all unique terms
        log.debug("Extracted " + uniq.size() + " unique term of length>=" + MIN_LENGTH + ", ngram-max=" + NGRAM_MAX
                  + ", total alphabet size=" + totalAlphabet.getSize());

        log.debug("Created anagram dictionary with " + te.anagramDict.primarySize() + " primary entries");
        // Iterate terms and find like terms
        Alphabet focusAlphabet = new Alphabet();

        int uCount = uniq.size();
        int each = uCount < 100 ? 1 : uCount / 100;
        int index = 0;
        for (String u: uniq) {
            if (index++ % each == 0) {
                System.out.print(".");
            }
            focusAlphabet.clear();
            focusAlphabet.add(0);
            AnagramHashing.addToAlphabet(focusAlphabet, u, NGRAM_MAX);
            focusAlphabet.removeDuplicates();
            final long uHash = AnagramUtil.hash(u);
            for (int focusIndex = 0 ; focusIndex < focusAlphabet.getSize() ; focusIndex++) {
                for (int totalIndex = 0 ; totalIndex < totalAlphabet.getSize() ; totalIndex++) {
                    final long newHash = uHash - focusAlphabet.get(focusIndex) + totalAlphabet.get(totalIndex);
                    te.anagramDict.addDerivative(newHash, u);
                }
            }
        }
        System.out.println("");
        //AnagramHashing.dumpAnagramTerms(te.anagramDict, 4, -1);
        AnagramHashing.dumpAnagramTerms(te.anagramDict, 2, Integer.MAX_VALUE, 2, true);
        if (dumpSingle) {
            System.out.println("\nWords that have no similar words");
            AnagramHashing.dumpAnagramTerms(te.anagramDict, 1, 1, 2, true);
        }
    }
}
