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
 * Diff complex field definition interface.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public interface DiffComplexFieldDefinition extends Serializable {

    /**
     * Gets the complex field schema.
     */
    String getSchema();

    /**
     * Gets the complex field name.
     */
    String getName();

    /**
     * Gets the complex field included items.
     */
    List<DiffFieldItemDefinition> getIncludedItems();

    /**
     * Gets the complex field excluded items.
     */
    List<DiffFieldItemDefinition> getExcludedItems();

}
