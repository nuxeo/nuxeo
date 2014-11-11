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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFactory;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFactory;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentFactory;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.properties.FieldIO;
import org.nuxeo.theme.relations.DefaultPredicate;
import org.nuxeo.theme.relations.DyadicRelation;
import org.nuxeo.theme.relations.Predicate;
import org.nuxeo.theme.relations.Relation;
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.Identifiable;
import org.nuxeo.theme.uids.UidManager;

public final class ThemeManager implements Registrable {

    private static final Log log = LogFactory.getLog(ThemeManager.class);

    public static final Pattern PRESET_PATTERN = Pattern.compile("^\"(.*?)\"$");

    private Long lastModified = 0L;

    private final Map<String, ThemeElement> themes = new HashMap<String, ThemeElement>();

    private final Map<String, PageElement> pages = new HashMap<String, PageElement>();

    private final Map<String, List<Integer>> formatsByTypeName = new HashMap<String, List<Integer>>();

    private final Map<String, Map<String, Integer>> namedObjects = new HashMap<String, Map<String, Integer>>();

    private final Predicate PREDICATE_FORMAT_INHERIT = new DefaultPredicate(
            "_ inherits from _");

    private String cachedStyles;

    private final Map<String, String> cachedResources = new HashMap<String, String>();

    public void clear() {
        themes.clear();
        pages.clear();
        formatsByTypeName.clear();
        namedObjects.clear();
    }

    public Set<String> getThemeNames() {
        return themes.keySet();
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

    public ThemeElement getThemeByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 3) {
            return null;
        }
        final String themeName = path[3];
        return getThemeByName(themeName);
    }

    public PageElement getThemePageByUrl(final URL url) {
        if (url == null) {
            return null;
        }
        final String[] path = url.getPath().split("/");
        if (path.length <= 4) {
            return null;
        }
        final String pagePath = path[3] + '/' + path[4];
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
        if (path.length <= 5) {
            return null;
        }
        final String perspectiveName = path[5];
        return (PerspectiveType) Manager.getTypeRegistry().lookup(
                TypeFamily.PERSPECTIVE, perspectiveName);
    }

    // Named objects
    public Identifiable getNamedObject(final String themeName,
            final String realm, final String name) {
        final Map<String, Integer> objectsInTheme = namedObjects.get(themeName);
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

    public void setNamedObject(final String themeName, final String realm,
            final Identifiable object) {
        if (!namedObjects.containsKey(themeName)) {
            namedObjects.put(themeName, new HashMap<String, Integer>());
        }
        final String name = object.getName();
        if (name == null) {
            log.error("Cannot register unnamed object");
            return;
        }
        namedObjects.get(themeName).put(String.format("%s/%s", realm, name),
                object.getUid());
    }

    public List<Identifiable> getNamedObjects(final String themeName,
            final String realm) {
        final List<Identifiable> objects = new ArrayList<Identifiable>();
        final Map<String, Integer> objectsInTheme = namedObjects.get(themeName);
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

    public void removeNamedObjects(final String themeName) {
        namedObjects.remove(themeName);
    }

    // Element actions
    public Element duplicateElement(final Element element,
            final boolean duplicateFormats) {
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

    public void destroyElement(final Element element) {
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
    public Format duplicateFormat(final Format format) {
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
        for (String key : formatsByTypeName.keySet()) {
            for (Integer uid : formatsByTypeName.get(key)) {
                Format format = (Format) uidManager.getObjectByUid(uid);
                formats.add(format);
            }
        }
        return formats;
    }

    public void registerFormat(final Format format) {
        Integer id = format.getUid();
        String formatTypeName = format.getFormatType().getTypeName();
        if (!formatsByTypeName.containsKey(formatTypeName)) {
            formatsByTypeName.put(formatTypeName, new ArrayList<Integer>());
        }
        formatsByTypeName.get(formatTypeName).add(id);
    }

    public void unregisterFormat(final Format format) {
        Integer id = format.getUid();
        String formatTypeName = format.getFormatType().getTypeName();
        if (formatsByTypeName.containsKey(formatTypeName)) {
            formatsByTypeName.get(formatTypeName).remove(id);
        }
    }

    public Set<String> getFormatTypeNames() {
        return formatsByTypeName.keySet();
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

    // Presets
    public static PresetType getPresetByName(final String name) {
        return (PresetType) Manager.getTypeRegistry().lookup(TypeFamily.PRESET,
                name);
    }

    public static PresetType resolvePreset(final String value) {
        PresetType preset;
        Matcher presetMatcher = PRESET_PATTERN.matcher(value);
        if (presetMatcher.find()) {
            preset = getPresetByName(presetMatcher.group(1));
        } else {
            return null;
        }
        return preset;
    }

    public static String resolvePresets(final String value) {
        Pattern presetPattern = Pattern.compile(".*?\"(.*?)\".*?");
        Matcher presetMatcher = presetPattern.matcher(value);
        StringBuilder sb = new StringBuilder();
        int end = 0;
        while (presetMatcher.find()) {
            end = presetMatcher.end(1) + 1;
            sb.append(value.substring(presetMatcher.start(),
                    presetMatcher.start(1) - 1));
            String presetName = presetMatcher.group(1);
            PresetType preset = getPresetByName(presetName);
            if (preset == null) {
                sb.append('\"').append(presetName).append('\"');
            } else {
                sb.append(preset.getValue());
            }
        }
        sb.append(value.substring(end));
        return sb.toString();
    }

    // Cache management
    public void themeModified() {
        lastModified = new Date().getTime();
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public void stylesModified() {
        setCachedStyles(null);
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

    public void registerPage(final ThemeElement theme, final PageElement page) {
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
    public void loadTheme(String src) throws ThemeIOException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ThemeDescriptor themeDescriptor = (ThemeDescriptor) typeRegistry.lookup(
                TypeFamily.THEME, src);
        if (themeDescriptor == null) {
            throw new ThemeIOException("Theme not found: " + src);
        }
        URL url = themeDescriptor.getUrl();

        if (url != null) {
            String themeName = ThemeParser.registerTheme(url);
            if (themeName == null) {
                throw new ThemeIOException("Could not parse theme: " + src);
            }
            themeDescriptor.setName(themeName);
            themeDescriptor.setLastLoaded(new Date());
            themeModified();
        }
    }

    public static void saveTheme(final String src) throws ThemeIOException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ThemeDescriptor themeDescriptor = (ThemeDescriptor) typeRegistry.lookup(
                TypeFamily.THEME, src);
        if (themeDescriptor == null) {
            throw new ThemeIOException("Theme not found: " + src);
        }

        if (!themeDescriptor.isWritable()) {
            throw new ThemeIOException("Protocol does not support output: "
                    + src);
        }

        URL url = themeDescriptor.getUrl();
        OutputStream os = null;
        try {
            URLConnection urlc = url.openConnection();
            os = urlc.getOutputStream();
        } catch (IOException e) {
            log.error(e);
        }

        ThemeSerializer serializer = new ThemeSerializer();
        String themeName = themeDescriptor.getName();
        ThemeElement theme = Manager.getThemeManager().getThemeByName(themeName);
        final String xml = serializer.serializeToXml(theme);

        if (os != null) {
            try {
                os.write(xml.getBytes());
                os.flush();
            } catch (IOException e) {
                log.error(e);
            } finally {
                try {
                    os.close();
                } catch (IOException e) {
                    log.error(e);
                } finally {
                    os = null;
                }
            }
        }
    }

    public static void repairTheme(ThemeElement theme) {
        ThemeRepairer.repair(theme);
    }

    public void removeOrphanedFormats() {
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

    private static void destroyDescendants(Element element) {
        for (Node node : element.getDescendants()) {
            removeRelationsOf((Element) node);
        }
        element.removeDescendants();
    }

    // Format inheritance
    public void makeFormatInherit(Format format, Format ancestor) {
        if (format.equals(ancestor)) {
            FormatType formatType = format.getFormatType();
            String formatName = formatType != null ? formatType.getTypeName() : "unknown";
            log.error(String.format(
                    "A format ('%s' with type '%s') cannot inherit from itself, aborting",
                    format.getName(), formatName));
            return;
        }
        if (listAncestorFormatsOf(ancestor).contains(format)) {
            log.error("Cycle detected.in format inheritance, aborting.");
            return;
        }
        DyadicRelation relation = new DyadicRelation(PREDICATE_FORMAT_INHERIT,
                format, ancestor);
        Manager.getRelationStorage().add(relation);
    }

    public Format getAncestorFormatOf(Format format) {
        Collection<Relation> relations = Manager.getRelationStorage().search(
                PREDICATE_FORMAT_INHERIT, format, null);
        Iterator<Relation> it = relations.iterator();
        if (it.hasNext()) {
            return (Format) it.next().getRelate(2);
        }
        return null;
    }

    public List<Format> listAncestorFormatsOf(Format format) {
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

    // Cached styles
    public String getCachedStyles() {
        return cachedStyles;
    }

    public synchronized void setCachedStyles(String cachedStyles) {
        this.cachedStyles = cachedStyles;
    }

    public String getResource(String name) {
        return cachedResources.get(name);
    }

    public synchronized void setResource(String name, String content) {
        cachedResources.put(name, content);
    }

}
