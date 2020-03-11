/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.publisher;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SCHEMA;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.impl.core.RootSectionsPublicationTree;
import org.nuxeo.ecm.platform.rendition.Constants;

/**
 * Implementation of {@link org.nuxeo.ecm.platform.publisher.api.PublicationTree} that retrieve also any published
 * Rendition documents for the given document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.4.1
 */
public class RenditionPublicationCoreTree extends RootSectionsPublicationTree {

    private static final long serialVersionUID = 1L;

    public static final String RENDITION_PUBLISHED_DOCUMENTS_FROM_PROXY_DOCUMENT = "SELECT * FROM Document WHERE rend:sourceId = '%s' "
            + "AND ecm:path STARTSWITH '%s'" + " AND ecm:isProxy = 1";

    public static final String RENDITION_PUBLISHED_DOCUMENTS_FROM_LIVE_DOCUMENT = "SELECT * FROM Document WHERE rend:sourceVersionableId = '%s' "
            + "AND ecm:path STARTSWITH '%s' " + "AND ecm:isProxy = 1";

    @Override
    public List<PublishedDocument> getExistingPublishedDocument(DocumentLocation docLoc) {
        List<PublishedDocument> publishedDocuments = super.getExistingPublishedDocument(docLoc);

        DocumentModel sourceDocument = coreSession.getDocument(docLoc.getDocRef());
        if (sourceDocument.isProxy()) {
            // If on a proxy, we want all the others proxy pointing to the same
            // version
            if (!sourceDocument.hasFacet(RENDITION_FACET) && !sourceDocument.hasSchema(RENDITION_SCHEMA)) {
                return publishedDocuments;
            }
            DocumentRef docRef = new IdRef(
                    (String) sourceDocument.getPropertyValue(Constants.RENDITION_SOURCE_ID_PROPERTY));
            publishedDocuments.addAll(getPublishedDocumentsFromProxyDocument(docRef, sourceDocument));
        } else {
            publishedDocuments.addAll(getPublishedDocumentsFromLiveDocument(docLoc.getDocRef()));
        }
        return publishedDocuments;
    }

    protected List<PublishedDocument> getPublishedDocumentsFromProxyDocument(DocumentRef docRef,
            DocumentModel sourceDocument) {
        List<PublishedDocument> publishedDocuments = new ArrayList<>();
        List<DocumentModel> docs = coreSession.query(String.format(RENDITION_PUBLISHED_DOCUMENTS_FROM_PROXY_DOCUMENT,
                docRef, NXQL.escapeStringInner(rootPath)));
        for (DocumentModel doc : docs) {
            if (!doc.getRef().equals(sourceDocument.getRef())) {
                publishedDocuments.add(factory.wrapDocumentModel(doc));
            }
        }
        return publishedDocuments;
    }

    protected List<PublishedDocument> getPublishedDocumentsFromLiveDocument(DocumentRef docRef) {
        List<PublishedDocument> publishedDocuments = new ArrayList<>();
        List<DocumentModel> docs = coreSession.query(String.format(RENDITION_PUBLISHED_DOCUMENTS_FROM_LIVE_DOCUMENT,
                docRef, NXQL.escapeStringInner(rootPath)));
        for (DocumentModel doc : docs) {
            publishedDocuments.add(factory.wrapDocumentModel(doc));
        }
        return publishedDocuments;
    }

}
