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
package org.nuxeo.ecm.user.registration.lang.test;

import org.nuxeo.ecm.platform.test.TranslationEnFrTestCase;

/**
 * Simple integrity tests on messages file(s).
 *
 * @since 7.3
 */
public class TestWebengineMessages extends TranslationEnFrTestCase {

    @Override
    protected String getEnTranslationsPath() {
        return "i18n/messages_en.properties";
    }

    @Override
    protected String getFrTranslationsPath() {
        return "i18n/messages_fr.properties";
    }

}
