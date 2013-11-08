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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Map from anagram hashes to primary words and sets of matching words.
 */
public class AnagramDictionary {
    private static Log log = LogFactory.getLog(AnagramDictionary.class);

    private final Map<Long, Word>  dict = new HashMap<Long, Word>();

    public void add(Long hash, String word) {
        Word w = dict.get(hash);
        if (w != null) {
            w.add(word);
            return;
        }
        dict.put(hash, new Word(hash, word));
    }

    public void addIfExists(Long hash, String word) {
        Word w = dict.get(hash);
        if (w != null) {
            w.add(word);
        }
    }

    public Map<Long, Word> getDict() {
        return dict;
    }

    public final class Word {
        private final Long hash;
        private final String primary;
        private final Set<String> secondaries = new HashSet<String>();

        private Word(Long hash, String primary) {
            this.hash = hash;
            this.primary = primary;
        }

        public void add(String secondary) {
            if (primary.equals(secondary)) {
                return;
            }
            secondaries.add(secondary);
        }

        public Long getHash() {
            return hash;
        }

        public String getPrimary() {
            return primary;
        }

        public Set<String> getSecondaries() {
            return secondaries;
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
}
