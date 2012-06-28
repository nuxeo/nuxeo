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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.quota.core")
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

        DocumentModel firstFolder = session.createDocumentModel(
                ws.getPathAsString(), "folder1", "Folder");
        firstFolder = session.createDocument(firstFolder);
        firstFolderRef = firstFolder.getRef();

        DocumentModel firstSubFolder = session.createDocumentModel(
                firstFolder.getPathAsString(), "subfolder1", "Folder");
        firstSubFolder = session.createDocument(firstSubFolder);
        firstSubFolderRef = firstSubFolder.getRef();

        DocumentModel firstFile = session.createDocumentModel(
                firstSubFolder.getPathAsString(), "file1", "File");
        firstFile = session.createDocument(firstFile);
        firstFileRef = firstFile.getRef();

        DocumentModel secondFile = session.createDocumentModel(
                firstSubFolder.getPathAsString(), "file2", "File");
        secondFile = session.createDocument(secondFile);
        secondFileRef = secondFile.getRef();

        DocumentModel secondSubFolder = session.createDocumentModel(
                firstFolder.getPathAsString(), "subfolder2", "Folder");
        secondSubFolder = session.createDocument(secondSubFolder);
        secondSubFolderRef = secondSubFolder.getRef();

        DocumentModel secondFolder = session.createDocumentModel(
                ws.getPathAsString(), "folder2", "Folder");
        secondFolder = session.createDocument(secondFolder);
        session.save();
        secondFolderRef = secondFolder.getRef();
    }

    @Test
    public void testDocumentsCount() throws Exception {
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertTrue(firstSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                firstSubFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        assertFalse(secondSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
    }

    @Test
    public void testRemoveFile() throws Exception {
        session.removeDocument(firstFileRef);
        session.save();

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertTrue(firstSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                1L,
                firstSubFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                1L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
    }

    @Test
    public void testCopyFile() throws Exception {
        session.copy(firstFileRef, secondSubFolderRef, null);
        session.save();

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertTrue(firstSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                firstSubFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        assertTrue(secondSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                1L,
                secondSubFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                3L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
    }

    @Test
    public void testMoveFile() throws Exception {
        session.move(firstFileRef, secondSubFolderRef, null);
        session.save();

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        assertTrue(firstSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                1L,
                firstSubFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        assertTrue(secondSubFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                1L,
                secondSubFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
    }

    @Test
    public void testRemoveFolder() throws Exception {
        session.removeDocument(firstSubFolderRef);
        session.save();

        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        assertTrue(firstFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                0L,
                firstFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                0L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
    }

    @Test
    public void testCopyFolder() throws Exception {
        session.copy(firstSubFolderRef, secondFolderRef, null);
        session.save();

        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        assertTrue(secondFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                secondFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                4L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
    }

    @Test
    public void testMoveFolder() throws Exception {
        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        assertFalse(secondFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));

        session.move(firstSubFolderRef, secondFolderRef, null);
        session.save();

        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        assertTrue(firstFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));

        secondFolder = session.getDocument(secondFolderRef);
        assertTrue(secondFolder.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                secondFolder.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));

        DocumentModel ws = session.getDocument(wsRef);
        assertTrue(ws.hasFacet(DOCUMENTS_COUNT_STATISTICS_FACET));
        assertEquals(
                2L,
                ws.getPropertyValue(DOCUMENTS_COUNT_STATISTICS_DESCENDANTS_COUNT_PROPERTY));
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
        session.save();

        String updaterName = "documentsCountUpdater";
        quotaStatsService.launchInitialStatisticsComputation(updaterName,
                session.getRepositoryName());
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        String queueId = workManager.getCategoryQueueId(QuotaStatsInitialWork.CATEGORY_QUOTA_INITIAL);
        workManager.awaitCompletion(queueId, 10, TimeUnit.SECONDS);

        session.save(); // process invalidations
        testDocumentsCount();
    }

}
