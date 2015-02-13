/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.publisher;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SCHEMA;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.impl.core.RootSectionsPublicationTree;
import org.nuxeo.ecm.platform.rendition.Constants;

/**
 * Implementation of
 * {@link org.nuxeo.ecm.platform.publisher.api.PublicationTree} that retrieve
 * also any published Rendition documents for the given document.
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
    public List<PublishedDocument> getExistingPublishedDocument(
            DocumentLocation docLoc) throws ClientException {
        List<PublishedDocument> publishedDocuments = super.getExistingPublishedDocument(docLoc);

        DocumentModel sourceDocument = coreSession.getDocument(docLoc.getDocRef());
        if (sourceDocument.isProxy()) {
            // If on a proxy, we want all the others proxy pointing to the same
            // version
            if (!sourceDocument.hasFacet(RENDITION_FACET)
                    && !sourceDocument.hasSchema(RENDITION_SCHEMA)) {
                return publishedDocuments;
            }
            DocumentRef docRef = new IdRef(
                    (String) sourceDocument.getPropertyValue(Constants.RENDITION_SOURCE_ID_PROPERTY));
            publishedDocuments.addAll(getPublishedDocumentsFromProxyDocument(
                    docRef, sourceDocument));
        } else {
            publishedDocuments.addAll(getPublishedDocumentsFromLiveDocument(docLoc.getDocRef()));
        }
        return publishedDocuments;
    }

    protected List<PublishedDocument> getPublishedDocumentsFromProxyDocument(
            DocumentRef docRef, DocumentModel sourceDocument)
            throws ClientException {
        List<PublishedDocument> publishedDocuments = new ArrayList<PublishedDocument>();
        List<DocumentModel> docs = coreSession.query(String.format(
                RENDITION_PUBLISHED_DOCUMENTS_FROM_PROXY_DOCUMENT, docRef,
                rootPath));
        for (DocumentModel doc : docs) {
            if (!doc.getRef().equals(sourceDocument.getRef())) {
                publishedDocuments.add(factory.wrapDocumentModel(doc));
            }
        }
        return publishedDocuments;
    }

    protected List<PublishedDocument> getPublishedDocumentsFromLiveDocument(
            DocumentRef docRef) throws ClientException {
        List<PublishedDocument> publishedDocuments = new ArrayList<PublishedDocument>();
        List<DocumentModel> docs = coreSession.query(String.format(
                RENDITION_PUBLISHED_DOCUMENTS_FROM_LIVE_DOCUMENT, docRef,
                rootPath));
        for (DocumentModel doc : docs) {
            publishedDocuments.add(factory.wrapDocumentModel(doc));
        }
        return publishedDocuments;
    }

}
