/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.theme.negotiation;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.nuxeo.ecm.platform.ec.placeful.Annotation;

/**
 * Local theme configuration entity. Associates a theme and page to a document
 * id.
 *
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 * @see org.nuxeo.theme.themes.ThemeManager#getPageByPath()
 */
@Entity
public class LocalThemeConfig extends Annotation {

    public static final String LOCAL_THEME_NAME = "LocalThemeConfig";

    private static final long serialVersionUID = 1L;

    private int localThemeId;

    private String theme;

    private String page;

    private String perspective;

    private String engine;

    private String mode;

    private String docId;


    public LocalThemeConfig() {
        this(null, null, null, null, null, null);
    }

    public LocalThemeConfig(String theme, String page, String perspective,
            String engine, String mode, String docId) {
        this.theme = theme;
        this.page = page;
        this.perspective = perspective;
        this.engine = engine;
        this.mode = mode;
        this.docId = docId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return localThemeId;
    }

    public void setId(int id) {
        localThemeId = id;
    }

    public String computePagePath() {
        if (theme == null || page == null) {
            return null;
        }
        return String.format("%s/%s", theme, page);
    }

    // Properties
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    @Column(name="themode") /* mode is a reserved word for Oracle */
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
