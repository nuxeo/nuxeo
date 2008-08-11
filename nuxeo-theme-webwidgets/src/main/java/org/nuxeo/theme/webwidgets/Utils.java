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

package org.nuxeo.theme.webwidgets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    static final Pattern bodyPattern = Pattern.compile("<body.*?>(.*?)</body>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    static final Pattern scriptPattern = Pattern.compile(
            "<script.*?>(.*?)</script>", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    static final Pattern stylePattern = Pattern.compile(
            "<style.*?>(.*?)</style>", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    static final Pattern preferencesPattern = Pattern.compile(
            "<widget:preferences>(.*?)</widget:preferences>", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    static final Pattern metadataPattern = Pattern.compile(
            "<meta name=\"([^\"]+)\" content=\"(.*?)\" />", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    static final Pattern iconPattern = Pattern.compile(
            "<link.*?rel=\"icon\".*?href=\"([^\"]+)\".*? />", Pattern.DOTALL
                    | Pattern.CASE_INSENSITIVE);

    // Utility class.
    private Utils() {
    }

    public static String extractBody(String html) {
        Matcher matcher = bodyPattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return html;
    }

    public static String extractScripts(String html) {
        Matcher matcher = scriptPattern.matcher(html);
        StringBuilder scripts = new StringBuilder();
        while (matcher.find()) {
            final String before = html.substring(0, matcher.start());
            final String after = html.substring(matcher.end());
            if (before.contains("<head>") && after.contains("</head>")) {
                scripts.append(matcher.group(1)).append('\n');
            }
        }
        return scripts.toString();
    }

    public static String extractStyles(String html) {
        Matcher matcher = stylePattern.matcher(html);
        StringBuilder styles = new StringBuilder();
        while (matcher.find()) {
            final String before = html.substring(0, matcher.start());
            final String after = html.substring(matcher.end());
            if (before.contains("<head>") && after.contains("</head>")) {
                styles.append(matcher.group(1));
            }
        }
        return styles.toString();
    }

    public static String extractMetadata(String html, String name) {
        Matcher matcher = metadataPattern.matcher(html);
        while (matcher.find()) {
            if (matcher.group(1).equals(name)) {
                return matcher.group(2);
            }
        }
        return null;
    }

    public static String extractIcon(String html) {
        Matcher matcher = iconPattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static List<WidgetFieldType> extractSchema(String source) {
        final List<WidgetFieldType> schema = new ArrayList<WidgetFieldType>();

        source = scriptPattern.matcher(source).replaceAll("");
        source = stylePattern.matcher(source).replaceAll("");
        source = bodyPattern.matcher(source).replaceAll("");
        source = metadataPattern.matcher(source).replaceAll("");

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final Document document;
        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(source.getBytes());
            final DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(in);
        } catch (Exception e) {
            log.error("Could not parse widget code." + e);
            return schema;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        final String[] attrNames = { "label", "type", "name", "defaultValue",
                "min", "max", "step", "onchange" };
        final NodeList nodes = document.getElementsByTagName("widget:preferences");
        if (nodes.getLength() != 1) {
            return schema;
        }
        final Node node = nodes.item(0);
        final NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE
                    && n.getNodeName().equals("preference")) {
                final WidgetFieldType widgetFieldType = new WidgetFieldType();
                final NamedNodeMap attrs = n.getAttributes();
                final Node typeAttr = attrs.getNamedItem("type");
                for (String attrName : attrNames) {
                    Node attrNode = attrs.getNamedItem(attrName);
                    if (attrNode != null) {
                        Field field;
                        try {
                            field = widgetFieldType.getClass().getField(
                                    attrName);
                        } catch (Exception e) {
                            log.error("Could not access field: " + attrName + e);
                            continue;
                        }
                        final String value = attrNode.getNodeValue();
                        try {
                            field.set(widgetFieldType, value);
                        } catch (Exception e) {
                            log.error("Could not set field: " + attrName);
                            continue;
                        }

                        if (typeAttr != null
                                && typeAttr.getNodeValue().equals("list")) {
                            NodeList optionNodes = n.getChildNodes();
                            List<WidgetFieldType.Option> options = new ArrayList<WidgetFieldType.Option>();
                            for (int j = 0; j < optionNodes.getLength(); j++) {
                                final Node optionNode = optionNodes.item(j);
                                if (optionNode.getNodeType() == Node.ELEMENT_NODE
                                        && optionNode.getNodeName().equals(
                                                "option")) {
                                    final NamedNodeMap optionAttrs = optionNode.getAttributes();
                                    Node optionLabelAttr = optionAttrs.getNamedItem("label");
                                    Node optionValueAttr = optionAttrs.getNamedItem("value");
                                    if (optionLabelAttr != null
                                            && optionValueAttr != null) {
                                        options.add(widgetFieldType.new Option(
                                                optionLabelAttr.getNodeValue(),
                                                optionValueAttr.getNodeValue()));
                                    }
                                }
                            }
                            widgetFieldType.setOptions(options);
                        }
                    }
                }
                schema.add(widgetFieldType);
            }
        }
        return schema;
    }

    public static String readResource(final String path) {
        String content = null;
        InputStream is = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    path);
            if (is == null) {
                log.warn("Resource not found: " + path);
            } else {
                Reader in = null;
                try {
                    in = new BufferedReader(new InputStreamReader(is));
                    final StringBuilder sb = new StringBuilder();
                    int ch;
                    while ((ch = in.read()) > -1) {
                        sb.append((char) ch);
                    }
                    content = sb.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    is = null;
                }
            }
        }
        return content;
    }

    public static String fetchUrl(URL url) {
        String content = null;
        try {
            final InputStream in = url.openStream();
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int i;
            while ((i = in.read(buffer)) != -1) {
                os.write(buffer, 0, i);
            }
            content = os.toString();
            in.close();
            os.close();
        } catch (IOException e) {
            log.error("Could not retrieve URL: " + url.toString());
        }
        return content;
    }

}
