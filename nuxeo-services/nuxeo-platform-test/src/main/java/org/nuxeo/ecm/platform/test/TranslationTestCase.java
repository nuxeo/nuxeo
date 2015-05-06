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

import org.junit.Test;

/**
 * Basic class for messages files translations.
 *
 * @since 7.3
 */
public class TranslationTestCase extends AbstractTranslationTestCase {

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
        checkFormat(getEnTranslationsPath());
        checkFormat(getFrTranslationsPath());
    }

    @Test
    public void testTranslationsDupes() throws IOException {
        checkDuplicates(getEnTranslationsPath());
        checkDuplicates(getFrTranslationsPath());
    }

    @Test
    public void testEnFrTranslationsDiff() throws IOException {
        checkDiff(getEnTranslationsPath(), getFrTranslationsPath());
    }

    @Test
    public void testFrEnTranslationsDiff() throws IOException {
        checkDiff(getFrTranslationsPath(), getEnTranslationsPath());
    }

}
