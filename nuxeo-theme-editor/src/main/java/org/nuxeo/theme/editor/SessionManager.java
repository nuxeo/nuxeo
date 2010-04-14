/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.editor;

import java.util.List;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.session.AbstractComponent;
import org.nuxeo.ecm.webengine.session.UserSession;

public class SessionManager extends AbstractComponent {

    private static final long serialVersionUID = 1L;

    private static final String SELECTED_ELEMENT_ID = "org.nuxeo.theme.editor.selected_element";

    private static final String STYLE_EDIT_MODE = "org.nuxeo.theme.editor.style_edit_mode";

    private static final String STYLE_LAYER_ID = "org.nuxeo.theme.editor.style_layer";

    private static final String NAMED_STYLE_ID = "org.nuxeo.theme.editor.named_style";

    private static final String STYLE_SELECTOR = "org.nuxeo.theme.editor.style_selector";

    private static final String STYLE_PROPERTY_CATEGORY = "org.nuxeo.theme.editor.style_property_category";

    private static final String STYLE_CATEGORY = "org.nuxeo.theme.editor.style_category";

    private static final String STYLE_MANAGER_MODE = "org.nuxeo.theme.editor.style_manager_mode";

    private static final String PRESET_MANAGER_MODE = "org.nuxeo.theme.editor.preset_manager_mode";

    private static final String PRESET_GROUP = "org.nuxeo.theme.editor.preset_group";

    private static final String PRESET_CATEGORY = "org.nuxeo.theme.editor.preset_category";

    private static final String CLIPBOARD_ELEMENT_ID = "org.nuxeo.theme.editor.clipboard_element";

    private static final String CLIPBOARD_PRESET_ID = "org.nuxeo.theme.editor.clipboard_preset";

    private static final String SELECTED_FRAGMENT_TYPE = "org.nuxeo.theme.editor.fragment_type";

    private static final String SELECTED_FRAGMENT_VIEW = "org.nuxeo.theme.editor.fragment_view";

    private static final String SELECTED_FRAGMENT_STYLE = "org.nuxeo.theme.editor.fragment_style";

    private static final String WORKSPACE_THEMES = "org.nuxeo.theme.editor.workspace_themes";

    private static final String UNDO_BUFFER = "org.nuxeo.theme.editor.undo_buffer";

    private static UserSession getUserSession() {
        return WebEngine.getActiveContext().getUserSession();
    }

    public static synchronized void setElementId(String id) {
        getUserSession().put(SELECTED_ELEMENT_ID, id);
    }

    public static synchronized String getElementId() {
        return (String) getUserSession().get(SELECTED_ELEMENT_ID);
    }

    public static synchronized String getStyleEditMode() {
        return (String) getUserSession().get(STYLE_EDIT_MODE);
    }

    public static synchronized void setStyleEditMode(String mode) {
        getUserSession().put(STYLE_EDIT_MODE, mode);
    }

    public static synchronized String getStyleLayerId() {
        return (String) getUserSession().get(STYLE_LAYER_ID);
    }

    public static synchronized void setStyleLayerId(String id) {
        getUserSession().put(STYLE_LAYER_ID, id);
    }

    public static synchronized String getNamedStyleId() {
        return (String) getUserSession().get(NAMED_STYLE_ID);
    }

    public static synchronized void setNamedStyleId(String id) {
        getUserSession().put(NAMED_STYLE_ID, id);
    }

    public static synchronized String getStyleSelector() {
        return (String) getUserSession().get(STYLE_SELECTOR);
    }

    public static synchronized void setStyleSelector(String selector) {
        getUserSession().put(STYLE_SELECTOR, selector);
    }

    public static synchronized String getStylePropertyCategory() {
        return (String) getUserSession().get(STYLE_PROPERTY_CATEGORY);
    }

    public static synchronized void setStylePropertyCategory(String category) {
        getUserSession().put(STYLE_PROPERTY_CATEGORY, category);
    }

    public static synchronized String getStyleCategory() {
        return (String) getUserSession().get(STYLE_CATEGORY);
    }

    public static synchronized void setStyleCategory(String category) {
        getUserSession().put(STYLE_CATEGORY, category);
    }

    public static synchronized String getStyleManagerMode() {
        return (String) getUserSession().get(STYLE_MANAGER_MODE);
    }

    public static synchronized void setStyleManagerMode(String mode) {
        getUserSession().put(STYLE_MANAGER_MODE, mode);
    }

    public static synchronized String getPresetManagerMode() {
        return (String) getUserSession().get(PRESET_MANAGER_MODE);
    }

    public static synchronized void setPresetManagerMode(String mode) {
        getUserSession().put(PRESET_MANAGER_MODE, mode);
    }

    public static synchronized String getPresetGroup() {
        return (String) getUserSession().get(PRESET_GROUP);
    }

    public static synchronized void setPresetGroup(String group) {
        getUserSession().put(PRESET_GROUP, group);
    }

    public static synchronized String getPresetCategory() {
        return (String) getUserSession().get(PRESET_CATEGORY);
    }

    public static synchronized void setPresetCategory(String category) {
        getUserSession().put(PRESET_CATEGORY, category);
    }

    public static synchronized String getClipboardElementId() {
        return (String) getUserSession().get(CLIPBOARD_ELEMENT_ID);
    }

    public static synchronized void setClipboardElementId(String id) {
        getUserSession().put(CLIPBOARD_ELEMENT_ID, id);
    }

    public static synchronized void setClipboardPresetId(String id) {
        getUserSession().put(CLIPBOARD_PRESET_ID, id);
    }

    public static synchronized String getClipboardPresetId() {
        return (String) getUserSession().get(CLIPBOARD_PRESET_ID);
    }

    public static synchronized void setFragmentType(String type) {
        getUserSession().put(SELECTED_FRAGMENT_TYPE, type);
    }

    public static synchronized String getFragmentType() {
        return (String) getUserSession().get(SELECTED_FRAGMENT_TYPE);
    }

    public static synchronized void setFragmentView(String view) {
        getUserSession().put(SELECTED_FRAGMENT_VIEW, view);
    }

    public static synchronized String getFragmentView() {
        return (String) getUserSession().get(SELECTED_FRAGMENT_VIEW);
    }

    public static synchronized void setFragmentStyle(String style) {
        getUserSession().put(SELECTED_FRAGMENT_STYLE, style);
    }

    public static synchronized String getFragmentStyle() {
        return (String) getUserSession().get(SELECTED_FRAGMENT_STYLE);
    }

    @SuppressWarnings("unchecked")
    public static synchronized List<ThemeInfo> getWorkspaceThemes() {
        return (List<ThemeInfo>) getUserSession().get(WORKSPACE_THEMES);
    }

    public static synchronized void setWorkspaceThemes(List<ThemeInfo> themes) {
        getUserSession().put(WORKSPACE_THEMES, themes);
    }

    public static synchronized UndoBuffer getUndoBuffer(final String themeName) {
        return (UndoBuffer) getUserSession().get(
                String.format("%s.%s", UNDO_BUFFER, themeName));
    }

    public static synchronized void setUndoBuffer(final String themeName,
            UndoBuffer undoBuffer) {
        getUserSession().put(String.format("%s.%s", UNDO_BUFFER, themeName),
                undoBuffer);
    }

}
