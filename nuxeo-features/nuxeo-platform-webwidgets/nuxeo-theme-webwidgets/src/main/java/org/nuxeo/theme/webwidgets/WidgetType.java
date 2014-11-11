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

package org.nuxeo.theme.webwidgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("widget")
public final class WidgetType {

    public static final String DEFAULT_WIDGET_ICON_PATH = "nxthemes/webwidgets/icons/default-widget.png";

    @XNode("@name")
    private String name;

    @XNode("@id")
    private String id;

    @XNode("category")
    private String category;

    @XNode("path")
    private String path;

    @XNode("icon")
    private String iconPath = DEFAULT_WIDGET_ICON_PATH;

    private String icon;

    private String author = "";

    private String description = "";

    private String website = "";

    private String screenshot = "";

    private String thumbnail = "";

    private String source;

    private String body;

    private String scripts;

    private String styles;

    private Map<String, Object> data;

    private List<WidgetFieldType> schema;

    public String getTypeName() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<WidgetFieldType> getSchema() {
        return schema;
    }

    public void setSchema(List<WidgetFieldType> schema) {
        this.schema = schema;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setScripts(String scripts) {
        this.scripts = scripts;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }

    public String getScripts() {
        return scripts;
    }

    public String getStyles() {
        return styles;
    }

    public String getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(String screenshot) {
        this.screenshot = screenshot;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Map<String, Object> getInfo() {
        if (data == null) {
            data = new HashMap<String, Object>();
            data.put("name", name);
            data.put("icon", icon);

            Map<String, String> metas = new HashMap<String, String>();
            metas.put("author", author);
            metas.put("description", description);
            metas.put("website", website);
            metas.put("thumbnail", thumbnail);
            metas.put("screenshot", screenshot);
            data.put("metas", metas);

            final List<Map<String, Object>> preferences = new ArrayList<Map<String, Object>>();
            for (WidgetFieldType wft : schema) {
                preferences.add(wft.getInfo());
            }
            data.put("preferences", preferences);

            data.put("scripts", scripts);
            data.put("styles", styles);
            data.put("body", body);
        }
        return data;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
