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

package org.nuxeo.theme.themes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ThemeParser {

    private static final Log log = LogFactory.getLog(ThemeParser.class);

    private static final String DOCROOT_NAME = "theme";

    private static final XPath xpath = XPathFactory.newInstance().newXPath();

    public static String registerTheme(URL url) {
        String themeName = null;
        InputStream in = null;
        try {
            in = url.openStream();
            themeName = registerTheme(in);
        } catch (FileNotFoundException e) {
            log.error("File not found: " + url);
        } catch (IOException e) {
            log.error("Could not open file: " + url);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("Could not read theme", e);
                } finally {
                    in = null;
                }
            }
        }
        return themeName;
    }

    public static String registerTheme(final InputStream in) {
        try {
            final InputSource is = new InputSource(in);
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final ThemeManager themeManager = Manager.getThemeManager();

            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document document = db.parse(is);
            final org.w3c.dom.Element docElem = document.getDocumentElement();
            if (!docElem.getNodeName().equals(DOCROOT_NAME)) {
                log.warn("No <" + DOCROOT_NAME + "> document tag found in "
                        + in.toString() + ", ignoring the resource.");
                return null;
            }

            String themeName = docElem.getAttributes().getNamedItem("name").getNodeValue();
            if (!themeName.matches("[a-z0-9_\\-]+")) {
                log.error("Theme names may only contain lower-case alpha-numeric characters, underscores and hyphens.");
                return null;
            }

            // remove old theme
            ThemeElement oldTheme = themeManager.getThemeByName(themeName);
            if (oldTheme != null) {
                themeManager.destroyElement(oldTheme);
            }

            // create a new theme
            ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
            theme.setName(themeName);
            String description = getCommentAssociatedTo(docElem);
            theme.setDescription(description);

            // register formats
            for (Node n : getChildElementsByTagName(docElem, "formats")) {
                parseFormats(theme, docElem, n);
            }

            // register element properties
            for (Node n : getChildElementsByTagName(docElem, "properties")) {
                parseProperties(docElem, n);
            }

            Node baseNode = getBaseNode(docElem);
            if (baseNode == null) {
                log.warn(in.toString() + ": no <layout> section  found.");
                return null;
            }

            parseLayout(theme, baseNode);
            themeManager.registerTheme(theme);
            return themeName;

        } catch (Exception e) {
            log.error("Could not register theme", e);
        }
        return null;
    }

    private static void parseLayout(final Element parent, Node node)
            throws Exception {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (String formatName : typeRegistry.getTypeNames(TypeFamily.FORMAT)) {
            Object format = node.getUserData(formatName);
            if (format != null) {
                ElementFormatter.setFormat(parent, (Format) format);
            }
        }

        Properties properties = (Properties) node.getUserData("properties");
        if (properties != null) {
            FieldIO.updateFieldsFromProperties(parent, properties);
        }

        for (Node n : getChildElements(node)) {
            String nodeName = n.getNodeName();
            NamedNodeMap attributes = n.getAttributes();
            Element elem;

            if ("fragment".equals(nodeName)) {
                String fragmentType = attributes.getNamedItem("type").getNodeValue();
                elem = FragmentFactory.create(fragmentType);
                if (elem == null) {
                    log.error("Could not create fragment: " + fragmentType);
                    continue;
                }
                Fragment fragment = (Fragment) elem;
                Node perspectives = attributes.getNamedItem("perspectives");
                if (perspectives != null) {
                    for (String perspectiveName : perspectives.getNodeValue().split(
                            ",")) {

                        PerspectiveType perspective = (PerspectiveType) typeRegistry.lookup(
                                TypeFamily.PERSPECTIVE, perspectiveName);

                        if (perspective == null) {
                            log.warn("Could not find perspective: "
                                    + perspectiveName);
                        } else {
                            fragment.setVisibleInPerspective(perspective);
                        }
                    }
                }
            } else {
                elem = ElementFactory.create(nodeName);
            }

            if (elem == null) {
                log.error("Could not parse node: " + nodeName);
                return;
            }

            if (elem instanceof PageElement) {
                String pageName = attributes.getNamedItem("name").getNodeValue();
                if (!pageName.matches("[a-z0-9_\\-]+")) {
                    log.error("Page names may only contain lower-case alpha-numeric characters, underscores and hyphens.");
                    return;
                }
                elem.setName(pageName);
            }

            String description = getCommentAssociatedTo(n);
            if (description != null) {
                elem.setDescription(description);
            }

            parent.addChild(elem);
            parseLayout(elem, n);
        }
    }

    private static void parseFormats(final ThemeElement theme,
            org.w3c.dom.Element doc, Node node) {
        Node baseNode = getBaseNode(doc);
        String themeName = theme.getName();
        ThemeManager themeManager = Manager.getThemeManager();

        Map<Style, Map<String, Properties>> newStyles = new LinkedHashMap<Style, Map<String, Properties>>();

        for (Node n : getChildElements(node)) {
            String nodeName = n.getNodeName();
            NamedNodeMap attributes = n.getAttributes();
            Node elementItem = attributes.getNamedItem("element");
            String elementXPath = null;
            if (elementItem != null) {
                elementXPath = elementItem.getNodeValue();
            }

            Format format = FormatFactory.create(nodeName);
            format.setProperties(getPropertiesFromNode(n));

            String description = getCommentAssociatedTo(n);
            if (description != null) {
                format.setDescription(description);
            }

            if ("widget".equals(nodeName)) {
                List<Node> viewNodes = getChildElementsByTagName(n, "view");
                if (!viewNodes.isEmpty()) {
                    format.setName(viewNodes.get(0).getTextContent());
                }

            } else if ("layout".equals(nodeName)) {
                // TODO: validate layout properties

            } else if ("style".equals(nodeName)) {
                Node nameAttr = attributes.getNamedItem("name");
                Node inheritedAttr = attributes.getNamedItem("inherit");

                // register the style name
                String styleName = null;
                Style style = (Style) format;
                if (nameAttr != null) {
                    styleName = nameAttr.getNodeValue();
                    style.setName(styleName);
                    themeManager.setNamedObject(theme.getName(), "style", style);
                }

                if (inheritedAttr != null) {
                    String inheritedName = inheritedAttr.getNodeValue();
                    Style inheritedStyle = (Style) themeManager.getNamedObject(
                            themeName, "style", inheritedName);
                    if (inheritedStyle == null) {
                        log.error("Unknown style: " + inheritedName);
                    } else {
                        themeManager.makeFormatInherit(style, inheritedStyle);
                        log.debug("Made style " + style + " inherit from "
                                + inheritedName);
                    }
                }

                if (styleName != null && elementXPath != null) {
                    log.warn("Style parser: named style '" + styleName
                            + "' cannot have an 'element' attribute: '"
                            + elementXPath + "'.");
                    continue;
                }

                for (Node selectorNode : getChildElementsByTagName(n,
                        "selector")) {
                    NamedNodeMap attrs = selectorNode.getAttributes();
                    Node pathAttr = attrs.getNamedItem("path");
                    if (pathAttr == null) {
                        log.warn(String.format(
                                "Style parser: named style '%s' has a selector with no path: ignored",
                                styleName));
                        continue;
                    }
                    String path = pathAttr.getNodeValue();

                    String viewName = null;
                    Node viewAttr = attrs.getNamedItem("view");
                    if (viewAttr != null) {
                        viewName = viewAttr.getNodeValue();
                    }

                    String selectorDescription = getCommentAssociatedTo(selectorNode);
                    if (selectorDescription != null) {
                        style.setSelectorDescription(path, viewName,
                                selectorDescription);
                    }

                    if (elementXPath != null
                            && (viewName == null || viewName.equals("*"))) {
                        log.info("Style parser: trying to guess the view name for: "
                                + elementXPath);
                        viewName = guessViewNameFor(doc, elementXPath);
                        if (viewName == null) {
                            if (!newStyles.containsKey(style)) {
                                newStyles.put(style,
                                        new LinkedHashMap<String, Properties>());
                            }
                            newStyles.get(style).put(path,
                                    getPropertiesFromNode(selectorNode));
                        }
                    }

                    if (styleName != null) {
                        if (viewName != null) {
                            log.info("Style parser: ignoring view name '"
                                    + viewName + "' in named style '"
                                    + styleName + "'.");
                        }
                        viewName = "*";
                    }

                    if (viewName != null) {
                        style.setPropertiesFor(viewName, path,
                                getPropertiesFromNode(selectorNode));
                    }
                }
            }

            themeManager.registerFormat(format);
            if (elementXPath != null) {
                if ("".equals(elementXPath)) {
                    baseNode.setUserData(nodeName, format, null);
                } else {
                    for (Node element : getNodesByXPath(baseNode, elementXPath)) {
                        element.setUserData(nodeName, format, null);
                    }
                }
            }
        }

        // styles created by the parser
        int count = 1;
        for (Style parent : newStyles.keySet()) {
            Style s = (Style) FormatFactory.create("style");
            String name = "";
            while (true) {
                name = String.format("common style %s", count);
                if (themeManager.getNamedObject(themeName, "style", name) == null) {
                    break;
                }
                count += 1;
            }
            s.setName(name);
            themeManager.registerFormat(s);
            themeManager.setNamedObject(themeName, "style", s);
            Map<String, Properties> map = newStyles.get(parent);
            for (Map.Entry<String, Properties> entry : map.entrySet()) {
                s.setPropertiesFor("*", entry.getKey(), entry.getValue());
            }
            // if the style already inherits, preserve the inheritance
            Style ancestor = (Style) ThemeManager.getAncestorFormatOf(parent);
            if (ancestor != null) {
                themeManager.makeFormatInherit(s, ancestor);
            }

            themeManager.makeFormatInherit(parent, s);
            log.info("Created extra style: " + s.getName());
        }
    }

    private static void parseProperties(org.w3c.dom.Element doc, Node node) {
        NamedNodeMap attributes = node.getAttributes();
        Node elementAttr = attributes.getNamedItem("element");
        if (elementAttr == null) {
            log.error("<properties> node has no 'element' attribute.");
            return;
        }
        String elementXPath = elementAttr.getNodeValue();

        Node baseNode = getBaseNode(doc);
        Node element = null;
        try {
            element = (Node) xpath.evaluate(elementXPath, baseNode,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            log.error("Could not parse properties", e);
        }
        if (element == null) {
            log.warn("Could not find the element associated to: "
                    + elementXPath);
            return;
        }
        element.setUserData("properties", getPropertiesFromNode(node), null);
    }

    private static Properties getPropertiesFromNode(Node node) {
        Properties properties = new Properties();
        for (Node n : getChildElements(node)) {
            String textContent = n.getTextContent();
            Node presetAttr = n.getAttributes().getNamedItem("preset");
            if (presetAttr != null) {
                String presetName = presetAttr.getNodeValue();
                if (presetName != null) {
                    textContent = String.format("\"%s\"", presetName);
                }
            }
            properties.setProperty(n.getNodeName(), textContent);
        }
        return properties;
    }

    private static List<Node> getChildElements(Node node) {
        List<Node> nodes = new ArrayList<Node>();
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                nodes.add(n);
            }
        }
        return nodes;
    }

    private static List<Node> getChildElementsByTagName(Node node,
            String tagName) {
        List<Node> nodes = new ArrayList<Node>();
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE
                    && tagName.equals(n.getNodeName())) {
                nodes.add(n);
            }
        }
        return nodes;
    }

    private static Node getBaseNode(org.w3c.dom.Element doc) {
        Node baseNode = null;
        try {
            baseNode = (Node) xpath.evaluate('/' + DOCROOT_NAME + "/layout",
                    doc, XPathConstants.NODE);
        } catch (XPathExpressionException le) {
            log.warn("Could not find the layout node");
        }
        return baseNode;
    }

    private static String getCommentAssociatedTo(Node node) {
        Node n = node;
        while (true) {
            n = n.getPreviousSibling();
            if (n == null) {
                break;
            }
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                break;
            }
            if (n.getNodeType() == Node.COMMENT_NODE) {
                return n.getNodeValue().trim();
            }
        }
        return null;
    }

    private static String guessViewNameFor(org.w3c.dom.Element doc,
            String elementXPath) {
        NodeList widgetNodes = doc.getElementsByTagName("widget");
        Set<String> candidates = new HashSet<String>();
        String[] elements = elementXPath.split("\\|");
        for (int i = 0; i < widgetNodes.getLength(); i++) {
            Node node = widgetNodes.item(i);
            NamedNodeMap attributes = node.getAttributes();
            Node elementAttr = attributes.getNamedItem("element");
            if (elementAttr != null) {
                String[] widgetElements = elementAttr.getNodeValue().split(
                        "\\|");
                for (String element : elements) {
                    for (String widgetElement : widgetElements) {
                        if (element.equals(widgetElement)) {
                            List<Node> viewNodes = getChildElementsByTagName(
                                    node, "view");
                            if (!viewNodes.isEmpty()) {
                                candidates.add(viewNodes.get(0).getTextContent());
                            }
                        }
                    }
                }
            }
        }
        if (candidates.size() == 1) {
            return candidates.iterator().next();
        }
        return null;
    }

    private static List<Node> getNodesByXPath(Node baseNode, String elementXPath) {
        final List<Node> nodes = new ArrayList<Node>();
        if (elementXPath != null) {
            try {
                NodeList elementNodes = (NodeList) xpath.evaluate(elementXPath,
                        baseNode, XPathConstants.NODESET);
                for (int i = 0; i < elementNodes.getLength(); i++) {
                    nodes.add(elementNodes.item(i));
                }
            } catch (XPathExpressionException e) {
                log.warn("Could not parse the path: " + elementXPath);
            }
        }
        return nodes;
    }
}
