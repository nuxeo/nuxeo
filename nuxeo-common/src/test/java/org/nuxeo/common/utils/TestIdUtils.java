/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestIdUtils.java 19046 2007-05-21 13:03:50Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

/**
 * @author M.-A. Darche madarche@nuxeo.com
 * @author Stefane Fermigier sf@nuxeo.com
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestIdUtils extends TestCase {

    public void testGenerateIdStupid() {
        // stupid id should return random number
        String[] ids = { "", "-", " ", ".", ";", "\"", "  " };
        for (String id : ids) {
            String newId = IdUtils.generateId(id);
            assertNotNull(newId);
            assertTrue(newId.length() > 0);
        }
    }

    public void testGenerateIdAccentsLower() {
        // Testing that the generated strings are free of special characters and
        // lower case.
        String s1 = "C'est l'\u00E9t\u00E9 !";
        assertEquals("c-est-l-ete", IdUtils.generateId(s1));
        assertEquals("c-est-l-ete", IdUtils.generateId(s1, "-", true, 100));
        assertEquals("C-est-l-ete", IdUtils.generateId(s1, "-", false, 100));

        String s2 = "C'est !!!   l'\u00E9t\u00E9 !!!!";
        assertEquals("c-est-l-ete", IdUtils.generateId(s2));
        assertEquals("c-est-l-ete", IdUtils.generateId(s2, "-", true, 100));
        assertEquals("C-est-l-ete", IdUtils.generateId(s2, "-", false, 100));
    }

    public void testGenerateIdMaxChars() {
        // testing max chars
        String s = "On rails Nuxeooooo 5 is for a loooooooooooooooooooooooooooong time";
        // With max_chars = 0 the length of the generated ID should be the same
        // than this of the input.
        assertEquals(s.replace(" ", "-"), IdUtils.generateId(s, "-", false, 0));
        assertEquals(s.replace(" ", "-"), IdUtils.generateId(s, "-", false, s
                .length()));
        // With max_chars > 0 the length of the generated ID should be lower or
        // equal to max_chars.
        int maxChars = 24;
        assertTrue(IdUtils.generateId(s, "-", false, maxChars).length() <= maxChars);
    }

    public void testGenerateIdAccentsSeparator() {
        String s = "C'est l'\u00E9t\u00E9 !";
        assertEquals("c-est-l-ete", IdUtils.generateId(s, "-", true, 100));
        assertEquals("c_est_l_ete", IdUtils.generateId(s, "_", true, 100));
    }

    public void testGenerateIdDeterminism() {
        // test that generateId always retrun same value given same contraints
        String[] examples = { "We are belong to us",
                "C'est l'\u00E9t\u00E9 !", "?Mine", };
        // currently fails with string like "???", "???????????"
        for (String s : examples) {
            String res1 = IdUtils.generateId(s);
            String res2 = IdUtils.generateId(s);
            assertEquals(res1, res2);
        }
    }

    public void testGenerateIdExamples() {
        Map<String, String> examples = new HashMap<String, String>();
        examples.put("Le ciel est bleu", "Le-ciel-est-bleu");
        examples.put("Le ciel est bleu ", "Le-ciel-est-bleu");
        examples.put(" Le ciel est bleu ", "Le-ciel-est-bleu");
        examples.put("open+source", "open-source");
        examples.put("open + source", "open-source");
        examples.put("open  + source", "open-source");
        examples.put("S. Fermigier first law of project management",
                "S-Fermigier-first-law-of");
        for (Entry<String, String> example : examples.entrySet()) {
            assertEquals(example.getValue(), IdUtils.generateId(example
                    .getKey(), "-", false, 24));
        }

    }

}
