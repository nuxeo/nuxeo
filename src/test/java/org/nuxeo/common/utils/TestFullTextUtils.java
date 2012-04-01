/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.utils;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestFullTextUtils {

    @Test
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

    @Test
    public void testParseFullText() throws Exception {
        checkParseFullText("brown dog fail fox jump lazy over quick",
                "The quick brown fox jumps over the lazy dog -- and fails!");
        checkParseFullText("aime cafe jure pas",
                "J'aime PAS le caf\u00e9, je te jure.");
        checkParseFullText("007 bond jame thx1138", "James Bond 007 && THX1138");
    }

}
