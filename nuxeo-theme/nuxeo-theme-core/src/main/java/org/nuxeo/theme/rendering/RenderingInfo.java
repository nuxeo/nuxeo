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

package org.nuxeo.theme.rendering;

import java.net.URL;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.models.Info;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.uids.Identifiable;

public class RenderingInfo implements Info, Identifiable {

    private String markup = "";

    private Model model;

    private Element element;

    private Format format;

    private Integer uid;

    private String name;

    private URL themeUrl;

    private boolean dirty = false;

    public RenderingInfo() {
    }

    public RenderingInfo(Element element, URL themeUrl) {
        this.element = element;
        this.themeUrl = themeUrl;
        uid = element.getUid();
    }

    public RenderingInfo createCopy() {
        RenderingInfo clone = new RenderingInfo(element, themeUrl);
        clone.setDirty(dirty);
        return clone;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public String getMarkup() {
        return markup;
    }

    public void setMarkup(String markup) {
        this.markup = markup;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public EngineType getEngine() {
        return ThemeManager.getEngineByUrl(themeUrl);
    }

    public String getViewMode() {
        return ThemeManager.getViewModeByUrl(themeUrl);
    }

    public URL getThemeUrl() {
        return themeUrl;
    }

    public Element getElement() {
        return element;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ThemeElement getTheme() {
        return Manager.getThemeManager().getThemeByUrl(themeUrl);
    }

    public TemplateEngineType getTemplateEngine() {
        return ThemeManager.getTemplateEngineByUrl(themeUrl);
    }

    public boolean isRenderingPostponed(boolean cache) {
        return cache && isDirty();
    }

}
