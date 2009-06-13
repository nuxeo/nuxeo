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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
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
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.Identifiable;
import org.nuxeo.theme.uids.UidManager;
import org.nuxeo.theme.views.ViewType;


public final class ThemeManager implements Registrable {

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

    private static final File CUSTOM_THEME_DIR;

    private static final FilenameFilter CUSTOM_THEME_FILENAME_FILTER = new CustomThemeNameFilter();

    private static final int DEFAULT_THEME_INDENT = 2;

    static {
        CUSTOM_THEME_DIR = new File(Framework.getRuntime().getHome(), "themes");
        CUSTOM_THEME_DIR.mkdirs();
    }

    public void clear() {
        themes.clear();
        pages.clear();
        formatsByTypeName.clear();
        namedObjectsByTheme.clear();
        themeOfNamedObjects.clear();
    }

    public Map<String, Info> getGlobalInfoMap() {
        return infoMap;
    }
    
    public static boolean validateThemeName(String themeName) {
        return (themeName.matches("^([a-z]|[a-z][a-z0-9_\\-]*?[a-z0-9])$"));
    }

    public static String getCustomThemePath(String themeName)
            throws ThemeIOException {
        String themeFileName = String.format("theme-%s.xml", themeName);
        File file = new File(CUSTOM_THEME_DIR, themeFileName);
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new ThemeIOException("Could not get custom theme path: "
                    + themeName, e);
        }
    }

    public static List<File> getCustomThemeFiles() {
        List<File> files = new ArrayList<File>();
        for (File f : CUSTOM_THEME_DIR.listFiles(CUSTOM_THEME_FILENAME_FILTER)) {
            files.add(f);
        }
        return files;
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
            }
            int size = themeDescriptors.size();
            themeDescriptors.get(size - 1).setCustomized(false);
        }
    }

    public static String getDefaultTheme(final String applicationPath) {
        String defaultTheme = "";
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
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

    public String getPagePathByUrl(final URL url) {
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

    public ThemeElement getThemeByName(final String name) {
        return themes.get(name);
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
            final Identifiable object) {
        if (!namedObjectsByTheme.containsKey(themeName)) {
            namedObjectsByTheme.put(themeName,
                    new LinkedHashMap<String, Integer>());
        }
        final Integer uid = object.getUid();
        final String name = object.getName();
        if (name == null) {
            log.error("Cannot register unnamed object");
            return;
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
                    if (object != null) {
                        objects.add(object);
                    }
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
            final String inheritedName, final String currentThemeName) {
        final FormatType styleType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "style");
        Style style = (Style) ElementFormatter.getFormatByType(element,
                styleType);
        // Make the style no longer inherits from other another style if
        // 'inheritedName' is null
        if (inheritedName == null) {
            ThemeManager.removeInheritanceTowards(style);
        } else {
            final String themeName = currentThemeName.split("/")[0];
            final Style inheritedStyle = (Style) getNamedObject(themeName,
                    "style", inheritedName);
            if (inheritedStyle == null) {
                log.error("Unknown style: " + inheritedName);
            } else {
                makeFormatInherit(style, inheritedStyle);
            }
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
            }

            // duplicate formats or create a relation
            for (Format format : ElementFormatter.getFormatsFor(element)) {
                if (duplicateFormats) {
                    format = duplicateFormat(format);
                }
                ElementFormatter.setFormat(duplicate, format);
            }
        }
        return duplicate;
    }

    public void destroyElement(final Element element) throws ThemeException,
            NodeException {
        final Element parent = (Element) element.getParent();

        if (element instanceof ThemeElement) {
            removeNamedObjects(element.getName());
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
                // FIXME This shouldn't happen
                if (theme == null) {
                    // The style is associated to a non-existing element
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
                styles.add((Style) object);
            }
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
    }

    public void stylesModified(String themeName) {
        setCachedStyles(themeName, null);
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
        log.debug("Added theme: " + themeName);
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
            log.error("Page has no parent: " + page.getUid());
            return;
        }
        String themeName = theme.getName();
        String pageName = page.getName();
        pages.remove(String.format("%s/%s", themeName, pageName));
        log.debug("Removed page: " + pageName + " from theme: " + themeName);
    }

    // Theme management
    public void loadTheme(String src) throws ThemeIOException, ThemeException {
        ThemeDescriptor themeDescriptor = getThemeDescriptor(src);
        if (themeDescriptor == null) {
            throw new ThemeIOException("Theme not found: " + src);
        }
        final String oldThemeName = themeDescriptor.getName();
        themeDescriptor.setLoadingFailed(true);
        String themeName = ThemeParser.registerTheme(themeDescriptor);
        if (themeName == null) {
            throw new ThemeIOException("Could not parse theme: " + src);
        }
        themeDescriptor.setName(themeName);
        themeDescriptor.setLoadingFailed(false);
        themeDescriptor.setLastLoaded(new Date());
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
        log.debug("Loaded theme: " + src);
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
        final File backupFile = new File(CUSTOM_THEME_DIR, themeFileName);
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
                    e.printStackTrace();
                } finally {
                    is = null;
                }
            }
        }
        return result;
    }

    public void removeOrphanedFormats() throws ThemeException {
        RelationStorage relationStorage = Manager.getRelationStorage();
        UidManager uidManager = Manager.getUidManager();
        Set<Format> formatsToUnregister = new HashSet<Format>();
        for (Format format : listFormats()) {
            // Skip named formats since they are not directly associated to an
            // element.
            if (format.isNamed()) {
                continue;
            }
            if (ElementFormatter.getElementsFor(format).isEmpty()) {
                for (Relation relation : relationStorage.search(
                        PREDICATE_FORMAT_INHERIT, format, null)) {
                    relationStorage.remove(relation);
                }
                unregisterFormat(format);
                uidManager.unregister(format);
            }
        }

        for (Format format : listFormats()) {
            // Unregister named formats if no other format inherit from
            // them.
            if (format.isNamed()
                    && relationStorage.search(PREDICATE_FORMAT_INHERIT, null,
                            format).isEmpty()) {
                formatsToUnregister.add(format);
            }
        }
        for (Format f : formatsToUnregister) {
            unregisterFormat(f);
            uidManager.unregister(f);
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

    public static void removeInheritanceTowards(Format format) {
        Collection<Relation> relations = Manager.getRelationStorage().search(
                PREDICATE_FORMAT_INHERIT, format, null);
        Iterator<Relation> it = relations.iterator();
        if (it.hasNext()) {
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
        for (Format f : ThemeManager.listFormatsDirectlyInheritingFrom(format)) {
            ThemeManager.removeInheritanceTowards(f);
        }
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
    public String getCachedStyles(String themeName) {
        return cachedStyles.get(themeName);
    }

    public synchronized void setCachedStyles(String themeName, String css) {
        cachedStyles.put(themeName, css);
    }

    public String getResource(String name) {
        return cachedResources.get(name);
    }

    public synchronized void setResource(String name, String content) {
        cachedResources.put(name, content);
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
        return (Element) Manager.getUidManager().getObjectByUid(id);
    }

    public static Element getElementById(final String id) {
        return getElementById(Integer.valueOf(id));
    }

    public static Format getFormatById(final Integer id) {
        return (Format) Manager.getUidManager().getObjectByUid(id);
    }

    public static Format getFormatById(final String id) {
        return getFormatById(Integer.valueOf(id));
    }

    public static ThemeElement getThemeOfFormat(Format format) {
        Collection<Element> elements = ElementFormatter.getElementsFor(format);
        if (elements.isEmpty()) {
            return null;
        }
        // Get the first element assuming all elements belong to the same theme.
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


}
