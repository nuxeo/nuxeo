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

import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remove;

import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.perspectives.PerspectiveType;

@Local
public interface UiStatesLocal {

    List<String> getClipboardElements();

    void setClipboardElements(List<String> elements);

    Element getSelectedElement();

    void setSelectedElement(Element element);

    PerspectiveType getCurrentPerspective();

    String getCurrentViewMode();

    ThemeElement getCurrentTheme();

    PageElement getCurrentPage();

    String getApplicationPath();

    void setApplicationPath(String path);

    String getCurrentStyleSelector();

    void setCurrentStyleSelector(String selector);

    Style getCurrentStyleLayer();

    void setCurrentStyleLayer(Style layer);

    String getStyleCategory();

    void setStyleCategory(String category);

    String getPresetGroup();

    void setPresetGroup(String group);

    String getStyleEditMode();

    void setStyleEditMode(String mode);

    String getStylePropertyCategory();

    void setStylePropertyCategory(String category);

    @Remove
    void destroy();

}
