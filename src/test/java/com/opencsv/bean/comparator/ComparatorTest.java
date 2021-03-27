/*
 * Copyright 2018 Andrew Rucker Jones.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opencsv.bean.comparator;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class ComparatorTest {

    @Test
    public void testComparatorNull() {
        String[] array = new String[]{"abc", null, "bcd", "cde", "def", "xyz", "wxy"};
        Arrays.sort(array, new LiteralComparator<String>(null));
        assertArrayEquals(new String[]{null, "abc", "bcd", "cde", "def", "wxy", "xyz"}, array);
    }

    /**
     * Tests a {@link LiteralComparator} with an array of {@link java.lang.Integer}s.
     * <p>Also incidentally tests:</p>
     * <ul><li>Having a null in the data with no null in the comparator</li>
     * <li>Having data otherwise not in the comparator</li></ul>
     */
    @Test
    public void testLiteralComparatorInteger() {
        Integer[] array = new Integer[]{1, 2, 3, 4, 5, 6, null, 12, 10};
        Arrays.sort(array, new LiteralComparator<>(new Integer[]{2, 4, 6, 8, 1, 3, 5, 7}));
        assertArrayEquals(new Integer[]{2, 4, 6, 1, 3, 5, null, 10, 12}, array);
    }

    /**
     * Tests a {@link LiteralComparator} with an array of {@link java.lang.String}s.
     * <p>Also incidentally tests:</p>
     * <ul><li>Having a null in the data with a null in the comparator</li></ul>
     */
    @Test
    public void testLiteralComparatorString() {
        String[] array = new String[]{"abc", null, "bcd", "cde", "def", "xyz", "wxy"};
        Arrays.sort(array, new LiteralComparator<>(new String[]{null, "efg", "bcd", "cde", "abc", "def"}));
        assertArrayEquals(new String[]{null, "bcd", "cde", "abc", "def", "wxy", "xyz"}, array);
    }
}
