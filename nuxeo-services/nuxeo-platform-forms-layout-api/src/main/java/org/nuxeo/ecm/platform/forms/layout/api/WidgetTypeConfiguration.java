/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Widget configuration interface
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface WidgetTypeConfiguration extends Serializable {

    /**
     * Returns the version name since the widget type has been available (5.4,
     * 5.4.2, etc...)
     */
    String getSinceVersion();

    /**
     * Returns the version name since the widget type has been deprecated (5.4,
     * 5.4.2, etc...)
     *
     * @since 5.6
     */
    String getDeprecatedVersion();

    String getTitle();

    String getDescription();

    /**
     * Returns the identifier to be used for the demo, or null if no demo is
     * available.
     *
     * @since 5.4.2
     */
    String getDemoId();

    /**
     * Returns true is the preview is enabled on the demo.
     *
     * @since 5.4.2
     */
    boolean isDemoPreviewEnabled();

    List<String> getCategories();

    List<String> getSupportedModes();

    boolean isAcceptingSubWidgets();

    /**
     * Returns true if widget knows how to display its label (some widget types
     * might delegate this to their containing widget or layout, usually when
     * defining form layouts).
     *
     * @since 5.6
     */
    boolean isHandlingLabels();

    boolean isList();

    boolean isComplex();

    /**
     * Returns true if widget will be containing forms.
     * <p>
     * Since forms cannot contains any sub forms, layouts using this widget
     * should not be surrounded by any form. Other widgets in the same layouts
     * not containing forms may then need a surrounding form that could be
     * added by the layout template.
     *
     * @since 5.6
     */
    boolean isContainingForm();

    List<String> getSupportedFieldTypes();

    List<String> getDefaultFieldTypes();

    List<FieldDefinition> getDefaultFieldDefinitions();

    /**
     * Returns configuration properties.
     *
     * @since 5.4.2
     */
    Map<String, Serializable> getConfProperties();

    Serializable getConfProperty(String propName);

    List<LayoutDefinition> getPropertyLayouts(String mode, String additionalMode);

    Map<String, List<LayoutDefinition>> getPropertyLayouts();

    /**
     * Returns the list of layouts for given mode and additional modes.
     * <p>
     * These layouts are used to document accepted fields on the widget type,
     * depending on the rendering mode.
     *
     * @since 5.7.3
     * @param mode the mode to retrieve layouts for.
     * @param additionalMode additional mode to take into account, typically
     *            {@link BuiltinModes#ANY}
     */
    List<LayoutDefinition> getFieldLayouts(String mode, String additionalMode);

    /**
     * Returns the map of field layouts per mode.
     *
     * @since 5.7.3
     * @see #getFieldLayouts(String, String)
     */
    Map<String, List<LayoutDefinition>> getFieldLayouts();

    /**
     * Returns the default values for the widget type properties, by mode.
     *
     * @since 5.7.3
     */
    Map<String, Map<String, Serializable>> getDefaultPropertyValues();

    /**
     * Returns the default values for the widget type properties, for given
     * mode.
     *
     * @since 5.7.3
     */
    Map<String, Serializable> getDefaultPropertyValues(String mode);

    /**
     * @since 6.0
     */
    Map<String, Map<String, Serializable>> getDefaultControlValues();

    /**
     * @since 6.0
     */
    Map<String, Serializable> getDefaultControlValues(String mode);

    /**
     * Returns the list of supported controls, e.g. controls that are checked
     * on sub-widgets definitions.
     *
     * @since 5.9.1
     */
    List<String> getSupportedControls();

}
