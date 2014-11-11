/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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
 * @since 5.9.6
 */
public interface LayoutTypeConfiguration extends Serializable {

    /**
     * Returns the version name since the widget type has been available (5.4,
     * 5.4.2, etc...)
     */
    String getSinceVersion();

    /**
     * Returns the version name since the widget type has been deprecated (5.4,
     * 5.4.2, etc...)
     */
    String getDeprecatedVersion();

    String getTitle();

    String getDescription();

    /**
     * Returns the identifier to be used for the demo, or null if no demo is
     * available.
     */
    String getDemoId();

    /**
     * Returns true is the preview is enabled on the demo.
     */
    boolean isDemoPreviewEnabled();

    List<String> getCategories();

    List<String> getSupportedModes();

    /**
     * Returns true if widget knows how to display its label (some widget types
     * might delegate this to their containing widget or layout, usually when
     * defining form layouts).
     */
    boolean isHandlingLabels();

    /**
     * Returns true if widget will be containing forms.
     * <p>
     * Since forms cannot contains any sub forms, layouts using this widget
     * should not be surrounded by any form. Other widgets in the same layouts
     * not containing forms may then need a surrounding form that could be
     * added by the layout template.
     */
    boolean isContainingForm();

    List<LayoutDefinition> getPropertyLayouts(String mode, String additionalMode);

    Map<String, List<LayoutDefinition>> getPropertyLayouts();

    /**
     * Returns the default values for the layout type properties, by mode.
     */
    Map<String, Map<String, Serializable>> getDefaultPropertyValues();

    /**
     * Returns the default values for the layout type properties, for given
     * mode.
     */
    Map<String, Serializable> getDefaultPropertyValues(String mode);

    /**
     * Returns the list of supported controls, e.g. controls that are checked
     * on sub-widgets definitions.
     */
    List<String> getSupportedControls();

}
