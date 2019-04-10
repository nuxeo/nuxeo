/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Diff block definition interface.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffBlockDefinition extends Serializable {

    /**
     * Gets the diff block definition name.
     */
    String getName();

    /**
     * Returns the template to use in a given mode.
     */
    String getTemplate(String mode);

    /**
     * Returns a map of templates by mode.
     */
    Map<String, String> getTemplates();

    /**
     * Returns the list of field definitions.
     */
    List<DiffFieldDefinition> getFields();

    /**
     * Returns a map of properties to use in a given mode.
     */
    Map<String, Serializable> getProperties(String layoutMode);

    /**
     * Returns a map of properties by mode.
     */
    Map<String, Map<String, Serializable>> getProperties();
}
