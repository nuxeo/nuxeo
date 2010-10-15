/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.presets;

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
