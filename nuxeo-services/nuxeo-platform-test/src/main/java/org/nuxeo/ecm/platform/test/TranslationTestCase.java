/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import static org.junit.Assert.assertNotNull;

/**
 * Basic class for messages files translations.
 *
 * @since 7.3
 */
public class TranslationTestCase {

    /**
     * Useful for override.
     */
    protected String getEnTranslationsPath() {
        return "OSGI-INF/l10n/messages_en_US.properties";
    }

    /**
     * Useful for override.
     */
    protected String getFrTranslationsPath() {
        return "OSGI-INF/l10n/messages_fr_FR.properties";
    }

    @Test
    public void testTranslationsLoading() throws IOException {
        Properties en = TranslationMessagesDiffer.extractProps(getEnTranslationsPath());
        assertNotNull(en);
    }

    @Test
    public void testEnFrTranslationsDiff() throws IOException {
        Properties en = TranslationMessagesDiffer.extractProps(getEnTranslationsPath());
        Properties fr = TranslationMessagesDiffer.extractProps(getFrTranslationsPath());
        TranslationMessagesDiffer diff = new TranslationMessagesDiffer(en, fr);
        List<String> missing = diff.getMissingDestKeys();
        assertEquals(String.format("Missing translation keys in fr file: %s", missing), 0, missing.size());
    }

    @Test
    public void testFrEnTranslationsDiff() throws IOException {
        Properties en = TranslationMessagesDiffer.extractProps(getEnTranslationsPath());
        Properties fr = TranslationMessagesDiffer.extractProps(getFrTranslationsPath());
        TranslationMessagesDiffer diff = new TranslationMessagesDiffer(en, fr);
        List<String> added = diff.getAdditionalDestKeys();
        assertEquals(String.format("Missing translation keys in en file: %s", added), 0, added.size());
    }

}
