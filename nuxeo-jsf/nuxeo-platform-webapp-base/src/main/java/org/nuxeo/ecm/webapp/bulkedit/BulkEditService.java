/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service handling bulk edition of documents.
 *
 * @since 5.7.3
 */
public interface BulkEditService {

    String BULK_EDIT_PREFIX = "bulkEdit/";

    String CONTEXT_DATA = "contextData";

    /**
     * Copy all the marked properties (stored in the ContextData of {@code sourceDoc}) from {@code sourceDoc} to all the
     * {@code targetDocs}.
     *
     * @param session the {@code CoreSession} to use
     * @param sourceDoc the doc where to get the properties to copy
     * @param targetDocs the docs where to set the properties
     */
    void updateDocuments(CoreSession session, DocumentModel sourceDoc, List<DocumentModel> targetDocs);

}
