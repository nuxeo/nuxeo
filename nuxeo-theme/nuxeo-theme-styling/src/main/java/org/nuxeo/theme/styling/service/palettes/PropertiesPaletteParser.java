/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
        Map<String, String> entries = new LinkedHashMap<String, String>();
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
