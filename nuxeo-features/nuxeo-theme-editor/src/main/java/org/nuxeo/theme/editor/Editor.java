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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.SectionElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.html.CSSUtils;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.resources.ImageInfo;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.resources.SkinInfo;
import org.nuxeo.theme.resources.StyleInfo;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.themes.ThemeSerializer;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.uids.Identifiable;
import org.nuxeo.theme.views.ViewType;

public class Editor {

    private static final Log log = LogFactory.getLog(Editor.class);

    public static void updateElementWidget(Element element, String viewName)
            throws ThemeException {

        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "change widget view");

        FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "widget");
        Format widget = ElementFormatter.getFormatByType(element, widgetType);
        ThemeManager themeManager = Manager.getThemeManager();
        if (widget == null) {
            widget = themeManager.createWidget();
        }
        widget.setName(viewName);
        ElementFormatter.setFormat(element, widget);

        saveTheme(themeName);
    }

    public static void updateElementLayout(Element element,
            Map<String, String> propertyMap) throws ThemeException {

        if (element != null) {
            final String themeName = ThemeManager.getThemeOf(element).getName();
            saveToUndoBuffer(themeName, "change layout");

            Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                    "layout");
            if (layout == null) {
                layout = Manager.getThemeManager().createLayout();
                ElementFormatter.setFormat(element, layout);
            }
            for (Object key : propertyMap.keySet()) {
                layout.setProperty((String) key, propertyMap.get(key));
            }
            saveTheme(themeName);
        }
    }

    public static void updateElementVisibility(Element element,
            List<String> perspectives, boolean alwaysVisible)
            throws ThemeException {

        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "change element visibility");

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

        saveTheme(themeName);
    }

    public static void updateElementStyle(Element element, Style style,
            String path, String viewName, Map<String, String> propertyMap)
            throws ThemeException {

        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "update element style");

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

        saveTheme(themeName);
    }

    public static void setPageStyles(String themeName,
            Map<String, String> propertyMap) throws ThemeException {

        saveToUndoBuffer(themeName, "set page styles");

        ThemeManager themeManager = Manager.getThemeManager();
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String pageName = entry.getKey();
            String styleName = entry.getValue();
            if ("".equals(styleName)) {
                styleName = null;
            }
            String pagePath = String.format("%s/%s", themeName, pageName);
            PageElement page = themeManager.getPageByPath(pagePath);
            if (page != null) {
                Style pageStyle = (Style) ElementFormatter.getFormatFor(page,
                        "style");
                if (pageStyle == null) {
                    pageStyle = (Style) FormatFactory.create("style");
                    ElementFormatter.setFormat(page, pageStyle);
                }
                makeElementUseNamedStyle(page, styleName, themeName);
            }
        }
        saveTheme(themeName);
    }

    public static void splitElement(Element element) throws NodeException,
            ThemeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "split cell");

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

        saveTheme(themeName);
    }

    public static void updateElementStyleCss(Element element, Style style,
            String viewName, String cssSource) throws ThemeException {

        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "update style properties");

        if (style == null) {
            FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                    TypeFamily.FORMAT, "style");
            style = (Style) ElementFormatter.getFormatByType(element, styleType);
        }
        if (style.isNamed() || "".equals(viewName)) {
            viewName = "*";
        }
        org.nuxeo.theme.Utils.loadCss(style, cssSource, viewName);
        saveTheme(themeName);
    }

    public static void updateNamedStyleCss(Style style, String cssSource,
            String themeName) throws ThemeException {
        saveToUndoBuffer(themeName, "update style properties");
        if (style == null || style.getName() == null) {
            throw new ThemeException("A named style is required.");
        }
        final String viewName = "*";
        org.nuxeo.theme.Utils.loadCss(style, cssSource, viewName);
        // if the style came from a resource bank, it has now been customized.
        if (style.isRemote()) {
            style.setCustomized(true);
        }
        saveTheme(themeName);
    }

    public static void restoreNamedStyle(Style style, String themeName)
            throws ThemeException {
        saveToUndoBuffer(themeName, "restore style");
        if (style == null || style.getName() == null) {
            throw new ThemeException("A named style is required.");
        }
        if (!style.isRemote()) {
            throw new ThemeException(
                    "A style from a remote resource bank is required.");
        }
        // if the style came from a resource bank, it has now been customized.
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            throw new ThemeException("Theme not found: " + themeName);
        }
        String resourceBankName = themeDescriptor.getResourceBankName();
        if (resourceBankName != null) {
            ThemeManager.loadRemoteStyle(resourceBankName, style);
        }
        style.setCustomized(false);
        saveTheme(themeName);
    }

    public static void updateElementWidth(Format layout, String width)
            throws ThemeException {
        final String themeName = ThemeManager.getThemeOfFormat(layout).getName();
        saveToUndoBuffer(themeName, "change element width");

        layout.setProperty("width", width);

        saveTheme(themeName);
    }

    public static void updateElementProperties(Element element,
            Map<String, String> propertyMap) throws ThemeIOException,
            ThemeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "change element properties");

        Properties properties = new Properties();
        for (Object key : propertyMap.keySet()) {
            properties.put(key, propertyMap.get(key));
        }
        FieldIO.updateFieldsFromProperties(element, properties);

        saveTheme(themeName);
    }

    public static void updateElementDescription(Element element,
            String description) throws ThemeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "change element description");

        element.setDescription(description);

        saveTheme(themeName);
    }

    public static void repairTheme(String src) throws ThemeIOException,
            ThemeException {
        ThemeElement theme = Manager.getThemeManager().getThemeBySrc(src);
        if (theme == null) {
            throw new ThemeIOException("Unknown theme: " + src);
        }
        final String themeName = theme.getName();
        saveToUndoBuffer(themeName, "repair theme");

        ThemeManager.repairTheme(theme);
        saveTheme(themeName);
    }

    public static void deleteTheme(String src) throws ThemeIOException,
            ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.deleteTheme(src);
    }

    public static void deletePage(String pagePath) throws ThemeIOException,
            ThemeException {
        final String themeName = pagePath.split("/")[0];
        saveToUndoBuffer(themeName, "delete page");

        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.deletePage(pagePath);

        saveTheme(themeName);
    }

    public static void saveTheme(String themeName) throws ThemeException {
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            throw new ThemeException("Theme not found: " + themeName);
        }
        String themeSrc = themeDescriptor.getSrc();
        if (!themeDescriptor.isSaveable()) {
            if (themeDescriptor.isCustomizable()) {
                themeDescriptor = ThemeManager.customizeTheme(themeDescriptor);
                themeSrc = themeDescriptor.getSrc();
            } else {
                throw new ThemeException("Theme cannot be customized: "
                        + themeName);
            }
        }
        try {
            ThemeManager.saveTheme(themeSrc);
        } catch (ThemeIOException e) {
            throw new ThemeException("Theme cannot be saved: " + themeName, e);
        }
        final ThemeManager themeManager = Manager.getThemeManager();
        themeManager.themeModified(themeName);
        themeManager.stylesModified(themeName);
    }

    public static String renderCssPreview(Element element, Style style,
            String viewName, String basePath) {
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
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);

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
                Enumeration<?> propertyNames = CSSUtils.getCssProperties().propertyNames();
                while (propertyNames.hasMoreElements()) {
                    String propertyName = (String) propertyNames.nextElement();
                    String value = styleProperties.getProperty(propertyName);
                    if (value == null) {
                        continue;
                    }
                    css.append(propertyName);
                    css.append(':');
                    css.append(value);
                    css.append(';');
                }
                css.append('}');
            }
        }
        return CSSUtils.expandVariables(css.toString(), basePath,
                themeDescriptor);
    }

    public static void pasteElement(Element element, String destId)
            throws ThemeException, NodeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "paste element");

        Element destElement = ThemeManager.getElementById(destId);
        if (destElement.isLeaf()) {
            destElement = (Element) destElement.getParent();
        }
        if (element != null) {
            destElement.addChild(Manager.getThemeManager().duplicateElement(
                    element, true));
        }

        saveTheme(themeName);
    }

    public static void moveElement(Element srcElement, Element destElement,
            int order) throws ThemeException, NodeException {
        ThemeElement srcTheme = ThemeManager.getThemeOf(srcElement);
        if (srcTheme == null) {
            throw new ThemeException(
                    "Could not determing the theme of the element :"
                            + srcElement.computeXPath());
        }
        final String themeName = srcTheme.getName();
        saveToUndoBuffer(themeName, "move element");

        srcElement.moveTo(destElement, order);
        saveTheme(themeName);
    }

    public static void makeElementUseNamedStyle(Element element,
            String styleName, String themeName) throws ThemeException {

        Style currentStyle = (Style) ElementFormatter.getFormatFor(element,
                "style");
        if (currentStyle == null) {
            throw new ThemeException(String.format(
                    "Element %s has no style format.", element.computeXPath()));
        }

        saveToUndoBuffer(themeName, "change element style");

        ThemeManager themeManager = Manager.getThemeManager();
        if (styleName == null) {
            ThemeManager.removeInheritanceTowards(currentStyle);
        } else {
            Style namedStyle = (Style) themeManager.getNamedObject(themeName,
                    "style", styleName);
            if (namedStyle != null) {
                themeManager.makeFormatInherit(currentStyle, namedStyle);
            }
        }
        saveTheme(themeName);
    }

    public static void removeStyleInheritance(String styleName, String themeName)
            throws ThemeException {
        saveToUndoBuffer(themeName, "remove style inheritance");
        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(themeName, "style",
                styleName);
        if (style == null) {
            throw new ThemeException("Could not find named style: " + styleName);
        }
        ThemeManager.removeInheritanceTowards(style);
        saveTheme(themeName);
    }

    public static void setStyleInheritance(String styleName,
            String ancestorStyleName, String themeName) throws ThemeException {
        saveToUndoBuffer(themeName, "set style inheritance");
        final boolean allowMany = true;
        ThemeManager.setStyleInheritance(styleName, ancestorStyleName,
                themeName, allowMany);
        saveTheme(themeName);
    }

    public static String addPage(String path) throws ThemeException,
            NodeException, ThemeIOException {

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

        saveToUndoBuffer(themeName, "add page");

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
        // save the page
        saveTheme(themeName);
        return path;
    }

    public static String addTheme(String name) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        if (themeManager.getThemeByName(name) != null) {
            throw new ThemeException("The theme name is already taken: " + name);
        }
        ThemeDescriptor themeDef = ThemeManager.createCustomTheme(name);
        String themeName = themeDef.getName();
        return String.format("%s/default", themeName);
    }

    public static String uncustomizeTheme(String src) throws ThemeException {
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptor(src);
        if (themeDescriptor == null) {
            throw new ThemeException("Theme not found: " + src);
        }
        String themeName = themeDescriptor.getName();
        ThemeManager.uncustomizeTheme(themeDescriptor);
        return String.format("%s/default", themeName);
    }

    public static void assignStyleProperty(Element element,
            String propertyName, String value) throws ThemeException {

        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "change style");

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

        saveTheme(themeName);
    }

    public static void alignElement(Element element, String position)
            throws ThemeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "change element alignment");

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
        saveTheme(themeName);
    }

    public static void deleteElement(Element element) throws ThemeException,
            NodeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "delete element");

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

        saveTheme(themeName);
    }

    public static int duplicateElement(Element element) throws ThemeException,
            NodeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "duplicate element");

        Element duplicate = Manager.getThemeManager().duplicateElement(element,
                true);

        // insert the duplicated element
        element.getParent().addChild(duplicate);
        duplicate.moveTo(element.getParent(), element.getOrder() + 1);

        saveTheme(themeName);

        return duplicate.getUid();
    }

    public static void createStyle(Element element) throws ThemeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "create style");

        ThemeManager themeManager = Manager.getThemeManager();
        final Format style = themeManager.createStyle();
        ElementFormatter.setFormat(element, style);

        saveTheme(themeName);
    }

    public static void createNamedStyle(Element element, String styleName,
            String themeName) throws ThemeException {
        saveToUndoBuffer(themeName, "create style");

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

        if (element != null) {
            themeManager.makeElementUseNamedStyle(element, styleName, themeName);
        }

        saveTheme(themeName);
    }

    public static Style getNamedStyleOf(Element element) throws ThemeException {
        Style style = (Style) ElementFormatter.getFormatFor(element, "style");
        if (style == null) {
            return null;
        }
        Style ancestorStyle = (Style) ThemeManager.getAncestorFormatOf(style);
        if (ancestorStyle == null || !ancestorStyle.isNamed()) {
            return null;
        }
        return ancestorStyle;
    }

    public static void deleteNamedStyle(Element element, String styleName,
            String themeName) throws ThemeException {
        saveToUndoBuffer(themeName, "delete style");

        ThemeManager themeManager = Manager.getThemeManager();
        Style style = (Style) themeManager.getNamedObject(themeName, "style",
                styleName);
        themeManager.deleteFormat(style);
        if (element != null) {
            themeManager.makeElementUseNamedStyle(element, null, themeName);
        }
        themeManager.removeNamedObject(themeName, "style", styleName);
        saveTheme(themeName);
    }

    public static void deleteStyleView(Style style, String viewName)
            throws ThemeException {
        final String themeName = ThemeManager.getThemeOfFormat(style).getName();
        saveToUndoBuffer(themeName, "delete style view");

        style.clearPropertiesFor(viewName);
        saveTheme(themeName);
    }

    public static List<String> getHardcodedColors(final String themeName) {
        Set<String> colors = new HashSet<String>();
        for (Style style : Manager.getThemeManager().getStyles(themeName)) {
            for (Map.Entry<Object, Object> entry : style.getAllProperties().entrySet()) {
                String value = (String) entry.getValue();
                colors.addAll(org.nuxeo.theme.html.CSSUtils.extractCssColors(value));
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
                images.addAll(org.nuxeo.theme.html.CSSUtils.extractCssImages(value));
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
        saveToUndoBuffer(themeName, "create preset");

        if (presetName.equals("")) {
            throw new ThemeException("Preset name cannot be empty");
        }
        if (PresetManager.getCustomPreset(themeName, presetName) != null) {
            throw new ThemeException("Preset name already taken: " + presetName);
        }
        PresetManager.createCustomPreset(themeName, presetName, category,
                value, "", "");
        saveTheme(themeName);
        return presetName;
    }

    public static void editPreset(String themeName, String presetName,
            String value) throws ThemeException {
        saveToUndoBuffer(themeName, "modify preset");

        PresetManager.editPreset(themeName, presetName, value);
        saveTheme(themeName);
    }

    public static void setPresetCategory(String themeName, String presetName,
            String category) throws ThemeException {
        saveToUndoBuffer(themeName, "change preset category");

        PresetManager.setPresetCategory(themeName, presetName, category);
        saveTheme(themeName);
    }

    public static void renamePreset(String themeName, String oldName,
            String newName) throws ThemeException {
        saveToUndoBuffer(themeName, "rename preset");

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
        saveTheme(themeName);
    }

    public static void deletePreset(String themeName, String presetName)
            throws ThemeException {
        saveToUndoBuffer(themeName, "delete preset");

        PresetManager.deletePreset(themeName, presetName);
        saveTheme(themeName);
    }

    public static void convertCssValueToPreset(String themeName,
            String category, String presetName, String value)
            throws ThemeException {
        saveToUndoBuffer(themeName, "create preset from CSS property");

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
                            newText = org.nuxeo.theme.html.CSSUtils.replaceColor(
                                    text, value, presetStr);
                        } else if (category.equals("image")) {
                            newText = org.nuxeo.theme.html.CSSUtils.replaceImage(
                                    text, value, presetStr);
                        }
                        if (!newText.equals(text)) {
                            styleProperties.setProperty(key, newText);
                        }
                    }
                }
            }
        }
        saveTheme(themeName);
    }

    public static void loadTheme(String src) throws ThemeIOException,
            ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        themeManager.loadTheme(src);

        // Clean up the undo buffer
        ThemeDescriptor themeDef = ThemeManager.getThemeDescriptor(src);
        String themeName = themeDef.getName();
        UndoBuffer undoBuffer = SessionManager.getUndoBuffer(themeName);
        if (undoBuffer != null) {
            undoBuffer.clearBuffer();
        }
    }

    public static void insertFragment(Element destElement, String typeName,
            String styleName) throws NodeException, ThemeException {
        final String themeName = ThemeManager.getThemeOf(destElement).getName();
        saveToUndoBuffer(themeName, "add fragment");

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
        Widget widget = themeManager.createWidget();
        String viewTypeName = typeName.split("/")[1];
        widget.setName(viewTypeName);
        ElementFormatter.setFormat(fragment, widget);
        // set a style
        if (!"".equals(styleName)) {
            Style ancestor = (Style) themeManager.getNamedObject(themeName,
                    "style", styleName);
            if (ancestor != null) {
                Style style = themeManager.createStyle();
                themeManager.makeFormatInherit(style, ancestor);
                ElementFormatter.setFormat(fragment, style);
            }
        }
        // insert the fragment
        destContainer.addChild(fragment);
        // set the fragment order
        fragment.moveTo(destContainer, order);

        saveTheme(themeName);
    }

    public static void insertSectionAfter(Element element)
            throws NodeException, ThemeException {
        final String themeName = ThemeManager.getThemeOf(element).getName();
        saveToUndoBuffer(themeName, "insert section");

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

        saveTheme(themeName);
    }

    // UndoBuffer
    public static void saveToUndoBuffer(final String themeName,
            final String message) throws ThemeException {
        if (themeName == null) {
            throw new ThemeException("Theme not set.");
        }
        ThemeDescriptor themeDef = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDef == null) {
            throw new ThemeException("Theme not found: " + themeName);
        }
        ThemeSerializer serializer = new ThemeSerializer();
        String xmlSource;
        try {
            xmlSource = serializer.serializeToXml(themeDef.getSrc(), 0);
        } catch (ThemeIOException e) {
            throw new ThemeException(
                    "Could not save theme into the under buffer", e);
        }
        UndoBuffer undoBuffer = SessionManager.getUndoBuffer(themeName);
        if (undoBuffer == null) {
            undoBuffer = new UndoBuffer();
            SessionManager.setUndoBuffer(themeName, undoBuffer);
        }
        undoBuffer.save(xmlSource, message);
    }

    public static String undo(final String themeName) throws ThemeException {
        ThemeDescriptor themeDef = ThemeManager.getThemeDescriptorByThemeName(
                null, themeName);
        if (themeDef == null) {
            throw new ThemeException("Theme unknown." + themeName);
        }
        UndoBuffer undoBuffer = SessionManager.getUndoBuffer(themeName);
        if (undoBuffer == null) {
            throw new ThemeException("No history buffer found.");
        }
        String savedVersion = undoBuffer.getSavedVersion();
        if (savedVersion == null) {
            throw new ThemeException("No saved version found.");
        }
        try {
            Manager.getThemeManager().loadTheme(themeDef.getSrc(), savedVersion);
        } catch (ThemeIOException e) {
            throw new ThemeException(e.getMessage(), e);
        }
        undoBuffer.clearBuffer();
        saveTheme(themeName);
        return undoBuffer.getMessage();
    }

    public static void createFragmentPreview(String currentThemeName) {
        ThemeManager themeManager = Manager.getThemeManager();
        String fragmentType = SessionManager.getFragmentType();
        String viewName = SessionManager.getFragmentView();
        String styleName = SessionManager.getFragmentStyle();

        Fragment fragment = FragmentFactory.create(fragmentType);
        try {
            // View
            Widget widget = (Widget) FormatFactory.create("widget");
            widget.setName(viewName);
            ElementFormatter.setFormat(fragment, widget);

            // Style
            Style style = (Style) FormatFactory.create("style");
            ElementFormatter.setFormat(fragment, style);

            String themeName = currentThemeName.split("/")[0];
            themeManager.makeElementUseNamedStyle(fragment, styleName,
                    themeName);

            themeManager.fillScratchPage(themeName, fragment);

        } catch (Exception e) {
            log.error(e, e);
        }
        // Clean cache
        themeManager.themeModified(currentThemeName);
    }

    /*
     * Skin management
     */
    public static void activateSkin(String themeName, String bankName,
            String collectionName, String resourceName, boolean isBaseSkin)
            throws ThemeException {
        String currentTopSkinName = getCurrentTopSkinName(themeName);
        String currentBaseSkinName = getCurrentBaseSkinName(themeName);
        for (SkinInfo skin : getBankSkins(bankName)) {
            if (skin.getName().equals(currentTopSkinName)) {
                if (skin.isBase()) {
                    currentTopSkinName = null;
                }
            }
            if (skin.getName().equals(currentBaseSkinName)) {
                if (!skin.isBase()) {
                    currentBaseSkinName = null;
                }
            }
        }

        ThemeManager themeManager = Manager.getThemeManager();
        String skinName = String.format("%s (%s)", resourceName, collectionName);

        if (!isBaseSkin && currentBaseSkinName == null) {
            throw new ThemeException("Cannot set skin: " + skinName
                    + " (base skin is missing)");
        }

        if (isBaseSkin && (skinName.equals(currentBaseSkinName))) {
            return;
        }

        saveToUndoBuffer(themeName, "activate skin");

        if (!isBaseSkin && currentBaseSkinName != null) {
            if (skinName.equals(currentTopSkinName)) {
                return;
            }
            final boolean allowMany = false;
            ThemeManager.setStyleInheritance(skinName, currentBaseSkinName,
                    themeName, allowMany);
        }

        for (PageElement page : themeManager.getPagesOf(themeName)) {
            Style newStyle = themeManager.createStyle();
            ElementFormatter.setFormat(page, newStyle);
            themeManager.makeElementUseNamedStyle(page, skinName, themeName);
        }

        themeManager.removeOrphanedFormats();
        saveTheme(themeName);
    }

    public static void deactivateSkin(String themeName) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();

        saveToUndoBuffer(themeName, "deactivate skin");

        for (PageElement page : themeManager.getPagesOf(themeName)) {
            Style newStyle = themeManager.createStyle();
            ElementFormatter.setFormat(page, newStyle);
            themeManager.makeElementUseNamedStyle(page, null, themeName);
        }
        themeManager.removeOrphanedFormats();
        saveTheme(themeName);
    }

    public static String getCurrentTopSkinName(final String themeName) {
        Style skinStyle = getCurrentPageSkin(themeName);
        if (skinStyle == null) {
            return null;
        }
        return skinStyle.getName();
    }

    public static String getCurrentBaseSkinName(final String themeName) {
        Style skinStyle = getCurrentPageSkin(themeName);
        if (skinStyle == null) {
            return null;
        }
        Style ancestorStyle = (Style) ThemeManager.getAncestorFormatOf(skinStyle);
        if (ancestorStyle != null && ancestorStyle.isNamed()) {
            return ancestorStyle.getName();
        }
        return skinStyle.getName();
    }

    public static Style getCurrentPageSkin(final String themeName) {
        if (themeName == null) {
            return null;
        }
        ThemeManager themeManager = Manager.getThemeManager();
        final FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        Style skinStyle = null;
        String skinName = null;
        String previousSkinName = null;

        List<PageElement> themePages = themeManager.getPagesOf(themeName);
        if (themePages == null) {
            return null;
        }
        for (PageElement page : themePages) {
            Style style = (Style) ElementFormatter.getFormatByType(page,
                    styleType);
            if (style == null) {
                return null;
            }
            Style ancestorStyle = (Style) ThemeManager.getAncestorFormatOf(style);
            if (ancestorStyle == null || !ancestorStyle.isNamed()) {
                return null;
            }
            skinName = ancestorStyle.getName();
            if (previousSkinName != null && !skinName.equals(previousSkinName)) {
                return null;
            }
            skinStyle = ancestorStyle;
            previousSkinName = skinName;
        }
        return skinStyle;
    }

    public static void useResourceBank(String themeSrc, String bankName)
            throws ThemeException {
        ResourceBank resourceBank = ThemeManager.getResourceBank(bankName);
        if (!resourceBank.checkStatus()) {
            throw new ThemeException("Could not connect to bank: " + bankName);
        }
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptor(themeSrc);
        String themeName = themeDescriptor.getName();

        saveToUndoBuffer(themeName, "connect to theme bank: " + bankName);

        themeDescriptor.setResourceBankName(bankName);
        resourceBank.connect(themeName);

        saveTheme(themeName);
    }

    public static void useNoResourceBank(String themeSrc) throws ThemeException {
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptor(themeSrc);
        String bankName = themeDescriptor.getResourceBankName();
        ResourceBank resourceBank = ThemeManager.getResourceBank(bankName);
        if (!resourceBank.checkStatus()) {
            throw new ThemeException("Could not disconnect from bank: "
                    + bankName);
        }
        String themeName = themeDescriptor.getName();

        saveToUndoBuffer(themeName, "disconnect from theme bank: " + bankName);

        resourceBank.disconnect(themeName);
        themeDescriptor.setResourceBankName(null);

        saveTheme(themeName);
    }

    public static List<SkinInfo> getBankSkins(String bankName) {
        List<SkinInfo> info = new ArrayList<SkinInfo>();
        if (bankName != null) {
            ResourceBank resourceBank;
            try {
                resourceBank = ThemeManager.getResourceBank(bankName);
                info = resourceBank.getSkins();
            } catch (ThemeException e) {
                e.printStackTrace();
            }
        }
        return info;
    }

    public static List<StyleInfo> getBankStyles(String bankName) {
        List<StyleInfo> info = new ArrayList<StyleInfo>();
        if (bankName != null) {
            ResourceBank resourceBank;
            try {
                resourceBank = ThemeManager.getResourceBank(bankName);
                info = resourceBank.getStyles();
            } catch (ThemeException e) {
                e.printStackTrace();
            }
        }
        return info;
    }

    public static SkinInfo getSkinInfo(String bankName, String skinName) {
        ResourceBank resourceBank;
        try {
            resourceBank = ThemeManager.getResourceBank(bankName);
        } catch (ThemeException e) {
            return null;
        }
        for (SkinInfo skin : resourceBank.getSkins()) {
            if (skinName.equals(skin.getName())) {
                return skin;
            }
        }
        return null;
    }

    public static List<String> getBankCollections(String bankName) {
        List<String> collections = new ArrayList<String>();
        if (bankName != null) {
            ResourceBank resourceBank;
            try {
                resourceBank = ThemeManager.getResourceBank(bankName);
                collections.addAll(resourceBank.getCollections());
            } catch (ThemeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return collections;
    }

    public static List<ImageInfo> getBankImages(String bankName) {
        List<ImageInfo> images = new ArrayList<ImageInfo>();
        if (bankName != null) {
            ResourceBank resourceBank;
            try {
                resourceBank = ThemeManager.getResourceBank(bankName);
                images.addAll(resourceBank.getImages());
            } catch (ThemeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return images;
    }

    public static List<Style> getNamedStyles(String themeName,
            ResourceBank resourceBank) {
        List<Style> styles = new ArrayList<Style>();
        ThemeManager themeManager = Manager.getThemeManager();
        for (Identifiable s : themeManager.getNamedObjects(themeName, "style")) {
            styles.add((Style) s);
        }
        return styles;
    }

    public static List<Style> listNamedStylesDirectlyInheritingFrom(Style style) {
        List<Style> styles = new ArrayList<Style>();
        for (Format format : ThemeManager.listFormatsDirectlyInheritingFrom(style)) {
            if (format.isNamed()) {
                styles.add((Style) format);
            }
        }
        return styles;
    }

    public static Map<String, String> getPageStyles(String themeName) {
        Map<String, String> pageStyles = new LinkedHashMap<String, String>();
        List<PageElement> pages = Manager.getThemeManager().getPagesOf(
                themeName);
        if (!pages.isEmpty()) {
            for (PageElement page : pages) {
                Style namedStyle = null;
                try {
                    namedStyle = Editor.getNamedStyleOf(page);
                } catch (ThemeException e) {
                    e.printStackTrace();
                }
                String styleName = namedStyle == null ? ""
                        : namedStyle.getName();
                pageStyles.put(page.getName(), styleName);
            }
        }
        return pageStyles;
    }

    public static Style getThemeSkin(String themeName) {
        List<PageElement> pages = Manager.getThemeManager().getPagesOf(
                themeName);
        if (pages == null || pages.isEmpty()) {
            return null;
        }
        for (PageElement page : pages) {
            Style namedStyle = null;
            try {
                namedStyle = Editor.getNamedStyleOf(page);
            } catch (ThemeException e) {
                e.printStackTrace();
            }
            if (namedStyle != null) {
                return namedStyle;
            }
        }
        return null;
    }

    public static List<FragmentType> getFragments(String templateEngine) {
        List<FragmentType> fragments = new ArrayList<FragmentType>();
        for (Type f : Manager.getTypeRegistry().getTypes(TypeFamily.FRAGMENT)) {
            FragmentType fragmentType = (FragmentType) f;
            if (fragments.contains(fragmentType)) {
                continue;
            }
            List<ViewType> views = new ArrayList<ViewType>();
            for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
                if (templateEngine.equals(viewType.getTemplateEngine())) {
                    views.add(viewType);
                }
            }
            if (views.isEmpty()) {
                continue;
            }
            fragments.add(fragmentType);
        }
        return fragments;
    }

    public static List<ViewType> getViews(String fragmentTypeName,
            String templateEngine) {
        List<ViewType> views = new ArrayList<ViewType>();
        if (fragmentTypeName == null) {
            return views;
        }
        FragmentType fragmentType = (FragmentType) Manager.getTypeRegistry().lookup(
                TypeFamily.FRAGMENT, fragmentTypeName);
        if (fragmentType == null) {
            return views;
        }
        for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
            if (templateEngine.equals(viewType.getTemplateEngine())) {
                views.add(viewType);
            }
        }
        return views;
    }

}
