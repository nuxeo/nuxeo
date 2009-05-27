/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.nuxeo.ecm.platform.tag.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser of the property.
 * 
 * @author cpriceputu
 */
public class PropertyParser {

    private static final Pattern p = Pattern.compile("\\$\\{.*?\\}");

    public PropertyParser() {
    }

    public static String parse(String str) {
        StringBuilder sb = new StringBuilder();
        String[] pieces = p.split(str);
        Matcher m = p.matcher(str);
        for (int i = 0; i < pieces.length; i++) {
            sb.append(pieces[i]);
            if (m.find()) {
                sb.append(getProperty(str, m));
            }
        }
        return sb.toString();
    }

    private static String getProperty(String pseudo, Matcher m) {
        String prop = pseudo.substring(m.start(), m.end());
        return System.getProperty(prop.substring(2, prop.length() - 1));
    }
}
