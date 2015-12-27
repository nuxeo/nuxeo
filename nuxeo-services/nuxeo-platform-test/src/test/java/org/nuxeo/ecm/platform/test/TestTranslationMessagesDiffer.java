/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
