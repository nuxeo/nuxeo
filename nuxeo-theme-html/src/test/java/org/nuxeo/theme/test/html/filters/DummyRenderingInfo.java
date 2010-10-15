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

package org.nuxeo.theme.test.html.filters;

import java.net.URL;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.themes.ThemeManager;

public class DummyRenderingInfo extends RenderingInfo {

    private final URL themeUrl;

    private final Element element;

    private String markup = "";

    private Model model;

    private Format format;

    private boolean dirty = false;

    public DummyRenderingInfo(Element element, URL themeUrl) {
        this.element = element;
        this.themeUrl = themeUrl;
    }

    @Override
    public DummyRenderingInfo createCopy() {
        return new DummyRenderingInfo(element, themeUrl);
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void setModel(Model model) {
        this.model = model;
    }

    @Override
    public String getMarkup() {
        return markup;
    }

    @Override
    public void setMarkup(String markup) {
        this.markup = markup;
    }

    @Override
    public EngineType getEngine() {
        return ThemeManager.getEngineByUrl(themeUrl);
    }

    @Override
    public String getViewMode() {
        return ThemeManager.getViewModeByUrl(themeUrl);
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public void setFormat(Format format) {
        this.format = format;
    }

    @Override
    public Format getFormat() {
        return format;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public ThemeElement getTheme() {
        return Manager.getThemeManager().getThemeByUrl(themeUrl);
    }

    @Override
    public TemplateEngineType getTemplateEngine() {
        return ThemeManager.getTemplateEngineByUrl(themeUrl);
    }

}
