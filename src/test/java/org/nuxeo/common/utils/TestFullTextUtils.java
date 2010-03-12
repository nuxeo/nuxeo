/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.common.utils;

import java.util.Arrays;
import java.util.HashSet;

import junit.framework.TestCase;

public class TestFullTextUtils extends TestCase {

    public void testParse() {
        assertNull(FullTextUtils.parseWord("gr", false));
        assertNull(FullTextUtils.parseWord("are", false));
        assertNull(FullTextUtils.parseWord("THE", false));
        String str = "\u0153ufcaf\u00e9";
        assertEquals("oeufcafe", FullTextUtils.parseWord(str, true));
        assertEquals(str, FullTextUtils.parseWord(str, false));
        assertEquals("foo", FullTextUtils.parseWord("foo", false));
        assertEquals("foo", FullTextUtils.parseWord("fOoS", false));
    }

    protected static void checkParseFullText(String expected, String text) {
        assertEquals(new HashSet<String>(Arrays.asList(expected.split(" "))),
                FullTextUtils.parseFullText(text, true));
    }

    public void testParseFullText() throws Exception {
        checkParseFullText("brown dog fail fox jump lazy over quick",
                "The quick brown fox jumps over the lazy dog -- and fails!");
        checkParseFullText("aime cafe jure pas",
                "J'aime PAS le caf\u00e9, je te jure.");
        checkParseFullText("007 bond jame thx1138", "James Bond 007 && THX1138");
    }

}
