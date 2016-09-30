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

import org.junit.Test;

/**
 * Checks en and fr translations formats, and checks for identical keys.
 *
 * @since 7.3
 */
public class TranslationEnFrTestCase extends TranslationTestCase {

    /**
     * Useful for override.
     */
    protected String getFrTranslationsPath() {
        return "OSGI-INF/l10n/messages_fr_FR.properties";
    }

    @Test
    public void testTranslationsLoadingFr() throws IOException {
        checkFormat(getFrTranslationsPath());
    }

    @Test
    public void testTranslationsDupesFr() throws IOException {
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
