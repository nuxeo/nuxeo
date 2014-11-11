/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      M.-A. Darche
 *      Stefane Fermigier
 *      Anahide Tchertchian
 *      Florent Guillaume
 */

package org.nuxeo.common.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

public class TestIdUtils extends TestCase {

    public void testGenerateIdAccentsLower() {
        // Testing that the generated strings are free of special characters and
        // lower case.
        String s1 = "C'est l'\u00E9t\u00E9 !";
        assertEquals("c-est-l-ete", IdUtils.generateId(s1, "-", true, 100));
        assertEquals("C-est-l-ete", IdUtils.generateId(s1, "-", false, 100));

        String s2 = "C'est !!!   l'\u00E9t\u00E9 !!!!";
        assertEquals("c-est-l-ete", IdUtils.generateId(s2, "-", true, 100));
        assertEquals("C-est-l-ete", IdUtils.generateId(s2, "-", false, 100));
    }

    public void testGenerateIdMaxChars() {
        // testing max chars
        String s = "On rails Nuxeooooo 5 is for a loooooooooooooooooooooooooooong time";
        // With max_chars = 0 the length of the generated ID should be the same
        // than this of the input.
        assertEquals(s.replace(" ", "-"), IdUtils.generateId(s, "-", false, 0));
        assertEquals(s.replace(" ", "-"),
                IdUtils.generateId(s, "-", false, s.length()));
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
            assertEquals(example.getValue(),
                    IdUtils.generateId(example.getKey(), "-", false, 24));
        }
    }

    public void testGeneratePathSegment() {
        String s;

        // stupid ids -> random
        for (String id : Arrays.asList("", " ", "  ", "-", "./", ".", "..",
                " . ", " .. ", "\"", "'", "/", "//")) {
            String newId = IdUtils.generatePathSegment(id);
            assertTrue(id + " -> " + newId, newId.length() > 6);
            assertTrue(newId, Character.isDigit(newId.charAt(0)));
        }

        // keeps normal names
        s = "My Document.pdf";
        assertEquals(s, IdUtils.generatePathSegment(s));
        // keeps non-ascii chars and capitals
        s = "C'est l'\u00E9t\u00E9   !!";
        assertEquals(s, IdUtils.generatePathSegment(s));
        // trims spaces
        s = "  Foo  bar  ";
        assertEquals("Foo  bar", IdUtils.generatePathSegment(s));
        // converts slashes
        s = "foo/bar";
        assertEquals("foo-bar", IdUtils.generatePathSegment(s));

    }

}
