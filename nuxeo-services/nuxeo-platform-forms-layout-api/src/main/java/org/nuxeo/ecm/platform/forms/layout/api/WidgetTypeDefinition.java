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
 * Widget type definition
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface WidgetTypeDefinition extends Serializable {

    String getName();

    /**
     * Return alias names for this widget type definition (useful for
     * compatibility on old widget type names).
     *
     * @since 6.0
     */
    List<String> getAliases();

    String getHandlerClassName();

    Map<String, String> getProperties();

    WidgetTypeConfiguration getConfiguration();

}
