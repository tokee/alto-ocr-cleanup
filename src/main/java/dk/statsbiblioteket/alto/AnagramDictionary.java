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

import java.util.*;

/**
 * Map from anagram hashes to primary words and sets of matching words.
 */
public class AnagramDictionary {
    private static Log log = LogFactory.getLog(AnagramDictionary.class);

    private final Map<Long, List<Word>>  hashDict = new HashMap<Long, List<Word>>();
    private final Map<String, Word> primaryDict = new HashMap<String, Word>();

    /**
     * Adds the given word as a primary entry if the entry does not already exists.
     * If the entry exists, its occurrence count is increased by 1.
     * @param hash the anagram-hash for the word.
     * @param word the word to add.
     */
    // TODO: When we add two primaries with the same hash, we want two entries. Double HashMap maybe?
    public void addPrimary(Long hash, String word) {
        Word w = primaryDict.get(word);
        if (w != null) {
            w.add(word);
            return;
        }
        Word newWord = new Word(hash, word);
        add(newWord);
    }

    private void add(Word word) {
        primaryDict.put(word.getPrimary(), word);
        List<Word> existing = hashDict.get(word.getHash());
        if (existing == null) {
            existing = new ArrayList<Word>();
            hashDict.put(word.getHash(), existing);
        }
        existing.add(word);
    }

    public Word get(String primary) {
        return primaryDict.get(primary);
    }

    // Added purely based on hash
    public void addDerivative(Long hash, String word) {
        List<Word> words = hashDict.get(hash);
        if (words == null) {
            return;
        }
        for (Word w: words) {
            w.add(word);
        }
    }

    public final class Word {
        private final Long hash;
        private final String primary;
        private final Set<String> secondaries = new HashSet<String>();
        private int primaryOccurrences = 1; // No Word without 1 primary
        private int secondaryOccurrences = 0;

        private Word(Long hash, String primary) {
            this.hash = hash;
            this.primary = primary;
        }

        public void add(String secondary) {
            if (primary.equals(secondary)) {
                primaryOccurrences++;
                return;
            }
            secondaries.add(secondary);
            secondaryOccurrences++;
        }

        public Long getHash() {
            return hash;
        }

        public String getPrimary() {
            return primary;
        }

        public int getPrimaryOccurrences() {
            return primaryOccurrences;
        }

        public Set<String> getSecondaries() {
            return secondaries;
        }

        public int getSecondaryOccurrences() {
            return secondaryOccurrences;
        }

        public Set<String> getSecondaries(int maxLevenshteinDistance) {
            final Set<String> pruned = new HashSet<String>(secondaries.size());
            for (String secondary: secondaries) {
                if (AnagramUtil.levDist(primary, secondary) <= maxLevenshteinDistance) {
                    pruned.add(secondary);
                }
            }
            return pruned;
        }

        public int size() {
            return 1 + secondaries.size();
        }
    }

    public int primarySize() {
        return primaryDict.size();
    }

    public Map<String, Word> getPrimaryDict() {
        return primaryDict;
    }
}
