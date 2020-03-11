/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Jean-Marc Orliaguet, Chalmers
 *     Anahide Tchertchian
 */

package org.nuxeo.theme.styling.service.palettes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.nuxeo.runtime.api.Framework;

public class PropertiesPaletteParser extends PaletteParser {

    public static boolean checkSanity(byte[] bytes) {
        Properties properties = getProperties(bytes);
        for (Object value : properties.values()) {
            if (value.equals("")) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, String> parse(byte[] bytes) {
        Map<String, String> entries = new LinkedHashMap<>();
        Properties properties = getProperties(bytes);
        for (Object propertyName : properties.keySet()) {
            String key = (String) propertyName;
            entries.put(key, Framework.expandVars((String) properties.get(key)));
        }
        return entries;
    }

    private static Properties getProperties(byte[] bytes) {
        Properties properties = new Properties();
        try {
            properties.load(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
        }
        return properties;
    }

}
