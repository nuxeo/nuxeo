/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.forms.layout.io.plugins.helpers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Helper to manage labels translation using the default web message bundles
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class TranslationHelper {

    public static final String DEFAULT_LOCALE = "en";

    protected static ResourceBundle getBundle(String lang) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ResourceBundle.getBundle("messages", new Locale(lang), cl);
    }

    public static String getTranslation(String key, String lang) {
        ResourceBundle defaultBundle = null;
        ResourceBundle bundle = getBundle(lang);
        if (bundle == null) {
            defaultBundle = getBundle(DEFAULT_LOCALE);
            bundle = defaultBundle;
        }
        String translation = null;
        if (bundle != null && bundle.containsKey(key)) {
            translation = bundle.getString(key);
        }

        if (translation == null && !lang.equals(DEFAULT_LOCALE)) {
            if (defaultBundle == null) {
                defaultBundle = getBundle(DEFAULT_LOCALE);
            }
            if (defaultBundle.containsKey(key)) {
                translation = defaultBundle.getString(key);
            }
        }

        if (translation == null) {
            translation = key;
        }
        return translation;
    }

    public static Map<String, String> getTranslatedLabels(Map<String, String> labels, String lang) {
        if (labels != null) {
            Map<String, String> res = new HashMap<>();
            for (Map.Entry<String, String> label : labels.entrySet()) {
                res.put(label.getKey(), TranslationHelper.getTranslation(label.getValue(), lang));
            }
            return res;
        }
        return null;
    }

}
