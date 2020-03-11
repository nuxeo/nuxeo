/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */
package org.nuxeo.ecm.liveconnect.update.worker;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.runtime.api.Framework;

public class BlobProviderDocumentsUpdateWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(BlobProviderDocumentsUpdateWork.class);

    private static final long serialVersionUID = 1L;

    protected static final String TITLE = "Live Connect Update Documents Work";

    public static final String CATEGORY = "blobProviderDocumentsUpdate";

    protected String providerName;

    public BlobProviderDocumentsUpdateWork(final String id, final String providerName) {
        super(id);
        this.providerName = providerName;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public void work() {
        BatchUpdateBlobProvider blobProvider = (BatchUpdateBlobProvider) Framework.getService(
                BlobManager.class).getBlobProvider(providerName);
        setStatus("Updating");
        if (session == null) {
            openSystemSession();
        }
        final List<DocumentModel> results = docIds.stream().map(IdRef::new).map(session::getDocument)
                .collect(Collectors.toList());
        log.trace("Updating");
        List<DocumentModel> changedDocuments = blobProvider.checkChangesAndUpdateBlob(results);
        if (changedDocuments != null) {
            for (DocumentModel doc : changedDocuments) {
                session.saveDocument(doc);
            }
        }
        log.trace("Updating done");
        setStatus("Done");
    }

}
