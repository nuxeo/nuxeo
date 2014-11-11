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

    String getSinceVersion();

    String getTitle();

    String getDescription();

    List<String> getCategories();

    List<String> getSupportedModes();

    boolean isAcceptingSubWidgets();

    boolean isList();

    boolean isComplex();

    List<String> getSupportedFieldTypes();

    List<String> getDefaultFieldTypes();

    List<FieldDefinition> getDefaultFieldDefinitions();

    /**
     * Returns configuration properties.
     *
     * @since 5.4.1
     */
    Map<String, Serializable> getConfProperties();

    Serializable getConfProperty(String propName);

    List<LayoutDefinition> getPropertyLayouts(String mode, String additionalMode);

    Map<String, List<LayoutDefinition>> getPropertyLayouts();
}
