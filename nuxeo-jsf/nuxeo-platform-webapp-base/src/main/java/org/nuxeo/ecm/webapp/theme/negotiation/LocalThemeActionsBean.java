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

package org.nuxeo.ecm.webapp.theme.negotiation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.perspectives.PerspectiveType;

@Name("localThemeActions")
@Scope(ScopeType.PAGE)
public class LocalThemeActionsBean implements LocalThemeActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LocalThemeActionsBean.class);

    @In(required = false, create=true)
    protected DocumentModel currentSuperSpace;

    protected String theme;

    protected String page;

    private String perspective;

    private String engine;

    private String mode;


    @Create
    public void init() {
        if (currentSuperSpace != null) {
            LocalThemeConfig config = LocalThemeHelper.getLocalThemeConfig(currentSuperSpace);
            if (config != null) {
                theme = config.getTheme();
                page = config.getPage();
                perspective = config.getPerspective();
                engine = config.getEngine();
                mode = config.getMode();
            }
        }
    }

    public String getTheme() {
        return theme;
    }

    public String getPage() {
        return page;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public List<SelectItem> getAvailableThemes() {
        List<SelectItem> themes = new ArrayList<SelectItem>();
        for (String theme : Manager.getThemeManager().getThemeNames("jsf-facelets")) {
            themes.add(new SelectItem(theme, theme));
        }
        return themes;
    }

    public List<SelectItem> getAvailablePages() {
        List<SelectItem> pages = new ArrayList<SelectItem>();
        if (theme != null && !theme.equals("")) {
            for (String pageName : Manager.getThemeManager().getPageNames(theme)) {
                pages.add(new SelectItem(pageName, pageName));
            }
        }
        return pages;
    }

    public List<SelectItem> getAvailablePerspectives() {
        List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().listPerspectives()) {
            selectItemList.add(new SelectItem(perspectiveType.name,
                    perspectiveType.title));
        }
        return selectItemList;
    }

    public List<SelectItem> getAvailableEngines() {
        List<SelectItem> engines = new ArrayList<SelectItem>();
        // TODO
        return engines;
    }

    public void save() {
        if (currentSuperSpace == null) {
            log.error("Could not find the current space.");
            return;
        }
        LocalThemeHelper.setLocalThemeConfig(theme, page, perspective, engine,
                mode, currentSuperSpace);
    }

    public void delete() {
        if (currentSuperSpace == null) {
            log.error("Could not find the current space.");
            return;
        }
        LocalThemeHelper.removeLocalThemeConfig(currentSuperSpace);
        theme = null;
        page = null;
        perspective = null;
        engine = null;
        mode = null;
    }

    public boolean isConfigured() {
        if (currentSuperSpace != null) {
            LocalThemeConfig config = LocalThemeHelper.getLocalThemeConfig(currentSuperSpace);
            if (config != null) {
                return true;
            }
        }
        return false;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPerspective() {
        return perspective;
    }

    public void setPerspective(String perspective) {
        this.perspective = perspective;
    }

}
