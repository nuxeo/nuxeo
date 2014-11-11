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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.nodes.Node;

public final class ThemeRepairer {

    private static final Log log = LogFactory.getLog(ThemeRepairer.class);

    private static final String[] LAYOUT_PROPERTIES = { "width", "height",
            "text-align", "padding", "margin", "margin-left", "margin-right",
            "margin-top", "margin-bottom", "padding-left", "padding-right",
            "padding-bottom", "padding-top" };

    private static final String[] PAGE_LAYOUT_PROPERTIES = { "margin",
            "padding" };

    private static final String[] SECTION_LAYOUT_PROPERTIES = { "width",
            "height", "margin-right", "margin-left" };

    private static final String[] CELL_LAYOUT_PROPERTIES = { "width",
            "padding", "text-align" };

    private static final String[] PAGE_STYLE_PROPERTIES = { "border-top",
            "border-left", "border-bottom", "border-right", "background" };

    private static final String[] SECTION_STYLE_PROPERTIES = { "border-top",
            "border-left", "border-bottom", "border-right", "background" };

    private static final String[] CELL_STYLE_PROPERTIES = { "border-top",
            "border-left", "border-bottom", "border-right", "background" };

    public static void repair(ThemeElement theme) throws ThemeException {
        // Make sure that all shared formats are assigned to elements of a same
        // type
        checkSharedFormats(theme);

        List<Node> allElements = theme.getDescendants();
        allElements.add(theme);

        // Move layout-related properties found in styles to layout formats
        for (Node node : allElements) {
            Element element = (Element) node;
            if (element instanceof PageElement
                    || element instanceof SectionElement
                    || element instanceof CellElement) {
                moveLayoutProperties(element);
            }
        }

        // Clean up styles and layouts
        for (Node node : allElements) {
            Element element = (Element) node;
            if (element instanceof PageElement
                    || element instanceof SectionElement
                    || element instanceof CellElement) {
                cleanupStyles(element);
                cleanupLayouts(element);
            }
        }
    }

    public static void checkSharedFormats(ThemeElement theme)
            throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        for (Format format : Manager.getThemeManager().listFormats()) {
            Collection<Element> elements = ElementFormatter.getElementsFor(format);
            if (elements.size() < 2) {
                continue;
            }
            Map<ElementType, Format> formatsByElementTypes = new HashMap<ElementType, Format>();
            for (Element element : elements) {
                if (!element.isChildOf(theme)) {
                    continue;
                }
                ElementType elementType = element.getElementType();
                if (formatsByElementTypes.isEmpty()) {
                    formatsByElementTypes.put(elementType, format);
                } else if (!formatsByElementTypes.containsKey(elementType)) {
                    log.debug("Created format of type '"
                            + format.getFormatType().getTypeName()
                            + "' for element: '" + element.computeXPath()
                            + "' ");
                    formatsByElementTypes.put(elementType,
                            themeManager.duplicateFormat(format));
                }
            }
            for (Map.Entry<ElementType, Format> entry : formatsByElementTypes.entrySet()) {
                Format f = entry.getValue();
                ElementType elementType = entry.getKey();
                for (Element element : elements) {
                    if (element.getElementType().equals(elementType)) {
                        ElementFormatter.setFormat(element, f);
                    }
                }
            }
        }
    }

    private static void moveLayoutProperties(Element element)
            throws ThemeException {
        Widget widget = (Widget) ElementFormatter.getFormatFor(element,
                "widget");
        Style style = (Style) ElementFormatter.getFormatFor(element, "style");
        Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                "layout");
        String xpath = element.computeXPath();

        // Add missing layout formats
        if (layout == null) {
            layout = (Layout) FormatFactory.create("layout");
            Manager.getThemeManager().registerFormat(layout);
            ElementFormatter.setFormat(element, layout);
            log.debug("Added layout to element: " + xpath);
        }

        if (ElementFormatter.getFormatFor(element, "widget") == null) {
            log.error("Element " + xpath + " has no widget.");
        }

        // Move layout-related properties to layout formats
        if (style != null) {
            if (widget != null) {
                String viewName = widget.getName();
                if (viewName != null) {
                    Properties styleProperties = style.getPropertiesFor(
                            viewName, "");
                    if (styleProperties != null) {
                        Collection<String> propertiesToMove = new ArrayList<String>();
                        for (String key : LAYOUT_PROPERTIES) {
                            String value = (String) styleProperties.get(key);
                            if (value != null) {
                                propertiesToMove.add(key);
                            }
                        }

                        if (!propertiesToMove.isEmpty()) {
                            for (String key : propertiesToMove) {
                                layout.setProperty(key,
                                        styleProperties.getProperty(key));
                                log.debug("Moved property '"
                                        + key
                                        + "' from <style> to <layout> for element "
                                        + xpath);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void cleanupStyles(Element element) {
        Widget widget = (Widget) ElementFormatter.getFormatFor(element,
                "widget");
        Style style = (Style) ElementFormatter.getFormatFor(element, "style");
        String xpath = element.computeXPath();

        // Simplify styles by removing disallowed layout properties and by
        // cleaning up paths without properties
        if (style != null && widget != null) {
            String viewName = widget.getName();
            List<String> pathsToClear = new ArrayList<String>();

            for (String path : style.getPathsForView(viewName)) {
                Properties styleProperties = style.getPropertiesFor(viewName,
                        path);
                if (styleProperties == null) {
                    continue;
                }
                for (String key : LAYOUT_PROPERTIES) {
                    if (styleProperties.containsKey(key)) {
                        styleProperties.remove(key);
                        log.debug("Removed property: '" + key
                                + "' from <style> for element " + xpath);
                    }
                }

                if (styleProperties.isEmpty()) {
                    pathsToClear.add(path);
                    continue;
                }

                List<String> stylePropertiesToRemove = new ArrayList<String>();
                for (Object key : styleProperties.keySet()) {
                    String propertyName = (String) key;
                    if ((widget instanceof PageElement && !Utils.contains(
                            PAGE_STYLE_PROPERTIES, propertyName))
                            || (widget instanceof SectionElement && !Utils.contains(
                                    SECTION_STYLE_PROPERTIES, propertyName))
                            || (widget instanceof CellElement && !Utils.contains(
                                    CELL_STYLE_PROPERTIES, propertyName))) {
                        stylePropertiesToRemove.add(propertyName);
                    }
                }

                for (String propertyName : stylePropertiesToRemove) {
                    styleProperties.remove(propertyName);
                    log.debug("Removed style property: '" + propertyName
                            + " in path: " + path + "' for element " + xpath);
                }
            }

            for (String path : pathsToClear) {
                style.clearPropertiesFor(viewName, path);
                log.debug("Removed empty style path: '" + path
                        + "' for element " + xpath);
            }
        }
    }

    private static void cleanupLayouts(Element element) {
        Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                "layout");
        String xpath = element.computeXPath();
        Properties layoutProperties = layout.getProperties();

        List<String> layoutPropertiesToRemove = new ArrayList<String>();
        for (Object key : layoutProperties.keySet()) {
            String propertyName = (String) key;
            if ((element instanceof PageElement && !Utils.contains(
                    PAGE_LAYOUT_PROPERTIES, propertyName))
                    || (element instanceof SectionElement && !Utils.contains(
                            SECTION_LAYOUT_PROPERTIES, propertyName))
                    || (element instanceof CellElement && !Utils.contains(
                            CELL_LAYOUT_PROPERTIES, propertyName))) {
                layoutPropertiesToRemove.add(propertyName);
            }
        }

        for (String propertyName : layoutPropertiesToRemove) {
            layoutProperties.remove(propertyName);
            log.debug("Removed property '" + propertyName
                    + "' from <layout> for element " + xpath);
        }
    }

}
