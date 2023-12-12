/*
 * (C) Copyright 2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *  Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.elasticsearch.test.rest;

import java.util.Collections;
import java.util.Comparator;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Test operation to validate a {@link DocumentModelList} input composed of documents from multiple repositories.
 *
 * @since 2023.5
 */
@Operation(id = MultiRepositoryDummyOperation.ID)
public class MultiRepositoryDummyOperation {

    public static final String ID = "MultiRepositoryDummyOperation";

    protected DocumentTitleComparator comparator = new DocumentTitleComparator();

    @OperationMethod
    public DocumentModelList run(DocumentModelList documents) {
        Collections.sort(documents, comparator);
        return documents;
    }

    protected class DocumentTitleComparator implements Comparator<DocumentModel> {

        @Override
        public int compare(DocumentModel o1, DocumentModel o2) {
            return o1.getTitle().compareTo(o2.getTitle());
        }

    }

}
