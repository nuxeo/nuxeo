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

package org.nuxeo.theme.editor.managers;

import java.util.List;

import javax.ejb.Local;
import javax.faces.model.SelectItem;

import org.nuxeo.theme.editor.managers.UiManager.FieldProperty;
import org.nuxeo.theme.editor.managers.UiManager.FragmentInfo;
import org.nuxeo.theme.editor.managers.UiManager.PaddingInfo;
import org.nuxeo.theme.editor.managers.UiManager.PageInfo;
import org.nuxeo.theme.editor.managers.UiManager.PresetInfo;
import org.nuxeo.theme.editor.managers.UiManager.StyleCategory;
import org.nuxeo.theme.editor.managers.UiManager.StyleFieldProperty;
import org.nuxeo.theme.editor.managers.UiManager.StyleLayer;
import org.nuxeo.theme.editor.managers.UiManager.ThemeInfo;
import org.nuxeo.theme.formats.styles.Style;
import org.nuxeo.theme.formats.widgets.Widget;
import org.nuxeo.theme.themes.ThemeDescriptor;

@Local
public interface UiManagerLocal {

    String getCurrentPagePath();

    String getCurrentThemeName();

    String startEditor();

    List<FragmentInfo> getAvailableFragments();

    List<SelectItem> getAvailablePerspectives();

    List<String> getPerspectivesOfSelectedElement();

    boolean isSelectedElementAlwaysVisible();

    List<ThemeDescriptor> getThemesDescriptors();

    List<ThemeInfo> getAvailableThemes();

    List<PageInfo> getAvailablePages();

    List<FieldProperty> getElementProperties();

    List<SelectItem> getAvailablePresetGroupsForSelectedCategory();

    List<PresetInfo> getPresetsForCurrentGroup();

    List<StyleFieldProperty> getElementStyleProperties();

    String getRenderedElementStyleProperties();

    List<SelectItem> getAvailableViewNamesForSelectedElement();

    List<SelectItem> getAvailableStyleSelectorsForSelectedElement();

    Style getStyleOfSelectedElement();

    List<SelectItem> getAvailableStyleProperties();

    List<StyleCategory> getStyleCategories();

    List<SelectItem> getAvailableNamedStyles();

    String getInheritedStyleNameOfSelectedElement();

    Widget getWidgetOfSelectedElement();

    List<StyleLayer> getStyleLayersOfSelectedElement();

    String getCurrentViewName();

    PaddingInfo getPaddingOfSelectedElement();

}
