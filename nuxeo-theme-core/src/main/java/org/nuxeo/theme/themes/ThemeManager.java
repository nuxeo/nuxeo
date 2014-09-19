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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.CustomThemeNameFilter;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.formats.layouts.Layout;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.models.Info;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.nodes.NodeException;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.relations.DefaultPredicate;
import org.nuxeo.theme.relations.DyadicRelation;
import org.nuxeo.theme.relations.Predicate;
import org.nuxeo.theme.relations.Relation;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.resources.ResourceType;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.Identifiable;
import org.nuxeo.theme.uids.UidManager;
import org.nuxeo.theme.views.ViewType;

public final class ThemeManager implements Registrable {

    public static final String THEME_TOPIC = "org.nuxeo.theme";

    public static final String THEME_REGISTERED_EVENT_ID = "themeRegistered";

    private static final Log log = LogFactory.getLog(ThemeManager.class);

    private final Map<String, Long> lastModified = new HashMap<String, Long>();

    private final Map<String, ThemeElement> themes = new HashMap<String, ThemeElement>();

    private final Map<String, PageElement> pages = new HashMap<String, PageElement>();

    private final Map<String, List<Integer>> formatsByTypeName = new LinkedHashMap<String, List<Integer>>();

    private final Map<String, ModelType> modelsByClassname = new HashMap<String, ModelType>();

    private final Map<String, Map<String, Integer>> namedObjectsByTheme = new HashMap<String, Map<String, Integer>>();

    private final Map<Integer, String> themeOfNamedObjects = new HashMap<Integer, String>();

    private final Map<String, Info> infoMap = new HashMap<String, Info>();

    private static final Predicate PREDICATE_FORMAT_INHERIT = new DefaultPredicate(
            "_ inherits from _");

    private final Map<String, String> cachedStyles = new HashMap<String, String>();

    private final Map<String, String> cachedResources = new HashMap<String, String>();

    private final Map<String, byte[]> cachedBinaries = new HashMap<String, byte[]>();

    private final List<String> resourceOrdering = new ArrayList<String>();

    private static File THEME_DIR;

    private static final FilenameFilter CUSTOM_THEME_FILENAME_FILTER = new CustomThemeNameFilter();

    private static final int DEFAULT_THEME_INDENT = 2;

    private static final String COLLECTION_CSS_MARKER = "COLLECTION";

    private static final Pattern styleResourceNamePattern = Pattern.compile(
            "(.*?)\\s\\((.*?)\\)$", Pattern.DOTALL);

    public static void createThemeDir() {
        THEME_DIR = new File(Environment.getDefault().getData(), "themes");
        THEME_DIR.mkdirs();
    }

    public static File getThemeDir() {
        if (THEME_DIR == null || !THEME_DIR.exists()) {
            createThemeDir();
        }
        return THEME_DIR;
    }

    @Override
    public synchronized void clear() {
        themes.clear();
        pages.clear();
        formatsByTypeName.clear();
        modelsByClassname.clear();
        namedObjectsByTheme.clear();
        themeOfNamedObjects.clear();
        infoMap.clear();
        cachedStyles.clear();
        cachedResources.clear();
        cachedBinaries.clear();
        resourceOrdering.clear();
        lastModified.clear();
    }

    public Map<String, Info> getGlobalInfoMap() {
        return infoMap;
    }

    public static boolean validateThemeName(String themeName) {
        return (themeName.matches("^([a-zA-Z]|[a-zA-Z][a-zA-Z0-9_\\-]*?[a-zA-Z0-9])$"));
    }

    public static String getCustomThemePath(String themeName)
            throws ThemeIOException {
        String themeFileName = String.format("theme-%s.xml", themeName);
        File file = new File(getThemeDir(), themeFileName);
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new ThemeIOException("Could not get custom theme path: "
                    + themeName, e);
        }
    }

    public static List<File> getCustomThemeFiles() {
        List<File> files = new ArrayList<File>();
        for (File f : getThemeDir().listFiles(CUSTOM_THEME_FILENAME_FILTER)) {
            files.add(f);
        }
        return files;
    }

    public static ThemeDescriptor customizeTheme(ThemeDescriptor themeDescriptor)
            throws ThemeException {
        String themeName = themeDescriptor.getName();
        if (!themeDescriptor.isCustomizable()) {
            throw new ThemeException("Theme : " + themeName
                    + " cannot be customized.");
        }

        ThemeSerializer serializer = new ThemeSerializer();
        String xmlSource;
        try {
            xmlSource = serializer.serializeToXml(themeDescriptor.getSrc(), 0);
        } catch (ThemeIOException e) {
            throw new ThemeException("Could not serialize theme: " + themeName,
                    e);
        }
        ThemeDescriptor newThemeDescriptor = createCustomTheme(themeName);
        String newSrc = newThemeDescriptor.getSrc();
        try {
            Manager.getThemeManager().loadTheme(newSrc, xmlSource);
        } catch (ThemeIOException e) {
            throw new ThemeException("Could not update theme: " + newSrc, e);
        }
        try {
            saveTheme(newSrc);
        } catch (ThemeIOException e) {
            throw new ThemeException("Could not save theme: " + newSrc, e);
        }

        newThemeDescriptor.setCustomization(true);
        return newThemeDescriptor;
    }

    public static ThemeDescriptor uncustomizeTheme(
            ThemeDescriptor themeDescriptor) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        String themeName = themeDescriptor.getName();

        if (!themeDescriptor.isCustomization()) {
            throw new ThemeException("Theme : " + themeName
                    + " cannot be uncustomized.");
        }

        String themeSrc = themeDescriptor.getSrc();
        try {
            themeManager.deleteTheme(themeSrc);
        } catch (ThemeIOException e) {
            throw new ThemeException("Could not remove theme: " + themeSrc, e);
        }

        ThemeDescriptor newThemeDescriptor = getThemeDescriptorByThemeName(themeName);
        loadTheme(newThemeDescriptor);
        return newThemeDescriptor;
    }

    public static ThemeDescriptor createCustomTheme(String name)
            throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
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
        try {
            theme.addChild(page);
        } catch (NodeException e) {
            throw new ThemeException(e.getMessage(), e);
        }
        // create a theme descriptor
        ThemeDescriptor themeDescriptor = new ThemeDescriptor();
        themeDescriptor.setName(name);
        String path;
        try {
            path = ThemeManager.getCustomThemePath(name);
        } catch (ThemeIOException e) {
            throw new ThemeException("Could not get file path for theme: "
                    + name);
        }
        final String src = String.format("file:///%s", path);
        themeDescriptor.setSrc(src);
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        typeRegistry.register(themeDescriptor);
        // register the theme
        themeManager.registerTheme(theme);
        // save the theme
        try {
            ThemeManager.saveTheme(themeDescriptor.getSrc());
        } catch (ThemeIOException e) {
            throw new ThemeException("Could not save theme: " + name, e);
        }
        return themeDescriptor;
    }

    public static void updateThemeDescriptors() {
        Map<String, List<ThemeDescriptor>> names = new HashMap<String, List<ThemeDescriptor>>();
        for (ThemeDescriptor themeDescriptor : getThemeDescriptors()) {
            String themeName = themeDescriptor.getName();
            if (!names.containsKey(themeName)) {
                names.put(themeName, new ArrayList<ThemeDescriptor>());
            }
            names.get(themeName).add(themeDescriptor);
        }
        for (List<ThemeDescriptor> themeDescriptors : names.values()) {
            for (ThemeDescriptor themeDescriptor : themeDescriptors) {
                themeDescriptor.setCustomized(true);
                themeDescriptor.setCustomization(false);
            }
            int size = themeDescriptors.size();
            themeDescriptors.get(size - 1).setCustomized(false);
            if (size > 1) {
                themeDescriptors.get(size - 1).setCustomization(true);
            }
        }
    }

    public static String getDefaultTheme(final String applicationPath) {
        return getDefaultTheme(applicationPath, null);
    }

    public static String getDefaultTheme(final String... paths) {
        String defaultTheme = "";
        ApplicationType application = null;
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        application = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, paths);
        if (application != null) {
            NegotiationDef negotiation = application.getNegotiation();
            if (negotiation != null) {
                defaultTheme = negotiation.getDefaultTheme();
            }
        }
        return defaultTheme;
    }

    public static Set<String> getThemeNames(final String templateEngine) {
        Set<String> names = new HashSet<String>();
        for (ThemeDescriptor themeDef : getThemeDescriptors()) {
            // Skip customized themes
            if (themeDef.isCustomized()) {
                continue;
            }
            if (templateEngine != null
                    && !themeDef.isCompatibleWith(templateEngine)) {
                continue;
            }
            names.add(themeDef.getName());
        }
        return names;
    }

    public static ThemeDescriptor getThemeDescriptorByThemeName(
            final String templateEngine, final String themeName) {
        for (ThemeDescriptor themeDef : getThemeDescriptors()) {
            // Skip customized themes
            if (themeDef.isCustomized()) {
                continue;
            }
            if (templateEngine != null
                    && !themeDef.isCompatibleWith(templateEngine)) {
                continue;
            }
            final String name = themeDef.getName();
            if (name != null && name.equals(themeName)) {
                return themeDef;
            }
        }
        return null;
    }

    public static ThemeDescriptor getThemeDescriptorByThemeName(
            final String themeName) {
        return getThemeDescriptorByThemeName(null, themeName);
    }

    public static Set<String> getThemeNames() {
        return getThemeNames(null);
    }

    public Set<String> getPageNames(final String themeName) {
        final ThemeElement theme = getThemeByName(themeName);
        final Set<String> pageNames = new LinkedHashSet<String>();
        if (theme != null) {
            for (PageElement page : getPagesOf(theme)) {
                pageNames.add(page.getName());
            }
        }
        return pageNames;
    }

    public static List<PageElement> getPagesOf(final ThemeElement theme) {
        final List<PageElement> themePages = new ArrayList<PageElement>();
        for (Node node : theme.getChildren()) {
            final PageElement page = (PageElement) node;
            themePages.add(page);
        }
        return themePages;
    }

    public List<PageElement> getPagesOf(final String themeName) {
        final ThemeElement theme = getThemeByName(themeName);
        if (theme == null) {
            return null;
        }
        return getPagesOf(theme);
    }

    public static ThemeElement getThemeOf(final Element element) {
        ThemeElement theme = null;
        Element current = element;
        while (current != null) {
            if (current instanceof ThemeElement) {
                theme = (ThemeElement) current;
                break;
            }
            current = (Element) current.getParent();
        }
        return theme;
    }

    public static boolean belongToSameTheme(final Element element1,
            final Element element2) {
        return getThemeOf(element1) == getThemeOf(element2);
    }

    // Object lookups by URL
    public static EngineType getEngineByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 1) {
            return null;
        }
        final String engineName = path[1];
        return (EngineType) Manager.getTypeRegistry().lookup(TypeFamily.ENGINE,
                engineName);
    }

    public static String getViewModeByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 2) {
            return null;
        }
        return path[2];
    }

    public static TemplateEngineType getTemplateEngineByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 3) {
            return null;
        }
        final String templateEngineName = path[3];
        return (TemplateEngineType) Manager.getTypeRegistry().lookup(
                TypeFamily.TEMPLATE_ENGINE, templateEngineName);
    }

    public ThemeElement getThemeBySrc(final String src) throws ThemeException {
        ThemeDescriptor themeDef = getThemeDescriptor(src);
        if (themeDef.isCustomized()) {
            throw new ThemeException("Cannot access customized theme: " + src);
        }
        String themeName = themeDef.getName();
        return getThemeByName(themeName);
    }

    public ThemeElement getThemeByUrl(final URL url) {
        String themeName = getThemeNameByUrl(url);
        if (themeName == null) {
            return null;
        }
        return getThemeByName(themeName);
    }

    public static String getThemeNameByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        if (!url.getHost().equals("theme")) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 4) {
            return null;
        }
        return path[4];
    }

    public static String getPagePathByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        if (!url.getHost().equals("theme")) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 5) {
            return null;
        }
        final String pagePath = path[4] + '/' + path[5];
        return pagePath;
    }

    public PageElement getThemePageByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        if (!url.getHost().equals("theme")) {
            return null;
        }
        final String pagePath = getPagePathByUrl(url);
        return getPageByPath(pagePath);
    }

    public PageElement getPageByPath(final String path) {
        return pages.get(path);
    }

    public static String getPageNameFromPagePath(final String path) {
        if (path.contains("/")) {
            return path.split("/")[1];
        }
        return null;
    }

    public ThemeElement getThemeByName(final String name) {
        return themes.get(name);
    }

    public void fillScratchPage(final String themeName, final Element element)
            throws NodeException, ThemeException {
        String pagePath = String.format("%s/~", themeName);

        PageElement scratchPage = getPageByPath(pagePath);
        if (scratchPage != null) {
            destroyDescendants(scratchPage);
            removeRelationsOf(scratchPage);
            pages.remove(pagePath);
            removeOrphanedFormats();
        }

        // create a new scratch page
        scratchPage = (PageElement) ElementFactory.create("page");
        Widget pageWidget = (Widget) FormatFactory.create("widget");
        pageWidget.setName("page frame");
        registerFormat(pageWidget);
        ElementFormatter.setFormat(scratchPage, pageWidget);

        UidManager uidManager = Manager.getUidManager();
        uidManager.register(scratchPage);
        pages.put(pagePath, scratchPage);

        scratchPage.addChild(element);
    }

    public static Element getElementByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        if (!url.getHost().equals("element")) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length < 1) {
            return null;
        }
        final String uid = path[path.length - 1];
        return (Element) Manager.getUidManager().getObjectByUid(
                Integer.valueOf(uid));
    }

    public static PerspectiveType getPerspectiveByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        if (!url.getHost().equals("theme")) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 6) {
            return null;
        }
        final String perspectiveName = path[6];
        return (PerspectiveType) Manager.getTypeRegistry().lookup(
                TypeFamily.PERSPECTIVE, perspectiveName);
    }

    public static String getCollectionNameByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        if (!url.getHost().equals("theme")) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 7) {
            return null;
        }
        final String collectionName = path[7];
        // TODO: check to see if the collection exists?
        return collectionName;
    }

    public static String getUrlDescription(URL url) {
        final String[] path = url.getPath().split("/");
        String host = url.getHost();
        String description = "[???]";
        if ("theme".equals(host)) {
            description = String.format(
                    "[THEME %s, PAGE %s, ENGINE %s, TEMPLATE %s, PERSPECTIVE %s, MODE %s]",
                    path[4], path[5], path[1], path[3], path[6], path[2]);
        } else if ("element".equals(host)) {
            description = String.format(
                    "[ELEMENT %s, ENGINE %s, TEMPLATE %s, MODE %s]", path[4],
                    path[1], path[3], path[2]);
        }
        return description;
    }

    // Named objects
    public Identifiable getNamedObject(final String themeName,
            final String realm, final String name) {
        final Map<String, Integer> objectsInTheme = namedObjectsByTheme.get(themeName);
        if (objectsInTheme == null) {
            return null;
        }
        final Integer uid = objectsInTheme.get(String.format("%s/%s", realm,
                name));

        if (uid != null) {
            return (Identifiable) Manager.getUidManager().getObjectByUid(uid);
        }
        return null;
    }

    public String getThemeNameOfNamedObject(Identifiable object) {
        return themeOfNamedObjects.get(object.getUid());
    }

    public void setNamedObject(final String themeName, final String realm,
            final Identifiable object) throws ThemeException {
        if (!namedObjectsByTheme.containsKey(themeName)) {
            namedObjectsByTheme.put(themeName,
                    new LinkedHashMap<String, Integer>());
        }
        final Integer uid = object.getUid();
        final String name = object.getName();
        if (name == null) {
            throw new ThemeException("Cannot register unnamed object, uid: "
                    + uid);
        }
        namedObjectsByTheme.get(themeName).put(
                String.format("%s/%s", realm, name), uid);
        themeOfNamedObjects.put(uid, themeName);
    }

    public List<Identifiable> getNamedObjects(final String themeName,
            final String realm) {
        final List<Identifiable> objects = new ArrayList<Identifiable>();
        final Map<String, Integer> objectsInTheme = namedObjectsByTheme.get(themeName);
        final String prefix = String.format("%s/", realm);
        final UidManager uidManager = Manager.getUidManager();
        if (objectsInTheme != null) {
            for (Map.Entry<String, Integer> entry : objectsInTheme.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    final Identifiable object = (Identifiable) uidManager.getObjectByUid(entry.getValue());
                    objects.add(object);
                }
            }
        }
        return objects;
    }

    public void removeNamedObject(final String themeName, final String realm,
            final String name) {
        final String key = String.format("%s/%s", realm, name);
        Identifiable object = getNamedObject(themeName, realm, name);
        themeOfNamedObjects.remove(object.getUid());
        namedObjectsByTheme.get(themeName).remove(key);
    }

    public void removeNamedObjects(final String themeName) {
        namedObjectsByTheme.remove(themeName);
        List<Integer> toDelete = new ArrayList<Integer>();
        for (Map.Entry<Integer, String> entry : themeOfNamedObjects.entrySet()) {
            if (entry.getValue().equals(themeName)) {
                toDelete.add(entry.getKey());
            }
        }
        for (Integer key : toDelete) {
            themeOfNamedObjects.remove(key);
        }
        toDelete = null;
    }

    public void makeElementUseNamedStyle(final Element element,
            final String inheritedName, final String themeName)
            throws ThemeException {
        final FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        Style style = (Style) ElementFormatter.getFormatByType(element,
                styleType);

        if (style == null) {
            throw new ThemeException("Element has no assigned style: "
                    + element.computeXPath());
        }
        // Make the style no longer inherits from other another style if
        // 'inheritedName' is null
        if (inheritedName == null) {
            ThemeManager.removeInheritanceTowards(style);
        } else {
            Style inheritedStyle = (Style) getNamedObject(themeName, "style",
                    inheritedName);
            if (inheritedStyle == null) {
                throw new ThemeException("Could not find named style: "
                        + inheritedName);
            }
            makeFormatInherit(style, inheritedStyle);
        }
    }

    public static void setStyleInheritance(String styleName,
            String ancestorStyleName, String themeName, boolean allowMany)
            throws ThemeException {

        ThemeManager themeManager = Manager.getThemeManager();
        ThemeDescriptor themeDescriptor = ThemeManager.getThemeDescriptorByThemeName(themeName);
        if (themeDescriptor == null) {
            throw new ThemeException("Theme not found: " + themeName);
        }
        Style style = (Style) themeManager.getNamedObject(themeName, "style",
                styleName);
        if (style == null) {
            throw new ThemeException("Could not find named style: " + styleName);
        }

        Style ancestorStyle = (Style) themeManager.getNamedObject(themeName,
                "style", ancestorStyleName);
        if (ancestorStyle == null) {
            throw new ThemeException("Could not find named style: "
                    + ancestorStyleName);
        }
        if (!allowMany) {
            ThemeManager.removeInheritanceFrom(ancestorStyle);
        }
        themeManager.makeFormatInherit(style, ancestorStyle);
    }

    public static void loadRemoteStyle(String resourceBankName, Style style)
            throws ThemeException {
        if (!style.isNamed()) {
            throw new ThemeException(
                    "Only named styles can be loaded from resource banks.");
        }
        String styleName = style.getName();
        final Matcher resourceNameMatcher = styleResourceNamePattern.matcher(styleName);
        if (resourceNameMatcher.find()) {
            String collectionName = resourceNameMatcher.group(2);
            String resourceId = resourceNameMatcher.group(1) + ".css";
            String cssSource = ResourceManager.getBankResource(
                    resourceBankName, collectionName, "style", resourceId);
            style.setCollection(collectionName);
            Utils.loadCss(style, cssSource, "*");
        } else {
            throw new ThemeException("Incorrect remote style name: "
                    + styleName);
        }
    }

    // Element actions
    public Element duplicateElement(final Element element,
            final boolean duplicateFormats) throws ThemeException {
        Element duplicate;
        final String typeName = element.getElementType().getTypeName();

        if (element instanceof Fragment) {
            final FragmentType fragmentType = ((Fragment) element).getFragmentType();
            duplicate = FragmentFactory.create(fragmentType.getTypeName());
        } else {
            duplicate = ElementFactory.create(typeName);
        }

        if (duplicate == null) {
            log.warn("Could not duplicate: " + element);
        } else {
            // duplicate the fields
            try {
                FieldIO.updateFieldsFromProperties(duplicate,
                        FieldIO.dumpFieldsToProperties(element));
            } catch (Exception e) {
                log.warn("Could not copy the fields of: " + element);
                log.debug(e.getMessage(), e);
            }

            // duplicate formats or create a relation
            for (Format format : ElementFormatter.getFormatsFor(element)) {
                if (duplicateFormats) {
                    format = duplicateFormat(format);
                }
                ElementFormatter.setFormat(duplicate, format);
            }

            // duplicate description
            duplicate.setDescription(element.getDescription());

            // duplicate visibility
            PerspectiveManager perspectiveManager = Manager.getPerspectiveManager();
            for (PerspectiveType perspective : perspectiveManager.getPerspectivesFor(element)) {
                PerspectiveManager.setVisibleInPerspective(duplicate,
                        perspective);
            }
        }
        return duplicate;
    }

    public void destroyElement(final Element element) throws ThemeException,
            NodeException {
        final Element parent = (Element) element.getParent();

        if (element instanceof ThemeElement) {
            removeNamedStylesOf(element.getName());
            unregisterTheme((ThemeElement) element);
            destroyDescendants(element);
            removeRelationsOf(element);

        } else if (element instanceof PageElement) {
            unregisterPage((PageElement) element);
            destroyDescendants(element);
            removeRelationsOf(element);
            if (parent != null) {
                parent.removeChild(element);
            }

        } else {
            destroyDescendants(element);
            removeRelationsOf(element);
            if (parent != null) {
                parent.removeChild(element);
            }
        }

        // Final cleanup: remove formats that are not used by any element.
        removeOrphanedFormats();
    }

    public void removeNamedStylesOf(String themeName) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        final UidManager uidManager = Manager.getUidManager();
        for (Style style : themeManager.getNamedStyles(themeName)) {
            removeNamedObject(themeName, "style", style.getName());
            deleteFormat(style);
            uidManager.unregister(style);
        }
    }

    // Formats
    public Format duplicateFormat(final Format format) throws ThemeException {
        final String typeName = format.getFormatType().getTypeName();
        final Format duplicate = FormatFactory.create(typeName);
        registerFormat(duplicate);

        duplicate.setName(format.getName());
        duplicate.setDescription(format.getDescription());
        duplicate.clonePropertiesOf(format);

        final Format ancestor = getAncestorFormatOf(format);
        if (ancestor != null) {
            makeFormatInherit(duplicate, ancestor);
        }
        return duplicate;
    }

    public List<Format> listFormats() {
        final UidManager uidManager = Manager.getUidManager();
        List<Format> formats = new ArrayList<Format>();
        for (Map.Entry<String, List<Integer>> entry : formatsByTypeName.entrySet()) {
            for (Integer uid : entry.getValue()) {
                Format format = (Format) uidManager.getObjectByUid(uid);
                formats.add(format);
            }
        }
        return formats;
    }

    public void registerFormat(final Format format) throws ThemeException {
        final Integer id = format.getUid();
        if (id == null) {
            throw new ThemeException("Cannot register a format without an id");
        }
        final String formatTypeName = format.getFormatType().getTypeName();
        if (formatTypeName == null) {
            throw new ThemeException("Cannot register a format without a type");
        }
        if (!formatsByTypeName.containsKey(formatTypeName)) {
            formatsByTypeName.put(formatTypeName, new ArrayList<Integer>());
        }
        final List<Integer> ids = formatsByTypeName.get(formatTypeName);
        if (ids.contains(id)) {
            throw new ThemeException("Cannot register a format twice: " + id);
        }
        ids.add(id);
    }

    public void unregisterFormat(final Format format) throws ThemeException {
        final Integer id = format.getUid();
        if (id == null) {
            throw new ThemeException("Cannot unregister a format without an id");
        }
        final String formatTypeName = format.getFormatType().getTypeName();
        if (formatTypeName == null) {
            throw new ThemeException(
                    "Cannot unregister a format without a type");
        }
        if (formatsByTypeName.containsKey(formatTypeName)) {
            final List<Integer> ids = formatsByTypeName.get(formatTypeName);
            if (!ids.contains(id)) {
                throw new ThemeException("Format with id: " + id
                        + " is not registered.");
            }
            ids.remove(id);
        }
        removeInheritanceTowards(format);
        removeInheritanceFrom(format);
    }

    public Set<String> getFormatTypeNames() {
        return new LinkedHashSet<String>(formatsByTypeName.keySet());
    }

    public List<Format> getFormatsByTypeName(final String formatTypeName) {
        List<Format> formats = new ArrayList<Format>();
        if (!formatsByTypeName.containsKey(formatTypeName)) {
            return formats;
        }
        UidManager uidManager = Manager.getUidManager();
        for (Integer id : formatsByTypeName.get(formatTypeName)) {
            formats.add((Format) uidManager.getObjectByUid(id));
        }
        return formats;
    }

    public List<Style> getStyles() {
        return getStyles(null);
    }

    public List<Style> getStyles(String themeName) {
        List<Style> styles = new ArrayList<Style>();
        for (Format format : getFormatsByTypeName("style")) {
            Style style = (Style) format;
            if (themeName != null) {
                ThemeElement theme = getThemeOfFormat(style);
                if (theme == null) {
                    if (!style.isNamed()) {
                        log.warn("THEME inconsistency: " + style
                                + " is not associated to any element.");
                    }
                    continue;
                }
                if (!themeName.equals(theme.getName())) {
                    continue;
                }
            }
            styles.add(style);
        }
        return styles;
    }

    public List<Style> getNamedStyles(String themeName) {
        List<Style> styles = new ArrayList<Style>();
        // Add named styles
        if (themeName != null) {
            for (Identifiable object : getNamedObjects(themeName, "style")) {
                if (!(object instanceof Style)) {
                    log.error("Expected Style object, got instead " + object);
                    continue;
                }
                styles.add((Style) object);
            }
        }
        return styles;
    }

    public List<Style> getSortedNamedStyles(String themeName) {
        List<Style> namedStyles = getNamedStyles(themeName);
        DAG graph = new DAG();
        for (Style s : namedStyles) {
            String styleName = s.getName();
            graph.addVertex(styleName);
            for (Format f : listFormatsDirectlyInheritingFrom(s)) {
                if (!f.isNamed()) {
                    continue;
                }
                try {
                    graph.addEdge(styleName, f.getName());
                } catch (CycleDetectedException e) {
                    log.error("Cycle detected in style dependencies: ", e);
                    return namedStyles;
                }
            }
        }

        List<Style> styles = new ArrayList<Style>();
        for (Object name : TopologicalSorter.sort(graph)) {
            styles.add((Style) getNamedObject(themeName, "style", (String) name));
        }
        return styles;
    }

    // Cache management
    public Long getLastModified(String themeName) {
        final Long date = lastModified.get(themeName);
        if (date == null) {
            return 0L;
        }
        return date;
    }

    public void setLastModified(String themeName, Long date) {
        lastModified.put(themeName, date);
    }

    public Long getLastModified(final URL url) {
        final String themeName = getThemeNameByUrl(url);
        return getLastModified(themeName);
    }

    public void themeModified(String themeName) {
        setLastModified(themeName, new Date().getTime());
        Manager.getResourceManager().clearGlobalCache(themeName);
    }

    public void stylesModified(String themeName) {
        resetCachedStyles(themeName);
    }

    /**
     * Reset cached static resources, useful for hot reload
     *
     * @since 5.6
     */
    public void resetCachedResources() {
        cachedResources.clear();
    }

    // Registration
    public void registerTheme(final ThemeElement theme) {
        String themeName = theme.getName();

        // store to the theme
        themes.put(themeName, theme);

        // store the pages
        for (Node node : theme.getChildren()) {
            PageElement page = (PageElement) node;
            String pagePath = String.format("%s/%s", themeName, page.getName());
            pages.put(pagePath, page);
        }

        // hook to notify potential listeners that the theme was registered
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.sendEvent(new Event(THEME_TOPIC,
                THEME_REGISTERED_EVENT_ID, this, themeName));

        themeModified(themeName);
        stylesModified(themeName);
    }

    public void registerPage(final ThemeElement theme, final PageElement page)
            throws NodeException {
        theme.addChild(page);
        String themeName = theme.getName();
        String pageName = page.getName();
        pages.put(String.format("%s/%s", themeName, pageName), page);
        log.debug("Added page: " + pageName + " to theme: " + themeName);
    }

    public void unregisterTheme(final ThemeElement theme) {
        String themeName = theme.getName();
        // remove pages
        for (PageElement page : getPagesOf(theme)) {
            unregisterPage(page);
        }
        // remove theme
        themes.remove(themeName);
        log.debug("Removed theme: " + themeName);
    }

    public void unregisterPage(PageElement page) {
        ThemeElement theme = (ThemeElement) page.getParent();
        if (theme == null) {
            log.debug("Page has no parent: " + page.getUid());
            return;
        }
        String themeName = theme.getName();
        String pageName = page.getName();
        pages.remove(String.format("%s/%s", themeName, pageName));
        log.debug("Removed page: " + pageName + " from theme: " + themeName);
    }

    public static void loadTheme(ThemeDescriptor themeDescriptor) {
        themeDescriptor.setLoadingFailed(true);
        String src = themeDescriptor.getSrc();
        if (src == null) {
            log.error("Could not load theme, source not set. ");
            return;
        }
        try {
            final boolean preload = false;
            ThemeParser.registerTheme(themeDescriptor, preload);
            themeDescriptor.setLoadingFailed(false);
        } catch (ThemeIOException e) {
            log.error("Could not register theme: " + src + " " + e.getMessage());
        }
    }

    // Theme management
    public void loadTheme(String src, String xmlSource)
            throws ThemeIOException, ThemeException {
        ThemeDescriptor themeDescriptor = getThemeDescriptor(src);
        if (themeDescriptor == null) {
            throw new ThemeIOException("Theme not found: " + src);
        }
        final String oldThemeName = themeDescriptor.getName();
        themeDescriptor.setLoadingFailed(true);
        final boolean preload = false;
        ThemeParser.registerTheme(themeDescriptor, xmlSource, preload);
        String themeName = themeDescriptor.getName();
        themeDescriptor.setName(themeName);

        themeModified(themeName);
        stylesModified(themeName);
        updateThemeDescriptors();
        // remove or restore customized themes
        if (!themeName.equals(oldThemeName)) {
            themes.remove(oldThemeName);
            for (ThemeDescriptor themeDef : getThemeDescriptors()) {
                if (oldThemeName.equals(themeDef.getName())
                        && !themeDef.isCustomized()) {
                    loadTheme(themeDef.getSrc());
                }
            }
        }
    }

    public void loadTheme(String src) throws ThemeIOException, ThemeException {
        loadTheme(src, null);
    }

    public void deleteTheme(String src) throws ThemeIOException, ThemeException {
        ThemeDescriptor themeDescriptor = getThemeDescriptor(src);
        if (themeDescriptor.isXmlConfigured()) {
            throw new ThemeIOException(
                    "Themes registered as contributions cannot be deleted: "
                            + src);
        }
        final ThemeManager themeManager = Manager.getThemeManager();
        final String themeName = themeDescriptor.getName();
        ThemeElement theme = themeManager.getThemeByName(themeName);
        if (theme == null) {
            throw new ThemeIOException("Theme not found: " + themeName);
        }

        URL url = null;
        try {
            url = new URL(src);
        } catch (MalformedURLException e) {
            throw new ThemeIOException(e);
        }

        if (!url.getProtocol().equals("file")) {
            throw new ThemeIOException("Theme source is not that of a file: "
                    + src);
        }

        final File file = new File(url.getFile());
        if (!file.exists()) {
            throw new ThemeIOException("File not found: " + src);
        }

        final String themeFileName = String.format("theme-%s.bak", themeName);
        final File backupFile = new File(getThemeDir(), themeFileName);
        if (backupFile.exists()) {
            if (!backupFile.delete()) {
                throw new ThemeIOException("Error while deleting backup file: "
                        + backupFile.getPath());
            }
        }
        if (!file.renameTo(backupFile)) {
            throw new ThemeIOException("Error while creating backup file: "
                    + backupFile.getPath());
        }

        try {
            themeManager.destroyElement(theme);
        } catch (NodeException e) {
            throw new ThemeIOException("Failed to delete theme: " + themeName,
                    e);
        } catch (ThemeException e) {
            throw new ThemeIOException("Failed to delete theme: " + themeName,
                    e);
        }

        themes.remove(themeName);
        deleteThemeDescriptor(src);

        updateThemeDescriptors();

        for (ThemeDescriptor themeDef : getThemeDescriptors()) {
            if (themeName.equals(themeDef.getName())
                    && !themeDef.isCustomized()) {
                loadTheme(themeDef.getSrc());
            }
        }
    }

    public void deletePage(String path) throws ThemeIOException, ThemeException {
        PageElement page = getPageByPath(path);
        if (page == null) {
            throw new ThemeIOException("Failed to delete unkown page: " + path);
        }
        try {
            destroyElement(page);
        } catch (NodeException e) {
            throw new ThemeIOException("Failed to delete page: " + path, e);
        }
    }

    public static void saveTheme(final String src) throws ThemeIOException,
            ThemeException {
        saveTheme(src, DEFAULT_THEME_INDENT);
    }

    public static void saveTheme(final String src, final int indent)
            throws ThemeIOException, ThemeException {
        ThemeDescriptor themeDescriptor = getThemeDescriptor(src);

        if (themeDescriptor == null) {
            throw new ThemeIOException("Theme not found: " + src);
        }

        if (!themeDescriptor.isWritable()) {
            throw new ThemeIOException("Protocol does not support output: "
                    + src);
        }

        ThemeSerializer serializer = new ThemeSerializer();
        final String xml = serializer.serializeToXml(src, indent);

        // Write the file
        URL url = null;
        try {
            url = new URL(src);
        } catch (MalformedURLException e) {
            throw new ThemeIOException("Could not save theme to " + src, e);
        }
        try {
            Utils.writeFile(url, xml);
        } catch (IOException e) {
            throw new ThemeIOException("Could not save theme to " + src, e);
        }
        themeDescriptor.setLastSaved(new Date());
        log.debug("Saved theme: " + src);
    }

    public static void repairTheme(ThemeElement theme) throws ThemeIOException {
        try {
            ThemeRepairer.repair(theme);
        } catch (ThemeException e) {
            throw new ThemeIOException("Could not repair theme: "
                    + theme.getName(), e);
        }
        log.debug("Repaired theme: " + theme.getName());
    }

    public static String renderElement(URL url) throws ThemeException {
        String result = null;
        InputStream is = null;
        try {
            is = url.openStream();
            Reader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(is));
                StringBuilder rendered = new StringBuilder();
                int ch;
                while ((ch = in.read()) > -1) {
                    rendered.append((char) ch);
                }
                result = rendered.toString();
            } catch (IOException e) {
                throw new ThemeException(e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new ThemeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error(e, e);
                } finally {
                    is = null;
                }
            }
        }
        return result;
    }

    public void removeOrphanedFormats() throws ThemeException {
        UidManager uidManager = Manager.getUidManager();
        for (Format format : listFormats()) {
            // Skip named formats since they are not directly associated to an
            // element.
            if (format.isNamed()) {
                continue;
            }
            if (ElementFormatter.getElementsFor(format).isEmpty()) {
                deleteFormat(format);
                uidManager.unregister(format);
            }
        }
    }

    private static void removeRelationsOf(Element element) {
        UidManager uidManager = Manager.getUidManager();
        PerspectiveManager perspectiveManager = Manager.getPerspectiveManager();
        for (Format format : ElementFormatter.getFormatsFor(element)) {
            ElementFormatter.removeFormat(element, format);
        }
        perspectiveManager.setAlwaysVisible(element);
        uidManager.unregister(element);
    }

    private static void destroyDescendants(Element element)
            throws NodeException {
        for (Node node : element.getDescendants()) {
            removeRelationsOf((Element) node);
        }
        element.removeDescendants();
    }

    // Format inheritance
    public void makeFormatInherit(Format format, Format ancestor) {
        if (format.equals(ancestor)) {
            FormatType formatType = format.getFormatType();
            String formatName = formatType != null ? formatType.getTypeName()
                    : "unknown";
            log.error(String.format(
                    "A format ('%s' with type '%s') cannot inherit from itself, aborting",
                    format.getName(), formatName));
            return;
        }
        if (listAncestorFormatsOf(ancestor).contains(format)) {
            log.error("Cycle detected.in format inheritance, aborting.");
            return;
        }
        // remove old inheritance relations
        removeInheritanceTowards(format);
        // set new ancestor
        DyadicRelation relation = new DyadicRelation(PREDICATE_FORMAT_INHERIT,
                format, ancestor);
        Manager.getRelationStorage().add(relation);
    }

    public static void removeInheritanceTowards(Format descendant) {
        Collection<Relation> relations = Manager.getRelationStorage().search(
                PREDICATE_FORMAT_INHERIT, descendant, null);
        Iterator<Relation> it = relations.iterator();
        while (it.hasNext()) {
            Relation relation = it.next();
            Manager.getRelationStorage().remove(relation);
        }
    }

    public static void removeInheritanceFrom(Format ancestor) {
        Collection<Relation> relations = Manager.getRelationStorage().search(
                PREDICATE_FORMAT_INHERIT, null, ancestor);
        Iterator<Relation> it = relations.iterator();
        while (it.hasNext()) {
            Relation relation = it.next();
            Manager.getRelationStorage().remove(relation);
        }
    }

    public static Format getAncestorFormatOf(Format format) {
        Collection<Relation> relations = Manager.getRelationStorage().search(
                PREDICATE_FORMAT_INHERIT, format, null);
        Iterator<Relation> it = relations.iterator();
        if (it.hasNext()) {
            return (Format) it.next().getRelate(2);
        }
        return null;
    }

    public static List<Format> listAncestorFormatsOf(Format format) {
        List<Format> ancestors = new ArrayList<Format>();
        Format current = format;
        while (current != null) {
            current = getAncestorFormatOf(current);
            if (current == null) {
                break;
            }
            // cycle detected
            if (ancestors.contains(current)) {
                break;
            }
            ancestors.add(current);
        }
        return ancestors;
    }

    public static List<Format> listFormatsDirectlyInheritingFrom(Format format) {
        List<Format> formats = new ArrayList<Format>();
        Collection<Relation> relations = Manager.getRelationStorage().search(
                PREDICATE_FORMAT_INHERIT, null, format);
        Iterator<Relation> it = relations.iterator();
        while (it.hasNext()) {
            formats.add((Format) it.next().getRelate(1));
        }
        return formats;
    }

    public void deleteFormat(Format format) throws ThemeException {
        ThemeManager.removeInheritanceTowards(format);
        ThemeManager.removeInheritanceFrom(format);
        unregisterFormat(format);
    }

    public static List<String> getUnusedStyleViews(Style style) {
        List<String> views = new ArrayList<String>();
        if (style.isNamed()) {
            return views;
        }
        for (Element element : ElementFormatter.getElementsFor(style)) {
            Widget widget = (Widget) ElementFormatter.getFormatFor(element,
                    "widget");
            String viewName = widget.getName();
            for (String name : style.getSelectorViewNames()) {
                if (!name.equals(viewName)) {
                    views.add(name);
                }
            }
        }
        return views;
    }

    // Cached styles
    public String getCachedStyles(String themeName, String basePath,
            String collectionName) {
        String key = String.format("%s|%s|%s", themeName,
                basePath != null ? basePath : "",
                collectionName != null ? collectionName : "");
        return cachedStyles.get(key);
    }

    public synchronized void setCachedStyles(String themeName, String basePath,
            String collectionName, String css) {
        String key = String.format("%s|%s|%s", themeName,
                basePath != null ? basePath : "",
                collectionName != null ? collectionName : "");
        cachedStyles.put(key, css);
    }

    private synchronized void resetCachedStyles(String themeName) {
        for (String key : cachedStyles.keySet()) {
            if (key.startsWith(themeName)) {
                cachedStyles.put(key, null);
            }
        }
    }

    // Resources
    public String getResource(String name) {
        return cachedResources.get(name);
    }

    public synchronized void setResource(String name, String content) {
        cachedResources.put(name, content);
    }

    public synchronized void updateResourceOrdering() {
        DAG graph = new DAG();
        for (Type type : Manager.getTypeRegistry().getTypes(TypeFamily.RESOURCE)) {
            ResourceType resourceType = (ResourceType) type;
            String resourceName = resourceType.getName();
            graph.addVertex(resourceName);
            for (String dependency : resourceType.getDependencies()) {
                try {
                    graph.addEdge(resourceName, dependency);
                } catch (CycleDetectedException e) {
                    log.error("Cycle detected in resource dependencies: ", e);
                    return;
                }
            }
        }
        resourceOrdering.clear();
        for (Object r : TopologicalSorter.sort(graph)) {
            resourceOrdering.add((String) r);
        }
    }

    public List<String> getResourceOrdering() {
        return resourceOrdering;
    }

    /**
     * Returns all the ordered resource names and their dependencies, given a
     * list of resources names.
     *
     * @since 5.5
     * @param resourceNames
     */
    // TODO: optimize?
    public List<String> getOrderedResourcesAndDeps(List<String> resourceNames) {
        List<String> res = new ArrayList<String>();
        if (resourceNames == null) {
            return res;
        }
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (String resourceName : resourceNames) {
            ResourceType resource = (ResourceType) typeRegistry.lookup(
                    TypeFamily.RESOURCE, resourceName);
            if (resource == null) {
                log.error(String.format("Resource not registered %s.",
                        resourceName));
                continue;
            }
            String[] deps = resource.getDependencies();
            if (deps != null) {
                for (String dep : deps) {
                    res.add(dep);
                }
            }
            res.add(resourceName);
        }
        List<String> orderedRes = new ArrayList<String>();
        List<String> ordered = getResourceOrdering();
        if (ordered != null) {
            for (String resource : ordered) {
                if (res.contains(resource)) {
                    orderedRes.add(resource);
                }
            }
        }
        return orderedRes;
    }

    public void unregisterResourceOrdering(ResourceType resourceType) {
        String resourceName = resourceType.getName();
        if (resourceOrdering.contains(resourceName)) {
            resourceOrdering.remove(resourceName);
        }
    }

    public byte[] getImageResource(String path) throws ThemeException {
        String key = String.format("image/%s", path);
        byte[] data = cachedBinaries.get(key);
        if (data == null) {
            String[] parts = path.split("/");
            if (parts.length != 3) {
                throw new ThemeException("Incorrect image path: " + path);
            }
            String resourceBankName = parts[0];
            String collectionName = parts[1];
            String resourceName = parts[2];
            data = ResourceManager.getBinaryBankResource(resourceBankName,
                    collectionName, "image", resourceName);
            cachedBinaries.put(key, data);
        }
        return data;
    }

    public static List<ViewType> getViewTypesForFragmentType(
            final FragmentType fragmentType) {
        final List<ViewType> viewTypes = new ArrayList<ViewType>();

        for (Type v : Manager.getTypeRegistry().getTypes(TypeFamily.VIEW)) {
            final ViewType viewType = (ViewType) v;
            final String viewName = viewType.getViewName();

            if ("*".equals(viewName)) {
                continue;
            }

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
            if (fragmentType.getModelType() != modelType) {
                continue;
            }
            viewTypes.add(viewType);
        }
        return viewTypes;
    }

    // Resource banks
    public static ResourceBank getResourceBank(String name)
            throws ThemeException {
        if (name == null) {
            throw new ThemeException("Resource bank name not set");
        }
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ResourceBank resourceBank = (ResourceBank) typeRegistry.lookup(
                TypeFamily.RESOURCE_BANK, name);
        if (resourceBank != null) {
            return resourceBank;
        } else {
            throw new ThemeException("Resource bank not found: " + name);
        }
    }

    public static List<ResourceBank> getResourceBanks() {
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        List<ResourceBank> resourceBanks = new ArrayList<ResourceBank>();
        for (Type type : typeRegistry.getTypes(TypeFamily.RESOURCE_BANK)) {
            resourceBanks.add((ResourceBank) type);
        }
        return resourceBanks;
    }

    // Theme descriptors
    public static List<ThemeDescriptor> getThemeDescriptors() {
        final List<ThemeDescriptor> themeDescriptors = new ArrayList<ThemeDescriptor>();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (Type type : typeRegistry.getTypes(TypeFamily.THEME)) {
            if (type != null) {
                ThemeDescriptor themeDescriptor = (ThemeDescriptor) type;
                themeDescriptors.add(themeDescriptor);
            }
        }
        return themeDescriptors;
    }

    public static ThemeDescriptor getThemeDescriptor(String src)
            throws ThemeException {
        ThemeDescriptor themeDef = (ThemeDescriptor) Manager.getTypeRegistry().lookup(
                TypeFamily.THEME, src);
        if (themeDef == null) {
            throw new ThemeException("Unknown theme: " + src);
        }
        return themeDef;
    }

    public static void deleteThemeDescriptor(String src) throws ThemeException {
        ThemeDescriptor themeDef = getThemeDescriptor(src);
        Manager.getTypeRegistry().unregister(themeDef);
    }

    // Template engines
    public static List<String> getTemplateEngineNames() {
        List<String> types = new ArrayList<String>();
        for (Type type : Manager.getTypeRegistry().getTypes(
                TypeFamily.TEMPLATE_ENGINE)) {
            types.add(type.getTypeName());
        }
        return types;
    }

    public static String getTemplateEngineName(String applicationPath) {
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        if (applicationPath == null) {
            return ThemeManager.getDefaultTemplateEngineName();
        }
        final ApplicationType application = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, applicationPath);

        if (application != null) {
            return application.getTemplateEngine();
        }
        return getDefaultTemplateEngineName();
    }

    public static String getDefaultTemplateEngineName() {
        // TODO use XML configuration
        return "jsf-facelets";
    }

    public static Element getElementById(final Integer id) {
        Object object = Manager.getUidManager().getObjectByUid(id);
        if (!(object instanceof Element)) {
            return null;
        }
        return (Element) object;
    }

    public static Element getElementById(final String id) {
        return getElementById(Integer.valueOf(id));
    }

    public static Format getFormatById(final Integer id) {
        Object object = Manager.getUidManager().getObjectByUid(id);
        if (!(object instanceof Format)) {
            return null;
        }
        return (Format) object;
    }

    public static Format getFormatById(final String id) {
        return getFormatById(Integer.valueOf(id));
    }

    public static ThemeElement getThemeOfFormat(Format format) {
        Collection<Element> elements = ElementFormatter.getElementsFor(format);
        if (elements.isEmpty()) {
            return null;
        }
        // Get the first element assuming all elements belong to the same
        // theme.
        Element element = elements.iterator().next();
        return getThemeOf(element);
    }

    public Layout createLayout() {
        Layout layout = null;
        try {
            layout = (Layout) FormatFactory.create("layout");
            registerFormat(layout);
        } catch (ThemeException e) {
            log.error("Layout creation failed", e);
        }
        return layout;
    }

    public Widget createWidget() {
        Widget widget = null;
        try {
            widget = (Widget) FormatFactory.create("widget");
            registerFormat(widget);
        } catch (ThemeException e) {
            log.error("Widget creation failed", e);
        }
        return widget;
    }

    public Style createStyle() {
        Style style = null;
        try {
            style = (Style) FormatFactory.create("style");
            registerFormat(style);
        } catch (ThemeException e) {
            log.error("Style creation failed", e);
        }
        return style;
    }

    public void registerModelByClassname(ModelType modelType) {
        modelsByClassname.put(modelType.getClassName(), modelType);
    }

    public void unregisterModelByClassname(ModelType modelType) {
        modelsByClassname.remove(modelType.getClassName());
    }

    public ModelType getModelByClassname(String className) {
        return modelsByClassname.get(className);
    }

    // Theme sets
    public List<ThemeSet> getThemeSets() {
        List<ThemeSet> themeSets = new ArrayList<ThemeSet>();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (Type type : typeRegistry.getTypes(TypeFamily.THEMESET)) {
            themeSets.add((ThemeSet) type);
        }
        return themeSets;
    }

    public ThemeSet getThemeSetByName(final String name) {
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        return (ThemeSet) typeRegistry.lookup(TypeFamily.THEMESET, name);
    }

    public static String getCollectionCssMarker() {
        return COLLECTION_CSS_MARKER;
    }

}
