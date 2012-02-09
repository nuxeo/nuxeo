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
package org.nuxeo.ecm.diff.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.service.impl.DiffDisplayDescriptor;

/**
 * Handles the configuration of a document diff display.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffDisplayService extends Serializable {

    String DEFAULT_DIFF_DISPLAY_NAME = "default";

    /**
     * Gets the contributions.
     * 
     * @return the contributions
     */
    Map<String, DiffDisplayDescriptor> getContributions();

    /**
     * Gets the diff block definitions.
     * 
     * @param diffDisplayName the diff display name
     * @return the diff block definitions
     * @throws ClientException the client exception
     */
    List<DiffBlockDefinition> getDiffBlockDefinitions(String diffDisplayName);

    /**
     * Gets the default diff block definitions.
     * 
     * @return the default diff block definitions
     * @throws ClientException the client exception
     */
    List<DiffBlockDefinition> getDefaultDiffBlockDefinitions();

    List<DiffDisplayBlock> getDiffDisplayBlocks(String diffDisplayName,
            DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException;

    List<DiffDisplayBlock> getDefaultDiffDisplayBlocks(DocumentDiff docDiff,
            DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException;

    /**
     * Apply complex items order.
     * 
     * @param schemaName the schema name
     * @param fieldName the field name
     * @param complexItems the complex items
     */
    // void applyComplexItemsOrder(String schemaName, String fieldName,
    // List<String> complexItems);

}
