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

public class AlphabetTest extends TestCase {
    private static Log log = LogFactory.getLog(AlphabetTest.class);

    public void testAlphabetBasic() {
        final long[] ENTRIES = new long[]{1, 2, 3, 3, 1, 4};
        Alphabet alphabet = new Alphabet();
        for (long entry: ENTRIES) {
            alphabet.add(entry);
        }
        assertEquals("Base insert count", ENTRIES.length, alphabet.getSize());
        assertEquals("Base insert", ENTRIES, alphabet.getAlphabet(), alphabet.getSize());
        alphabet.removeDuplicates();
        assertEquals("Duplicates removed", new long[]{1, 2, 3, 4}, alphabet.getAlphabet(), alphabet.getSize());
        alphabet.clear();
        assertEquals("Clear", 0, alphabet.getSize());
    }

    public void assertEquals(String message, long[] expected, long[] actual, int length) {
        for (int i = 0 ; i < length ; i++) {
            if (expected[i] != actual[i]) {
                fail(message + ". Expected " + expected[i] + " ast index " + i + " but found " + actual[i]);
            }
        }
    }
}
