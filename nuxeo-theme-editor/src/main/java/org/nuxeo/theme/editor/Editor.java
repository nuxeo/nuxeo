/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
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
            widget = themeManager.createWidget();
        }
        widget.setName(viewName);
        ElementFormatter.setFormat(element, widget);

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);
    }

    public static void updateElementLayout(Element element,
            Map<String, String> propertyMap) {
        if (element != null) {
            Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                    "layout");
            if (layout == null) {
                layout = Manager.getThemeManager().createLayout();
                ElementFormatter.setFormat(element, layout);
            }
            for (Object key : propertyMap.keySet()) {
                layout.setProperty((String) key, propertyMap.get(key));
            }

            final String themeName = ThemeManager.getThemeOf(element).getName();
            Manager.getThemeManager().themeModified(themeName);

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

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

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

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void splitElement(Element element) throws NodeException {
        if (!element.getElementType().getTypeName().equals("cell")) {
            return;
        }
        ThemeManager themeManager = Manager.getThemeManager();
        Element newCell = ElementFactory.create("cell");
        Format cellWidget = themeManager.createWidget();
        cellWidget.setName("cell frame");
        Format cellLayout = themeManager.createLayout();
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
        Format cellStyle = themeManager.createStyle();
        ElementFormatter.setFormat(newCell, cellWidget);
        ElementFormatter.setFormat(newCell, cellLayout);
        ElementFormatter.setFormat(newCell, cellStyle);
        newCell.insertAfter(element);

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

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

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().stylesModified(themeName);
    }

    public static void updateNamedStyleCss(Style style, String cssSource)
            throws ThemeException {
        if (style == null || style.getName() == null) {
            throw new ThemeException("A named style is required.");
        }
        final String viewName = "*";
        org.nuxeo.theme.html.Utils.loadCss(style, cssSource, viewName);
        final String themeName = ThemeManager.getThemeOfFormat(style).getName();
        Manager.getThemeManager().stylesModified(themeName);
    }

    public static void updateElementWidth(Format layout, String width) {
        layout.setProperty("width", width);
        final String themeName = ThemeManager.getThemeOfFormat(layout).getName();
        Manager.getThemeManager().themeModified(themeName);

    }

    public static void updateElementProperties(Element element,
            Map<String, String> propertyMap) throws ThemeIOException {
        Properties properties = new Properties();
        for (Object key : propertyMap.keySet()) {
            properties.put(key, propertyMap.get(key));
        }
        FieldIO.updateFieldsFromProperties(element, properties);

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

    }

    public static void updateElementDescription(Element element,
            String description) {
        element.setDescription(description);

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

    }

    public static void repairTheme(String src) throws ThemeIOException,
            ThemeException {
        ThemeElement theme = Manager.getThemeManager().getThemeBySrc(src);
        if (theme == null) {
            throw new ThemeIOException("Unknown theme: " + src);
        }
        ThemeManager.repairTheme(theme);

        final String themeName = theme.getName();
        Manager.getThemeManager().themeModified(themeName);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void saveTheme(String src, int indent)
            throws ThemeIOException, ThemeException {
        ThemeManager.saveTheme(src, indent);
    }

    public static void deleteTheme(String src) throws ThemeIOException,
            ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.deleteTheme(src);
    }

    public static void saveChanges() throws ThemeIOException, ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        boolean ok = false;
        for (ThemeDescriptor themeDef : ThemeManager.getThemeDescriptors()) {
            Long lastModified = themeManager.getLastModified(themeDef.getName());
            if (themeDef.isSaveable()) {
                ok = true;
            } else {
                continue;
            }
            Date lastSaved = themeDef.getLastSaved();
            if (lastSaved != null && lastSaved.getTime() > lastModified) {
                continue;
            }
            ThemeManager.saveTheme(themeDef.getSrc());
        }
        if (!ok) {
            throw new ThemeIOException(
                    "None of the existing themes can be saved.");
        }
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
                    value = PresetManager.resolvePresets(themeName, value);
                    css.append(value);
                    css.append(';');
                }
                css.append('}');
            }
        }
        return css.toString();
    }

    public static void pasteElement(Element element, String destId)
            throws ThemeException, NodeException {
        Element destElement = ThemeManager.getElementById(destId);
        if (destElement.isLeaf()) {
            destElement = (Element) destElement.getParent();
        }
        if (element != null) {
            destElement.addChild(Manager.getThemeManager().duplicateElement(
                    element, true));
        }

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void moveElement(Element srcElement, Element destElement,
            int order) throws NodeException {
        srcElement.moveTo(destElement, order);

        final String themeName = ThemeManager.getThemeOf(srcElement).getName();
        Manager.getThemeManager().themeModified(themeName);

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

        Manager.getThemeManager().themeModified(themeName);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static String addPage(String path) throws ThemeException,
            NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        if (!path.contains("/")) {
            throw new ThemeException("Incorrect theme path: " + path);
        }
        String themeName = path.split("/")[0];
        String pageName = path.split("/")[1];
        if (themeManager.getPageByPath(path) != null) {
            throw new ThemeException("Theme page name is already taken: "
                    + pageName);
        }
        ThemeElement theme = themeManager.getThemeByName(themeName);
        PageElement page = (PageElement) ElementFactory.create("page");
        page.setName(pageName);
        Format pageWidget = themeManager.createWidget();
        pageWidget.setName("page frame");
        Format pageLayout = themeManager.createLayout();
        Format pageStyle = themeManager.createStyle();
        ElementFormatter.setFormat(page, pageWidget);
        ElementFormatter.setFormat(page, pageStyle);
        ElementFormatter.setFormat(page, pageLayout);
        themeManager.registerPage(theme, page);
        return path;
    }

    public static String addTheme(String name) throws ThemeException,
            NodeException, ThemeIOException {
        ThemeManager themeManager = Manager.getThemeManager();
        if (themeManager.getThemeByName(name) != null) {
            throw new ThemeException("The theme name is already taken: " + name);
        }
        ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        theme.setName(name);
        Format themeWidget = themeManager.createWidget();
        themeWidget.setName("theme view");
        ElementFormatter.setFormat(theme, themeWidget);
        // default page
        PageElement page = (PageElement) ElementFactory.create("page");
        page.setName("default");
        Format pageWidget = themeManager.createWidget();
        pageWidget.setName("page frame");
        Format pageLayout = themeManager.createLayout();
        Format pageStyle = themeManager.createStyle();
        ElementFormatter.setFormat(page, pageWidget);
        ElementFormatter.setFormat(page, pageStyle);
        ElementFormatter.setFormat(page, pageLayout);
        theme.addChild(page);
        // create a theme descriptor
        ThemeDescriptor themeDescriptor = new ThemeDescriptor();
        themeDescriptor.setName(name);
        final String path = ThemeManager.getCustomThemePath(name);
        if (path == null) {
            throw new ThemeException("Could not get file path for theme: "
                    + name);
        }
        final String src = String.format("file://%s", path);
        themeDescriptor.setSrc(src);
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        typeRegistry.register(themeDescriptor);
        // register the theme
        themeManager.registerTheme(theme);
        // save the theme
        ThemeManager.saveTheme(themeDescriptor.getSrc());
        return String.format("%s/%s", name, "default");
    }

    public static void assignStyleProperty(Element element,
            String propertyName, String value) {
        Style style = (Style) ElementFormatter.getFormatFor(element, "style");
        if (style == null) {
            style = Manager.getThemeManager().createStyle();
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

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void alignElement(Element element, String position) {
        ThemeManager themeManager = Manager.getThemeManager();
        Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                "layout");
        if (layout == null) {
            layout = themeManager.createLayout();
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

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

    }

    public static void deleteElement(Element element) throws ThemeException,
            NodeException {
        Element parent = (Element) element.getParent();
        ThemeManager themeManager = Manager.getThemeManager();
        ThemeElement theme = ThemeManager.getThemeOf(element);
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

        final String themeName = theme.getName();
        Manager.getThemeManager().themeModified(themeName);
    }

    public static int duplicateElement(Element element) throws ThemeException,
            NodeException {
        Element duplicate = Manager.getThemeManager().duplicateElement(element,
                true);

        // insert the duplicated element
        element.getParent().addChild(duplicate);
        duplicate.moveTo(element.getParent(), element.getOrder() + 1);

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

        Manager.getThemeManager().stylesModified(themeName);

        return duplicate.getUid();
    }

    public static void createStyle(Element element) {
        ThemeManager themeManager = Manager.getThemeManager();
        final Format style = themeManager.createStyle();
        ElementFormatter.setFormat(element, style);

        final String themeName = ThemeManager.getThemeOf(element).getName();
        Manager.getThemeManager().themeModified(themeName);

    }

    public static void createNamedStyle(Element element, String styleName,
            String themeName) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(themeName, "style",
                styleName);
        if (style != null) {
            throw new ThemeException("Style name is already taken: "
                    + styleName);
        }
        style = themeManager.createStyle();
        style.setName(styleName);
        themeManager.setNamedObject(themeName, "style", style);

        themeManager.makeElementUseNamedStyle(element, styleName, themeName);

        Manager.getThemeManager().themeModified(themeName);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void deleteNamedStyle(Element element, String styleName,
            String themeName) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        Style inheritedStyle = (Style) themeManager.getNamedObject(themeName,
                "style", styleName);
        themeManager.deleteFormat(inheritedStyle);
        themeManager.makeElementUseNamedStyle(element, null, themeName);
        themeManager.removeNamedObject(themeName, "style", styleName);
    }

    public static void deleteStyleView(Style style, String viewName) {
        style.clearPropertiesFor(viewName);
        final String themeName = ThemeManager.getThemeOfFormat(style).getName();
        Manager.getThemeManager().stylesModified(themeName);
    }

    public static List<String> getHardcodedColors(final String themeName) {
        Set<String> colors = new HashSet<String>();
        for (Style style : Manager.getThemeManager().getStyles(themeName)) {
            for (Map.Entry<Object, Object> entry : style.getAllProperties().entrySet()) {
                String value = (String) entry.getValue();
                colors.addAll(org.nuxeo.theme.html.Utils.extractCssColors(value));
            }
        }
        Set<String> colorPresetValues = new HashSet<String>();
        for (PresetType preset : PresetManager.getCustomPresets(themeName,
                "color")) {
            colorPresetValues.add(preset.getValue());
        }
        colors.removeAll(colorPresetValues);
        return new ArrayList<String>(colors);
    }

    public static List<String> getHardcodedImages(final String themeName) {
        Set<String> images = new HashSet<String>();
        for (Style style : Manager.getThemeManager().getStyles(themeName)) {
            for (Map.Entry<Object, Object> entry : style.getAllProperties().entrySet()) {
                String value = (String) entry.getValue();
                images.addAll(org.nuxeo.theme.html.Utils.extractCssImages(value));
            }
        }
        Set<String> imagePresetValues = new HashSet<String>();
        for (PresetType preset : PresetManager.getCustomPresets(themeName)) {
            String category = preset.getCategory();
            if ("image".equals(category) || "background".equals(category)) {
                imagePresetValues.add(preset.getValue());
            }
        }
        images.removeAll(imagePresetValues);
        return new ArrayList<String>(images);
    }

    public static String addPreset(String themeName, String presetName,
            String category, String value) throws ThemeException {
        if (presetName.equals("")) {
            throw new ThemeException("Preset name cannot be empty");
        }
        if (PresetManager.getCustomPreset(themeName, presetName) != null) {
            throw new ThemeException("Preset name already taken: " + presetName);
        }
        PresetManager.createCustomPreset(themeName, presetName, category, value);
        return presetName;
    }

    public static void editPreset(String themeName, String presetName,
            String value) {
        PresetManager.editPreset(themeName, presetName, value);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void setPresetCategory(String themeName, String presetName,
            String category) {
        PresetManager.setPresetCategory(themeName, presetName, category);

        Manager.getThemeManager().stylesModified(themeName);

    }

    public static void renamePreset(String themeName, String oldName,
            String newName) throws ThemeException {
        PresetManager.renamePreset(themeName, oldName, newName);

        final String oldPresetStr = String.format("\"%s\"", oldName);
        final String newPresetStr = String.format("\"%s\"", newName);

        ThemeManager themeManager = Manager.getThemeManager();
        for (Style style : themeManager.getStyles(themeName)) {
            for (String viewName : style.getSelectorViewNames()) {
                for (String path : style.getPathsForView(viewName)) {
                    Properties styleProperties = style.getPropertiesFor(
                            viewName, path);
                    for (Map.Entry<Object, Object> entry : styleProperties.entrySet()) {
                        String text = (String) entry.getValue();
                        String key = (String) entry.getKey();
                        String newText = text.replace(oldPresetStr,
                                newPresetStr);
                        if (!newText.equals(text)) {
                            styleProperties.setProperty(key, newText);
                        }
                    }
                }
            }
        }

        Manager.getThemeManager().stylesModified(themeName);
    }

    public static void deletePreset(String themeName, String presetName)
            throws ThemeException {
        PresetManager.deletePreset(themeName, presetName);
    }

    public static void convertCssValueToPreset(String themeName,
            String category, String presetName, String value)
            throws ThemeException {
        if (!"color".equals(category) && !"image".equals(category)) {
            throw new ThemeException(
                    "Preset category not supported while converting css value to preset: "
                            + category);
        }

        addPreset(themeName, presetName, category, value);

        final String presetStr = String.format("\"%s\"", presetName);
        ThemeManager themeManager = Manager.getThemeManager();
        for (Style style : themeManager.getStyles(themeName)) {
            for (String viewName : style.getSelectorViewNames()) {
                for (String path : style.getPathsForView(viewName)) {
                    Properties styleProperties = style.getPropertiesFor(
                            viewName, path);
                    for (Map.Entry<Object, Object> entry : styleProperties.entrySet()) {
                        String text = (String) entry.getValue();
                        String key = (String) entry.getKey();
                        String newText = text;
                        if (category.equals("color")) {
                            newText = org.nuxeo.theme.html.Utils.replaceColor(
                                    text, value, presetStr);
                        } else if (category.equals("image")) {
                            newText = org.nuxeo.theme.html.Utils.replaceImage(
                                    text, value, presetStr);
                        }
                        if (!newText.equals(text)) {
                            styleProperties.setProperty(key, newText);
                        }
                    }
                }
            }
        }

        Manager.getThemeManager().stylesModified(themeName);
    }

    public static void loadTheme(String src) throws ThemeIOException,
            ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.loadTheme(src);
    }

    public static void insertFragment(Element destElement, String typeName)
            throws NodeException {
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
        Format widget = themeManager.createWidget();
        String viewTypeName = typeName.split("/")[1];
        widget.setName(viewTypeName);
        ElementFormatter.setFormat(fragment, widget);
        // insert the fragment
        destContainer.addChild(fragment);
        // set the fragment order
        fragment.moveTo(destContainer, order);

        final String themeName = ThemeManager.getThemeOf(destElement).getName();
        Manager.getThemeManager().themeModified(themeName);

    }

    public static void insertSectionAfter(Element element) throws NodeException {
        ThemeManager themeManager = Manager.getThemeManager();
        Element newSection = ElementFactory.create("section");
        Element newCell = ElementFactory.create("cell");
        // section
        Format sectionWidget = themeManager.createWidget();
        sectionWidget.setName("section frame");
        Format sectionLayout = themeManager.createLayout();
        sectionLayout.setProperty("width", "100%");
        Format sectionStyle = themeManager.createStyle();
        ElementFormatter.setFormat(newSection, sectionWidget);
        ElementFormatter.setFormat(newSection, sectionLayout);
        ElementFormatter.setFormat(newSection, sectionStyle);
        // cell
        Format cellWidget = themeManager.createWidget();
        cellWidget.setName("cell frame");
        Format cellLayout = themeManager.createLayout();
        cellLayout.setProperty("width", "100%");
        Format cellStyle = themeManager.createStyle();
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

        final String themeName = ThemeManager.getThemeOf(element).getName();
        themeManager.themeModified(themeName);
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
