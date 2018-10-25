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

package org.nuxeo.ecm.platform.picture.recompute;

import static org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants.PICTURE_FACET;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.work.AbstractWork;

public class ImagingRecomputeWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    protected static final int BATCH_SIZE = 10;

    protected String nxqlQuery;

    public ImagingRecomputeWork(String repositoryName, String nxqlQuery) {
        this.repositoryName = repositoryName;
        this.nxqlQuery = nxqlQuery;
    }

    @Override
    public String getTitle() {
        return "Picture Views Recomputation";
    }

    @Override
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);

        openSystemSession();
        DocumentModelList docs = session.query(nxqlQuery);
        long docsUpdated = 0;

        setStatus("Generating views");
        for (DocumentModel doc : docs) {
            if (doc.hasFacet(PICTURE_FACET)) {
                BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
                if (blobHolder.getBlob() != null) {
                    // make sure the blob property is dirty for the PictureChangedListener to not block the
                    // PictureViewsGenerationListener
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
