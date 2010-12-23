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

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.uids.Identifiable;
import org.w3c.dom.Document;

public class ThemeSerializer {

    private static final Log log = LogFactory.getLog(ThemeSerializer.class);

    private static final String DOCROOT_NAME = "theme";

    private Document doc;

    private List<Element> elements;

    public Document serialize(final String src) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        } catch (ParserConfigurationException e) {
            log.debug("Could not set DTD non-validation feature");
        }
        final DocumentBuilder db = dbf.newDocumentBuilder();
        doc = db.newDocument();
        elements = new ArrayList<Element>();
        final org.w3c.dom.Element root = doc.createElement(DOCROOT_NAME);
        final ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = themeManager.getThemeBySrc(src);

        // Theme description and name
        final String description = theme.getDescription();
        if (description != null) {
            root.setAttribute("description", description);
        }

        final String themeName = theme.getName();
        root.setAttribute("name", themeName);

        ThemeDescriptor themeDef = ThemeManager.getThemeDescriptor(src);
        List<String> templateEngines = themeDef.getTemplateEngines();
        if (templateEngines != null && templateEngines.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            final Iterator<String> it = templateEngines.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            root.setAttribute("template-engines", sb.toString());
        }

        // Resource bank
        String resourceBankName = themeDef.getResourceBankName();
        if (resourceBankName != null) {
            root.setAttribute("resource-bank", resourceBankName);
        }

        doc.appendChild(root);

        // layout
        final org.w3c.dom.Element layoutNode = doc.createElement("layout");
        root.appendChild(layoutNode);

        for (Node page : theme.getChildren()) {
            serializeLayout((Element) page, layoutNode);
        }

        // element properties
        for (Element element : elements) {
            serializeProperties(element, root);
        }

        // presets
        List<PresetType> customPresets = PresetManager.getCustomPresets(themeName);
        if (!customPresets.isEmpty()) {
            final org.w3c.dom.Element presetsNode = doc.createElement("presets");
            root.appendChild(presetsNode);
            for (PresetType preset : customPresets) {
                final org.w3c.dom.Element presetNode = doc.createElement("preset");
                presetNode.setAttribute("name", preset.getName());
                presetNode.setAttribute("category", preset.getCategory());
                presetNode.setAttribute("label", preset.getLabel());
                presetNode.setAttribute("description", preset.getDescription());
                presetNode.appendChild(doc.createTextNode(preset.getValue()));
                presetsNode.appendChild(presetNode);
            }
        }

        // formats
        final org.w3c.dom.Element formatNode = doc.createElement("formats");
        root.appendChild(formatNode);

        for (String formatTypeName : themeManager.getFormatTypeNames()) {
            // export named styles
            for (Identifiable object : themeManager.getNamedObjects(themeName,
                    formatTypeName)) {
                Format format = (Format) object;
                // skip unused remote styles
                if (!format.isCustomized()
                        && ThemeManager.listFormatsDirectlyInheritingFrom(
                                format).isEmpty()) {
                    continue;
                }
                serializeFormat(format, formatNode);
            }
            for (Format format : themeManager.getFormatsByTypeName(formatTypeName)) {
                if (format.isNamed()) {
                    continue;
                }
                // make sure that the format is used by this theme
                boolean isUsedByThisTheme = false;
                for (Element element : ElementFormatter.getElementsFor(format)) {
                    if (element.isChildOf(theme) || element == theme) {
                        isUsedByThisTheme = true;
                        break;
                    }
                }
                if (isUsedByThisTheme) {
                    serializeFormat(format, formatNode);
                }
            }
        }
        return doc;
    }

    private void serializeProperties(final Element parent,
            final org.w3c.dom.Element domParent) throws Exception {
        final org.w3c.dom.Element domProperties = doc.createElement("properties");
        domProperties.setAttribute("element", parent.computeXPath());
        for (Map.Entry<Object, Object> entry : FieldIO.dumpFieldsToProperties(
                parent).entrySet()) {
            final org.w3c.dom.Element domProperty = doc.createElement((String) entry.getKey());
            final String value = (String) entry.getValue();
            domProperty.appendChild(doc.createTextNode(Utils.cleanUp(value)));
            domProperties.appendChild(domProperty);
        }
        if (domProperties.hasChildNodes()) {
            domParent.appendChild(domProperties);
        }
    }

    private void serializeLayout(final Element parent,
            final org.w3c.dom.Element domParent) {
        final String typeName = parent.getElementType().getTypeName();
        final org.w3c.dom.Element domElement = doc.createElement(typeName);

        elements.add(parent);

        final String elementName = parent.getName();
        if (elementName != null) {
            domElement.setAttribute("name", elementName);
        }

        if (parent instanceof Fragment) {
            domElement.setAttribute("type",
                    ((Fragment) parent).getFragmentType().getTypeName());

            // perspectives
            final StringBuilder s = new StringBuilder();
            final Iterator<PerspectiveType> it = ((Fragment) parent).getVisibilityPerspectives().iterator();
            while (it.hasNext()) {
                PerspectiveType perspective = it.next();
                s.append(perspective.getTypeName());
                if (it.hasNext()) {
                    s.append(",");
                }
            }
            if (s.length() > 0) {
                domElement.setAttribute("perspectives", s.toString());
            }
        }

        String description = parent.getDescription();
        if (description != null) {
            domParent.appendChild(doc.createComment(String.format(" %s ",
                    description)));
        }

        domParent.appendChild(domElement);
        for (Node child : parent.getChildren()) {
            serializeLayout((Element) child, domElement);
        }
    }

    private void serializeFormat(final Format format,
            final org.w3c.dom.Element domParent) {
        final String typeName = format.getFormatType().getTypeName();
        final org.w3c.dom.Element domElement = doc.createElement(typeName);

        final String description = format.getDescription();
        if (description != null) {
            domParent.appendChild(doc.createComment(String.format(" %s ",
                    description)));
        }

        StringBuilder s = new StringBuilder();
        Iterator<Element> iter = ElementFormatter.getElementsFor(format).iterator();
        boolean hasElement = iter.hasNext();
        while (iter.hasNext()) {
            Element element = iter.next();
            s.append(element.computeXPath());
            if (iter.hasNext()) {
                s.append("|");
            }
        }
        if (hasElement) {
            domElement.setAttribute("element", s.toString());
        }

        // widgets
        if ("widget".equals(typeName)) {
            // view name
            String viewName = format.getName();
            org.w3c.dom.Element domView = doc.createElement("view");
            domView.appendChild(doc.createTextNode(viewName));
            domElement.appendChild(domView);

            // properties
            Properties properties = format.getProperties();
            Enumeration<?> names = properties.propertyNames();

            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if ("view".equals(name)) {
                    continue;
                }
                String value = properties.getProperty(name);
                org.w3c.dom.Element domAttr = doc.createElement(name);
                domAttr.appendChild(doc.createTextNode(Utils.cleanUp(value)));
                domElement.appendChild(domAttr);
            }
        }

        // layout
        else if ("layout".equals(typeName)) {
            Properties properties = format.getProperties();
            Enumeration<?> names = properties.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                String value = properties.getProperty(name);
                org.w3c.dom.Element domView = doc.createElement(name);
                domView.appendChild(doc.createTextNode(Utils.cleanUp(value)));
                domElement.appendChild(domView);
            }
        }

        // style
        else if ("style".equals(typeName)) {
            Style style = (Style) format;
            String styleName = style.getName();
            Style ancestor = (Style) ThemeManager.getAncestorFormatOf(style);
            if (styleName != null) {
                domElement.setAttribute("name", styleName);
            }
            if (ancestor != null) {
                domElement.setAttribute("inherit", ancestor.getName());
            }
            if (style.isRemote()) {
                domElement.setAttribute("remote", "true");
            }
            if (!style.isRemote() || style.isCustomized()) {
                for (String viewName : style.getSelectorViewNames()) {
                    for (String path : style.getPathsForView(viewName)) {
                        Properties styleProperties = style.getPropertiesFor(
                                viewName, path);
                        if (styleProperties.isEmpty()) {
                            continue;
                        }
                        org.w3c.dom.Element domSelector = doc.createElement("selector");
                        path = Utils.cleanUp(path);
                        domSelector.setAttribute("path", path);
                        if (!"*".equals(viewName)) {
                            domSelector.setAttribute("view", viewName);
                        }

                        for (Map.Entry<Object, Object> entry : styleProperties.entrySet()) {
                            org.w3c.dom.Element domProperty = doc.createElement((String) entry.getKey());
                            String value = (String) entry.getValue();
                            String presetName = PresetManager.extractPresetName(
                                    null, value);
                            if (presetName != null) {
                                domProperty.setAttribute("preset", presetName);
                            } else {
                                domProperty.appendChild(doc.createTextNode(Utils.cleanUp(value)));
                            }
                            domSelector.appendChild(domProperty);
                        }

                        // Set selector description
                        String selectorDescription = style.getSelectorDescription(
                                path, viewName);
                        if (selectorDescription != null) {
                            domElement.appendChild(doc.createComment(String.format(
                                    " %s ", selectorDescription)));
                        }

                        domElement.appendChild(domSelector);
                    }
                }
            }
        }
        domParent.appendChild(domElement);
    }

    public String serializeToXml(final String src) throws ThemeIOException {
        return serializeToXml(src, 0);
    }

    public String serializeToXml(final String src, final int indent)
            throws ThemeIOException {
        // serialize the theme into a document
        try {
            serialize(src);
            // convert the document to XML
            StringWriter sw = new StringWriter();
            OutputFormat format = new OutputFormat(doc);
            format.setIndenting(true);
            format.setIndent(indent);
            Writer output = new BufferedWriter(sw);
            XMLSerializer serializer = new XMLSerializer(output, format);
            serializer.serialize(doc);
            return sw.toString();
        } catch (Exception e) {
            throw new ThemeIOException(e);
        }

    }

}
