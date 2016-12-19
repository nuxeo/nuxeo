/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      M.-A. Darche
 *      Stefane Fermigier
 *      Anahide Tchertchian
 *      Florent Guillaume
 */
package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

public class TestIdUtils {

    @Test
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

    @Test
    public void testGenerateIdMaxChars() {
        // testing max chars
        String s = "On rails Nuxeooooo 5 is for a loooooooooooooooooooooooooooong time";
        // With max_chars = 0 the length of the generated ID should be the same
        // than this of the input.
        assertEquals(s.replace(" ", "-"), IdUtils.generateId(s, "-", false, 0));
        assertEquals(s.replace(" ", "-"), IdUtils.generateId(s, "-", false, s.length()));
        // With max_chars > 0 the length of the generated ID should be lower or
        // equal to max_chars.
        int maxChars = 24;
        assertTrue(IdUtils.generateId(s, "-", false, maxChars).length() <= maxChars);
    }

    @Test
    public void testGenerateIdAccentsSeparator() {
        String s = "C'est l'\u00E9t\u00E9 !";
        assertEquals("c-est-l-ete", IdUtils.generateId(s, "-", true, 100));
        assertEquals("c_est_l_ete", IdUtils.generateId(s, "_", true, 100));
    }

    @Test
    public void testGenerateIdExamples() {
        Map<String, String> examples = new HashMap<>();
        examples.put("Le ciel est bleu", "Le-ciel-est-bleu");
        examples.put("Le ciel est bleu ", "Le-ciel-est-bleu");
        examples.put(" Le ciel est bleu ", "Le-ciel-est-bleu");
        examples.put("open+source", "open-source");
        examples.put("open + source", "open-source");
        examples.put("open  + source", "open-source");
        examples.put("S. Fermigier first law of project management", "S-Fermigier-first-law-of");
        for (Entry<String, String> example : examples.entrySet()) {
            assertEquals(example.getValue(), IdUtils.generateId(example.getKey(), "-", false, 24));
        }
    }

}
