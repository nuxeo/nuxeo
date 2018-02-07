/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.platform.rendition.operation;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.utils.BlobUtils;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;

/**
 * Returns a Folderish Document or Collection default rendition.
 *
 * @since 9.3
 */
@Operation(id = GetContainerRendition.ID, category = Constants.CAT_BLOB, label = "Gets the folder's children or the collection's members default renditions", description = "Gets the list of blob of the folder's children or the collection's members default renditions. Returns a blob list file containing all the default rendition blobs.")
public class GetContainerRendition {

    private static final Log log = LogFactory.getLog(GetContainerRendition.class);

    public static final String ID = "Document.GetContainerRendition";

    @Context
    protected RenditionService renditionService;

    @Context
    protected CollectionManager collectionManager;

    @Param(name = "reason", required = false)
    protected String reason;

    @Param(name = "limit", description = "Limit of members to be returned. Default is 100.", required = false)
    protected int limit = 100;

    @Param(name = "maxDepth", description = "Depth of the hierarchy to be explored. Default is 1.", required = false)
    protected int maxDepth = 1;

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    protected BlobList getCollectionBlobs(Collection collection, int currentDepth) throws IOException {
        BlobList blobs = new BlobList();
        int added = 0;
        for (String memberId : collection.getCollectedDocumentIds()) {
            DocumentRef memberRef = new IdRef(memberId);
            if (session.exists(memberRef) && !session.isTrashed(memberRef)) {
                DocumentModel member = session.getDocument(memberRef);
                Blob blob = getDefaultRendition(member, currentDepth + 1);
                if (blob != null) {
                    blobs.add(blob);
                    if (limit > -1 && ++added >= limit) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format(
                                    "Limit of %s reached, increase the limit parameter to get more results.", limit));
                        }
                        break;
                    }
                }
            }
        }
        return blobs;
    }

    protected Blob getDefaultRendition(DocumentModel doc, int currentDepth) throws IOException {
        Blob blob = null;
        if (collectionManager.isCollection(doc) || doc.hasFacet(FacetNames.FOLDERISH)) {
            if (currentDepth >= maxDepth) {
                return null;
            }
            blob = processContainer(doc, currentDepth + 1);
        } else {
            Rendition rendition = renditionService.getDefaultRendition(doc, reason, null);
            if (rendition != null) {
                blob = rendition.getBlob();
                if (blob == null) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Default rendition '%s' has an null Blob for document '%s'",
                                rendition.getName(), doc.getPathAsString()));
                    }
                }
            }
        }
        return blob;
    }

    protected BlobList getFolderishBlobs(DocumentModel parent, int currentDepth) throws IOException {
        BlobList blobs = new BlobList();
        int added = 0;
        DocumentModelIterator it = session.getChildrenIterator(parent.getRef());
        while (it.hasNext()) {
            DocumentModel child = it.next();
            if (!child.isTrashed()) {
                Blob blob = getDefaultRendition(child, currentDepth);
                if (blob != null) {
                    blobs.add(blob);
                    if (limit > -1 && ++added >= limit) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format(
                                    "Limit of %s reached, increase the limit parameter to get more results.", limit));
                        }
                        break;
                    }
                }
            }
        }
        return blobs;
    }

    protected Blob processContainer(DocumentModel doc, int currentDepth) throws IOException {
        BlobList blobs;
        if (collectionManager.isCollection(doc)) {
            blobs = getCollectionBlobs(doc.getAdapter(Collection.class), currentDepth);
        } else if (doc.hasFacet(FacetNames.FOLDERISH)) {
            blobs = getFolderishBlobs(doc, currentDepth + 1);
        } else {
            throw new NuxeoException("The operation only accepts folderish document or collection");
        }
        return BlobUtils.zip(blobs, doc.getName() + ".zip");
    }

    @OperationMethod
    public Blob run(DocumentModel doc) throws IOException {
        if (maxDepth <= 0) {
            throw new NuxeoException("Maximum depth must greater or equal to 1.");
        }
        return processContainer(doc, 0);
    }

}
