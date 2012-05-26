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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;
import java.util.List;

/**
 * Diff field definition interface.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffFieldDefinition extends Serializable {

    /**
     * Optional category on the field: if this category is filled, the widget
     * instance will be looked up with this category in the store
     */
    String getCategory();

    /**
     * Gets the field schema.
     *
     * @return the field schema
     */
    String getSchema();

    /**
     * Gets the field name.
     *
     * @return the field name
     */
    String getName();

    /**
     * Checks if must display content diff links.
     *
     * @return true, if must display content diff links
     */
    boolean isDisplayContentDiffLinks();

    /**
     * Gets the field items.
     *
     * @return the field items
     */
    List<DiffFieldItemDefinition> getItems();

}
