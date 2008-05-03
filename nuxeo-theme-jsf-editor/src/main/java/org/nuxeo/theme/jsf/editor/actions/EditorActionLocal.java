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

package org.nuxeo.theme.jsf.editor.actions;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.ejb.Remove;

import org.jboss.seam.annotations.WebRemote;

@Local
public interface EditorActionLocal {

    @WebRemote
    String moveElement(String srcId, String destId, Integer order);

    @WebRemote
    List<String> copyElements(List<String> ids);

    @WebRemote
    List<String> pasteElements(String destId);

    @WebRemote
    String duplicateElement(String id);

    @WebRemote
    String selectElement(String id);

    @WebRemote
    String deleteElement(String id);

    @WebRemote
    void clearSelections();

    @WebRemote
    String insertFragment(String typeName, String destId, Integer order);

    @WebRemote
    String expireThemes();

    /* Clipboard */
    @WebRemote
    void addElementToClipboard(String id);

    @WebRemote
    void clearClipboard();

    @WebRemote
    void updateElementWidget(String id, String viewName);

    @WebRemote
    void setElementVisibility(String id, List<String> perspectives,
            boolean alwaysVisible);

    @WebRemote
    void updateElementProperties(String id, Map<Object, Object> propertyMap);

    @WebRemote
    void updateElementStyle(String id, String viewName, String path,
            Map<Object, Object> propertyMap);

    @WebRemote
    void updateElementStyleCss(String id, String viewName, String cssSource);

    @WebRemote
    void updateElementLayout(final Map<Object, Object> propertyMap);

    @WebRemote
    void setSize(String id, String width);

    @WebRemote
    void splitElement(String id);

    @WebRemote
    void insertSectionAfter(String id);

    @WebRemote
    void alignElement(String id, String position);

    @WebRemote
    String addTheme(String name);

    @WebRemote
    String addPage(String path);

    @WebRemote
    void setCurrentStyleSelector(String currentStyleSelector);

    @WebRemote
    void setStyleEditMode(String styleEditMode);

    @WebRemote
    void setStylePropertyCategory(String category);

    @WebRemote
    void setStyleCategory(String category);

    @WebRemote
    void setPresetGroup(String group);

    @WebRemote
    void createStyle();

    @WebRemote
    void assignStyleProperty(String id, String property, String value);

    @WebRemote
    void makeElementUseNamedStyle(String id, String inheritedName,
            String currentThemeName);

    @WebRemote
    void createNamedStyle(String id, String styleName, String currentThemeName);

    @WebRemote
    void deleteNamedStyle(String id, String styleName, String themeName);

    @WebRemote
    void setCurrentStyleLayer(Integer uid);

    @WebRemote
    boolean repairTheme(String themeName);

    @WebRemote
    boolean loadTheme(String src);

    @WebRemote
    boolean saveTheme(String src, int indent);

    @WebRemote
    String renderCssPreview(String cssPreviewId);

    @Remove
    void destroy();

}
