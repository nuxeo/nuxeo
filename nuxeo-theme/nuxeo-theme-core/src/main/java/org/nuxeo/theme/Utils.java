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

package org.nuxeo.theme;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.milyn.magger.CSSParser;
import org.milyn.magger.CSSProperty;
import org.milyn.magger.CSSRule;
import org.milyn.magger.CSSStylesheet;
import org.milyn.resource.URIResourceLocator;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.theme.formats.styles.Style;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.css.sac.Selector;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public final class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    private static final String EMPTY_CSS_SELECTOR = "EMPTY";

    private static final Pattern emptyCssSelectorPattern = Pattern.compile(
            "(.*?)\\{(.*?)\\}", Pattern.DOTALL);

    private Utils() {
        // This class is not supposed to be instantiated.
    }

    public static String listToCsv(List<String> list) {
        StringWriter sw = new StringWriter();
        CSVWriter writer = new CSVWriter(sw, ',');
        writer.writeNext(list.toArray(new String[0]));
        return sw.toString();
    }

    public static List<String> csvToList(String str) throws IOException {
        if ("".equals(str) || str == null) {
            return new ArrayList<String>();
        }
        StringReader sr = new StringReader(str);
        CSVReader reader = new CSVReader(sr, ',');
        return Arrays.asList(reader.readNext());
    }

    public static boolean contains(final String[] array, final String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static String cleanUp(String text) {
        return text.replaceAll("\n", " ").replaceAll("\\t+", " ").replaceAll(
                "\\s+", " ").trim();
    }

    public static byte[] readResourceAsBytes(final String path)
            throws IOException {
        return readResource(path).toByteArray();
    }

    public static String readResourceAsString(final String path)
            throws IOException {
        return readResource(path).toString();
    }

    private static ByteArrayOutputStream readResource(final String path)
            throws IOException {
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    path);
            if (is == null) {
                log.warn("Resource not found: " + path);
            } else {
                try {
                    os = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int i;
                    while ((i = is.read(buffer)) != -1) {
                        os.write(buffer, 0, i);
                    }
                    os.flush();
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } finally {
                    is = null;
                }
            }
        }
        return os;
    }

    public static byte[] fetchUrl(URL url) {
        byte[] data = null;
        try {
            final InputStream in = url.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int i;
            while ((i = in.read(buffer)) != -1) {
                os.write(buffer, 0, i);
            }
            data = os.toByteArray();
            in.close();
            os.close();
        } catch (IOException e) {
            log.error("Could not retrieve URL: " + url.toString());
        }
        return data;
    }

    public static void writeFile(URL url, String text) throws IOException {
        // local file system
        if (url.getProtocol().equals("file")) {
            String filepath = url.getFile();
            File file = new File(filepath);
            FileUtils.writeFile(file, text);
        } else {
            OutputStream os = null;
            URLConnection urlc;
            try {
                urlc = url.openConnection();
                os = urlc.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error(e);
            }

            if (os != null) {
                try {
                    os.write(text.getBytes());
                    os.flush();
                } catch (IOException e) {
                    log.error(e);
                } finally {
                    try {
                        os.close();
                    } catch (IOException e) {
                        log.error(e);
                    } finally {
                        os = null;
                    }
                }
            }

        }

    }

    public static void loadProperties(final Properties properties,
            final String resourceName) {
        if (properties.isEmpty()) {
            InputStream in = null;
            try {
                in = Utils.class.getResourceAsStream(resourceName);
                if (in != null) {
                    properties.load(in);
                }
            } catch (IOException e) {
                log.error("Could not load properties", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error("Failed to close stream", e);
                    }
                }
            }
        }
    }

    public static void loadCss(final Style style, String cssSource,
            final String viewName) {
        // pre-processing: replace empty selectors (which are invalid selectors)
        // with a marker selector

        final Matcher matcher = emptyCssSelectorPattern.matcher(cssSource);
        final StringBuilder buf = new StringBuilder();
        while (matcher.find()) {
            if (matcher.group(1).trim().equals("")) {
                buf.append(EMPTY_CSS_SELECTOR);
            }
            buf.append(matcher.group(0));
        }
        cssSource = buf.toString();
        CSSStylesheet styleSheet = null;
        CSSParser parser = new CSSParser(new URIResourceLocator());
        try {
            styleSheet = parser.parse(cssSource, URI.create(""), null);
        } catch (Exception e) {
            log.error("Could not parse CSS:\n" + cssSource, e);
            return;
        }

        // remove existing properties
        style.clearPropertiesFor(viewName);

        CssStringWriter cssWriter = new CssStringWriter();

        Iterator<CSSRule> rules = styleSheet.getRules().iterator();
        while (rules.hasNext()) {
            CSSRule rule = rules.next();

            final Properties styleProperties = new Properties();

            /* CSS selector */
            Selector selector = rule.getSelector();
            cssWriter.write(selector);
            String selectorStr = cssWriter.toText();

            if (selectorStr.equals(EMPTY_CSS_SELECTOR)) {
                selectorStr = "";
            }

            /* CSS properties */
            CSSProperty property = rule.getProperty();
            LexicalUnit value = property.getValue();
            cssWriter.write(value, " ");
            String strValue = cssWriter.toText();

            if (property == null) {
                styleProperties.setProperty("", "");
            } else {
                styleProperties.setProperty(property.getName(), strValue);
            }
            style.setPropertiesFor(viewName, selectorStr, styleProperties);
        }
    }
}
