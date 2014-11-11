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
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.CachingDef;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.properties.OrderedProperties;
import org.nuxeo.theme.themes.ThemeManager;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.css.CSSValue;

import com.steadystate.css.parser.CSSOMParser;

public final class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    private static final String[] lengthUnits = { "%", "em", "px", "ex", "pt",
            "in", "cm", "mm", "pc" };

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

    private static final Pattern imageUrlPattern = Pattern.compile(
            "^url\\s*\\([\\s,\",\']*(.*?)[\\s,\",\']*\\)$", Pattern.DOTALL);

    private static final Pattern rgbDigitPattern = Pattern.compile("([0-9]{1,3},[0-9]{1,3},[0-9]{1,3})");

    public static final Pattern PRESET_PATTERN = Pattern.compile("^\"(.*?)\"$",
            Pattern.DOTALL);

    private static final String EMPTY_CSS_SELECTOR = "EMPTY";

    private static final String CLASS_ATTR_PREFIX = "nxStyle";

    private static final String CSS_PROPERTIES_RESOURCE = "/nxthemes/html/styles/css.properties";

    private static final Properties cssProperties = new OrderedProperties();

    static {
        loadProperties(cssProperties, CSS_PROPERTIES_RESOURCE);
    }

    private Utils() {
        // This class is not supposed to be instantiated.
    }

    public static Properties getCssProperties() {
        return cssProperties;
    }

    public static String toJson(final Object object) {
        return JSONObject.fromObject(object).toString();
    }

    /* web lengths */

    public static String addWebLengths(final String length1,
            final String length2) {
        final WebLength webLength1 = getWebLength(length1);
        final WebLength webLength2 = getWebLength(length2);
        if (!webLength1.unit.equals(webLength2.unit)) {
            return null;
        }
        return new WebLength(webLength1.value + webLength2.value,
                webLength1.unit).toString();
    }

    public static String substractWebLengths(final String length1,
            final String length2) {
        final WebLength webLength1 = getWebLength(length1);
        final WebLength webLength2 = getWebLength(length2);
        if (!webLength1.unit.equals(webLength2.unit)) {
            return null;
        }
        return new WebLength(webLength1.value - webLength2.value,
                webLength1.unit).toString();
    }

    public static String divideWebLength(final String length, final int divider) {
        if (divider <= 0) {
            return null;
        }
        final WebLength webLength = getWebLength(length);
        if (webLength != null) {
            return new WebLength(webLength.value / divider, webLength.unit).toString();
        }
        return null;
    }

    public static WebLength getWebLength(final String length) {
        Integer value = null;
        String unit = null;
        for (String lengthUnit : lengthUnits) {
            if (length.endsWith(lengthUnit)) {
                unit = lengthUnit;
                try {
                    value = Integer.valueOf(length.substring(0, length.length()
                            - lengthUnit.length()));
                } catch (NumberFormatException e) {
                    log.error("Could not convert web lengths to integers", e);
                }
                break;
            }
        }
        if (value != null && unit != null) {
            return new WebLength(value, unit);
        }
        return null;
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

    public static String computeCssClassName(final Format style) {
        return String.format("%s%s", CLASS_ATTR_PREFIX, style.getUid());
    }

    public static String styleToCss(final Style style,
            final Collection<String> viewNames, final boolean resolvePresets,
            final boolean ignoreViewName, final boolean ignoreClassName,
            final boolean indent) {

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
                pSb.append(Utils.toUpperCamelCase(viewName));
                addSpace = true;
            }

            for (String path : style.getPathsForView(viewName)) {
                final Properties styleProperties = style.getPropertiesFor(
                        viewName, path);
                if (styleProperties.isEmpty()) {
                    continue;
                }

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
                    if (resolvePresets) {
                        value = PresetManager.resolvePresets(themeName, value);
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

        final CSSOMParser parser = new CSSOMParser();
        final InputSource is = new InputSource(new StringReader(cssSource));
        CSSStyleSheet css = null;
        try {
            css = parser.parseStyleSheet(is);
        } catch (NumberFormatException e) {
            log.error("Error while converting CSS value: \n" + cssSource);
        } catch (CSSException e) {
            log.error("Invalid CSS: \n" + cssSource);
        } catch (IOException e) {
            log.error("Could not parse CSS: \n" + cssSource);
        }

        if (css == null) {
            return;
        }

        // remove existing properties
        style.clearPropertiesFor(viewName);

        final CSSRuleList rules = css.getCssRules();
        for (int i = 0; i < rules.getLength(); i++) {
            final CSSRule rule = rules.item(i);
            if (rule.getType() == CSSRule.STYLE_RULE) {
                final CSSStyleRule sr = (CSSStyleRule) rule;
                final CSSStyleDeclaration s = sr.getStyle();
                final Properties properties = new Properties();
                for (int j = 0; j < s.getLength(); j++) {
                    final String propertyName = s.item(j);
                    final CSSValue value = s.getPropertyCSSValue(propertyName);
                    properties.setProperty(propertyName, value.toString());
                }
                String selector = sr.getSelectorText();
                if (selector.equals(EMPTY_CSS_SELECTOR)) {
                    selector = "";
                }
                style.setPropertiesFor(viewName, selector, properties);
            }
        }
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
        Matcher m = imageUrlPattern.matcher(value);
        if (m.matches()) {
            images.add(String.format("url(%s)", m.group(1)));
        }
        return images;
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
        Matcher m = imageUrlPattern.matcher(text);
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
            for (int i = 0; i < rgb.length; i++) {
                final int val = Integer.parseInt(rgb[i]);
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

    public static boolean supportsGzip(final HttpServletRequest request) {
        final String encoding = request.getHeader("Accept-Encoding");
        return encoding != null
                && encoding.toLowerCase(Locale.ENGLISH).contains("gzip");
    }

    public static void setCacheHeaders(final HttpServletResponse response,
            final CachingDef caching) {
        if (caching != null) {
            final String lifetime = caching.getLifetime();
            if (lifetime != null) {
                final long now = System.currentTimeMillis();
                response.addHeader("Cache-Control", "max-age=" + lifetime);
                response.addHeader("Cache-Control", "must-revalidate");
                response.setDateHeader("Last-Modified", now);
                response.setDateHeader("Expires", now + new Long(lifetime)
                        * 1000L);
            }
        }
    }

    public static boolean isVirtualHosting(final HttpServletRequest request) {
        if (request.getHeader("X-Forwarded-Host") != null) {
            return true;
        }
        return false;
    }
}
