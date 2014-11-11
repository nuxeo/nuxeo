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

package org.nuxeo.theme.html;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.properties.OrderedProperties;
import org.nuxeo.theme.resources.ImageInfo;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;

public final class CSSUtils {

    static final Log log = LogFactory.getLog(CSSUtils.class);

    private static final String EMPTY_CSS_SELECTOR = "EMPTY";

    private static final String CLASS_ATTR_PREFIX = "nxStyle";

    private static final String CSS_PROPERTIES_RESOURCE = "/nxthemes/html/styles/css.properties";

    private static final Pattern firstTagPattern = Pattern.compile("<(.*?)>",
            Pattern.DOTALL);

    private static final Pattern otherTagsPattern = Pattern.compile(
            "<.*?>(.*)", Pattern.DOTALL);

    private static final Pattern classAttrPattern = Pattern.compile(
            " class=\"(.*?)\"", Pattern.DOTALL);

    private static final Pattern emptyCssSelectorPattern = Pattern.compile(
            "(.*?)\\{(.*?)\\}", Pattern.DOTALL);

    private static final Pattern hexColorPattern = Pattern.compile(
            ".*?#(\\p{XDigit}{3,6}).*?", Pattern.DOTALL);

    private static final Pattern rgbColorPattern = Pattern.compile(
            ".*?rgb\\s*\\(\\s*([0-9,\\s]+)\\s*\\).*?", Pattern.DOTALL);

    private static final Pattern urlPattern = Pattern.compile(
            "^url\\s*\\([\\s,\",\']*(.*?)[\\s,\",\']*\\)$", Pattern.DOTALL);

    private static final Pattern partialUrlPattern = Pattern.compile(
            "url\\s*\\([\\s,\",\']*([^/].*?)[\\s,\",\']*\\)", Pattern.DOTALL);

    private static final Pattern rgbDigitPattern = Pattern.compile("([0-9]{1,3},[0-9]{1,3},[0-9]{1,3})");

    private static final Properties cssProperties = new OrderedProperties();

    static {
        org.nuxeo.theme.Utils.loadProperties(cssProperties,
                CSS_PROPERTIES_RESOURCE);
    }

    private CSSUtils() {
        // This class is not supposed to be instantiated.
    }

    public static Properties getCssProperties() {
        return cssProperties;
    }

    public static String styleToCss(final Style style,
            final Collection<String> viewNames, final boolean ignoreViewName,
            final boolean ignoreClassName, final boolean indent) {

        String themeName = null;
        if (style.isNamed()) {
            themeName = Manager.getThemeManager().getThemeNameOfNamedObject(
                    style);
        } else {
            ThemeElement theme = ThemeManager.getThemeOfFormat(style);
            if (theme != null) {
                themeName = theme.getName();
            }
        }

        final StringBuilder sb = new StringBuilder();
        final StringBuilder pSb = new StringBuilder();
        for (String viewName : viewNames) {
            final String className = computeCssClassName(style);
            pSb.setLength(0);
            boolean addSpace = false;
            if (!ignoreClassName) {
                pSb.append('.').append(className);
                addSpace = true;
            }
            if (!ignoreViewName && !"*".equals(viewName)) {
                pSb.append(toUpperCamelCase(viewName));
                addSpace = true;
            }

            for (String path : style.getPathsForView(viewName)) {
                final Properties styleProperties = style.getPropertiesFor(
                        viewName, path);
                // if (styleProperties.isEmpty()) {
                // continue;
                // }

                final String[] splitPaths = path.split(",");
                final int len = splitPaths.length;
                for (int i = 0; i < len; i++) {
                    sb.append(pSb);
                    if (addSpace && !"".equals(path)) {
                        sb.append(' ');
                    }
                    sb.append(splitPaths[i].trim());
                    if (i < len - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(" {");
                if (indent) {
                    sb.append('\n');
                }

                final Enumeration<?> propertyNames = cssProperties.propertyNames();
                while (propertyNames.hasMoreElements()) {
                    final String propertyName = (String) propertyNames.nextElement();
                    String value = styleProperties.getProperty(propertyName);
                    if (value == null) {
                        continue;
                    }
                    if (indent) {
                        sb.append("  ");
                    }
                    sb.append(propertyName);
                    sb.append(':');
                    if (indent) {
                        sb.append(' ');
                    }
                    sb.append(value).append(';');
                    if (indent) {
                        sb.append('\n');
                    }
                }
                sb.append("}\n");
                if (indent) {
                    sb.append('\n');
                }

            }
        }
        return sb.toString();
    }

    public static String insertCssClass(final String markup,
            final String className) {
        final Matcher firstMatcher = firstTagPattern.matcher(markup);
        final Matcher othersMatcher = otherTagsPattern.matcher(markup);

        if (!(firstMatcher.find() && othersMatcher.find())) {
            return markup;
        }

        // find a 'class="...."' match
        String inBrackets = firstMatcher.group(1);
        final Matcher classAttrMatcher = classAttrPattern.matcher(inBrackets);

        // build a new 'class="..."' string
        final StringBuilder classAttributes = new StringBuilder();
        if (classAttrMatcher.find()) {
            classAttributes.append(classAttrMatcher.group(1));
            if (!classAttributes.toString().endsWith(" ")) {
                classAttributes.append(' ');
            }
        }

        // add new attributes
        classAttributes.append(className);

        if (classAttributes.length() == 0) {
            return markup;

        }
        // remove the old 'class="..."' attributes, if there were some
        inBrackets = inBrackets.replaceAll(classAttrPattern.toString(), "");

        // write the final markup
        if (inBrackets.endsWith("/")) {
            return String.format("<%s class=\"%s\" />%s",
                    inBrackets.replaceAll("/$", "").trim(),
                    classAttributes.toString(), othersMatcher.group(1));
        }
        return String.format("<%s class=\"%s\">%s", inBrackets,
                classAttributes.toString(), othersMatcher.group(1));

    }

    public static String computeCssClassName(final Style style) {
        String collectionName = style.getCollection();
        String prefix = CLASS_ATTR_PREFIX;
        if (collectionName != null) {
            prefix = toCamelCase(collectionName);
        }
        return String.format("%s%s", prefix, style.getUid());
    }

    public static String replaceColor(String text, String before, String after) {
        Matcher m = hexColorPattern.matcher(text);
        text = text.trim();
        while (m.find()) {
            String found = "#" + optimizeHexColor(m.group(1));
            if (found.equals(before)) {
                text = text.replace(String.format("#%s", m.group(1)), after);
            }
        }
        m = rgbColorPattern.matcher(text);
        while (m.find()) {
            String found = "#" + optimizeHexColor(rgbToHex(m.group(1)));
            if (found.equals(before)) {
                text = text.replace(String.format("rgb(%s)", m.group(1)), after);
            }
        }
        return text;
    }

    public static String replaceImage(String text, String before, String after) {
        text = text.trim();
        Matcher m = urlPattern.matcher(text);
        if (m.matches()) {
            String found = String.format("url(%s)", m.group(1));
            if (found.equals(before)) {
                text = text.replace(String.format("url(%s)", m.group(1)), after);
            }
        }
        return text;
    }

    public static String optimizeHexColor(String value) {
        value = value.toLowerCase();
        if (value.length() != 6) {
            return value;
        }
        if ((value.charAt(0) == value.charAt(1))
                && (value.charAt(2) == value.charAt(3))
                && (value.charAt(4) == value.charAt(5))) {
            return String.format("%s%s%s", value.charAt(0), value.charAt(2),
                    value.charAt(4));
        }
        return value;
    }

    public static String rgbToHex(String value) {
        value = value.replaceAll("\\s", "");
        final Matcher m = rgbDigitPattern.matcher(value);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String[] rgb = m.group(1).split(",");
            final StringBuffer hexcolor = new StringBuffer();
            for (String element : rgb) {
                final int val = Integer.parseInt(element);
                if (val < 16) {
                    hexcolor.append("0");
                }
                hexcolor.append(Integer.toHexString(val));
            }
            m.appendReplacement(sb, hexcolor.toString());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static List<String> extractCssColors(String value) {
        final List<String> colors = new ArrayList<String>();
        value = value.trim();
        Matcher m = hexColorPattern.matcher(value);
        while (m.find()) {
            colors.add("#" + optimizeHexColor(m.group(1)));
        }
        m = rgbColorPattern.matcher(value);
        while (m.find()) {
            colors.add("#" + optimizeHexColor(rgbToHex(m.group(1))));
        }
        return colors;
    }

    public static List<String> extractCssImages(String value) {
        final List<String> images = new ArrayList<String>();
        value = value.trim();
        Matcher m = urlPattern.matcher(value);
        if (m.matches()) {
            images.add(String.format("url(%s)", m.group(1)));
        }
        return images;
    }

    public static String toCamelCase(final String value) {
        if (value == null || value.trim().equals("")) {
            return value;
        }
        final String newValue = value.replaceAll("[^\\p{Alnum}]+", " ");
        final StringBuilder sb = new StringBuilder();
        final String[] parts = newValue.trim().split("\\s+");
        sb.append(parts[0].toLowerCase(Locale.ENGLISH));
        for (int i = 1; i < parts.length; ++i) {
            sb.append(parts[i].substring(0, 1).toUpperCase());
            sb.append(parts[i].substring(1).toLowerCase(Locale.ENGLISH));
        }
        return sb.toString();
    }

    public static String toUpperCamelCase(final String value) {
        if ("".equals(value)) {
            return "";
        }
        final String newValue = toCamelCase(value);
        final StringBuilder sb = new StringBuilder();
        sb.append(newValue.substring(0, 1).toUpperCase());
        sb.append(newValue.substring(1));
        return sb.toString();
    }

    public static String compressSource(final String source)
            throws ThemeException {
        String compressedSource = source;
        Reader in = null;
        Writer out = null;
        final CssCompressor compressor;
        final int linebreakpos = -1;
        try {
            in = new StringReader(source);
            out = new StringWriter();
            compressor = new CssCompressor(in);
            compressor.compress(out, linebreakpos);
            compressedSource = out.toString();

        } catch (IOException e) {
            throw new ThemeException("Could not compress CSS", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    out = null;
                }
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.error(e, e);
            } finally {
                in = null;
            }
        }
        return compressedSource;
    }

    public static String expandPartialUrls(String text, String cssContextPath) {
        Matcher m = partialUrlPattern.matcher(text);
        if (!cssContextPath.endsWith("/")) {
            cssContextPath += "/";
        }
        String replacement = String.format("url(%s$1)",
                Matcher.quoteReplacement(cssContextPath));
        return m.replaceAll(replacement);
    }

    public static String expandVariables(String text, String basePath,
            ThemeDescriptor themeDescriptor) {

        String themeName = themeDescriptor.getName();

        if (basePath != null) {
            text = text.replaceAll("\\$\\{basePath\\}",
                    Matcher.quoteReplacement(basePath));
        }

        String contextPath = VirtualHostHelper.getContextPathProperty();

        // Replace presets
        text = PresetManager.resolvePresets(themeName, text);

        // Replace images from resource banks
        String resourceBankName = themeDescriptor.getResourceBankName();
        if (resourceBankName != null) {
            ResourceBank resourceBank;
            try {
                resourceBank = ThemeManager.getResourceBank(resourceBankName);
                for (ImageInfo image : resourceBank.getImages()) {
                    String path = image.getPath();
                    text = text.replaceAll(
                            Matcher.quoteReplacement(path),
                            Matcher.quoteReplacement(String.format(
                                    "%s/nxthemes-images/%s/%s", contextPath,
                                    resourceBankName, path.replace(" ", "%20"))));
                }
            } catch (ThemeException e) {
                log.warn("Could not get the list of bank images in: "
                        + resourceBankName);
            }
        }

        return text;
    }
}
