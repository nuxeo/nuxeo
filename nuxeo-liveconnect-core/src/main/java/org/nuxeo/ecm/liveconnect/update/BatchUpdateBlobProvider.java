/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
