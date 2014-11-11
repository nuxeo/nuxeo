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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.CustomPresetType;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ThemeParser {

    private static final Log log = LogFactory.getLog(ThemeParser.class);

    private static final String DOCROOT_NAME = "theme";

    private static final XPath xpath = XPathFactory.newInstance().newXPath();

    public static void registerTheme(final ThemeDescriptor themeDescriptor,
            final boolean preload) throws ThemeIOException {
        registerTheme(themeDescriptor, null, preload);
    }

    public static void registerTheme(final ThemeDescriptor themeDescriptor,
            final String xmlSource, final boolean preload)
            throws ThemeIOException {
        final String src = themeDescriptor.getSrc();
        InputStream in = null;
        try {
            if (xmlSource == null) {
                URL url = null;
                try {
                    url = new URL(src);
                } catch (MalformedURLException e) {
                    if (themeDescriptor.getContext() != null) {
                        url = themeDescriptor.getContext().getResource(src);
                    } else {
                        url = Thread.currentThread().getContextClassLoader().getResource(
                                src);
                    }
                }
                if (url == null) {
                    throw new ThemeIOException("Incorrect theme URL: " + src);
                }
                in = url.openStream();
            } else {
                in = new ByteArrayInputStream(xmlSource.getBytes());
            }
            registerThemeFromInputStream(themeDescriptor, in, preload);
        } catch (FileNotFoundException e) {
            throw new ThemeIOException("File not found: " + src, e);
        } catch (IOException e) {
            throw new ThemeIOException("Could not open file: " + src, e);
        } catch (ThemeException e) {
            throw new ThemeIOException("Parsing error: " + src, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                } finally {
                    in = null;
                }
            }
        }
    }

    private static void registerThemeFromInputStream(
            final ThemeDescriptor themeDescriptor, final InputStream in,
            boolean preload) throws ThemeIOException, ThemeException {
        String themeName = null;

        final InputSource is = new InputSource(in);
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        } catch (ParserConfigurationException e) {
            log.debug("Could not set DTD non-validation feature");
        }

        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ThemeIOException(e);
        }
        Document document;
        try {
            document = db.parse(is);
        } catch (SAXException e) {
            throw new ThemeIOException(e);
        } catch (IOException e) {
            throw new ThemeIOException(e);
        }
        final org.w3c.dom.Element docElem = document.getDocumentElement();
        if (!docElem.getNodeName().equals(DOCROOT_NAME)) {
            throw new ThemeIOException("No <" + DOCROOT_NAME
                    + "> document tag found in " + in.toString()
                    + ", ignoring the resource.");
        }

        themeName = docElem.getAttributes().getNamedItem("name").getNodeValue();
        if (!ThemeManager.validateThemeName(themeName)) {
            throw new ThemeIOException(
                    "Theme names may only contain alpha-numeric characters, underscores and hyphens: "
                            + themeName);
        }
        themeDescriptor.setName(themeName);

        loadTheme(themeDescriptor, docElem, preload);
    }

    private static void loadTheme(ThemeDescriptor themeDescriptor,
            org.w3c.dom.Element docElem, boolean preload)
            throws ThemeException, ThemeIOException {
        final ThemeManager themeManager = Manager.getThemeManager();

        // remove old theme
        String themeName = themeDescriptor.getName();
        ThemeElement oldTheme = themeManager.getThemeByName(themeName);
        if (oldTheme != null) {
            try {
                themeManager.destroyElement(oldTheme);
            } catch (NodeException e) {
                throw new ThemeIOException("Failed to destroy theme: "
                        + themeName, e);
            }
        }

        Node baseNode = getBaseNode(docElem);

        final Map<Integer, String> inheritanceMap = new HashMap<Integer, String>();
        final Map<Style, Map<String, Properties>> commonStyles = new LinkedHashMap<Style, Map<String, Properties>>();

        // create a new theme
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        theme.setName(themeName);
        Node description = docElem.getAttributes().getNamedItem("description");
        if (description != null) {
            theme.setDescription(description.getNodeValue());
        }

        String resourceBankName = null;
        Node resourceBankNode = docElem.getAttributes().getNamedItem(
                "resource-bank");
        if (resourceBankNode != null) {
            resourceBankName = resourceBankNode.getNodeValue();
            themeDescriptor.setResourceBankName(resourceBankName);
        }

        Node templateEngines = docElem.getAttributes().getNamedItem(
                "template-engines");
        if (templateEngines != null) {
            themeDescriptor.setTemplateEngines(Arrays.asList(templateEngines.getNodeValue().split(
                    ",")));
        }

        if (preload) {
            // Only register pages
            registerThemePages(theme, baseNode);

        } else {
            // Register resources from remote bank
            if (resourceBankName != null) {
                try {
                    ResourceBank resourceBank = ThemeManager.getResourceBank(resourceBankName);
                    resourceBank.connect(themeName);
                } catch (ThemeException e) {
                    log.warn("Resource bank not found: " + resourceBankName);
                }
            }

            // register custom presets
            for (Node n : getChildElementsByTagName(docElem, "presets")) {
                parsePresets(theme, n);
            }

            // register formats
            for (Node n : getChildElementsByTagName(docElem, "formats")) {
                parseFormats(theme, docElem, commonStyles, inheritanceMap, n);
            }

            // setup style inheritance
            for (Map.Entry<Integer, String> entry : inheritanceMap.entrySet()) {
                Integer styleUid = entry.getKey();
                String inheritedStyleName = entry.getValue();
                Format style = ThemeManager.getFormatById(styleUid);
                Format inheritedStyle = (Format) themeManager.getNamedObject(
                        themeName, "style", inheritedStyleName);
                if (inheritedStyle == null) {
                    log.warn("Cannot make style inherit from unknown style : "
                            + inheritedStyleName);
                    continue;
                }
                themeManager.makeFormatInherit(style, inheritedStyle);
            }

            // styles created by the parser
            createCommonStyles(themeName, commonStyles);

            // register element properties
            for (Node n : getChildElementsByTagName(docElem, "properties")) {
                parseProperties(docElem, n);
            }

            parseLayout(theme, baseNode);

            themeManager.removeOrphanedFormats();
        }

        if (preload) {
            log.debug("Registered THEME: " + themeName);
            themeDescriptor.setLastLoaded(null);
        } else {
            log.debug("Loaded THEME: " + themeName);
            themeDescriptor.setLastLoaded(new Date());
        }

        // Register in the type registry
        themeManager.registerTheme(theme);
    }

    public static boolean checkElementName(String name) throws ThemeIOException {
        return name.matches("[a-z][a-z0-9_\\-\\s]+");
    }

    public static void registerThemePages(final Element parent, Node node)
            throws ThemeIOException, ThemeException {
        for (Node n : getChildElements(node)) {
            String nodeName = n.getNodeName();
            NamedNodeMap attributes = n.getAttributes();
            Element elem;
            if ("page".equals(nodeName)) {
                elem = ElementFactory.create(nodeName);

                Node nameAttr = attributes.getNamedItem("name");
                if (nameAttr != null) {
                    String elementName = nameAttr.getNodeValue();
                    if (checkElementName(elementName)) {
                        elem.setName(elementName);
                    } else {
                        throw new ThemeIOException("Page name not allowed: "
                                + elementName);
                    }
                }

                try {
                    parent.addChild(elem);
                } catch (NodeException e) {
                    throw new ThemeIOException("Failed to parse layout.", e);
                }
            }
        }
    }

    public static void parseLayout(final Element parent, Node node)
            throws ThemeIOException, ThemeException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ThemeManager themeManager = Manager.getThemeManager();
        for (String formatName : typeRegistry.getTypeNames(TypeFamily.FORMAT)) {
            Format format = (Format) node.getUserData(formatName);
            if (format != null) {
                if (ElementFormatter.getElementsFor(format).isEmpty()) {
                    ElementFormatter.setFormat(parent, format);
                } else {
                    Format duplicatedFormat = themeManager.duplicateFormat(format);
                    ElementFormatter.setFormat(parent, duplicatedFormat);
                }
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
                throw new ThemeIOException("Could not parse node: " + nodeName);
            }

            Node nameAttr = attributes.getNamedItem("name");
            if (nameAttr != null) {
                String elementName = nameAttr.getNodeValue();
                if (checkElementName(elementName)) {
                    elem.setName(elementName);
                } else {
                    log.warn("Element names may only contain lower-case alpha-numeric characters, digits, underscores, spaces and dashes: "
                            + elementName);
                }
            }

            String description = getCommentAssociatedTo(n);
            if (description != null) {
                elem.setDescription(description);
            }

            try {
                parent.addChild(elem);
            } catch (NodeException e) {
                throw new ThemeIOException("Failed to parse layout.", e);
            }
            parseLayout(elem, n);
        }
    }

    public static void parsePresets(final ThemeElement theme, Node node) {
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        final String themeName = theme.getName();
        PresetManager.clearCustomPresets(themeName);
        for (Node n : getChildElements(node)) {
            NamedNodeMap attrs = n.getAttributes();
            final String name = attrs.getNamedItem("name").getNodeValue();
            final String category = attrs.getNamedItem("category").getNodeValue();
            final String value = n.getTextContent();
            final String group = themeName; // use the theme's name as
            // group name

            final Node labelAttr = attrs.getNamedItem("label");
            String label = "";
            if (labelAttr != null) {
                label = labelAttr.getNodeValue();
            }

            final Node descriptionAttr = attrs.getNamedItem("description");
            String description = "";
            if (descriptionAttr != null) {
                description = descriptionAttr.getNodeValue();
            }

            PresetType preset = new CustomPresetType(name, value, group,
                    category, label, description);
            typeRegistry.register(preset);
        }
    }

    public static void parseFormats(final ThemeElement theme,
            org.w3c.dom.Element doc,
            Map<Style, Map<String, Properties>> commonStyles,
            Map<Integer, String> inheritanceMap, Node node)
            throws ThemeIOException, ThemeException {
        Node baseNode = getBaseNode(doc);
        String themeName = theme.getName();

        String resourceBankName = null;
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor != null) {
            resourceBankName = themeDescriptor.getResourceBankName();
        }

        ThemeManager themeManager = Manager.getThemeManager();

        for (Node n : getChildElements(node)) {
            String nodeName = n.getNodeName();
            NamedNodeMap attributes = n.getAttributes();
            Node elementItem = attributes.getNamedItem("element");
            String elementXPath = null;
            if (elementItem != null) {
                elementXPath = elementItem.getNodeValue();
            }

            Format format;
            try {
                format = FormatFactory.create(nodeName);
            } catch (ThemeException e) {
                throw new ThemeIOException(e);
            }
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
                Style style = (Style) format;

                // register the style name
                String styleName = null;
                if (nameAttr != null) {
                    styleName = nameAttr.getNodeValue();
                    // the style may have been registered already
                    Style registeredStyle = (Style) themeManager.getNamedObject(
                            themeName, "style", styleName);
                    if (registeredStyle == null) {
                        style.setName(styleName);
                        themeManager.setNamedObject(themeName, "style", style);
                    } else {
                        style = registeredStyle;
                    }
                }

                Node inheritedAttr = attributes.getNamedItem("inherit");
                if (inheritedAttr != null) {
                    String inheritedName = inheritedAttr.getNodeValue();
                    if ("".equals(inheritedName)) {
                        continue;
                    }
                    inheritanceMap.put(style.getUid(), inheritedName);
                }

                Node remoteAttr = attributes.getNamedItem("remote");
                if (remoteAttr != null) {
                    Boolean remote = Boolean.valueOf(remoteAttr.getNodeValue());
                    if (style.isNamed()) {
                        style.setRemote(remote);
                    } else {
                        log.warn("Only named styles can be remote, ignoring remote attribute on"
                                + style.getUid());
                    }
                }

                if (styleName != null && elementXPath != null) {
                    log.warn("Style parser: named style '" + styleName
                            + "' cannot have an 'element' attribute: '"
                            + elementXPath + "'.");
                    continue;
                }

                List<Node> selectorNodes = getChildElementsByTagName(n,
                        "selector");

                if (style.isRemote() && resourceBankName != null) {
                    if (!selectorNodes.isEmpty()) {
                        style.setCustomized(true);
                    }
                }

                // Use style properties from the theme
                for (Node selectorNode : selectorNodes) {
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

                    // BBB: remove in a later release
                    if (elementXPath != null
                            && (viewName == null || viewName.equals("*"))) {
                        log.warn("Style parser: trying to guess the view name for: "
                                + elementXPath);
                        viewName = guessViewNameFor(doc, elementXPath);
                        if (viewName == null) {
                            if (!commonStyles.containsKey(style)) {
                                commonStyles.put(style,
                                        new LinkedHashMap<String, Properties>());
                            }
                            commonStyles.get(style).put(path,
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

    }

    public static void createCommonStyles(String themeName,
            Map<Style, Map<String, Properties>> commonStyles)
            throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        int count = 1;
        for (Style parent : commonStyles.keySet()) {
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
            Map<String, Properties> map = commonStyles.get(parent);
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

    public static void parseProperties(org.w3c.dom.Element doc, Node node)
            throws ThemeIOException {
        NamedNodeMap attributes = node.getAttributes();
        Node elementAttr = attributes.getNamedItem("element");
        if (elementAttr == null) {
            throw new ThemeIOException(
                    "<properties> node has no 'element' attribute.");
        }
        String elementXPath = elementAttr.getNodeValue();

        Node baseNode = getBaseNode(doc);
        Node element = null;
        try {
            element = (Node) xpath.evaluate(elementXPath, baseNode,
                    XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new ThemeIOException(e);
        }
        if (element == null) {
            throw new ThemeIOException(
                    "Could not find the element associated to: " + elementXPath);
        }
        Properties properties = getPropertiesFromNode(node);
        if (properties != null) {
            element.setUserData("properties", properties, null);
        }
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
            properties.setProperty(n.getNodeName(),
                    Framework.expandVars(textContent));
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

    public static List<Node> getChildElementsByTagName(Node node, String tagName) {
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

    public static Node getBaseNode(org.w3c.dom.Element doc)
            throws ThemeIOException {
        Node baseNode = null;
        try {
            baseNode = (Node) xpath.evaluate('/' + DOCROOT_NAME + "/layout",
                    doc, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new ThemeIOException(e);
        }
        if (baseNode == null) {
            throw new ThemeIOException("No <layout> section found.");
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

    // BBB shouldn't have to guess view names
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

    private static List<Node> getNodesByXPath(Node baseNode, String elementXPath)
            throws ThemeIOException {
        final List<Node> nodes = new ArrayList<Node>();
        if (elementXPath != null) {
            try {
                NodeList elementNodes = (NodeList) xpath.evaluate(elementXPath,
                        baseNode, XPathConstants.NODESET);
                for (int i = 0; i < elementNodes.getLength(); i++) {
                    nodes.add(elementNodes.item(i));
                }
            } catch (XPathExpressionException e) {
                throw new ThemeIOException(e);
            }
        }
        return nodes;
    }
}
