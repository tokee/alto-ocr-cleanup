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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extremely experimental experimenting experiment with anagram hashing. Does not scale, is not thread safe,
 * probably only works on Toke's laptop.
 * </p><p>
 * Ideas from
 * Reynaert, M.W.C. (2008). Non-interactive OCR post-correction for giga-scale digitization. In A. Gelbukh (Ed.),
 * Proceedings of the computational linguistics and intelligent text processing 9th international conference
 * (pp. 617-630)  http://ilk.uvt.nl/downloads/pub/papers/CICLING08.TICCL.MRE.postpublication.pdf
 */
public class AnagramHashing {
    private static Log log = LogFactory.getLog(AnagramHashing.class);

    public List<String> splitAndPrune(List<String> textStrings) {
        List<String> terms = new ArrayList<String>();
        for (String textString: textStrings) {
            for (String token: textString.split(" ")) {
                String pruned = prune(token);
                if (!pruned.isEmpty()) {
                    terms.add(pruned);
                }
            }
        }
        return terms;
    }

    public static final Locale DA = new Locale("da");
    public static final String LEGAL_CHARS = "abcdefghijklmnopqrstuvwxyzæøåö";
    private static final Set<Character> LEGAL = new HashSet<Character>(LEGAL_CHARS.length());
    static {
        for (Character c: LEGAL_CHARS.toCharArray()) {
            LEGAL.add(c);
        }
    }
    private String prune(String token) {
        // TODO: Do we really want to lowercase?
        token = token.toLowerCase(DA);
        while (!token.isEmpty() && !LEGAL.contains(token.charAt(0))) {
            token = token.substring(1);
        }
        while (!token.isEmpty() && !LEGAL.contains(token.charAt(token.length()-1))) {
            token = token.substring(0, token.length()-1);
        }
        return token;
    }

    // <String ID="S46" CONTENT="Iiave" WC="0.889" CC="9 8 7 8 8" HEIGHT="104" WIDTH="244" HPOS="292" VPOS="3256"/>
    private static final Pattern SPATTERN = Pattern.compile("<String [^>]*CONTENT=\"([^\"]+)\"[^>]+>", Pattern.DOTALL);
    public List<String> getStrings(File alto) throws IOException {
        String xml = read(alto);
        Matcher m = SPATTERN.matcher(xml);
        List<String> textStrings = new ArrayList<String>();
        while (m.find()) {
            textStrings.add(m.group(1).
                    replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&"));
        }
        return textStrings;
    }

    private String read(File alto) throws IOException {
        final char[] BUF = new char[4096];

        FileInputStream in = new FileInputStream(alto);
        InputStreamReader inr = new InputStreamReader(in, "utf-8");
        StringBuilder sb = new StringBuilder();
        int r;
        while ((r = inr.read(BUF)) != -1) {
            sb.append(BUF, 0, r);
        }
        return sb.toString();
    }

    /**
     * Get all ALTO XML files from the folder and sub folders.
     * @param altos  located ALTO XML files will be added to this.
     * @param folder where to start looking for ALTO XML files.
     */
    public void getALTOs(List<File> altos, File folder) {
        altos.addAll(Arrays.asList(folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".alto.xml"));
            }
        })));
        for (File sub: folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        })) {
            getALTOs(altos, sub);
        }
    }

    public static void addToAlphabet(Alphabet alphabet, String term, int maxNGram) {
        for (int i = 1 ; i <= maxNGram ; i++) {
            addToAlphabetImpl(alphabet, term, i);
        }
    }

    // Plain ngram
    private static void addToAlphabetImpl(Alphabet alphabet, String term, int nlength) {
        for (int start = 0 ; start <= term.length()-nlength ; start++) {
            long hash = 0;
            for (int i = 0 ; i < nlength ; i++) {
                final char c = term.charAt(start+i);
                hash += c*c*c*c*c;
            }
            alphabet.add(hash);
        }
    }

    public AnagramDictionary anagramDict = new AnagramDictionary();

    public static void dumpAnagramTerms(AnagramDictionary anagramDictionary, int minVariants, int maxVariants,
                                        int maxLev, boolean onlyStrongPrimaries) {
        System.out.println("minTerms=" + minVariants + ", maxTerms=" + maxVariants
                           + ", maxLev=" + (maxLev < 0 ? "N/A" : maxLev));

        List<Pair<Integer, String>> values = new ArrayList<Pair<Integer, String>>();
        primaryLoop:
        for (Map.Entry<String, AnagramDictionary.Word> entry: anagramDictionary.getPrimaryDict().entrySet()) {
            AnagramDictionary.Word word = entry.getValue();
            Set<String> secondaries = maxLev < 0 ? word.getSecondaries() : word.getSecondaries(maxLev);

            if (secondaries.size() + 1 < minVariants || secondaries.size() +1 > maxVariants) {
                continue;
            }
            String output =  word.getPrimary() + "(" + word.getPrimaryOccurrences() + "):";
            for (String secondary: secondaries) {
                int secondaryOccurrences = anagramDictionary.get(secondary).getPrimaryOccurrences();
                if (onlyStrongPrimaries) {
                    if (secondaryOccurrences > word.getPrimaryOccurrences()) {
                        continue primaryLoop;
                    }
                }
                output += " " + secondary + "(" + secondaryOccurrences + ")";
            }
            values.add(new Pair<Integer, String>(word.getPrimaryOccurrences(), output));
        }
        Collections.sort(values);
        Collections.reverse(values);
        for (Pair<Integer, String> value: values) {
            System.out.println(value.getPayload());
        }
    }

    private static class Pair<S extends Comparable<S>, T> implements Comparable<Pair<S, T>> {
        private final S sortValue;
        private final T payload;

        private Pair(S sortValue, T payload) {
            this.sortValue = sortValue;
            this.payload = payload;
        }

        public S getSortValue() {
            return sortValue;
        }

        public T getPayload() {
            return payload;
        }

        @Override
        public int compareTo(Pair<S, T> o) {
            return sortValue.compareTo(o.getSortValue());
        }
    }
}
