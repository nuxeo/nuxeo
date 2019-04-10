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
package org.nuxeo.ecm.diff.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffComplexFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;

/**
 * Handles the configuration of a document diff display.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public interface DiffDisplayService extends Serializable {

    Map<String, List<String>> getDiffExcludedSchemas();

    List<String> getDiffExcludedFields(String schemaName);

    List<DiffComplexFieldDefinition> getDiffComplexFields();

    DiffComplexFieldDefinition getDiffComplexField(String schemaName, String fieldName);

    Map<String, List<String>> getDiffDisplays();

    List<String> getDiffDisplay(String docType);

    Map<String, DiffBlockDefinition> getDiffBlockDefinitions();

    DiffBlockDefinition getDiffBlockDefinition(String name);

    List<DiffDisplayBlock> getDiffDisplayBlocks(DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc)
            throws ClientException;
}