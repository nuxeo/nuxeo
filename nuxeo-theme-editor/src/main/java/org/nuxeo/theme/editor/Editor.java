/*
 * (C) Copyright 2006-2008 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.events.EventContext;
import org.nuxeo.theme.events.EventManager;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.views.ViewType;

public class Editor {

    public static void updateElementWidget(Element element, String viewName) {
        FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "widget");
        Format widget = ElementFormatter.getFormatByType(element, widgetType);
        ThemeManager themeManager = Manager.getThemeManager();
        if (widget == null) {
            widget = FormatFactory.create("widget");
            themeManager.registerFormat(widget);
        }
        widget.setName(viewName);
        ElementFormatter.setFormat(element, widget);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void updateElementLayout(Element element,
            Map<String, String> propertyMap) {
        if (element != null) {
            Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                    "layout");
            if (layout == null) {
                layout = (Layout) FormatFactory.create("layout");
                Manager.getThemeManager().registerFormat(layout);
                ElementFormatter.setFormat(element, layout);
            }
            for (Object key : propertyMap.keySet()) {
                layout.setProperty((String) key, propertyMap.get(key));
            }
            EventManager eventManager = Manager.getEventManager();
            eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                    element, null));
        }
    }

    public static void updateElementVisibility(Element element,
            List<String> perspectives, boolean alwaysVisible) {
        PerspectiveManager perspectiveManager = Manager.getPerspectiveManager();
        if (alwaysVisible) {
            perspectiveManager.setAlwaysVisible(element);
        } else {
            // initially make the element visible in all perspectives
            if (perspectives == null || perspectives.isEmpty()) {
                perspectiveManager.setVisibleInAllPerspectives(element);
            } else {
                perspectiveManager.setVisibleInPerspectives(element,
                        perspectives);
            }
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void updateElementStyle(Element element, Style style,
            String path, String viewName, Map<String, String> propertyMap) {
        Properties properties = new Properties();
        for (Object key : propertyMap.keySet()) {
            properties.put(key, propertyMap.get(key));
        }
        if (style == null) {
            FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                    TypeFamily.FORMAT, "style");
            style = (Style) ElementFormatter.getFormatByType(element, styleType);
        }
        if (style.getName() != null || "".equals(viewName)) {
            viewName = "*";
        }
        style.setPropertiesFor(viewName, path, properties);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void splitElement(Element element) {
        if (!element.getElementType().getTypeName().equals("cell")) {
            return;
        }
        ThemeManager themeManager = Manager.getThemeManager();
        Element newCell = ElementFactory.create("cell");
        Format cellWidget = FormatFactory.create("widget");
        cellWidget.setName("cell frame");
        themeManager.registerFormat(cellWidget);
        Format cellLayout = FormatFactory.create("layout");
        themeManager.registerFormat(cellLayout);
        FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "layout");
        Format layout = ElementFormatter.getFormatByType(element, layoutType);
        String width = layout.getProperty("width");
        if (width != null) {
            String halfWidth = org.nuxeo.theme.html.Utils.divideWebLength(
                    width, 2);
            if (halfWidth != null) {
                cellLayout.setProperty("width", halfWidth);
                layout.setProperty("width",
                        org.nuxeo.theme.html.Utils.substractWebLengths(width,
                                halfWidth));
            }
        }
        Format cellStyle = FormatFactory.create("style");
        themeManager.registerFormat(cellStyle);
        ElementFormatter.setFormat(newCell, cellWidget);
        ElementFormatter.setFormat(newCell, cellLayout);
        ElementFormatter.setFormat(newCell, cellStyle);
        newCell.insertAfter(element);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void updateElementStyleCss(Element element, Style style,
            String viewName, String cssSource) {
        if (style == null) {
            FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                    TypeFamily.FORMAT, "style");
            style = (Style) ElementFormatter.getFormatByType(element, styleType);
        }
        if (style.getName() != null || "".equals(viewName)) {
            viewName = "*";
        }
        org.nuxeo.theme.html.Utils.loadCss(style, cssSource, viewName);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void updateElementWidth(Format layout, String width) {
        layout.setProperty("width", width);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                layout, null));
    }

    public static void updateElementProperties(Element element,
            Map<String, String> propertyMap) {
        Properties properties = new Properties();
        for (Object key : propertyMap.keySet()) {
            properties.put(key, propertyMap.get(key));
        }
        try {
            FieldIO.updateFieldsFromProperties(element, properties);
        } catch (Exception e) {
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void updateElementDescription(Element element,
            String description) {
        element.setDescription(description);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static int repairTheme(String themeName) {
        ThemeElement theme = Manager.getThemeManager().getThemeByName(themeName);
        if (theme == null) {
            return 0;
        }
        ThemeManager.repairTheme(theme);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                theme, null));
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                theme, null));
        return 1;
    }

    public static int saveTheme(String src, int indent) {
        int res = 1;
        try {
            ThemeManager.saveTheme(src, indent);
        } catch (ThemeIOException e) {
            res = 0;
        }
        return res;
    }

    public static String renderCssPreview(Element element, Style style,
            String viewName) {
        FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        if (style == null) {
            style = (Style) ElementFormatter.getFormatByType(element, styleType);
        }
        if (style == null) {
            return "";
        }
        ThemeElement theme = ThemeManager.getThemeOf(element);
        String themeName = theme.getName();

        StringBuilder css = new StringBuilder();
        List<Style> styles = new ArrayList<Style>();
        for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
            styles.add(0, (Style) ancestor);
        }
        styles.add(style);
        for (Style s : styles) {
            String name = viewName;
            if (s.getName() != null) {
                name = "*";
            }
            for (String path : s.getPathsForView(name)) {
                css.append("#stylePreviewArea");
                css.append(' ').append(path).append(" {");
                Properties styleProperties = s.getPropertiesFor(name, path);
                Enumeration<?> propertyNames = org.nuxeo.theme.html.Utils.getCssProperties().propertyNames();
                while (propertyNames.hasMoreElements()) {
                    String propertyName = (String) propertyNames.nextElement();
                    String value = styleProperties.getProperty(propertyName);
                    if (value == null) {
                        continue;
                    }
                    css.append(propertyName);
                    css.append(':');
                    PresetType preset = null;
                    String presetName = PresetManager.extractPresetName(
                            themeName, value);
                    if (presetName != null) {
                        preset = PresetManager.getPresetByName(presetName);
                    }
                    if (preset != null) {
                        value = preset.getValue();
                    }
                    css.append(value);
                    css.append(';');
                }
                css.append('}');
            }
        }
        return css.toString();
    }

    public static void pasteElement(Element element, String destId) {
        Element destElement = ThemeManager.getElementById(destId);
        if (destElement.isLeaf()) {
            destElement = (Element) destElement.getParent();
        }
        if (element != null) {
            destElement.addChild(Manager.getThemeManager().duplicateElement(
                    element, true));
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                null, null));
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null,
                destElement));
    }

    public static void moveElement(Element srcElement, Element destElement,
            int order) {
        // move the element
        srcElement.moveTo(destElement, order);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                srcElement, destElement));
    }

    public static void makeElementUseNamedStyle(Element element,
            String styleName, String themeName) {
        FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        Style style = (Style) ElementFormatter.getFormatByType(element,
                styleType);
        ThemeManager themeManager = Manager.getThemeManager();
        // Make the style no longer inherits from other another style if
        // 'inheritedName' is null
        if (styleName == null) {
            ThemeManager.removeInheritanceTowards(style);
        } else {
            String themeId = themeName.split("/")[0];
            Style inheritedStyle = (Style) themeManager.getNamedObject(themeId,
                    "style", styleName);
            if (inheritedStyle != null) {
                themeManager.makeFormatInherit(style, inheritedStyle);
            }
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static String addPage(String path) {
        ThemeManager themeManager = Manager.getThemeManager();
        if (!path.contains("/")) {
            return "";
        }
        if (themeManager.getPageByPath(path) != null) {
            return "";
        }
        String themeName = path.split("/")[0];
        ThemeElement theme = themeManager.getThemeByName(themeName);
        PageElement page = (PageElement) ElementFactory.create("page");
        String pageName = path.split("/")[1];
        page.setName(pageName);
        Format pageWidget = FormatFactory.create("widget");
        pageWidget.setName("page frame");
        themeManager.registerFormat(pageWidget);
        Format pageLayout = FormatFactory.create("layout");
        themeManager.registerFormat(pageLayout);
        Format pageStyle = FormatFactory.create("style");
        themeManager.registerFormat(pageStyle);
        ElementFormatter.setFormat(page, pageWidget);
        ElementFormatter.setFormat(page, pageStyle);
        ElementFormatter.setFormat(page, pageLayout);
        themeManager.registerPage(theme, page);
        return path;
    }

    public static String addTheme(String name) {
        ThemeManager themeManager = Manager.getThemeManager();
        String res = "";
        if (themeManager.getThemeByName(name) == null) {
            ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
            theme.setName(name);
            Format themeWidget = FormatFactory.create("widget");
            themeWidget.setName("theme view");
            themeManager.registerFormat(themeWidget);
            ElementFormatter.setFormat(theme, themeWidget);
            // default page
            PageElement page = (PageElement) ElementFactory.create("page");
            page.setName("default");
            Format pageWidget = FormatFactory.create("widget");
            themeManager.registerFormat(pageWidget);
            pageWidget.setName("page frame");
            Format pageLayout = FormatFactory.create("layout");
            themeManager.registerFormat(pageLayout);
            Format pageStyle = FormatFactory.create("style");
            themeManager.registerFormat(pageStyle);
            ElementFormatter.setFormat(page, pageWidget);
            ElementFormatter.setFormat(page, pageStyle);
            ElementFormatter.setFormat(page, pageLayout);
            theme.addChild(page);
            // create a theme descriptor
            ThemeDescriptor themeDescriptor = new ThemeDescriptor();
            themeDescriptor.setName(name);
            final String path = Manager.getLocalThemePath(name);
            if (path != null) {
                final String src = String.format("file://%s", path);
                themeDescriptor.setSrc(src);
            }
            TypeRegistry typeRegistry = Manager.getTypeRegistry();
            typeRegistry.register(themeDescriptor);
            // register the theme
            themeManager.registerTheme(theme);
            res = String.format("%s/%s", name, "default");
        }
        return res;
    }

    public static void assignStyleProperty(Element element,
            String propertyName, String value) {
        Style style = (Style) ElementFormatter.getFormatFor(element, "style");
        if (style == null) {
            style = (Style) FormatFactory.create("style");
            Manager.getThemeManager().registerFormat(style);
            ElementFormatter.setFormat(element, style);
        }
        Widget widget = (Widget) ElementFormatter.getFormatFor(element,
                "widget");
        if (widget == null) {
            return;
        }
        String viewName = widget.getName();
        Properties properties = style.getPropertiesFor(viewName, "");
        if (properties == null) {
            properties = new Properties();
        }
        if ("".equals(value)) {
            properties.remove(propertyName);
        } else {
            properties.setProperty(propertyName, value);
        }
        style.setPropertiesFor(viewName, "", properties);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                style, null));
    }

    public static void alignElement(Element element, String position) {
        ThemeManager themeManager = Manager.getThemeManager();
        Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                "layout");
        if (layout == null) {
            layout = (Layout) FormatFactory.create("layout");
            themeManager.registerFormat(layout);
            ElementFormatter.setFormat(element, layout);
        }
        if (element instanceof SectionElement) {
            if (position.equals("left")) {
                layout.setProperty("margin-left", "0");
                layout.setProperty("margin-right", "auto");
            } else if (position.equals("center")) {
                layout.setProperty("margin-left", "auto");
                layout.setProperty("margin-right", "auto");
            } else if (position.equals("right")) {
                layout.setProperty("margin-left", "auto");
                layout.setProperty("margin-right", "0");
            }
        } else {
            if (position.equals("left")) {
                layout.setProperty("text-align", "left");
            } else if (position.equals("center")) {
                layout.setProperty("text-align", "center");
            } else if (position.equals("right")) {
                layout.setProperty("text-align", "right");
            }
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void deleteElement(Element element) {
        Element parent = (Element) element.getParent();
        ThemeManager themeManager = Manager.getThemeManager();
        if (element instanceof ThemeElement || element instanceof PageElement) {
            themeManager.destroyElement(element);
        } else if (element instanceof CellElement) {
            if (element.hasSiblings()) {
                Element sibling = (Element) element.getNextNode();
                if (sibling == null) {
                    sibling = (Element) element.getPreviousNode();
                }
                FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(
                        TypeFamily.FORMAT, "layout");
                Format layout1 = ElementFormatter.getFormatByType(element,
                        layoutType);
                if (layout1 != null) {
                    String width1 = layout1.getProperty("width");
                    if (width1 != null) {
                        Format layout2 = ElementFormatter.getFormatByType(
                                sibling, layoutType);
                        if (layout2 != null) {
                            String width2 = layout2.getProperty("width");
                            String newWidth = org.nuxeo.theme.html.Utils.addWebLengths(
                                    width1, width2);
                            if (newWidth != null) {
                                layout2.setProperty("width", newWidth);
                            }
                        }
                    }
                }
                // remove cell
                themeManager.destroyElement(element);
            } else {
                // remove parent section
                themeManager.destroyElement(parent);
            }
        } else if (element instanceof Fragment) {
            themeManager.destroyElement(element);
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null,
                null));
    }

    public static int duplicateElement(Element element) {
        Element duplicate = Manager.getThemeManager().duplicateElement(element,
                true);
        // insert the duplicated element
        element.getParent().addChild(duplicate);
        duplicate.moveTo(element.getParent(), element.getOrder() + 1);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                null, null));
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null,
                element));
        return duplicate.getUid();
    }

    public static void createStyle(Element element) {
        if (element == null) {
            return;
        }
        final Format style = FormatFactory.create("style");
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.registerFormat(style);
        ElementFormatter.setFormat(element, style);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                element, null));
    }

    public static void createNamedStyle(Element element, String styleName,
            String themeName) {
        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(themeName, "style",
                styleName);
        if (style == null) {
            style = (Style) FormatFactory.create("style");
            style.setName(styleName);
            themeManager.setNamedObject(themeName, "style", style);
            themeManager.registerFormat(style);
        }
        themeManager.makeElementUseNamedStyle(element, styleName, themeName);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                style, null));
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                style, null));
    }

    public static void deleteNamedStyle(Element element, String styleName,
            String themeName) {
        ThemeManager themeManager = Manager.getThemeManager();
        Style inheritedStyle = (Style) themeManager.getNamedObject(themeName,
                "style", styleName);
        themeManager.deleteFormat(inheritedStyle);
        themeManager.makeElementUseNamedStyle(element, null, themeName);
        themeManager.removeNamedObject(themeName, "style", styleName);
    }

    public static String addPreset(String themeName, String presetName,
            String category) {
        if (PresetManager.getCustomPreset(themeName, presetName) != null) {
            return "";
        }
        PresetManager.createCustomPreset(themeName, presetName, category);
        return presetName;
    }

    public static void editPreset(String themeName, String presetName,
            String value) {
        PresetManager.editPreset(themeName, presetName, value);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                null, null));
    }

    public static void expireThemes() {
        Manager.getEventManager().notify(Events.THEME_MODIFIED_EVENT,
                new EventContext(null, null));
    }

    public static int loadTheme(String src) {
        try {
            Manager.getThemeManager().loadTheme(src);
        } catch (ThemeIOException e) {
            return 0;
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(null,
                null));
        eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(
                null, null));
        return 1;
    }

    public static void insertFragment(Element destElement, String typeName) {
        int order = 0;
        Element destContainer = destElement;
        if (destElement instanceof Fragment) {
            order = destElement.getOrder() + 1;
            destContainer = (Element) destElement.getParent();
        } else if (destElement instanceof CellElement) {
            order = destElement.getChildren().size();
        }
        ThemeManager themeManager = Manager.getThemeManager();
        // create the new fragment
        String fragmentTypeName = typeName.split("/")[0];
        Fragment fragment = FragmentFactory.create(fragmentTypeName);
        // add a temporary view to the fragment
        Format widget = FormatFactory.create("widget");
        String viewTypeName = typeName.split("/")[1];
        widget.setName(viewTypeName);
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(fragment, widget);
        // insert the fragment
        destContainer.addChild(fragment);
        // set the fragment order
        fragment.moveTo(destContainer, order);
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                fragment, destContainer));
    }

    public static void insertSectionAfter(Element element) {
        Element newSection = ElementFactory.create("section");
        Element newCell = ElementFactory.create("cell");
        // section
        Format sectionWidget = FormatFactory.create("widget");
        sectionWidget.setName("section frame");
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.registerFormat(sectionWidget);
        Format sectionLayout = FormatFactory.create("layout");
        sectionLayout.setProperty("width", "100%");
        themeManager.registerFormat(sectionLayout);
        Format sectionStyle = FormatFactory.create("style");
        themeManager.registerFormat(sectionStyle);
        ElementFormatter.setFormat(newSection, sectionWidget);
        ElementFormatter.setFormat(newSection, sectionLayout);
        ElementFormatter.setFormat(newSection, sectionStyle);
        // cell
        Format cellWidget = FormatFactory.create("widget");
        cellWidget.setName("cell frame");
        themeManager.registerFormat(cellWidget);
        Format cellLayout = FormatFactory.create("layout");
        themeManager.registerFormat(cellLayout);
        cellLayout.setProperty("width", "100%");
        Format cellStyle = FormatFactory.create("style");
        themeManager.registerFormat(cellStyle);
        ElementFormatter.setFormat(newCell, cellWidget);
        ElementFormatter.setFormat(newCell, cellLayout);
        ElementFormatter.setFormat(newCell, cellStyle);
        newSection.addChild(newCell);
        String elementTypeName = element.getElementType().getTypeName();
        if (elementTypeName.equals("section")) {
            newSection.insertAfter(element);
        } else if (elementTypeName.equals("page")) {
            element.addChild(newSection);
        }
        EventManager eventManager = Manager.getEventManager();
        eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(
                newSection, null));
    }

    public static byte[] getViewIconContent(final String viewTypeName) {
        ViewType viewType = (ViewType) Manager.getTypeRegistry().lookup(
                TypeFamily.VIEW, viewTypeName);
        String iconResourcePath = null;
        if (viewType != null) {
            iconResourcePath = viewType.getIcon();
        }
        if (iconResourcePath == null) {
            iconResourcePath = "nxthemes/html/icons/no-icon.png";
        }
        return org.nuxeo.theme.Utils.readResourceAsBytes(iconResourcePath);
    }

}
