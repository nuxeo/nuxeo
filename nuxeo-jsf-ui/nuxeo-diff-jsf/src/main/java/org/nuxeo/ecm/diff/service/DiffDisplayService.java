/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    List<DiffDisplayBlock> getDiffDisplayBlocks(DocumentDiff docDiff, DocumentModel leftDoc, DocumentModel rightDoc);

}
