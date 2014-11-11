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

package org.nuxeo.theme.jsf.editor.states;

import static org.jboss.seam.ScopeType.SESSION;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.jsf.negotiation.CookieManager;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;

@Stateful
@Name("nxthemesUiStates")
@Scope(SESSION)
public class UiStates implements UiStatesLocal {

    private static final Log log = LogFactory.getLog(UiStates.class);

    private List<String> clipboardElements = new ArrayList<String>();

    private Element selectedElement;

    private String currentStyleSelector;

    private String styleCategory;

    private String presetGroup;

    private String styleEditMode;

    private String stylePropertyCategory = "";

    private Style styleLayer;

    private String applicationPath = "";

    public List<String> getClipboardElements() {
        return clipboardElements;
    }

    public void setClipboardElements(final List<String> elements) {
        clipboardElements = elements;
    }

    public Element getSelectedElement() {
        return selectedElement;
    }

    public void setSelectedElement(final Element element) {
        selectedElement = element;
    }

    public Style getCurrentStyleLayer() {
        return styleLayer;
    }

    public void setCurrentStyleLayer(final Style layer) {
        styleLayer = layer;
    }

    public PerspectiveType getCurrentPerspective() {
        final String perspectiveName = CookieManager.getCookie(
                "nxthemes.perspective",
                FacesContext.getCurrentInstance().getExternalContext());
        if (perspectiveName == null) {
            return null;
        }
        return PerspectiveManager.getPerspectiveByName(
                perspectiveName);
    }

    public String getCurrentViewMode() {
        String viewMode = CookieManager.getCookie("nxthemes.mode",
                FacesContext.getCurrentInstance().getExternalContext());
        if (viewMode == null) {
            viewMode = "wysiwyg";
        }
        return viewMode;
    }

    public ThemeElement getCurrentTheme() {
        final String themeCookie = CookieManager.getCookie("nxthemes.theme",
                FacesContext.getCurrentInstance().getExternalContext());
        String themeName = "default";
        if (themeCookie != null) {
            themeName = themeCookie.split("/")[0];
        }
        return Manager.getThemeManager().getThemeByName(themeName);
    }

    public PageElement getCurrentPage() {
        String themeCookie = CookieManager.getCookie("nxthemes.theme",
                FacesContext.getCurrentInstance().getExternalContext());
        String pagePath = "default/default";
        if (themeCookie != null) {
            pagePath = themeCookie;
        }
        return Manager.getThemeManager().getPageByPath(pagePath);
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(final String path) {
        applicationPath = path;
    }

    /* style editor */
    public String getCurrentStyleSelector() {
        return currentStyleSelector;
    }

    public void setCurrentStyleSelector(final String selector) {
        currentStyleSelector = selector;
    }

    public String getStyleCategory() {
        return styleCategory;
    }

    public void setStyleCategory(final String category) {
        styleCategory = category;
    }

    public String getPresetGroup() {
        return presetGroup;
    }

    public void setPresetGroup(final String group) {
        presetGroup = group;
    }

    public String getStyleEditMode() {
        return styleEditMode;
    }

    public void setStyleEditMode(final String mode) {
        styleEditMode = mode;
    }

    public String getStylePropertyCategory() {
        return stylePropertyCategory;
    }

    public void setStylePropertyCategory(final String category) {
        stylePropertyCategory = category;
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removed SEAM component: nxthemesUiStates");
    }

}
