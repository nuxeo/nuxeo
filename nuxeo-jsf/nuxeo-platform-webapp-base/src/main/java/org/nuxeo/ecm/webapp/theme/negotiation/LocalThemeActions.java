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

import java.util.List;

import javax.faces.model.SelectItem;

public interface LocalThemeActions {

    void init();

    String getTheme();

    void setTheme(String theme);

    String getPage();

    void setPage(String page);

    String getEngine();

    void setEngine(String engine);

    String getMode();

    void setMode(String mode);

    String getPerspective();

    void setPerspective(String perspective);

    List<SelectItem> getAvailableThemes();

    List<SelectItem> getAvailablePages();

    List<SelectItem> getAvailablePerspectives();

    List<SelectItem> getAvailableEngines();

    void save();

    void delete();

    boolean isConfigured();

}
