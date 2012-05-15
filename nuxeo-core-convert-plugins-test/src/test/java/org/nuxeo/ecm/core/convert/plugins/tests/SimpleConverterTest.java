/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertTrue;

public class SimpleConverterTest extends AbstractConverterTest {

    protected void checkTextConversion(String textContent) {
        assertTrue(textContent.trim().startsWith("Hello"));
    }

    protected void checkAny2TextConversion(String textContent) {
        checkTextConversion(textContent);
    }

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
