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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 *
 */
public class AnagramHashingTest extends TestCase {
    private static Log log = LogFactory.getLog(AnagramHashingTest.class);

    public static final File[] INPUT_FOLDERS = new File[] {
            new File("alto_sample_tiny/"),
//            new File("/home/te/projects/data/ninestars_alto"),
            new File("/mnt/bulk/quack3/tilbud2_src/Ninestars_optionB3")
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

    public void testDump() throws IOException {
        final int NGRAM_MAX = 1;

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
                uniq.add(ts);
                AnagramHashing.addToAlphabet(totalAlphabet, ts, NGRAM_MAX);
                totalAlphabet.removeDuplicates(); // Prune regularly to prevent insane growth
            }
        }

        // Create an entry for all unique terms
        for (String u: uniq) {
            long h = AnagramUtil.hash(u);
            te.anagramDict.add(h, u);
        }

        // Iterate terms and find like terms
        Alphabet focusAlphabet = new Alphabet();
        for (String u: uniq) {
            focusAlphabet.clear();
            focusAlphabet.add(0);
            AnagramHashing.addToAlphabet(focusAlphabet, u, NGRAM_MAX);
            focusAlphabet.removeDuplicates();
            final long uHash = AnagramUtil.hash(u);
            for (int focusIndex = 0 ; focusIndex < focusAlphabet.getSize() ; focusIndex++) {
                for (int totalIndex = 0 ; totalIndex < totalAlphabet.getSize() ; totalIndex++) {
                    final long newHash = uHash - focusAlphabet.get(focusIndex) + totalAlphabet.get(totalIndex);
                    te.anagramDict.add(newHash, u);
                }
            }
        }
        AnagramHashing.dumpAnagramTerms(te.anagramDict, 4, -1);
        AnagramHashing.dumpAnagramTerms(te.anagramDict, 4, 1);
    }
}
