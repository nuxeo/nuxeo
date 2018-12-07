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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.work;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;

/**
 * Work to recompute the thumbnail of the documents resulting from the provided NXQL query.
 *
 * @since 10.10
 */
public class ThumbnailRecomputeWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    protected static final int BATCH_SIZE = 10;

    protected String nxqlQuery;

    public ThumbnailRecomputeWork(String repositoryName, String nxqlQuery) {
        this.repositoryName = repositoryName;
        this.nxqlQuery = nxqlQuery;
    }

    @Override
    public String getTitle() {
        return "Thumbnails Recomputation";
    }

    @Override
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);

        openSystemSession();
        DocumentModelList docs = session.query(nxqlQuery);
        long docsUpdated = 0;

        setStatus("Generating thumbnails");
        for (DocumentModel doc : docs) {
            if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
                if (blobHolder.getBlob() != null) {
                    // make sure the blob property is dirty for the CheckBlobUpdateListener to trigger the
                    // UpdateThumbnailListener
                    blobHolder.setBlob(blobHolder.getBlob());
                    session.saveDocument(doc);
                    docsUpdated++;
                    if (docsUpdated % BATCH_SIZE == 0) {
                        commitOrRollbackTransaction();
                        startTransaction();
                    }
                }
            }
        }
        setStatus("Done");
    }

}
