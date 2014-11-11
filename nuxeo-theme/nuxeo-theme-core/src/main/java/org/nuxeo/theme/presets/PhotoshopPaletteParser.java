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

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PhotoshopPaletteParser extends PaletteParser {

    private static final Log log = LogFactory.getLog(PhotoshopPaletteParser.class);

    private static final int RGB = 0;

    private static final int HSB = 1;

    private static final int CMYK = 2;

    private static final int LAB = 7;

    private static final int GRAYSCALE = 8;

    private static final int WIDE_CMYK = 9;

    public static boolean checkSanity(byte[] bytes) {
        return true;
    }

    public static Map<String, String> parse(byte[] bytes) {
        Map<String, String> entries = new LinkedHashMap<String, String>();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(is);

        char[] words = new char[bytes.length];
        int size = 0;
        while (true) {
            try {
                words[size] = dis.readChar();
                size++;
            } catch (Exception e) {
                break;
            }
        }

        try {
            is.close();
            dis.close();
        } catch (IOException e) {
            log.error(e, e);
        }

        int offset = 1;
        int version = words[0] & 0xffff;
        int nc = words[1] & 0xffff;

        // get version 2 if it exists
        if (version == 1 && size > nc * 5 + 2) {
            offset += nc * 5 + 2;
            version = words[offset - 1] & 0xffff;
            nc = words[offset] & 0xffff;
        }

        if (version == 1) {
            log.debug("Found ACO v1 color file (Photoshop < 7.0)");
        } else if (version == 2) {
            log.debug("Found ACO v2 color file (Photoshop >= 7.0)");
        } else {
            log.error("Unknown ACO file version: " + version);
            return entries;
        }

        log.debug("Found " + nc + " colors.");

        int counter = 1;
        for (int j = 0; j < nc; j++) {
            String value = null;
            int colorSpace = words[offset + 1] & 0xff;
            int w = words[offset + 2] & 0xffff;
            int x = words[offset + 3] & 0xffff;
            int y = words[offset + 4] & 0xffff;
            int z = words[offset + 5] & 0xffff;

            if (colorSpace == RGB) {
                value = rgbToHex(w / 256, x / 256, y / 256);

            } else if (colorSpace == HSB) {
                float hue = w / 65535F; // [0.0-1.0]
                float saturation = x / 65535F; // [0.0-1.0]
                float brightness = y / 65535F; // [0.0-1.0]
                Color color = Color.getHSBColor(hue, saturation, brightness);
                value = rgbToHex(color.getRed(), color.getGreen(),
                        color.getBlue());

            } else if (colorSpace == CMYK) {
                float cyan = 1F - w / 65535F; // [0.0-1.0]
                float magenta = 1F - x / 65535F; // [0.0-1.0]
                float yellow = 1F - y / 65535F; // [0.0-1.0]
                float black = 1F - z / 65535F; // [0.0-1.0]
                // TODO: do the conversion to RGB. An ICC profile is required.
                log.warn("Unsupported color space: CMYK");

            } else if (colorSpace == GRAYSCALE) {
                int gray = (int) (w * 256F / 10000F); // [0-256]
                value = rgbToHex(gray, gray, gray);

            } else if (colorSpace == LAB) {
                float l = w / 100F;
                float a = x / 100F;
                float b = y / 100F;
                // TODO: do the conversion to RGB. An ICC profile is required.
                log.warn("Unsupported color space: CIE Lab");

            } else if (colorSpace == WIDE_CMYK) {
                float cyan = w / 10000F; // [0.0-1.0]
                float magenta = x / 10000F; // [0.0-1.0]
                float yellow = y / 10000F; // [0.0-1.0]
                float black = z / 10000F; // [0.0-1.0]
                // TODO: do the conversion to RGB. An ICC profile is required.
                log.warn("Unsupported color space: Wide CMYK");

            } else {
                log.warn("Unknown color space: " + colorSpace);
            }

            String name = "";
            if (version == 1) {
                name = String.format("Color %s", counter);
            }

            else if (version == 2) {
                int len = (words[offset + 7] & 0xffff) - 1;
                name = String.copyValueOf(words, offset + 8, len);
                offset += len + 3;

                String n = name;
                int c = 2;
                while (entries.containsKey(n)) {
                    n = String.format("%s %s", name, c);
                    c++;
                }
                name = n;
            }

            if (value != null) {
                entries.put(name, value);
            }

            offset += 5;
            counter++;
        }

        return entries;
    }

}
