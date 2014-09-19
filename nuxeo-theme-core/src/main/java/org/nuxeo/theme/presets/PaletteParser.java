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

package org.nuxeo.theme.presets;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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
        } catch (PaletteIdentifyException e) {
            log.error("Could not identify palette type: " + url);
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
        if (hexcolor.charAt(1) == hexcolor.charAt(2)
                && hexcolor.charAt(3) == hexcolor.charAt(4)
                && hexcolor.charAt(5) == hexcolor.charAt(6)) {
            return String.format("#%s%s%s", hexcolor.charAt(1),
                    hexcolor.charAt(4), hexcolor.charAt(6));
        }
        return hexcolor.toString();
    }

    public static PaletteFamily identifyPaletteType(byte[] bytes,
            String filename) throws PaletteIdentifyException {
        if (filename.endsWith(".aco")
                && PhotoshopPaletteParser.checkSanity(bytes)) {
            return PaletteFamily.PHOTOSHOP;
        } else if (filename.endsWith(".gpl")
                && GimpPaletteParser.checkSanity(bytes)) {
            return PaletteFamily.GIMP;
        } else if (filename.endsWith(".properties")
                && PropertiesPaletteParser.checkSanity(bytes)) {
            return PaletteFamily.PROPERTIES;
        }
        throw new PaletteIdentifyException();
    }

    public static Map<String, String> parse(InputStream in, String filename)
            throws IOException, PaletteIdentifyException, PaletteParseException {
        DataInputStream dis = new DataInputStream(in);
        byte[] bytes = new byte[dis.available()];
        dis.read(bytes);
        return parse(bytes, filename);
    }

    public static Map<String, String> parse(byte[] bytes, String filename)
            throws PaletteIdentifyException, PaletteParseException {
        PaletteFamily paletteFamily = identifyPaletteType(bytes, filename);
        if (paletteFamily == PaletteFamily.PHOTOSHOP) {
            return PhotoshopPaletteParser.parse(bytes);
        } else if (paletteFamily == PaletteFamily.GIMP) {
            return GimpPaletteParser.parse(bytes);
        } else if (paletteFamily == PaletteFamily.PROPERTIES) {
            return PropertiesPaletteParser.parse(bytes);
        }
        if (paletteFamily == null) {
            throw new PaletteParseException();
        }
        return null;
    }

    public static String renderPaletteAsCsv(byte[] bytes, String fileName) {
        StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw, '\t');
        try {
            for (Map.Entry<String, String> entry : parse(bytes, fileName).entrySet()) {
                String[] s = new String[2];
                s[0] = entry.getKey();
                s[1] = entry.getValue();
                writer.writeNext(s);
            }
        } catch (PaletteIdentifyException e) {
            log.warn("Could not identify palette type: " + fileName);
        } catch (PaletteParseException e) {
            log.warn("Could not parse palette: " + fileName);
        }
        return sw.toString();
    }

    public static Map<String, String> parseCsv(String text) {
        Map<String, String> properties = new HashMap<>();
        if (text == null) {
            return properties;
        }
        StringReader sr = new StringReader(text);
        CSVReader reader = new CSVReader(sr, '\t');
        String[] nextLine;
        try {
            while ((nextLine = reader.readNext()) != null) {
                properties.put(nextLine[0], nextLine[1]);
            }
        } catch (IOException e) {
            log.error(e, e);
        }
        return properties;
    }

}
