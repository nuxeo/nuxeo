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

package org.nuxeo.theme.jsf.editor.managers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Stateless;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.jsf.Utils;
import org.nuxeo.theme.jsf.editor.previews.Preview;
import org.nuxeo.theme.jsf.editor.states.UiStatesLocal;
import org.nuxeo.theme.jsf.negotiation.CookieManager;
import org.nuxeo.theme.jsf.negotiation.JSFNegotiator;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.negotiation.NegotiationException;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.properties.FieldInfo;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.views.ViewType;
import org.nuxeo.theme.vocabularies.VocabularyItem;

@Stateless
@Name("nxthemesUiManager")
@Scope(SESSION)
@SerializedConcurrentAccess
public class UiManager implements UiManagerLocal {

    private static final Log log = LogFactory.getLog(UiManager.class);

    private static final String PREVIEW_PROPERTIES_RESOURCE = "/nxthemes/jsf/editor/styles/previews.properties";

    private static final boolean RESOLVE_PRESETS = false;

    private static final boolean IGNORE_VIEW_NAME = true;

    private static final boolean IGNORE_CLASSNAME = true;

    private static final boolean INDENT = true;

    private static final Pattern cssChoicePattern = Pattern.compile("\\[(.*?)\\]");

    private static final Pattern cssCategoryPattern = Pattern.compile("<(.*?)>");

    private final Properties previewProperties;

    @In(value = "nxthemesUiStates", create = true)
    public UiStatesLocal uiStates;

    public UiManager() {
        previewProperties = new Properties();
    }

    public String startEditor() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        final HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        final String referrer = request.getHeader("referer");
        if (referrer == null) {
            log.error("No referrer found, cannot start the theme editor.");
            return null;
        }
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        final String root = externalContext.getRequestContextPath();
        final ApplicationType application = (ApplicationType) Manager.getTypeRegistry().lookup(
                TypeFamily.APPLICATION, root);
        final String strategy = application.getNegotiation().getStrategy();
        final JSFNegotiator negotiator = new JSFNegotiator(strategy,
                facesContext);

        // Store the current theme in a cookie
        String currentTheme = null;
        try {
            currentTheme = negotiator.negotiate("theme");
        } catch (NegotiationException e) {
        }
        if (currentTheme != null) {
            CookieManager.setCookie("nxthemes.theme", currentTheme,
                    externalContext);
        }

        // Switch to the editor
        CookieManager.setCookie("nxthemes.engine", "editor", externalContext);
        try {
            response.sendRedirect(referrer);
        } catch (IOException e) {
            CookieManager.expireCookie("nxthemes.engine", externalContext);
            log.error("Redirection failed while attempting to start the theme editor.");
        }
        return null;
    }

    /* Fragments and views */
    public static class FragmentInfo {

        private final FragmentType fragmentType;

        public FragmentInfo(FragmentType fragmentType) {
            this.fragmentType = fragmentType;
        }

        public FragmentType getFragmentType() {
            return fragmentType;
        }

        private final List<ViewType> viewTypes = new ArrayList<ViewType>();

        public void addView(final ViewType viewType) {
            viewTypes.add(viewType);
        }

        public List<ViewType> getViews() {
            return viewTypes;
        }
    }

    public List<FragmentInfo> getAvailableFragments() {
        final List<FragmentInfo> fragments = new ArrayList<FragmentInfo>();

        final List<Type> fragmentTypes = Manager.getTypeRegistry().getTypes(
                TypeFamily.FRAGMENT);

        for (Type f : fragmentTypes) {
            final FragmentType fragmentType = (FragmentType) f;
            final FragmentInfo fragmentInfo = new FragmentInfo(fragmentType);
            for (ViewType viewType : getViewTypesForFragmentType(fragmentType)) {
                fragmentInfo.addView(viewType);
            }
            fragments.add(fragmentInfo);
        }
        return fragments;
    }

    public final List<ViewType> getViewTypesForFragmentType(
            final FragmentType fragmentType) {
        final List<ViewType> viewTypes = new ArrayList<ViewType>();
        for (Type v : Manager.getTypeRegistry().getTypes(TypeFamily.VIEW)) {
            final ViewType viewType = (ViewType) v;

            // select fragment views
            final ElementType elementType = viewType.getElementType();
            if (elementType != null
                    && !elementType.getTypeName().equals("fragment")) {
                continue;
            }

            // select widget view types
            if (!viewType.getFormatType().getTypeName().equals("widget")) {
                continue;
            }

            // match model types
            final ModelType modelType = viewType.getModelType();
            if (fragmentType.getModelType() == modelType) {
                viewTypes.add(viewType);
            }
        }
        return viewTypes;
    }

    /* Perspectives */
    public List<SelectItem> getAvailablePerspectives() {
        final List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        for (PerspectiveType perspectiveType : PerspectiveManager.listPerspectives()) {
            selectItemList.add(new SelectItem(perspectiveType.name,
                    perspectiveType.title));
        }
        return selectItemList;
    }

    public List<String> getPerspectivesOfSelectedElement() {
        final List<String> perspectives = new ArrayList<String>();
        final Element selectedElement = uiStates.getSelectedElement();
        for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(
                selectedElement)) {
            perspectives.add(perspectiveType.name);
        }
        return perspectives;
    }

    public boolean isSelectedElementAlwaysVisible() {
        final Element selectedElement = uiStates.getSelectedElement();
        return Manager.getPerspectiveManager().isAlwaysVisible(selectedElement);
    }

    /* Themes */
    public List<ThemeDescriptor> getThemesDescriptors() {
        final List<ThemeDescriptor> themeDescriptors = new ArrayList<ThemeDescriptor>();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        final Set<String> themeNames = Manager.getThemeManager().getThemeNames();
        for (Type type : typeRegistry.getTypes(TypeFamily.THEME)) {
            if (type != null) {
                ThemeDescriptor themeDescriptor = (ThemeDescriptor) type;
                themeDescriptors.add(themeDescriptor);
                themeNames.remove(themeDescriptor.getName());
            }
        }

        /* Create temporary theme descriptors for unregistered themes */
        for (String themeName : themeNames) {
            ThemeDescriptor themeDescriptor = new ThemeDescriptor();
            themeDescriptor.setName(themeName);
            themeDescriptors.add(themeDescriptor);
        }
        return themeDescriptors;
    }

    public static class ThemeInfo {

        final String name;

        final String link;

        final String className;

        public ThemeInfo(String name, String link, String className) {
            this.name = name;
            this.link = link;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getLink() {
            return link;
        }

        public String getClassName() {
            return className;
        }
    }

    public static class PageInfo {

        final String name;

        final String link;

        final String className;

        public PageInfo(String name, String link, String className) {
            this.name = name;
            this.link = link;
            this.className = className;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }

        public String getLink() {
            return link;
        }
    }

    public List<ThemeInfo> getAvailableThemes() {
        final List<ThemeInfo> themes = new ArrayList<ThemeInfo>();

        final String defaultThemeName = getDefaultTheme();
        final String defaultPageName = defaultThemeName.split("/")[1];

        final ThemeManager themeManager = Manager.getThemeManager();
        final ThemeElement currentTheme = uiStates.getCurrentTheme();
        final String currentThemeName = currentTheme == null ? null
                : currentTheme.getName();
        for (String themeName : themeManager.getThemeNames()) {
            final String link = String.format("%s/%s", themeName,
                    defaultPageName);
            String className = themeName.equals(currentThemeName) ? "selected"
                    : "";
            if (link.equals(defaultThemeName)) {
                className += " default";
            }
            themes.add(new ThemeInfo(themeName, link, className));
        }
        return themes;
    }

    public List<PageInfo> getAvailablePages() {
        final String defaultThemeName = getDefaultTheme();
        final String defaultPageName = defaultThemeName.split("/")[1];

        final List<PageInfo> availablePages = new ArrayList<PageInfo>();
        final ThemeElement currentTheme = uiStates.getCurrentTheme();
        final PageElement currentPage = uiStates.getCurrentPage();
        final String currentPageName = currentPage == null ? null
                : currentPage.getName();
        if (currentTheme != null) {
            final String currentThemeName = currentTheme.getName();
            for (PageElement page : ThemeManager.getPagesOf(currentTheme)) {
                final String pageName = page.getName();
                final String link = String.format("%s/%s", currentThemeName,
                        pageName);
                String className = pageName.equals(currentPageName) ? "selected"
                        : "";
                if (defaultPageName.equals(pageName)) {
                    className += " default";
                }
                availablePages.add(new PageInfo(pageName, link, className));
            }
        }
        return availablePages;
    }

    /* Elements */
    public static class FieldProperty {
        private final String name;

        private final String value;

        private final FieldInfo info;

        public FieldProperty(String name, String value, FieldInfo info) {
            this.name = name;
            this.value = value;
            this.info = info;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getRendered() {
            final StringBuilder rendered = new StringBuilder();

            // label
            final String label = info == null ? name : info.label();
            rendered.append("<label>").append(label);

            // description
            final String description = info == null ? "" : info.description();
            if (!"".equals(description)) {
                rendered.append(String.format(
                        "<span class=\"description\">%s</span>", description));
            }
            rendered.append("</label>");

            // widget
            final String type = info == null ? "" : info.type();
            if ("text area".equals(type)) {
                rendered.append(String.format(
                        "<textarea name=\"%s\" class=\"fieldInput\">%s</textarea>",
                        name, value));

            } else if ("string".equals(type)) {
                rendered.append(String.format(
                        "<input type=\"text\" class=\"textInput fieldInput\" name=\"%s\" value=\"%s\" />",
                        name, value));

            } else if ("selection".equals(type)) {
                String source = info.source();
                if (!source.equals("")) {
                    List<VocabularyItem> items = Manager.getVocabularyManager().getItems(
                            source);
                    if (items != null) {
                        rendered.append(String.format(
                                "<select class=\"fieldInput\" name=\"%s\">",
                                name));
                        boolean found = false;
                        for (VocabularyItem item : items) {
                            final String itemValue = item.getValue();
                            if (itemValue.equals(value)) {
                                rendered.append(String.format(
                                        "<option selected=\"selected\" value=\"%s\">%s</option>",
                                        itemValue, item.getLabel()));
                                found = true;
                            } else {
                                rendered.append(String.format(
                                        "<option value=\"%s\">%s</option>",
                                        itemValue, item.getLabel()));
                            }
                        }
                        if (!found) {
                            rendered.append(String.format(
                                    "<option>Invalid option: %s</option>",
                                    value));
                        }
                        rendered.append("</select>");
                    }
                }
            }

            if (info.required() && value.equals("")) {
                rendered.append("<span style=\"color: red\"> * </span>");
            }

            return rendered.toString();
        }
    }

    public List<FieldProperty> getElementProperties() {
        final Element selectedElement = uiStates.getSelectedElement();
        final List<FieldProperty> fieldProperties = new ArrayList<FieldProperty>();
        if (selectedElement == null) {
            return fieldProperties;
        }

        Properties properties;
        try {
            properties = FieldIO.dumpFieldsToProperties(selectedElement);
        } catch (Exception e) {
            return fieldProperties;
        }

        if (properties == null) {
            return fieldProperties;
        }

        final Class<?> c = selectedElement.getClass();
        final Enumeration<?> names = properties.propertyNames();
        while (names.hasMoreElements()) {
            final String name = (String) names.nextElement();
            final String value = properties.getProperty(name);
            fieldProperties.add(new FieldProperty(name, value.trim(),
                    getFieldInfo(c, name)));
        }
        return fieldProperties;
    }

    public class StyleFieldProperty {

        private final String name;

        private final String value;

        private final String type;

        public StyleFieldProperty(String name, String value, String type) {
            this.name = name;
            // escape quotes (used internally to represent presets)
            this.value = value.replace("\"", "&quot;");
            this.type = type;
        }

        public String getName() {
            return String.format("&quot;%s&quot;", name);
        }

        public String getValue() {
            return value;
        }

        public String getRendered() {
            final StringBuilder rendered = new StringBuilder();
            final String label = name;
            rendered.append("<label>").append(label).append("</label>");

            final Matcher choiceMatcher = cssChoicePattern.matcher(type);
            final Matcher categoryMatcher = cssCategoryPattern.matcher(type);

            final boolean hasChoices = choiceMatcher.find();
            final boolean hasCategory = categoryMatcher.find();

            if (hasChoices) {
                // render selection list
                String choices = choiceMatcher.group(1);
                rendered.append(String.format("<select name=\"property:%s\">",
                        name));
                rendered.append("<option></option>");
                for (String choice : choices.split("\\|")) {
                    rendered.append(String.format("<option%s>%s</option>",
                            choice.equals(value) ? " selected=\"selected\""
                                    : "", choice));
                }
                rendered.append("</select>");
            } else {
                // render input area
                String input = String.format(
                        "<input type=\"text\" class=\"textInput\" name=\"property:%s\" value=\"%s\" />",
                        name, value);
                rendered.append(input);
            }

            if (hasCategory) {
                String category = categoryMatcher.group(1);
                // add a style picker
                rendered.append(String.format(
                        "<input type=\"button\" class=\"picker\" property=\"%s\" category=\"%s\" value=\"\" />",
                        name, category));
            }

            return rendered.toString();
        }
    }

    public class PresetInfo {

        final PresetType preset;

        public PresetInfo(PresetType preset) {
            this.preset = preset;
        }

        public String getName() {
            return preset.getTypeName();
        }

        public String getPreview() {
            final String category = preset.getCategory();
            final String previewClassName = getPreviewProperties().getProperty(
                    category);
            if (previewClassName == null) {
                return "";
            }
            Preview preview = null;
            try {
                preview = (Preview) Class.forName(previewClassName).newInstance();
            } catch (Exception e) {
                log.error("Could not get preview", e);
            }
            if (preview == null) {
                return "";
            }
            String content = preset.getName();
            return preview.render(preset.getValue(), content);
        }
    }

    public List<SelectItem> getAvailablePresetGroupsForSelectedCategory() {
        final List<SelectItem> groups = new ArrayList<SelectItem>();
        final String category = uiStates.getStyleCategory();
        groups.add(new SelectItem("", ""));
        if (category == null) {
            return groups;
        }
        final Set<String> groupNames = new HashSet<String>();
        for (Type type : Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
            final PresetType preset = (PresetType) type;
            final String group = preset.getGroup();
            if (!preset.getCategory().equals(category)) {
                continue;
            }
            if (!groupNames.contains(group)) {
                groups.add(new SelectItem(group, group));
            }
            groupNames.add(group);
        }
        return groups;
    }

    public List<PresetInfo> getPresetsForCurrentGroup() {
        final String category = uiStates.getStyleCategory();
        final String group = uiStates.getPresetGroup();
        final List<PresetInfo> presets = new ArrayList<PresetInfo>();
        for (Type type : Manager.getTypeRegistry().getTypes(TypeFamily.PRESET)) {
            PresetType preset = (PresetType) type;
            if (!preset.getCategory().equals(category)) {
                continue;
            }
            if (!preset.getGroup().equals(group)) {
                continue;
            }
            presets.add(new PresetInfo(preset));
        }
        return presets;
    }

    public List<StyleFieldProperty> getElementStyleProperties() {
        Style style = getStyleOfSelectedElement();
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }

        final List<StyleFieldProperty> fieldProperties = new ArrayList<StyleFieldProperty>();
        if (style == null) {
            return fieldProperties;
        }
        final String path = uiStates.getCurrentStyleSelector();
        if (path == null) {
            return fieldProperties;
        }

        String viewName = getCurrentViewName();
        if (style.getName() != null) {
            viewName = "*";
        }
        final Properties properties = style.getPropertiesFor(viewName, path);
        final String selectedCategory = uiStates.getStylePropertyCategory();

        final Properties cssProperties = Utils.getCssProperties();
        final Enumeration<?> propertyNames = cssProperties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            final String name = (String) propertyNames.nextElement();
            final String value = properties == null ? ""
                    : properties.getProperty(name, "");
            final String type = cssProperties.getProperty(name);

            if (!selectedCategory.equals("")) {
                final Matcher categoryMatcher = cssCategoryPattern.matcher(type);
                if (!categoryMatcher.find()) {
                    continue;
                }
                if (!categoryMatcher.group(1).equals(selectedCategory)) {
                    continue;
                }
            }

            fieldProperties.add(new StyleFieldProperty(name, value, type));
        }
        return fieldProperties;
    }

    public String getRenderedElementStyleProperties() {
        Style style = getStyleOfSelectedElement();
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }
        if (style == null) {
            return "";
        }
        final List<String> viewNames = new ArrayList<String>();
        String viewName = getCurrentViewName();
        if (style.getName() != null) {
            viewName = "*";
        }
        viewNames.add(viewName);
        return Utils.styleToCss(style, viewNames, RESOLVE_PRESETS,
                IGNORE_VIEW_NAME, IGNORE_CLASSNAME, INDENT);
    }

    private FieldInfo getFieldInfo(final Class<?> c,
            final String name) {
        try {
            return c.getField(name).getAnnotation(FieldInfo.class);
        } catch (Exception e) {
            log.error("Could not get field information", e);
        }
        return null;
    }

    public List<SelectItem> getAvailableViewNamesForSelectedElement() {
        final List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        final Element selectedElement = uiStates.getSelectedElement();
        if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
            return selectItemList;
        }
        FragmentType fragmentType = ((Fragment) selectedElement).getFragmentType();
        for (ViewType viewType : getViewTypesForFragmentType(fragmentType)) {
            selectItemList.add(new SelectItem(viewType.getViewName(),
                    viewType.getViewName()));
        }
        return selectItemList;
    }

    public Style getStyleOfSelectedElement() {
        final Element selectedElement = uiStates.getSelectedElement();
        final FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        return (Style) ElementFormatter.getFormatByType(selectedElement,
                styleType);
    }

    public List<SelectItem> getAvailableStyleSelectorsForSelectedElement() {
        final List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        Style style = getStyleOfSelectedElement();
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        if (currentStyleLayer != null) {
            style = currentStyleLayer;
        }

        String viewName = getCurrentViewName();
        if (style != null) {
            // named styles are not associated to any view
            if (style.getName() != null) {
                viewName = "*";
            }
            Set<String> paths = style.getPathsForView(viewName);
            String current = uiStates.getCurrentStyleSelector();
            if (current != null && !paths.contains(current)) {
                selectItemList.add(new SelectItem(current, current));
            }
            for (String path : paths) {
                selectItemList.add(new SelectItem(path, path));
            }
        }
        return selectItemList;
    }

    public List<SelectItem> getAvailableStyleProperties() {
        final List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        final Properties properties = Utils.getCssProperties();
        if (properties.isEmpty()) {
            return selectItemList;
        }

        Set<Object> sortedKeys = new TreeSet<Object>();
        sortedKeys.addAll(properties.keySet());
        for (Object key : sortedKeys) {
            String name = (String) key;
            selectItemList.add(new SelectItem(name, name));
        }
        return selectItemList;
    }

    /* Load properties */
    private Properties getPreviewProperties() {
        Utils.loadProperties(previewProperties, PREVIEW_PROPERTIES_RESOURCE);
        return previewProperties;
    }

    public class StyleCategory {

        private final String category;

        private final String title;

        private final boolean selected;

        public StyleCategory(final String category, final String title,
                final boolean selected) {
            this.category = category;
            this.title = title;
            this.selected = selected;
        }

        public String getCategory() {
            return category;
        }

        public String getRendered() {
            final String className = selected ? "selected" : "";
            return String.format(
                    "<a href=\"javascript:void(0)\" class=\"%s\" onclick=\"NXThemesStyleEditor.setStylePropertyCategory('%s')\">%s</a>\n",
                    className, category, title);
        }
    }

    public List<StyleCategory> getStyleCategories() {
        final Map<String, StyleCategory> categories = new LinkedHashMap<String, StyleCategory>();
        final Properties cssProperties = Utils.getCssProperties();
        final Enumeration<?> elements = cssProperties.elements();
        final String selectedStyleCategory = uiStates.getStylePropertyCategory();
        categories.put("", new StyleCategory("", "all",
                selectedStyleCategory.equals("")));
        while (elements.hasMoreElements()) {
            final String element = (String) elements.nextElement();
            final Matcher categoryMatcher = cssCategoryPattern.matcher(element);
            if (categoryMatcher.find()) {
                final String value = categoryMatcher.group(1);
                boolean selected = value.equals(selectedStyleCategory);
                categories.put(value, new StyleCategory(value, value, selected));
            }
        }
        return new ArrayList<StyleCategory>(categories.values());
    }

    public Widget getWidgetOfSelectedElement() {
        final Element selectedElement = uiStates.getSelectedElement();
        final FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "widget");
        return (Widget) ElementFormatter.getFormatByType(selectedElement,
                widgetType);
    }

    public class StyleLayer {

        private final String name;

        private final Integer uid;

        private final boolean selected;

        public StyleLayer(final String name, final Integer uid,
                final boolean selected) {
            this.name = name;
            this.uid = uid;
            this.selected = selected;
        }

        public String getRendered() {
            final String className = selected ? "selected" : "";
            return String.format(
                    "<a href=\"javascript:void(0)\" class=\"%s\" onclick=\"NXThemesStyleEditor.setCurrentStyleLayer(%s)\" >%s</a>",
                    className, uid, name);
        }
    }

    public List<StyleLayer> getStyleLayersOfSelectedElement() {
        Style style = getStyleOfSelectedElement();
        final Style currentStyleLayer = uiStates.getCurrentStyleLayer();
        final List<StyleLayer> layers = new ArrayList<StyleLayer>();
        layers.add(new StyleLayer("This style", style.getUid(),
                style == currentStyleLayer || currentStyleLayer == null));
        for (Format ancestor : ThemeManager.listAncestorFormatsOf(style)) {
            layers.add(1, new StyleLayer(ancestor.getName(), ancestor.getUid(),
                    ancestor == currentStyleLayer));
        }
        return layers;
    }

    public String getCurrentViewName() {
        return getWidgetOfSelectedElement().getName();
    }

    /* Layout */
    public class PaddingInfo {

        private final String top;

        private final String bottom;

        private final String left;

        private final String right;

        public PaddingInfo(final String top, final String bottom,
                final String left, final String right) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }

        public String getBottom() {
            return bottom;
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }

        public String getTop() {
            return top;
        }
    }

    public PaddingInfo getPaddingOfSelectedElement() {
        Element element = uiStates.getSelectedElement();
        String top = "";
        String bottom = "";
        String left = "";
        String right = "";
        if (element != null) {
            Layout layout = (Layout) ElementFormatter.getFormatFor(element, "layout");
            top = layout.getProperty("padding-top");
            bottom = layout.getProperty("padding-bottom");
            left = layout.getProperty("padding-left");
            right = layout.getProperty("padding-right");
        }
        return new PaddingInfo(top, bottom, left, right);
    }

    /* Private API */
    private String getDefaultTheme() {
        String defaultTheme = "";
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        final String applicationPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
        final ApplicationType application = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, applicationPath);

        if (application != null) {
            NegotiationDef negotiation = application.getNegotiation();
            if (negotiation != null) {
                defaultTheme = negotiation.getDefaultTheme();
            }
        }
        return defaultTheme;
    }
}
