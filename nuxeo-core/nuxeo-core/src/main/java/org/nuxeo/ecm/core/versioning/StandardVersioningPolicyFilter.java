/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     Kevin Leturc
 *
 */

package org.nuxeo.ecm.core.versioning;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.DocumentType;

import java.util.List;

/**
 * @since 9.1
 */
public class StandardVersioningPolicyFilter implements VersioningPolicyFilter {

    protected List<String> types;

    protected List<String> facets;

    protected List<String> schemas;

    protected String condition;

    public StandardVersioningPolicyFilter(List<String> types, List<String> facets, List<String> schemas,
            String condition) {
        this.types = types;
        this.facets = facets;
        this.schemas = schemas;
        this.condition = condition;
    }

    @Override
    public boolean test(DocumentModel previousDocument, DocumentModel currentDocument) {
        if (!types.contains(currentDocument.getType())) {
            return false;
        } else {
            DocumentType docType = currentDocument.getDocumentType();
            if (!schemas.stream().anyMatch(docType::hasSchema) || !facets.stream().anyMatch(docType::hasFacet)) {
                return false;
            } else {
                // TODO Resolve EL expression
                return true;
            }
        }
    }
}
