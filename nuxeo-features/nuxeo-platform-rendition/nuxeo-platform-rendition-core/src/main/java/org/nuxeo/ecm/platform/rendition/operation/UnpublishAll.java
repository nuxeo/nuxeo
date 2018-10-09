/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.operation;

import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Unpublish all publications of the document.
 *
 * @since 10.3
 */
@Operation(id = UnpublishAll.ID, category = Constants.CAT_DOCUMENT, label = "Unpublish Document's Publications", description = "Unpublish all publications of the input document..")
public class UnpublishAll {

    public static final String ID = "Document.UnpublishAll";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run(DocumentModel doc) {
        String escapedId = NXQL.escapeString(doc.getId());
        session.queryProjection(
                String.format(org.nuxeo.ecm.platform.rendition.Constants.ALL_PUBLICATION_QUERY, escapedId, escapedId),
                0, 0)
               .stream()
               .map(publication -> new IdRef(publication.get(ECM_UUID).toString()))
               .forEach(session::removeDocument);
        // XXX in case of published renditions, do we want to clean up the placeless stored rendition?
        // AFAIK, if a document is published a couple of time with the same rendition,
        // there's only one placeless stored rendition. So it may be worth keeping it
    }

}
