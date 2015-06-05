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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.liveconnect.update.listener.BlobProviderDocumentsUpdateListener;
import org.nuxeo.ecm.liveconnect.update.worker.BlobProviderDocumentsUpdateWork;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

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

    static final long MAX_RESULT = 50;

    /**
     * Check the given list of document for change and update if needed. Note that session.save still needs to be called
     * on changed documents.
     *
     * @param documents to be checked for update
     * @return the list of DocumentModel that have changed
     * @throws IOException
     */
    List<DocumentModel> checkChangesAndUpdateBlob(List<DocumentModel> doc);

    String getPageProviderNameForUpdate();

    String getBlobPrefix();

    /**
     * Trigger the documents update for the implementing providers.
     */
    default void processDocumentsUpdate() {
        final RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        final WorkManager workManager = Framework.getLocalService(WorkManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            CoreSession session = null;
            try {
                session = CoreInstance.openCoreSessionSystem(repositoryName);

                long offset = 0;
                List<DocumentModel> nextDocumentsToBeUpdated;
                PageProviderService ppService = Framework.getService(PageProviderService.class);
                Map<String, Serializable> props = new HashMap<String, Serializable>();
                props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
                @SuppressWarnings("unchecked")
                PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(
                        getPageProviderNameForUpdate(), null, null, null, props);
                final long maxResult = pp.getPageSize();
                do {
                    pp.setCurrentPageOffset(offset);
                    pp.refresh();
                    nextDocumentsToBeUpdated = pp.getCurrentPage();

                    if (nextDocumentsToBeUpdated.isEmpty()) {
                        break;
                    }
                    List<String> docIds = new ArrayList<>();
                    for (DocumentModel doc : nextDocumentsToBeUpdated) {
                        docIds.add(doc.getId());
                    }
                    BlobProviderDocumentsUpdateWork work = new BlobProviderDocumentsUpdateWork(
                            getBlobPrefix() + ":" + repositoryName + ":" + offset, getBlobPrefix());
                    work.setDocuments(repositoryName, docIds);
                    workManager.schedule(work, WorkManager.Scheduling.IF_NOT_SCHEDULED, true);
                    offset += maxResult;
                } while (nextDocumentsToBeUpdated.size() == maxResult);

            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

}
