/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Helper test class to check for multiple translations formats, as well as checks for identical keys.
 *
 * @since 8.4
 */
public abstract class TranslationMultiTestCase extends AbstractTranslationTestCase {

    protected abstract String getReferenceTranslationsPath();

    protected abstract String[] getOtherTranslationsPath();

    @Test
    public void testTranslationsLoading() throws IOException {
        checkFormat(getReferenceTranslationsPath());
        String[] others = getOtherTranslationsPath();
        if (others != null) {
            for (String other : others) {
                checkFormat(other);
            }
        }
    }

    @Test
    public void testTranslationsDupes() throws IOException {
        checkDuplicates(getReferenceTranslationsPath());
        String[] others = getOtherTranslationsPath();
        if (others != null) {
            for (String other : others) {
                checkDuplicates(other);
            }
        }
    }

    @Test
    public void testTranslationsDiff() throws IOException {
        String[] others = getOtherTranslationsPath();
        if (others != null) {
            for (String other : others) {
                checkDiff(getReferenceTranslationsPath(), other);
            }
        }
    }

}
