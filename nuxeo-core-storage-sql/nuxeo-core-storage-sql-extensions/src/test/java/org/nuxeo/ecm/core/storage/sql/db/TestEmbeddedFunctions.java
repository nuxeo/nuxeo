/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Florent Guillaume
 */
public class TestEmbeddedFunctions {

    public static void checkSplit(String string, String... expected) {
        assertEquals(new HashSet<String>(Arrays.asList(expected)),
                EmbeddedFunctions.split(string));
    }

    public static void checkSplit(char sep, String string, String... expected) {
        assertEquals(new HashSet<String>(Arrays.asList(expected)),
                EmbeddedFunctions.split(string, sep));
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
        assertEquals(new HashSet<String>(Arrays.asList(expected.split(" "))),
                EmbeddedFunctions.parseFullText(text));
    }

    @Test
    public void testParseFullText() {
        checkParseFullText("brown dog fail fox jump lazy over quick",
                "The quick brown fox jumps over the lazy dog -- and fails!");
        checkParseFullText("aime cafe jure pas",
                "J'aime PAS le caf\u00e9, je te jure.");
        checkParseFullText("007 bond jame thx1138", "James Bond 007 && THX1138");
    }

}
