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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.webengine.WebEngine;

public class SessionManager {

    private static final String SELECTED_ELEMENT_ID = "org.nuxeo.theme.editor.selected_element";

    private static final String STYLE_EDIT_MODE = "org.nuxeo.theme.editor.style_edit_mode";

    private static final String STYLE_LAYER_ID = "org.nuxeo.theme.editor.style_layer";

    private static final String NAMED_STYLE_ID = "org.nuxeo.theme.editor.named_style";

    private static final String STYLE_SELECTOR = "org.nuxeo.theme.editor.style_selector";

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

    private static final String SELECTED_CSS_CATEGORIES = "org.nuxeo.theme.editor.css_categories";

    private static final String SELECTED_RESOURCE_BANK = "org.nuxeo.theme.editor.resource_bank";

    private static final String WORKSPACE_THEME_NAMES = "org.nuxeo.theme.editor.workspace_theme_names";

    private static final String UNDO_BUFFER = "org.nuxeo.theme.editor.undo_buffer";

    private static final String SELECTED_EDIT_FIELD = "org.nuxeo.theme.editor.edit_field";

    private static final String SELECTED_BANK_COLLECTION = "org.nuxeo.theme.editor.selected_bank_collection";

    private static HttpSession getHttpSession() {
        return WebEngine.getActiveContext().getRequest().getSession();
    }

    public static synchronized void setElementId(String id) {
        getHttpSession().setAttribute(SELECTED_ELEMENT_ID, id);
    }

    public static synchronized String getElementId() {
        return (String) getHttpSession().getAttribute(SELECTED_ELEMENT_ID);
    }

    public static synchronized String getStyleEditMode() {
        return (String) getHttpSession().getAttribute(STYLE_EDIT_MODE);
    }

    public static synchronized void setStyleEditMode(String mode) {
        getHttpSession().setAttribute(STYLE_EDIT_MODE, mode);
    }

    public static synchronized String getStyleLayerId() {
        return (String) getHttpSession().getAttribute(STYLE_LAYER_ID);
    }

    public static synchronized void setStyleLayerId(String id) {
        getHttpSession().setAttribute(STYLE_LAYER_ID, id);
    }

    public static synchronized String getNamedStyleId() {
        return (String) getHttpSession().getAttribute(NAMED_STYLE_ID);
    }

    public static synchronized void setNamedStyleId(String id) {
        getHttpSession().setAttribute(NAMED_STYLE_ID, id);
    }

    public static synchronized String getStyleSelector() {
        return (String) getHttpSession().getAttribute(STYLE_SELECTOR);
    }

    public static synchronized void setStyleSelector(String selector) {
        getHttpSession().setAttribute(STYLE_SELECTOR, selector);
    }

    public static synchronized String getStyleCategory() {
        return (String) getHttpSession().getAttribute(STYLE_CATEGORY);
    }

    public static synchronized void setStyleCategory(String category) {
        getHttpSession().setAttribute(STYLE_CATEGORY, category);
    }

    public static synchronized String getStyleManagerMode() {
        return (String) getHttpSession().getAttribute(STYLE_MANAGER_MODE);
    }

    public static synchronized void setStyleManagerMode(String mode) {
        getHttpSession().setAttribute(STYLE_MANAGER_MODE, mode);
    }

    public static synchronized String getPresetManagerMode() {
        return (String) getHttpSession().getAttribute(PRESET_MANAGER_MODE);
    }

    public static synchronized void setPresetManagerMode(String mode) {
        getHttpSession().setAttribute(PRESET_MANAGER_MODE, mode);
    }

    public static synchronized String getPresetGroup() {
        return (String) getHttpSession().getAttribute(PRESET_GROUP);
    }

    public static synchronized void setPresetGroup(String group) {
        getHttpSession().setAttribute(PRESET_GROUP, group);
    }

    public static synchronized String getPresetCategory() {
        return (String) getHttpSession().getAttribute(PRESET_CATEGORY);
    }

    public static synchronized void setPresetCategory(String category) {
        getHttpSession().setAttribute(PRESET_CATEGORY, category);
    }

    public static synchronized String getClipboardElementId() {
        return (String) getHttpSession().getAttribute(CLIPBOARD_ELEMENT_ID);
    }

    public static synchronized void setClipboardElementId(String id) {
        getHttpSession().setAttribute(CLIPBOARD_ELEMENT_ID, id);
    }

    public static synchronized void setClipboardPresetId(String id) {
        getHttpSession().setAttribute(CLIPBOARD_PRESET_ID, id);
    }

    public static synchronized String getClipboardPresetId() {
        return (String) getHttpSession().getAttribute(CLIPBOARD_PRESET_ID);
    }

    public static synchronized void setFragmentType(String type) {
        getHttpSession().setAttribute(SELECTED_FRAGMENT_TYPE, type);
    }

    public static synchronized String getFragmentType() {
        return (String) getHttpSession().getAttribute(SELECTED_FRAGMENT_TYPE);
    }

    public static synchronized void setFragmentView(String view) {
        getHttpSession().setAttribute(SELECTED_FRAGMENT_VIEW, view);
    }

    public static synchronized String getFragmentView() {
        return (String) getHttpSession().getAttribute(SELECTED_FRAGMENT_VIEW);
    }

    public static synchronized void setFragmentStyle(String style) {
        getHttpSession().setAttribute(SELECTED_FRAGMENT_STYLE, style);
    }

    public static synchronized String getFragmentStyle() {
        return (String) getHttpSession().getAttribute(SELECTED_FRAGMENT_STYLE);
    }

    @SuppressWarnings("unchecked")
    public static synchronized Set<String> getWorkspaceThemeNames() {
        Set<String> themes = (Set<String>) getHttpSession().getAttribute(
                WORKSPACE_THEME_NAMES);
        if (themes == null) {
            themes = new LinkedHashSet<String>();
        }
        return themes;
    }

    public static synchronized void setWorkspaceThemeNames(Set<String> themes) {
        getHttpSession().setAttribute(WORKSPACE_THEME_NAMES, themes);
    }

    public static synchronized UndoBuffer getUndoBuffer(final String themeName) {
        return (UndoBuffer) getHttpSession().getAttribute(
                String.format("%s.%s", UNDO_BUFFER, themeName));
    }

    public static synchronized void setUndoBuffer(final String themeName,
            UndoBuffer undoBuffer) {
        getHttpSession().setAttribute(
                String.format("%s.%s", UNDO_BUFFER, themeName), undoBuffer);
    }

    @SuppressWarnings("unchecked")
    public static synchronized List<String> getSelectedCssCategories() {
        List<String> categories = (List<String>) getHttpSession().getAttribute(
                SELECTED_CSS_CATEGORIES);
        if (categories == null) {
            categories = new ArrayList<String>();
        }
        return categories;
    }

    public static synchronized void toggleCssCategory(String name) {
        List<String> categories = getSelectedCssCategories();
        if (categories.contains(name)) {
            categories.remove(name);
        } else {
            categories.add(name);
        }
        setSelectedCssCategories(categories);
    }

    public static synchronized void setSelectedCssCategories(
            List<String> categories) {
        getHttpSession().setAttribute(SELECTED_CSS_CATEGORIES, categories);
    }

    public static synchronized String getSelectedEditField() {
        return (String) getHttpSession().getAttribute(SELECTED_EDIT_FIELD);
    }

    public static synchronized void setSelectedEditField(String fieldName) {
        getHttpSession().setAttribute(SELECTED_EDIT_FIELD, fieldName);
    }

    public static void setSelectedBankCollection(String collection) {
        getHttpSession().setAttribute(SELECTED_BANK_COLLECTION, collection);
    }

    public static String getSelectedBankCollection() {
        return (String) getHttpSession().getAttribute(SELECTED_BANK_COLLECTION);
    }

    public static void setSelectedResourceBank(String bankName) {
        getHttpSession().setAttribute(SELECTED_RESOURCE_BANK, bankName);
    }

    public static String getSelectedResourceBank() {
        return (String) getHttpSession().getAttribute(SELECTED_RESOURCE_BANK);
    }

}
