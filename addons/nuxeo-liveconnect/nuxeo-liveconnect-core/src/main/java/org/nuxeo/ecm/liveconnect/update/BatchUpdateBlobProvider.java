/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.liveconnect.update;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.liveconnect.update.listener.BlobProviderDocumentsUpdateListener;
import org.nuxeo.ecm.liveconnect.update.worker.BlobProviderDocumentsUpdateWork;

/**
 * Interface to batch update documents provided by implementing provider. The method {@link #processDocumentsUpdate()}
 * is called by {@link BlobProviderDocumentsUpdateListener}.
 * <p>
 * The implementation of {@link #processDocumentsUpdate()} must schedule a {@link BlobProviderDocumentsUpdateWork} with
 * the document ids to be checked and updated if needed.
 * <p>
 * The @{link BlobProviderDocumentsUpdateWork} will then call the implementation of
 * {@link #checkChangesAndUpdateBlob(List)}.
 * <p>
 * Note that it is recommended to schedule many workers dealing with a smaller amount of documents (using
 * {@link #MAX_RESULT}) rather than a single one processing all document brought by the provider.
 *
 * @since 7.3
 */
public interface BatchUpdateBlobProvider {

    long MAX_RESULT = 50;

    /**
     * Check the given list of document for change and update if needed. Note that session.save still needs to be called
     * on changed documents.
     *
     * @param documents to be checked for update
     * @return the list of DocumentModel that have changed
     */
    List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> documents);

    /**
     * Trigger the documents update for the implementing providers.
     */
    void processDocumentsUpdate();

}
