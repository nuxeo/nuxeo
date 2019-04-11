/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

/**
 * @author Florent Guillaume
 */
public class TestEmbeddedFunctions {

    public static void checkSplit(String string, String... expected) {
        assertEquals(new HashSet<>(Arrays.asList(expected)), EmbeddedFunctions.split(string));
    }

    public static void checkSplit(char sep, String string, String... expected) {
        assertEquals(new HashSet<>(Arrays.asList(expected)), EmbeddedFunctions.split(string, sep));
    }

    @Test
    public void testSplit() {
        checkSplit("");
        checkSplit("A", "A");
        checkSplit("A|B|C", "A", "B", "C");
        checkSplit("A||B", "A", "B", "");
        checkSplit("|A|B|C", "A", "B", "C", "");
        checkSplit("A|B|C|", "A", "B", "C", "");
        checkSplit("||", "");
        checkSplit('-', "A-B-C", "A", "B", "C");
    }

    @Test
    public void testParse() {
        assertNull(EmbeddedFunctions.parseWord("gr"));
        assertNull(EmbeddedFunctions.parseWord("are"));
        assertNull(EmbeddedFunctions.parseWord("THE"));
        assertEquals("foo", EmbeddedFunctions.parseWord("foo"));
        assertEquals("foo", EmbeddedFunctions.parseWord("fOoS"));
    }

    protected static void checkParseFullText(String expected, String text) {
        assertEquals(new HashSet<>(Arrays.asList(expected.split(" "))), EmbeddedFunctions.parseFullText(text));
    }

    @Test
    public void testParseFullText() {
        checkParseFullText("brown dog fail fox jump lazy over quick",
                "The quick brown fox jumps over the lazy dog -- and fails!");
        checkParseFullText("aime cafe jure pas", "J'aime PAS le caf\u00e9, je te jure.");
        checkParseFullText("007 bond jame thx1138", "James Bond 007 && THX1138");
    }

    protected static void checkMatchesFullText(boolean expected, String fulltext, String query) {
        boolean actual = EmbeddedFunctions.matchesFullText(fulltext, query);
        assertFalse(expected ^ actual);
    }

    @Test
    public void testMatchesFulltext() {
        checkMatchesFullText(false, "abc def", "ghi");
        checkMatchesFullText(true, "abc", "abc");
        checkMatchesFullText(true, "abc", "ABC");
        checkMatchesFullText(true, "ABC", "abc");
        checkMatchesFullText(true, "ABC", "ABC");
        checkMatchesFullText(true, "abc def", "abc");
        checkMatchesFullText(true, "abcdef", "abc*");
        checkMatchesFullText(true, "abcdef", "abc%");
    }

}
