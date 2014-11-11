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

package org.nuxeo.theme.resources;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.presets.PresetManager;
import org.nuxeo.theme.presets.PresetType;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.UidManager;

@XObject("bank")
public class ResourceBank implements Type {

    private static final Log log = LogFactory.getLog(ResourceBank.class);

    @XNode("@name")
    public String name;

    private String connectionUrl;

    @XNode("@url")
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = Framework.expandVars(connectionUrl);
    }

    public ResourceBank() {
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public String getTypeName() {
        return name;
    }

    @Override
    public TypeFamily getTypeFamily() {
        return TypeFamily.RESOURCE_BANK;
    }

    public boolean checkStatus() {
        String src = String.format("%s/status", connectionUrl);
        byte[] status;
        try {
            status = Utils.fetchUrl(new URL(src));
        } catch (Exception e) {
            return false;
        }
        return status != null && "OK".equals(new String(status));
    }

    public byte[] getResourceContent(String collectionName, String typeName,
            String resourceId) {
        String src = String.format("%s/%s/%s/%s", connectionUrl,
                URIUtils.quoteURIPathComponent(collectionName, true),
                URIUtils.quoteURIPathComponent(typeName, true),
                URIUtils.quoteURIPathComponent(resourceId, true));
        log.debug("Loading THEME " + typeName + " from: " + src);
        try {
            return Utils.fetchUrl(new URL(src));
        } catch (Exception e) {
            log.error("Could not retrieve RESOURCE: " + src
                    + " from THEME BANK: " + name);
        }
        return null;
    }

    public List<ImageInfo> getImages() {
        List<ImageInfo> images = new ArrayList<ImageInfo>();
        String src = String.format("%s/json/images", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve image list: " + src
                    + " from THEME BANK: " + name);
            return images;
        }
        for (Object object : JSONArray.fromObject(list)) {
            Map<String, Object> image = JSONObject.fromObject(object);
            images.add(new ImageInfo((String) image.get("name"),
                    (String) image.get("collection")));
        }
        return images;
    }

    public List<String> getCollections() {
        List<String> paths = new ArrayList<String>();
        String src = String.format("%s/json/collections", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve collection list: " + src
                    + " from THEME BANK: " + name);
            return paths;
        }
        for (Object path : JSONArray.fromObject(list)) {
            paths.add((String) path);
        }
        return paths;
    }

    @SuppressWarnings("unchecked")
    public List<SkinInfo> getSkins() {
        List<SkinInfo> skins = new ArrayList<SkinInfo>();
        String src = String.format("%s/json/skins", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve skin list: " + src
                    + " from THEME BANK: " + name);
            return skins;
        }
        for (Object object : JSONArray.fromObject(list)) {
            Map<String, Object> skin = JSONObject.fromObject(object);
            skins.add(new SkinInfo((String) skin.get("name"),
                    (String) skin.get("bank"), (String) skin.get("collection"),
                    (String) skin.get("resource"),
                    (String) skin.get("preview"), (Boolean) skin.get("base")));
        }
        return skins;
    }

    @SuppressWarnings("unchecked")
    public List<StyleInfo> getStyles() {
        List<StyleInfo> styles = new ArrayList<StyleInfo>();
        String src = String.format("%s/json/styles", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve the style list: " + src
                    + " from THEME BANK: " + name);
            return styles;
        }
        for (Object object : JSONArray.fromObject(list)) {
            Map<String, Object> style = JSONObject.fromObject(object);
            styles.add(new StyleInfo((String) style.get("name"),
                    (String) style.get("bank"),
                    (String) style.get("collection"),
                    (String) style.get("resource"),
                    (String) style.get("preview")));
        }
        return styles;
    }

    @SuppressWarnings("unchecked")
    public List<PresetInfo> getPresets() {
        List<PresetInfo> presets = new ArrayList<PresetInfo>();
        String src = String.format("%s/json/presets", connectionUrl);
        String list = "";
        try {
            list = new String(Utils.fetchUrl(new URL(src)));
        } catch (Exception e) {
            log.error("Could not retrieve the preset list: " + src
                    + " from THEME BANK: " + name);
            return presets;
        }
        for (Object object : JSONArray.fromObject(list)) {
            Map<String, Object> preset = JSONObject.fromObject(object);
            presets.add(new PresetInfo((String) preset.get("name"),
                    (String) preset.get("bank"),
                    (String) preset.get("collection"),
                    (String) preset.get("category"),
                    (String) preset.get("value")));
        }
        return presets;
    }

    public String getName() {
        return name;
    }

    public void connect(String themeName) throws ThemeException {
        loadRemotePresets();
        loadRemoteStyles(themeName);
    }

    public void disconnect(String themeName) throws ThemeException {
        unloadRemotePresets();
        unloadRemoteStyles(themeName);
    }

    private void loadRemotePresets() throws ThemeException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (PresetInfo presetInfo : getPresets()) {
            String name = presetInfo.getName();
            String label = name;
            String category = presetInfo.getCategory();
            String group = String.format("%s %s", presetInfo.getCollection(),
                    presetInfo.getCategory());
            String value = presetInfo.getValue();

            String typeName = String.format("%s (%s)", name, group);
            PresetType preset = PresetManager.getPresetByName(typeName);
            if (preset == null) {
                preset = new PresetType();
                preset.setName(name);
                preset.setGroup(group);
                typeRegistry.register(preset);
            }
            preset.setLabel(label);
            preset.setCategory(category);
            preset.setValue(value);
        }
    }

    private void loadRemoteStyles(String themeName) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        List<StyleInfo> bankStyles = getStyles();
        for (StyleInfo styleInfo : bankStyles) {
            String styleName = styleInfo.getName();
            Style style = (Style) themeManager.getNamedObject(themeName,
                    "style", styleName);

            if (style == null) {
                style = themeManager.createStyle();
                style.setName(styleName);
                style.setRemote(true);
                themeManager.setNamedObject(themeName, "style", style);
            }
            String collectionName = styleInfo.getCollection();
            String resourceId = styleInfo.getResource();
            String cssSource = ResourceManager.getBankResource(name,
                    collectionName, "style", resourceId);
            style.setCollection(collectionName);
            Utils.loadCss(style, cssSource, "*");
        }
    }

    private void unloadRemotePresets() throws ThemeException {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (PresetInfo presetInfo : getPresets()) {
            String name = presetInfo.getName();
            String group = String.format("%s %s", presetInfo.getCollection(),
                    presetInfo.getCategory());
            String typeName = String.format("%s (%s)", name, group);
            PresetType preset = PresetManager.getPresetByName(typeName);
            if (preset != null) {
                typeRegistry.unregister(preset);
            }
        }
    }

    private void unloadRemoteStyles(String themeName) throws ThemeException {
        ThemeManager themeManager = Manager.getThemeManager();
        UidManager uidManager = Manager.getUidManager();
        List<StyleInfo> bankStyles = getStyles();
        for (StyleInfo styleInfo : bankStyles) {
            String styleName = styleInfo.getName();
            Style style = (Style) themeManager.getNamedObject(themeName,
                    "style", styleName);
            if (style == null || style.isCustomized()) {
                continue;
            }
            themeManager.removeNamedObject(themeName, "style", styleName);
            themeManager.deleteFormat(style);
            uidManager.unregister(style);
        }
    }

}
