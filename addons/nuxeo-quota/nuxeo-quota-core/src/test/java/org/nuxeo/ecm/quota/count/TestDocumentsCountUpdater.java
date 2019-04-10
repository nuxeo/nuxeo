/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 *     Florent Guillaume
 */
package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY;
import static org.nuxeo.ecm.quota.count.Constants.DOCUMENTS_COUNT_STATISTICS_FACET;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ QuotaFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentsCountUpdater {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    DocumentRef wsRef;

    DocumentRef firstFolderRef;

    DocumentRef secondFolderRef;

    DocumentRef firstSubFolderRef;

    DocumentRef secondSubFolderRef;

    DocumentRef firstFileRef;

    DocumentRef secondFileRef;

    @Test
    public void testServiceRegistration() {
        assertNotNull(quotaStatsService);
    }

    @Before
    public void addFiles() throws ClientException {
        session.save();
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        wsRef = ws.getRef();

        DocumentModel firstFolder = session.createDocumentModel(ws.getPathAsString(), "folder1", "Folder");
        firstFolder = session.createDocument(firstFolder);
        firstFolderRef = firstFolder.getRef();

        DocumentModel firstSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder1",
                "Folder");
        firstSubFolder = session.createDocument(firstSubFolder);
        firstSubFolderRef = firstSubFolder.getRef();

        DocumentModel firstFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file1", "File");
        firstFile = session.createDocument(firstFile);
        firstFileRef = firstFile.getRef();

        DocumentModel secondFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file2", "File");
        secondFile = session.createDocument(secondFile);
        secondFileRef = secondFile.getRef();

        DocumentModel secondSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder2",
                "Folder");
        secondSubFolder = session.createDocument(secondSubFolder);
        secondSubFolderRef = secondSubFolder.getRef();

        DocumentModel secondFolder = session.createDocumentModel(ws.getPathAsString(), "folder2", "Folder");
        secondFolder = session.createDocument(secondFolder);
        session.save();
        secondFolderRef = secondFolder.getRef();
    }

    protected static void assertHasCountFacet(DocumentModel doc) {
        assertTrue(doc.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
    }

    protected static void assertHasNoCountFacet(DocumentModel doc) {
        assertFalse(doc.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
    }

    protected static void assertDescendantsCount(long expected, DocumentModel doc) {
        assertHasCountFacet(doc);
        assertEquals(expected,
                ((Number) doc.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY)).longValue());
    }

    @Test
    public void testDocumentsCount() throws Exception {
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertDescendantsCount(2, firstSubFolder);

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        assertHasNoCountFacet(secondSubFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(2, ws);
    }

    @Test
    public void testRemoveFile() throws Exception {
        session.removeDocument(firstFileRef);
        session.save();

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertDescendantsCount(1, firstSubFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(1, ws);
    }

    @Test
    public void testCopyFile() throws Exception {
        session.copy(firstFileRef, secondSubFolderRef, null);
        session.save();

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertDescendantsCount(2, firstSubFolder);

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        assertDescendantsCount(1, secondSubFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(3, ws);
    }

    @Test
    public void testMoveFile() throws Exception {
        session.move(firstFileRef, secondSubFolderRef, null);
        session.save();

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertDescendantsCount(1, firstSubFolder);

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        assertDescendantsCount(1, secondSubFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(2, ws);
    }

    @Test
    public void testRemoveFolder() throws Exception {
        session.removeDocument(firstSubFolderRef);
        session.save();

        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        assertDescendantsCount(0, firstFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(0, ws);
    }

    @Test
    public void testCopyFolder() throws Exception {
        session.copy(firstSubFolderRef, secondFolderRef, null);
        session.save();

        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        assertDescendantsCount(2, secondFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(4, ws);
    }

    @Test
    public void testMoveFolder() throws Exception {
        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        assertHasNoCountFacet(secondFolder);

        session.move(firstSubFolderRef, secondFolderRef, null);
        session.save();

        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        assertHasCountFacet(firstFolder);

        secondFolder = session.getDocument(secondFolderRef);
        assertDescendantsCount(2, secondFolder);

        DocumentModel ws = session.getDocument(wsRef);
        assertDescendantsCount(2, ws);
    }

    @Test
    public void testComputeInitialStatistics() throws Exception {
        List<DocumentModel> folders = session.query("SELECT * FROM Document where ecm:mixinType = 'Folderish'");
        for (DocumentModel folder : folders) {
            if (folder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET)) {
                folder.removeFacet(DOCUMENTS_COUNT_STATISTICS_FACET);
                session.saveDocument(folder);
            }
        }

        String updaterName = "documentsCountUpdater";
        quotaStatsService.launchInitialStatisticsComputation(updaterName, session.getRepositoryName());
        TransactionHelper.commitOrRollbackTransaction();
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        String queueId = workManager.getCategoryQueueId(QuotaStatsInitialWork.CATEGORY_QUOTA_INITIAL);
        workManager.awaitCompletion(queueId, 10, TimeUnit.SECONDS);

        TransactionHelper.startTransaction();
        testDocumentsCount();
    }

}
