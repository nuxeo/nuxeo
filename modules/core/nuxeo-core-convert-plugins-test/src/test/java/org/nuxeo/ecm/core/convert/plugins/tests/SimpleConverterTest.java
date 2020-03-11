/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertTrue;

public class SimpleConverterTest extends AbstractConverterTest {

    @Override
    protected void checkTextConversion(String textContent) {
        assertTrue(textContent.trim().startsWith("Hello"));
    }

    @Override
    protected void checkAny2TextConversion(String textContent) {
        checkTextConversion(textContent);
    }

    @Override
    protected void checkArabicConversion(String textContent) {

        String trimedTextContent = textContent.trim();

        // this is the wikipedia article for "Internet"
        assertTrue(trimedTextContent.contains("\u0625\u0646\u062a\u0631\u0646\u062a"));

        // other words
        assertTrue(trimedTextContent.contains("\u062a\u0645\u062b\u064a\u0644"));
        assertTrue(trimedTextContent.contains("\u0644\u0634\u0628\u0643\u0629"));
        assertTrue(trimedTextContent.contains("\u0645\u0646"));
        assertTrue(trimedTextContent.contains("\u0627\u0644\u0637\u0631\u0642"));
        assertTrue(trimedTextContent.contains("\u0641\u064a"));
        assertTrue(trimedTextContent.contains("\u062c\u0632\u0621"));
        assertTrue(trimedTextContent.contains("\u0628\u0633\u064a\u0637"));
        assertTrue(trimedTextContent.contains("\u0645\u0646"));
        assertTrue(trimedTextContent.contains("FTP"));
    }
}
