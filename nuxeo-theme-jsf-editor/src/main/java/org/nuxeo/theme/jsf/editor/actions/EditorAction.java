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

package org.nuxeo.theme.jsf.editor.actions;

import static org.jboss.seam.ScopeType.SESSION;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ejb.Remove;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
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
import org.nuxeo.theme.jsf.Utils;
import org.nuxeo.theme.jsf.editor.managers.UiManagerLocal;
import org.nuxeo.theme.jsf.editor.states.UiStatesLocal;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.uids.UidManager;

@Stateless
@Name("nxthemesEditorAction")
@Scope(SESSION)
@SerializedConcurrentAccess
public class EditorAction implements EditorActionLocal {

    private static final Log log = LogFactory.getLog(EditorAction.class);

    private static final UidManager uidManager = Manager.getUidManager();

    private static final EventManager eventManager = Manager.getEventManager();

    private static final ThemeManager themeManager = Manager.getThemeManager();

    private static final String THEME_MODIFIED_EVENT = "theme modified";

    private static final String STYLES_MODIFIED_EVENT = "styles modified";

    @In(value = "nxthemesUiStates", create = true)
    public UiStatesLocal uiStates;

    @In(value = "nxthemesUiManager", create = true)
    public UiManagerLocal uiManager;

    /* Canvas */

    public String addTheme(final String name) {
        if (themeManager.getThemeByName(name) != null) {
            log.debug("Theme already exists: " + name);
            return "";
        }

        // theme
        final ThemeElement theme = (ThemeElement) ElementFactory.create("theme");
        theme.setName(name);
        final Format themeWidget = FormatFactory.create("widget");
        themeWidget.setName("theme view");
        themeManager.registerFormat(themeWidget);
        ElementFormatter.setFormat(theme, themeWidget);

        // default page
        final PageElement page = (PageElement) ElementFactory.create("page");
        page.setName("default");
        final Format pageWidget = FormatFactory.create("widget");
        themeManager.registerFormat(pageWidget);
        pageWidget.setName("page frame");
        final Format pageLayout = FormatFactory.create("layout");
        themeManager.registerFormat(pageLayout);
        final Format pageStyle = FormatFactory.create("style");
        themeManager.registerFormat(pageStyle);
        ElementFormatter.setFormat(page, pageWidget);
        ElementFormatter.setFormat(page, pageStyle);
        ElementFormatter.setFormat(page, pageLayout);

        theme.addChild(page);

        themeManager.registerTheme(theme);
        log.debug("Added theme: " + name);
        return String.format("%s/%s", name, "default");
    }

    public String addPage(final String path) {
        if (!path.contains("/")) {
            log.debug("Page paths must contain '/': " + path);
            return "";
        }
        if (themeManager.getPageByPath(path) != null) {
            log.debug("Page already exists: " + path);
            return "";
        }

        final String themeName = path.split("/")[0];
        final ThemeElement theme = themeManager.getThemeByName(themeName);

        // add page
        final PageElement page = (PageElement) ElementFactory.create("page");
        final String pageName = path.split("/")[1];
        page.setName(pageName);
        final Format pageWidget = FormatFactory.create("widget");
        pageWidget.setName("page frame");
        themeManager.registerFormat(pageWidget);
        final Format pageLayout = FormatFactory.create("layout");
        themeManager.registerFormat(pageLayout);
        final Format pageStyle = FormatFactory.create("style");
        themeManager.registerFormat(pageStyle);
        ElementFormatter.setFormat(page, pageWidget);
        ElementFormatter.setFormat(page, pageStyle);
        ElementFormatter.setFormat(page, pageLayout);

        themeManager.registerPage(theme, page);
        return path;
    }

    public String moveElement(final String srcId, final String destId,
            final Integer order) {
        final Element srcElement = getElementById(srcId);
        final Element destElement = getElementById(destId);
        // move the element
        srcElement.moveTo(destElement, order);
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(srcElement,
                destElement));
        log.debug("Moved element: " + srcId + " to: " + destId);
        return "";
    }

    public String selectElement(final String id) {
        final Element element = getElementById(id);
        if (element != null) {
            uiStates.setSelectedElement(element);
            uiStates.setCurrentStyleSelector(null);
            uiStates.setStyleCategory(null);
            uiStates.setCurrentStyleLayer(null);
        }
        return id;
    }

    public void clearSelections() {
        uiStates.setSelectedElement(null);
        uiStates.setCurrentStyleSelector(null);
        uiStates.setStyleCategory(null);
        uiStates.setCurrentStyleLayer(null);
        clearClipboard();
    }

    public List<String> copyElements(final List<String> ids) {
        // Clear the clipboard before adding elements to it.
        clearClipboard();
        // Copied each element
        for (String id : ids) {
            addElementToClipboard(id);
        }
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(null, null));
        log.debug("Copied the elements: " + ids + " to the clipboard.");
        return null;
    }

    public String duplicateElement(final String id) {
        final Element element = getElementById(id);
        final Element duplicate = themeManager.duplicateElement(element, false);

        // insert the duplicated element
        element.getParent().addChild(duplicate);
        duplicate.moveTo(element.getParent(), element.getOrder() + 1);

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(null,
                element));
        log.debug("Duplicated the element: " + element);
        return duplicate.getUid().toString();
    }

    public List<String> pasteElements(final String destId) {
        Element destElement = getElementById(destId);
        if (destElement.isLeaf()) {
            destElement = (Element) destElement.getParent();
        }
        // Paste elements
        final List<String> ids = uiStates.getClipboardElements();
        for (String id : ids) {
            final Element element = getElementById(id);
            if (element == null) {
                log.debug("Element to paste not found: " + id);
            } else {
                boolean duplicateFormats = true;
                if (ThemeManager.belongToSameTheme(element, destElement)) {
                    duplicateFormats = false;
                }
                destElement.addChild(themeManager.duplicateElement(element,
                        duplicateFormats));
            }
        }

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(null,
                destElement));
        log.debug("Pasted the elements: " + ids + " from the clipboard.");
        return null;
    }

    public String deleteElement(final String id) {
        final Element element = getElementById(id);
        final Element parent = (Element) element.getParent();

        if (element instanceof ThemeElement || element instanceof PageElement) {
            themeManager.destroyElement(element);

        } else if (element instanceof CellElement) {
            if (element.hasSiblings()) {
                Element sibling = (Element) element.getNextNode();
                if (sibling == null) {
                    sibling = (Element) element.getPreviousNode();
                }
                final FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(
                        TypeFamily.FORMAT, "layout");
                final Format layout1 = ElementFormatter.getFormatByType(
                        element, layoutType);
                if (layout1 != null) {
                    final String width1 = layout1.getProperty("width");
                    if (width1 != null) {
                        final Format layout2 = ElementFormatter.getFormatByType(
                                sibling, layoutType);
                        if (layout2 != null) {
                            final String width2 = layout2.getProperty("width");
                            final String newWidth = Utils.addWebLengths(width1,
                                    width2);
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

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(null, null));
        log.debug("Deleted element: " + id);
        return id;
    }

    public String insertFragment(final String typeName, final String destId,
            final Integer order) {
        if (destId == null) {
            return "";
        }
        final Element destElement = getElementById(destId);

        // create the new fragment
        final String fragmentTypeName = typeName.split("/")[0];
        final Fragment fragment = FragmentFactory.create(fragmentTypeName);

        // add a temporary view to the fragment
        final Format widget = FormatFactory.create("widget");
        final String viewTypeName = typeName.split("/")[1];
        widget.setName(viewTypeName);
        themeManager.registerFormat(widget);
        ElementFormatter.setFormat(fragment, widget);

        // insert the fragment
        destElement.addChild(fragment);

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(fragment,
                destElement));
        return "";
    }

    public String expireThemes() {
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(null, null));
        return "";
    }

    /* Clipboard */
    public void addElementToClipboard(final String id) {
        uiStates.getClipboardElements().add(id);
    }

    public void clearClipboard() {
        uiStates.getClipboardElements().clear();
    }

    public void updateElementWidget(final String id, final String viewName) {
        final Element element = getElementById(id);
        final FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "widget");
        Format widget = ElementFormatter.getFormatByType(element, widgetType);
        if (widget == null) {
            widget = FormatFactory.create("widget");
            themeManager.registerFormat(widget);
        }
        widget.setName(viewName);
        ElementFormatter.setFormat(element, widget);
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void updateElementStyle(final String id, String viewName,
            final String path, final Map<Object, Object> propertyMap) {
        final Element element = getElementById(id);
        final Properties properties = new Properties();
        for (Object key : propertyMap.keySet()) {
            properties.put(key, propertyMap.get(key));
        }

        final FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        Style style = (Style) ElementFormatter.getFormatByType(element,
                styleType);
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }

        if (style.getName() != null || "".equals(viewName)) {
            viewName = "*";
        }
        style.setPropertiesFor(viewName, path, properties);
        eventManager.notify(STYLES_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void updateElementStyleCss(final String id, String viewName,
            String cssSource) {
        final Element element = getElementById(id);

        final FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        Style style = (Style) ElementFormatter.getFormatByType(element,
                styleType);
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }

        if (style.getName() != null || "".equals(viewName)) {
            viewName = "*";
        }

        Utils.loadCss(style, cssSource, viewName);
        eventManager.notify(STYLES_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void setElementVisibility(final String id,
            final List<String> perspectives, final boolean alwaysVisible) {
        final Element element = getElementById(id);
        final PerspectiveManager perspectiveManager = Manager.getPerspectiveManager();

        if (alwaysVisible) {
            perspectiveManager.setAlwaysVisible(element);
        } else {
            // initially make the element visible in all perspectives
            if (perspectives.isEmpty()) {
                perspectiveManager.setVisibleInAllPerspectives(element);
            } else {
                perspectiveManager.setVisibleInPerspectives(element,
                        perspectives);
            }
        }
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void updateElementProperties(final String id,
            final Map<Object, Object> propertyMap) {
        final Element element = getElementById(id);
        final Properties properties = new Properties();
        for (Object key : propertyMap.keySet()) {
            properties.put(key, propertyMap.get(key));
        }
        try {
            FieldIO.updateFieldsFromProperties(element, properties);
        } catch (Exception e) {
            log.warn("Could not update properties of element: " + id);
            return;
        }
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void updateElementLayout(final Map<Object, Object> propertyMap) {
        final Element element = uiStates.getSelectedElement();
        if (element != null) {
            Layout layout = (Layout) ElementFormatter.getFormatFor(element,
                    "layout");
            if (layout == null) {
                layout = (Layout) FormatFactory.create("layout");
                themeManager.registerFormat(layout);
                ElementFormatter.setFormat(element, layout);
            }
            for (Object key : propertyMap.keySet()) {
                layout.setProperty((String) key, (String) propertyMap.get(key));
            }
            eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                    null));
        }
    }

    public void setSize(final String id, final String width) {
        final Format layout = getFormatById(id);
        layout.setProperty("width", width);
        eventManager.notify(THEME_MODIFIED_EVENT,
                new EventContext(layout, null));
    }

    public void splitElement(final String id) {
        final Element element = getElementById(id);
        if (!element.getElementType().getTypeName().equals("cell")) {
            return;
        }
        final Element newCell = ElementFactory.create("cell");
        final Format cellWidget = FormatFactory.create("widget");
        cellWidget.setName("cell frame");
        themeManager.registerFormat(cellWidget);
        final Format cellLayout = FormatFactory.create("layout");
        themeManager.registerFormat(cellLayout);
        final FormatType layoutType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "layout");

        final Format layout = ElementFormatter.getFormatByType(element,
                layoutType);
        final String width = layout.getProperty("width");
        if (width != null) {
            final String halfWidth = Utils.divideWebLength(width, 2);
            if (halfWidth != null) {
                cellLayout.setProperty("width", halfWidth);
                layout.setProperty("width", Utils.substractWebLengths(width,
                        halfWidth));
            }
        }

        final Format cellStyle = FormatFactory.create("style");
        themeManager.registerFormat(cellStyle);
        ElementFormatter.setFormat(newCell, cellWidget);
        ElementFormatter.setFormat(newCell, cellLayout);
        ElementFormatter.setFormat(newCell, cellStyle);
        newCell.insertAfter(element);
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void insertSectionAfter(final String id) {
        final Element element = getElementById(id);

        final Element newSection = ElementFactory.create("section");
        final Element newCell = ElementFactory.create("cell");

        // section
        final Format sectionWidget = FormatFactory.create("widget");
        sectionWidget.setName("section frame");
        themeManager.registerFormat(sectionWidget);
        final Format sectionLayout = FormatFactory.create("layout");
        sectionLayout.setProperty("width", "100%");
        themeManager.registerFormat(sectionLayout);
        final Format sectionStyle = FormatFactory.create("style");
        themeManager.registerFormat(sectionStyle);

        ElementFormatter.setFormat(newSection, sectionWidget);
        ElementFormatter.setFormat(newSection, sectionLayout);
        ElementFormatter.setFormat(newSection, sectionStyle);

        // cell
        final Format cellWidget = FormatFactory.create("widget");
        cellWidget.setName("cell frame");
        themeManager.registerFormat(cellWidget);
        final Format cellLayout = FormatFactory.create("layout");
        themeManager.registerFormat(cellLayout);
        cellLayout.setProperty("width", "100%");
        final Format cellStyle = FormatFactory.create("style");
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

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(newSection,
                null));
    }

    public void alignElement(final String id, final String position) {
        final Element element = getElementById(id);
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

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    /* style editor */
    public void setCurrentStyleSelector(final String currentStyleSelector) {
        uiStates.setCurrentStyleSelector(currentStyleSelector);
    }

    public void setStyleEditMode(final String mode) {
        uiStates.setStyleEditMode(mode);
    }

    public void setStylePropertyCategory(final String category) {
        uiStates.setStylePropertyCategory(category);
    }

    public void setStyleCategory(final String category) {
        uiStates.setStyleCategory(category);
    }

    public void setPresetGroup(final String group) {
        uiStates.setPresetGroup(group);
    }

    public void assignStyleProperty(final String id, final String property,
            final String value) {
        final Element element = getElementById(id);
        if (element == null) {
            return;
        }
        Style style = (Style) ElementFormatter.getFormatFor(element, "style");
        if (style == null) {
            style = (Style) FormatFactory.create("style");
            themeManager.registerFormat(style);
            ElementFormatter.setFormat(element, style);
        }

        final Widget widget = (Widget) ElementFormatter.getFormatFor(element,
                "widget");
        if (widget == null) {
            log.error("Element " + element.computeXPath() + " has no widget.");
            return;
        }
        String viewName = widget.getName();

        Properties properties = style.getPropertiesFor(viewName, "");
        if (properties == null) {
            properties = new Properties();
        }
        if (value == null) {
            properties.remove(property);
        } else {
            properties.setProperty(property, value);
        }
        style.setPropertiesFor(viewName, "", properties);

        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
        eventManager.notify(STYLES_MODIFIED_EVENT,
                new EventContext(style, null));
    }

    public void createStyle() {
        final Element element = uiStates.getSelectedElement();
        if (element == null) {
            return;
        }
        final Format style = FormatFactory.create("style");
        themeManager.registerFormat(style);
        ElementFormatter.setFormat(element, style);
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(element,
                null));
    }

    public void setCurrentStyleLayer(final Integer uid) {
        final Style layer = (Style) uidManager.getObjectByUid(uid);
        if (layer != null) {
            uiStates.setCurrentStyleLayer(layer);
        }
    }

    /* Theme management */
    public boolean repairTheme(final String themeName) {
        final ThemeElement theme = themeManager.getThemeByName(themeName);
        if (theme == null) {
            log.error("Theme not found: " + themeName);
            return false;
        }
        ThemeManager.repairTheme(theme);
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(theme, null));
        eventManager.notify(STYLES_MODIFIED_EVENT,
                new EventContext(theme, null));
        log.debug("Theme repaired: " + themeName);
        return true;
    }

    public boolean loadTheme(final String src) {
        try {
            themeManager.loadTheme(src);
        } catch (ThemeIOException e) {
            log.error(e);
            return false;
        }
        eventManager.notify(THEME_MODIFIED_EVENT, new EventContext(null, null));
        eventManager.notify(STYLES_MODIFIED_EVENT, new EventContext(null, null));
        log.debug("Theme loaded: " + src);
        return true;
    }

    public boolean saveTheme(final String src, final int indent) {
        try {
            ThemeManager.saveTheme(src, indent);
        } catch (ThemeIOException e) {
            log.error(e);
            return false;
        }
        log.debug("Theme saved: " + src);
        return true;
    }

    public String renderCssPreview(final String cssPreviewId) {
        Style style = uiManager.getStyleOfSelectedElement();
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }
        if (style == null) {
            return "";
        }
        StringBuilder css = new StringBuilder();
        // TODO use Utils.styleToCss()

        List<Style> styles = new ArrayList<Style>();
        for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
            styles.add(0, (Style) ancestor);
        }
        styles.add(style);

        String currentViewName = uiManager.getCurrentViewName();
        for (Style s : styles) {
            String viewName = currentViewName;
            if (s.getName() != null) {
                viewName = "*";
            }
            for (String path : s.getPathsForView(viewName)) {
                css.append('#').append(cssPreviewId);
                css.append(' ').append(path).append(" {");

                final Properties styleProperties = s.getPropertiesFor(viewName,
                        path);
                final Enumeration<?> propertyNames = Utils.getCssProperties().propertyNames();
                while (propertyNames.hasMoreElements()) {
                    final String propertyName = (String) propertyNames.nextElement();
                    String value = styleProperties.getProperty(propertyName);
                    if (value == null) {
                        continue;
                    }
                    css.append(propertyName);
                    css.append(':');
                    PresetType preset = ThemeManager.resolvePreset(value);
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

    /* Private API */
    private Element getElementById(final String id) {
        return (Element) uidManager.getObjectByUid(Integer.valueOf(id));
    }

    private Format getFormatById(final String id) {
        return (Format) uidManager.getObjectByUid(Integer.valueOf(id));
    }

    @Remove
    public void destroy() {
        log.debug("Removed SEAM component: nxthemesActions");
    }

}
