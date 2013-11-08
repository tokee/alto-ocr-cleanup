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

import java.util.Arrays;

/**
 * Extremely not thread safe not very smart alphabet representation
 */
public class Alphabet {
    private long[] alphabet = new long[100];
    private int size;

    public void add(long entry) {
        if (size == alphabet.length) {
            long[] newAlphabet = new long[alphabet.length*2];
            System.arraycopy(alphabet, 0, newAlphabet, 0, alphabet.length);
            alphabet = newAlphabet;
        }
        alphabet[size++] = entry;
    }
    public long get(int index) {
        return alphabet[index];
    }
    public long[] getAlphabet() {
        return alphabet;
    }

    public int getSize() {
        return size;
    }
    public void clear() {
        size = 0;
    }
    // O(n*log(n))
    public void removeDuplicates() {
        if (size <= 1) {
            return;
        }
        Arrays.sort(alphabet, 0, size);
        int lastUnique = 0;
        int index = 1;
        while (index < size) {
            if (alphabet[index] != alphabet[lastUnique]) {
                alphabet[++lastUnique] = alphabet[index];
            }
            index++;
        }
        size = lastUnique+1;
    }
}
