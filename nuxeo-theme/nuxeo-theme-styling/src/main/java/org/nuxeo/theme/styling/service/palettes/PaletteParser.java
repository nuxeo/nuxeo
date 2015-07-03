/*
 * (C) Copyright 2006-2014 Nuxeo SA <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 */

package org.nuxeo.theme.styling.service.palettes;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PaletteParser {

    private static final Log log = LogFactory.getLog(PaletteParser.class);

    public static Map<String, String> parse(URL url) {
        Map<String, String> entries = new HashMap<>();
        InputStream in = null;
        try {
            in = url.openStream();
            entries = parse(in, url.getFile());
        } catch (FileNotFoundException e) {
            log.error("File not found: " + url);
        } catch (IOException e) {
            log.error("Could not open file: " + url);
        } catch (PaletteParseException e) {
            log.error("Could not parse palette: " + url);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    in = null;
                }
            }
        }
        return entries;
    }

    public static boolean checkSanity(byte[] bytes) {
        return false;
    }

    public static String rgbToHex(int r, int g, int b) {
        final StringBuffer hexcolor = new StringBuffer("#");
        final int[] rgb = { r, g, b };
        for (final int val : rgb) {
            if (val < 16) {
                hexcolor.append("0");
            }
            hexcolor.append(Integer.toHexString(val));
        }
        // optimize #aabbcc to #abc
        if (hexcolor.charAt(1) == hexcolor.charAt(2) && hexcolor.charAt(3) == hexcolor.charAt(4)
                && hexcolor.charAt(5) == hexcolor.charAt(6)) {
            return String.format("#%s%s%s", hexcolor.charAt(1), hexcolor.charAt(4), hexcolor.charAt(6));
        }
        return hexcolor.toString();
    }

    public static Map<String, String> parse(InputStream in, String filename) throws IOException, PaletteParseException {
        DataInputStream dis = new DataInputStream(in);
        byte[] bytes = new byte[dis.available()];
        dis.read(bytes);
        return parse(bytes, filename);
    }

    public static Map<String, String> parse(byte[] bytes, String filename) throws PaletteParseException {
        return PropertiesPaletteParser.parse(bytes);
    }

}
