/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.test;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 7.3
 */
public class TestTranslationMessagesDiffer extends AbstractTranslationTestCase {

    @Test
    public void testDiff() throws IOException {
        String enpath = "messages_en_US.properties";
        String frpath = "messages_fr_FR.properties";
        Properties en = extractProps(enpath);
        Properties fr = extractProps(frpath);
        TranslationMessagesDiffer diff = new TranslationMessagesDiffer(en, fr);
        List<String> missing = diff.getMissingDestKeys();
        assertEquals(1, missing.size());
        assertEquals("label.onlyInFr", missing.get(0));
        List<String> added = diff.getAdditionalDestKeys();
        assertEquals(1, added.size());
        assertEquals("label.onlyInEn", added.get(0));
    }

}
