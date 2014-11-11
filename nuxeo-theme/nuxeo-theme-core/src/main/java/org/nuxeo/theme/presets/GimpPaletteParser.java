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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GimpPaletteParser extends PaletteParser {

    static final Pattern colorPattern = Pattern.compile(
            "^\\s*(\\d{1,3})\\s+(\\d{1,3})\\s+(\\d{1,3})\\t{0,1}(.*)$",
            Pattern.MULTILINE);

    public static boolean checkSanity(byte[] bytes) {
        return new String(bytes).startsWith("GIMP Palette");
    }

    public static Map<String, String> parse(byte[] bytes) {
        Map<String, String> entries = new LinkedHashMap<String, String>();
        Matcher matcher = colorPattern.matcher(new String(bytes));
        int counter = 1;
        while (matcher.find()) {
            String key = matcher.group(4).trim();
            int r = Integer.parseInt(matcher.group(1));
            int g = Integer.parseInt(matcher.group(2));
            int b = Integer.parseInt(matcher.group(3));
            String value = rgbToHex(r, g, b);
            if (key.equals("Untitled")) {
                key = String.format("Color %s", counter);
            }
            entries.put(key, value);
            counter += 1;
        }
        return entries;
    }
}
