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
package org.nuxeo.ecm.platform.diff.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Handles the display of a diff between two documents.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public interface DocumentDiffDisplayService extends Serializable {

    /**
     * Gets the contributions.
     * 
     * @return the contributions
     */
    Map<String, ComplexItemsDescriptor> getContributions();

    /**
     * Gets the complex items.
     * 
     * @param schemaName the schema name
     * @param fieldName the field name
     * @return the complex items
     */
    List<String> getComplexItems(String schemaName, String fieldName);

    /**
     * Apply complex items order.
     * 
     * @param schemaName the schema name
     * @param fieldName the field name
     * @param complexItems the complex items
     */
    void applyComplexItemsOrder(String schemaName, String fieldName,
            List<String> complexItems);

}
