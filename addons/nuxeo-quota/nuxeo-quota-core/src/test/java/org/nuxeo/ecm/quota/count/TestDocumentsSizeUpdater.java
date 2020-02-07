/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.quota.count.QuotaFeature.assertQuota;
import static org.nuxeo.ecm.quota.count.QuotaFeature.createFakeBlob;
import static org.nuxeo.ecm.quota.size.QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.trash.TrashService;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocument;
import org.nuxeo.ecm.quota.size.QuotaExceededException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Various test that verify quota Sizes are updated accordingly to various operations. Due to the asynchronous nature of
 * size update, these test rely on {@link Thread#sleep(long)} and can fail because of that.
 *
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ QuotaFeature.class, CoreBulkFeature.class, UserManagerFeature.class })
public class TestDocumentsSizeUpdater {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected UserWorkspaceService uwm;

    @Inject
    protected WorkManager workManager;

    @Inject
    protected UserManager userManager;

    protected DocumentRef wsRef;

    protected DocumentRef firstFolderRef;

    protected DocumentRef secondFolderRef;

    protected DocumentRef firstSubFolderRef;

    protected DocumentRef secondSubFolderRef;

    protected DocumentRef firstFileRef;

    protected DocumentRef secondFileRef;

    @Test
    public void testQuotaOnAddContent() {

        addContent();

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 300L);
        assertQuota(getFirstFolder(), 0L, 300L);
        assertQuota(getWorkspace(), 0L, 300L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnAddAndModifyContent() {

        addContent();

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 300L);
        assertQuota(getFirstFolder(), 0L, 300L);
        assertQuota(getWorkspace(), 0L, 300L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        doUpdateContent();

        dump();
        assertQuota(getFirstFile(), 380L, 380L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 580L);
        assertQuota(getFirstFolder(), 0L, 580L);
        assertQuota(getWorkspace(), 50L, 630L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaInitialCheckIn() {

        addContent();

        dump();
        assertQuota(getFirstFile(), 100, 100, 0, 0);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
        assertQuota(getFirstFolder(), 0, 300, 0, 0);
        assertQuota(getWorkspace(), 0, 300, 0, 0);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        doCheckIn();

        dump();
        DocumentModel firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        // checked in: We count immediatly because at user (UI) level
        // checkout is not a visible action
        assertQuota(firstFile, 100, 200, 0, 100);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
        assertQuota(getFirstFolder(), 0, 400, 0, 100);
        assertQuota(getWorkspace(), 0, 400, 0, 100);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // checkout the doc
        doCheckOut();

        dump();
        firstFile = getFirstFile();
        assertTrue(firstFile.isCheckedOut());
        assertEquals("0.1+", firstFile.getVersionLabel());
        assertQuota(firstFile, 100, 200, 0, 100);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
        assertQuota(getFirstFolder(), 0, 400, 0, 100);
        assertQuota(getWorkspace(), 0, 400, 0, 100);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnCheckInCheckOut() {

        addContent();

        dump();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        doCheckIn();

        dump();
        DocumentModel firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());
        assertQuota(firstFile, 100, 200, 0, 100);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
        assertQuota(getFirstFolder(), 0, 400, 0, 100);
        assertQuota(getWorkspace(), 0, 400, 0, 100);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // checkout the doc
        doCheckOut();

        dump();
        firstFile = getFirstFile();
        assertTrue(firstFile.isCheckedOut());
        assertEquals("0.1+", firstFile.getVersionLabel());
        assertQuota(firstFile, 100, 200, 0, 100);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
        assertQuota(getFirstFolder(), 0, 400, 0, 100);
        assertQuota(getWorkspace(), 0, 400, 0, 100);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnVersions() {

        addContent();

        dump();
        assertQuota(getFirstFile(), 100, 100, 0, 0);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
        assertQuota(getFirstFolder(), 0, 300, 0, 0);
        assertQuota(getWorkspace(), 0, 300, 0, 0);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version

        dump();
        DocumentModel firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());
        assertQuota(firstFile, 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);

        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // create another version
        doSimpleVersion();

        dump();
        firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.2", firstFile.getVersionLabel());
        assertQuota(firstFile, 380, 1140, 0, 760);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 1340, 0, 760);
        assertQuota(getFirstFolder(), 0, 1340, 0, 760);
        assertQuota(getWorkspace(), 50, 1390, 0, 760);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // remove a version
        doRemoveFirstVersion();

        dump();
        firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.2", firstFile.getVersionLabel());
        assertQuota(firstFile, 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // remove doc and associated version
        doRemoveContent();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        assertFalse(session.exists(firstFileRef));
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 200, 0, 0);
        assertQuota(getFirstFolder(), 0, 200, 0, 0);
        assertQuota(getWorkspace(), 50, 250, 0, 0);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnMoveContent() {
        // Given some content
        addContent();

        dump();

        // When i Move the content
        doMoveContent();

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 200L);
        assertQuota(getSecondSubFolder(), 0L, 100L);
        assertQuota(getFirstFolder(), 0L, 300L);
        assertQuota(getWorkspace(), 0L, 300L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnRemoveContent() {

        addContent();
        doRemoveContent();

        dump();
        assertFalse(session.exists(firstFileRef));
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 200L);
        assertQuota(getFirstFolder(), 0L, 200L);
        assertQuota(getWorkspace(), 0L, 200L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnCopyContent() {

        addContent();

        dump();

        doCopyContent();

        dump();
        DocumentModel copiedFile = session.getChildren(secondSubFolderRef).get(0);
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(copiedFile, 100L, 100L);
        assertQuota(getFirstSubFolder(), 0L, 300L);
        assertQuota(getSecondSubFolder(), 0L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L);
        assertQuota(getWorkspace(), 0L, 400L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnCopyFolderishContent() {

        addContent();

        dump();

        doCopyFolderishContent();

        dump();
        DocumentModel copiedFolder = session.getChildren(secondFolderRef).get(0);
        DocumentModel copiedFirstFile = session.getChild(copiedFolder.getRef(), "file1");
        DocumentModel copiedSecondFile = session.getChild(copiedFolder.getRef(), "file2");
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 300L);
        assertQuota(getFirstFolder(), 0L, 300L);
        assertQuota(getSecondFolder(), 0L, 300L);
        assertQuota(copiedFolder, 0L, 300L);
        assertQuota(copiedFirstFile, 100L, 100L);
        assertQuota(copiedSecondFile, 200L, 200L);
        assertQuota(getWorkspace(), 0L, 600L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnRemoveFoldishContent() {
        addContent();

        dump();

        doRemoveFolderishContent();

        dump();
        assertFalse(session.exists(firstSubFolderRef));
        assertQuota(getFirstFolder(), 0L, 0L);
        assertQuota(getWorkspace(), 0L, 0L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaOnMoveFoldishContent() {

        addContent();

        dump();

        doMoveFolderishContent();

        dump();
        assertEquals(1, session.getChildren(firstFolderRef).size());
        assertQuota(getWorkspace(), 0L, 300L);
        assertQuota(getFirstFolder(), 0L, 0L);
        assertQuota(getSecondFolder(), 0L, 300L);
        assertQuota(getFirstSubFolder(), 0L, 300L);
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaExceeded() {

        addContent();

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        // now add quota limit
        QuotaAware qa = getWorkspace().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(300L, qa.getTotalSize());
        assertEquals(-1L, qa.getMaxQuota());
        // set the quota to 400
        qa.setMaxQuota(400);
        qa.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        try {
            // now try to update one
            DocumentModel firstFile = session.getDocument(firstFileRef);
            firstFile.setPropertyValue("file:content", createFakeBlob(250));
            session.saveDocument(firstFile);
            fail("Should have failed due to quota exceeded");
        } catch (Exception e) {
            assertTrue(QuotaExceededException.isQuotaExceededException(e));
            // rollback the transaction
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        // now remove the quota limit
        qa = getWorkspace().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(300L, qa.getTotalSize());
        assertEquals(400L, qa.getMaxQuota());
        // set the quota to -1 / unlimited
        qa.setMaxQuota(-1);
        qa.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        // now try to update one
        DocumentModel firstFile = session.getDocument(firstFileRef);
        firstFile.setPropertyValue("file:content", createFakeBlob(250));
        session.saveDocument(firstFile);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaExceededAfterDelete() {
        addContent();

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        // now add quota limit
        QuotaAware qa = getWorkspace().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(300L, qa.getTotalSize());
        assertEquals(-1L, qa.getMaxQuota());
        // set the quota to 300
        qa.setMaxQuota(500);
        qa.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        DocumentModel doc = session.copy(firstFileRef, firstSubFolderRef, "newCopy");
        doc.setPropertyValue("file:content", createFakeBlob(100));
        session.createDocument(doc);
        session.save();

        session.removeChildren(firstFolderRef);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", createFakeBlob(299));
        doc.setPropertyValue("dc:title", "Other file");
        doc.setPathInfo(getWorkspace().getPathAsString(), "otherfile");
        session.createDocument(doc);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaExceededMassCopy() {
        addContent();

        dump();
        assertQuota(getFirstFile(), 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L);
        assertQuota(getFirstSubFolder(), 0L, 300L);
        assertQuota(getFirstFolder(), 0L, 300L);
        assertQuota(getWorkspace(), 0L, 300L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        final String title = "MassCopyDoc";
        final int nbrDocs = 20;
        final int fileSize = 50;
        final int firstSubFolderExistingFilesNbr = session.getChildren(firstSubFolderRef, "File").size();
        assertEquals(2, firstSubFolderExistingFilesNbr);

        Blob blob = createFakeBlob(fileSize);
        for (int i = 0; i < nbrDocs; i++) {
            DocumentModel doc = session.createDocumentModel("File");
            doc.setPropertyValue("file:content", (Serializable) blob);
            doc.setPropertyValue("dc:title", title);
            doc.setPathInfo(getSecondFolder().getPathAsString(), "myfile" + i);
            session.createDocument(doc);
        }
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        final long maxSize = 455L;
        QuotaAware qa = getFirstSubFolder().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        final long firstSubFolderTotalSize = qa.getTotalSize();

        assertEquals(nbrDocs, session.getChildren(secondFolderRef, "File").size());
        dump();
        qa = getSecondFolder().getAdapter(QuotaAware.class);
        assertEquals(nbrDocs * fileSize, qa.getTotalSize());
        // now add quota limit
        assertEquals(300L, firstSubFolderTotalSize);
        assertEquals(-1L, qa.getMaxQuota());
        // set the quota
        qa = getFirstSubFolder().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        qa.setMaxQuota(maxSize);
        qa.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        qa = getFirstSubFolder().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(maxSize, qa.getMaxQuota());
        DocumentModelList docsToCopy = session.query("SELECT * FROM Document WHERE " + NXQL.ECM_PARENTID + " = '"
                + getSecondFolder().getId() + "' AND dc:title = '" + title + "'");
        List<DocumentRef> refsToCopy = new ArrayList<>(docsToCopy.size());
        for (DocumentModel doc : docsToCopy) {
            refsToCopy.add(doc.getRef());
        }
        try {
            session.move(refsToCopy, firstSubFolderRef);
            fail("Should have failed due to quota exceeded");
        } catch (Exception e) {
            assertTrue(QuotaExceededException.isQuotaExceededException(e));
            // rollback the transaction
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        qa = getFirstSubFolder().getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertTrue(qa.getTotalSize() < maxSize);
        // assertEquals(firstSubFolderTotalSize+(expectedNbrDocsCopied*fileSize), qa.getTotalSize());
        assertEquals(firstSubFolderTotalSize, qa.getTotalSize());
        DocumentModelList children = session.getChildren(firstSubFolderRef, "File");
        // assertEquals(firstSubFolderExistingFilesNbr + expectedNbrDocsCopied, children.size());
        assertEquals(firstSubFolderExistingFilesNbr, children.size());
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        DocumentModel doc = session.createDocumentModel("File");
        doc.setPropertyValue("file:content", createFakeBlob(50));
        doc.setPropertyValue("dc:title", "Other file");
        doc.setPathInfo(getFirstSubFolder().getPathAsString(), "otherfile");
        session.createDocument(doc);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testQuotaExceededOnVersion() {

        addContent();

        // now add quota limit
        DocumentModel ws = session.getDocument(wsRef);
        QuotaAware qa = ws.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(300L, qa.getTotalSize());
        assertEquals(-1L, qa.getMaxQuota());
        // set the quota to 350
        qa.setMaxQuota(350);
        qa.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        try {
            // create a version
            DocumentModel firstFile = session.getDocument(firstFileRef);
            firstFile.checkIn(VersioningOption.MINOR, null);
            fail("Should have failed due to quota exceeded");
        } catch (Exception e) {
            assertTrue(QuotaExceededException.isQuotaExceededException(e));
            // rollback the transaction
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testComputeInitialStatistics() {

        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        eventAdmin.setListenerEnabledFlag("quotaStatsListener", false);

        addContent();
        doCheckIn();
        doDeleteFileContent(secondFileRef);

        dump();
        DocumentModel ws = session.getDocument(wsRef);
        assertFalse(ws.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET));
        String updaterName = "documentsSizeUpdater";
        quotaStatsService.launchInitialStatisticsComputation(updaterName, session.getRepositoryName(), null);
        workManager.getCategoryQueueId(QuotaStatsInitialWork.CATEGORY_QUOTA_INITIAL);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
        assertQuota(getFirstSubFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getWorkspace(), 0L, 400L, 200L, 100L);

        eventAdmin.setListenerEnabledFlag("quotaStatsListener", true);

        DocumentModel firstFile = getFirstFile();
        // modify file to checkout
        firstFile.setPropertyValue("dc:title", "File1");
        firstFile = session.saveDocument(firstFile);
        session.save(); // process invalidations
        assertTrue(firstFile.isCheckedOut());
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
        assertQuota(getFirstSubFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getWorkspace(), 0L, 400L, 200L, 100L);
    }

    @Test
    public void testComputeInitialStatisticsAfterFileMovedToTrash() {

        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        eventAdmin.setListenerEnabledFlag("quotaStatsListener", true);

        addContent(true);
        doDeleteFileContent(firstFileRef);

        coreFeature.waitForAsyncCompletion(); // commit the transaction
        dump();
        assertQuota(getFirstFile(), 100L, 200L, 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 0L, 0L);
        assertQuota(getFirstSubFolder(), 0L, 400L, 100L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L, 100L, 100L);
        assertQuota(getWorkspace(), 0L, 400L, 100L, 100L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        String updaterName = "documentsSizeUpdater";
        quotaStatsService.launchInitialStatisticsComputation(updaterName, session.getRepositoryName(), null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        assertQuota(getFirstFile(), 100L, 200L, 100L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 0L, 0L);
        assertQuota(getFirstSubFolder(), 0L, 400L, 100L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L, 100L, 100L);
        assertQuota(getWorkspace(), 0L, 400L, 100L, 100L);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    @Test
    public void testRecomputeStatisticsAfterCorruption() {

        addContent();
        doCheckIn();
        doDeleteFileContent(secondFileRef);
        // also add a blob on the workspace above the one for which we're recomputing
        DocumentModel ws = getWorkspace();
        ws.setPropertyValue("file:content", createFakeBlob(1000));
        session.saveDocument(ws);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // correct quotas
        dump();
        assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
        assertQuota(getFirstSubFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getWorkspace(), 1000L, 1400L, 200L, 100L);

        // corrupt quota info in a folder, and add a max size
        DocumentModel firstSubFolder = getFirstSubFolder();
        QuotaAware qa = firstSubFolder.getAdapter(QuotaAware.class);
        qa.addInnerSize(11);
        qa.addTotalSize(22);
        qa.addTrashSize(33);
        qa.addVersionsSize(44);
        qa.setMaxQuota(12341234); // set a max size on this folder
        qa.save();

        // quotas are now corrupted
        dump();
        assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
        assertQuota(getFirstSubFolder(), 11L, 422L, 233L, 144L); // <-- corruption
        assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getWorkspace(), 1000L, 1400L, 200L, 100L);

        // recompute quotas from folder
        String updaterName = "documentsSizeUpdater";
        quotaStatsService.launchInitialStatisticsComputation(updaterName, session.getRepositoryName(),
                firstSubFolder.getPathAsString());
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // quotas are correct again
        dump();
        assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
        assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
        assertQuota(getFirstSubFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
        assertQuota(getWorkspace(), 1000L, 1400L, 200L, 100L);
        qa = firstSubFolder.getAdapter(QuotaAware.class);
        // max quota still set
        assertEquals(12341234, qa.getMaxQuota());
    }

    @Test
    public void testQuotaOnDeleteContent() {
        addContent();
        doDeleteFileContent();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);

        assertQuota(firstFile, 100L, 100L, 100L);
        assertQuota(firstFolder, 0L, 300L, 100L);
        assertQuota(ws, 0L, 300L, 100L);

        doUndeleteFileContent();

        assertQuota(getFirstFile(), 100L, 100L, 0L);
        assertQuota(getFirstFolder(), 0L, 300L, 0L);
        assertQuota(getWorkspace(), 0L, 300L, 0L);

        // sent file to trash
        doDeleteFileContent();
        // then permanently delete file when file is in the trash
        doRemoveContent();

        assertFalse(session.exists(firstFileRef));
        assertQuota(getFirstFolder(), 0L, 200L, 0L);
        assertQuota(getWorkspace(), 0L, 200L, 0L);
    }

    @Test
    public void testQuotaOnDeleteVersion() {
        addContent();

        dump();
        assertQuota(getFirstFile(), 100, 100, 0, 0);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
        assertQuota(getFirstFolder(), 0, 300, 0, 0);
        assertQuota(getWorkspace(), 0, 300, 0, 0);

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version
        dump();
        DocumentModel firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);

        doDeleteFileContent(firstFileRef);
        dump();
        assertQuota(getFirstFile(), 380, 760, 380, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 380, 380);
        assertQuota(getFirstFolder(), 0, 960, 380, 380);
        assertQuota(getWorkspace(), 50, 1010, 380, 380);

        doRemoveContent();
        coreFeature.waitForAsyncCompletion(); // commit the transaction
        dump();
        assertFalse(session.exists(firstFileRef));
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 200, 0, 0);
        assertQuota(getFirstFolder(), 0, 200, 0, 0);
        assertQuota(getWorkspace(), 50, 250, 0, 0);
    }

    /**
     * NXP-17350
     */
    @Test
    public void testQuotaOnDeleteFolder() {
        addContent();

        dump();
        assertQuota(getFirstFile(), 100, 100, 0, 0);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
        assertQuota(getFirstFolder(), 0, 300, 0, 0);
        assertQuota(getWorkspace(), 0, 300, 0, 0);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version
        dump();
        DocumentModel firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);

        doDeleteFileContent(firstSubFolderRef);
        dump();
        // inner, total, trash, versions
        assertQuota(getFirstFile(), 380, 760, 380, 380);
        assertQuota(getSecondFile(), 200, 200, 200, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 580, 380);
        assertQuota(getFirstFolder(), 0, 960, 580, 380);
        assertQuota(getWorkspace(), 50, 1010, 580, 380);
    }

    @Test
    public void testQuotaOnMoveContentWithVersions() {

        addContent();
        // update and create a version
        doUpdateAndVersionContent();

        assertQuota(getFirstFile(), 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);

        doMoveFileContent();

        assertQuota(getFirstFile(), 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 200, 0, 0);
        assertQuota(getFirstFolder(), 0, 200, 0, 0);
        assertQuota(getSecondFolder(), 0, 760, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);
    }

    @Test
    public void testQuotaOnCopyContentWithVersions() {

        addContent();
        // update and create a version
        doUpdateAndVersionContent();

        dump();
        assertQuota(getFirstFile(), 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);

        doCopyContent();

        dump();
        DocumentModel copiedFile = session.getChildren(secondSubFolderRef).get(0);
        assertQuota(getFirstFile(), 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 1340, 0, 380);
        assertQuota(copiedFile, 380, 380, 0, 0);
        assertQuota(getSecondSubFolder(), 0, 380, 0, 0);
        assertQuota(getWorkspace(), 50, 1390, 0, 380);
    }

    @Test
    public void testAllowSettingMaxQuota() {
        addContent();

        // now add quota limit
        DocumentModel ws = session.getDocument(wsRef);
        QuotaAware qa = ws.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        // set the quota to 400
        qa.setMaxQuota(400);
        qa.save();
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        QuotaAware qaFSF = firstSubFolder.getAdapter(QuotaAware.class);
        qaFSF.setMaxQuota(200);
        qaFSF.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        QuotaAware qaSecFolder = secondFolder.getAdapter(QuotaAware.class);
        try {
            qaSecFolder.setMaxQuota(300);
            fail("Should have failed due to quota exceeded");
        } catch (QuotaExceededException e) {
            // ok
        }
        qaSecFolder.setMaxQuota(200);
        qaSecFolder.save();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        qaSecFolder = getSecondFolder().getAdapter(QuotaAware.class);
        assertEquals(200L, qaSecFolder.getMaxQuota());
        QuotaAware qaFirstFolder = getFirstFolder().getAdapter(QuotaAware.class);
        try {
            qaFirstFolder.setMaxQuota(50);
            fail("Should have failed due to quota exceeded");
        } catch (QuotaExceededException e) {
            // ok
        }
        QuotaAware qaSecSubFolder = getSecondSubFolder().getAdapter(QuotaAware.class);
        try {
            qaSecSubFolder.setMaxQuota(50);
            fail("Should have failed due to quota exceeded");
        } catch (QuotaExceededException e) {
            // ok
        }
    }

    @Test
    public void testQuotaOnRevertVersion() {

        addContent();

        dump();
        assertQuota(getFirstFile(), 100, 100, 0, 0);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
        assertQuota(getFirstFolder(), 0, 300, 0, 0);
        assertQuota(getWorkspace(), 0, 300, 0, 0);

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version

        dump();
        DocumentModel firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());
        assertQuota(firstFile, 380, 760, 0, 380);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
        assertQuota(getFirstFolder(), 0, 960, 0, 380);
        assertQuota(getWorkspace(), 50, 1010, 0, 380);

        // create another version
        doSimpleVersion();

        firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.2", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 1140, 0, 760);
        assertQuota(getSecondFile(), 200, 200, 0, 0);
        assertQuota(getFirstSubFolder(), 0, 1340, 0, 760);
        assertQuota(getFirstFolder(), 0, 1340, 0, 760);
        assertQuota(getWorkspace(), 50, 1390, 0, 760);

        List<DocumentModel> versions = session.getVersions(firstFileRef);
        for (DocumentModel documentModel : versions) {
            if ("0.1".equals(documentModel.getVersionLabel())) {
                firstFile = session.restoreToVersion(firstFileRef, documentModel.getRef(), true, true);
            }
        }
        firstFileRef = firstFile.getRef();
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        dump();
        firstFile = getFirstFile();
        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 1140, 0, 760);
        assertQuota(getFirstSubFolder(), 0, 1340, 0, 760);
        assertQuota(getFirstFolder(), 0, 1340, 0, 760);
        assertQuota(getWorkspace(), 50, 1390, 0, 760);
    }

    @Test
    public void testAllowSettingMaxQuotaOnUserWorkspace() {
        addContent();
        createUser("titi");
        createUser("toto");

        try (CloseableCoreSession userSession = coreFeature.openCoreSession("toto")) {
            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession);
            assertNotNull(uw);
            userSession.save();
        }
        try (CloseableCoreSession userSession = coreFeature.openCoreSession("titi")) {
            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession);
            assertNotNull(uw);
            userSession.save();
        }
        quotaStatsService.activateQuotaOnUserWorkspaces(300L, session);
        quotaStatsService.launchSetMaxQuotaOnUserWorkspaces(300L, session.getRootDocument(), session);
        coreFeature.waitForAsyncCompletion(); // commit the transaction

        WorkQueueMetrics metrics = workManager.getMetrics("quota");
        assertEquals(0, metrics.getRunning().longValue() + metrics.getScheduled().longValue());

        DocumentModel totoUW = uwm.getUserPersonalWorkspace("toto",
                session.getDocument(new PathRef("/default-domain/")));
        QuotaAware totoUWQuota = totoUW.getAdapter(QuotaAware.class);
        assertEquals(300L, totoUWQuota.getMaxQuota());
        DocumentModel titiUW = uwm.getUserPersonalWorkspace("titi",
                session.getDocument(new PathRef("/default-domain/")));
        QuotaAware titiUWQuota = titiUW.getAdapter(QuotaAware.class);
        assertEquals(300L, titiUWQuota.getMaxQuota());

        // create content in the 2 user workspaces
        DocumentModel firstFile = session.createDocumentModel(totoUW.getPathAsString(), "file1", "File");
        firstFile.setPropertyValue("file:content", createFakeBlob(200));
        firstFile = session.createDocument(firstFile);
        session.saveDocument(firstFile);

        DocumentModel secondFile = session.createDocumentModel(titiUW.getPathAsString(), "file2", "File");
        secondFile.setPropertyValue("file:content", createFakeBlob(200));
        secondFile = session.createDocument(secondFile);
        session.saveDocument(secondFile);

        coreFeature.waitForAsyncCompletion(); // commit the transaction

        totoUW = uwm.getUserPersonalWorkspace("toto", session.getDocument(new PathRef("/default-domain/")));
        totoUWQuota = totoUW.getAdapter(QuotaAware.class);
        assertEquals(200L, totoUWQuota.getTotalSize());
        titiUW = uwm.getUserPersonalWorkspace("titi", session.getDocument(new PathRef("/default-domain/")));
        titiUWQuota = titiUW.getAdapter(QuotaAware.class);
        assertEquals(200L, titiUWQuota.getTotalSize());

        try {
            secondFile = session.createDocumentModel(titiUW.getPathAsString(), "file2", "File");
            secondFile.setPropertyValue("file:content", createFakeBlob(200));
            secondFile = session.createDocument(secondFile);
            session.saveDocument(secondFile);
            fail("Should have failed due to quota exceeded");
        } catch (Exception e) {
            assertTrue("Should have failed with a QuotaExceededException cause",
                    e.getCause() instanceof QuotaExceededException);
        }

        try {
            firstFile = session.createDocumentModel(totoUW.getPathAsString(), "file1", "File");
            firstFile.setPropertyValue("file:content", createFakeBlob(200));
            firstFile = session.createDocument(firstFile);
            session.saveDocument(firstFile);
            fail("Should have failed due to quota exceeded");
        } catch (Exception e) {
            assertTrue("Should have failed with a QuotaExceededException cause",
                    e.getCause() instanceof QuotaExceededException);
        }
    }

    protected void addContent() {
        addContent(false);
    }

    protected void addContent(final boolean checkInFirstFile) {
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        ws = session.saveDocument(ws);
        wsRef = ws.getRef();

        DocumentModel firstFolder = session.createDocumentModel(ws.getPathAsString(), "folder1", "Folder");
        firstFolder = session.createDocument(firstFolder);
        firstFolderRef = firstFolder.getRef();

        DocumentModel firstSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder1",
                "Folder");
        firstSubFolder = session.createDocument(firstSubFolder);

        firstSubFolderRef = firstSubFolder.getRef();

        DocumentModel firstFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file1", "File");
        firstFile.setPropertyValue("file:content", createFakeBlob(100));
        firstFile = session.createDocument(firstFile);
        if (checkInFirstFile) {
            firstFile.checkIn(VersioningOption.MINOR, null);
        }

        firstFileRef = firstFile.getRef();

        DocumentModel secondFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file2", "File");
        secondFile.setPropertyValue("file:content", createFakeBlob(200));

        secondFile = session.createDocument(secondFile);
        secondFileRef = secondFile.getRef();

        DocumentModel secondSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder2",
                "Folder");
        secondSubFolder = session.createDocument(secondSubFolder);
        secondSubFolderRef = secondSubFolder.getRef();

        DocumentModel secondFolder = session.createDocumentModel(ws.getPathAsString(), "folder2", "Folder");
        secondFolder = session.createDocument(secondFolder);
        secondFolderRef = secondFolder.getRef();
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doMoveContent() {
        session.move(firstFileRef, secondSubFolderRef, null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doMoveFolderishContent() {
        session.move(firstSubFolderRef, secondFolderRef, null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doMoveFileContent() {
        session.move(firstFileRef, secondFolderRef, null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doUpdateContent() {
        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);

        ws.setPropertyValue("file:content", createFakeBlob(50));
        session.saveDocument(ws);

        List<Map<String, Serializable>> files = new ArrayList<>();

        for (int i = 1; i < 5; i++) {
            Map<String, Serializable> files_entry = new HashMap<>();
            files_entry.put("file", createFakeBlob(70));
            files.add(files_entry);
        }

        firstFile.setPropertyValue("files:files", (Serializable) files);
        session.saveDocument(firstFile);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doCheckIn() {
        DocumentModel firstFile = session.getDocument(firstFileRef);
        firstFile.checkIn(VersioningOption.MINOR, null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doCheckOut() {
        DocumentModel firstFile = session.getDocument(firstFileRef);
        firstFile.checkOut();
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doUpdateAndVersionContent() {
        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);

        ws.setPropertyValue("file:content", createFakeBlob(50));
        session.saveDocument(ws);

        List<Map<String, Serializable>> files = new ArrayList<>();

        for (int i = 1; i < 5; i++) {
            Map<String, Serializable> files_entry = new HashMap<>();
            files_entry.put("file", createFakeBlob(70));
            files.add(files_entry);
        }

        firstFile.setPropertyValue("files:files", (Serializable) files);
        // create minor version
        firstFile.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        session.saveDocument(firstFile);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doSimpleVersion() {
        DocumentModel firstFile = session.getDocument(firstFileRef);
        firstFile.setPropertyValue("dc:title", "a version");
        firstFile.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        session.saveDocument(firstFile);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doRemoveFirstVersion() {
        List<DocumentModel> versions = session.getVersions(firstFileRef);
        session.removeDocument(versions.get(0).getRef());
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doRemoveContent() {
        session.removeDocument(firstFileRef);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doRemoveFolderishContent() {
        session.removeDocument(firstSubFolderRef);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doDeleteFileContent() {
        List<DocumentModel> docs = new ArrayList<>();
        docs.add(session.getDocument(firstFileRef));
        Framework.getService(TrashService.class).trashDocuments(docs);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doDeleteFileContent(final DocumentRef fileRef) {
        DocumentModel doc = session.getDocument(fileRef);
        Framework.getService(TrashService.class).trashDocument(doc);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doUndeleteFileContent() {
        DocumentModel doc = session.getDocument(firstFileRef);
        Framework.getService(TrashService.class).untrashDocument(doc);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doCopyContent() {
        session.copy(firstFileRef, secondSubFolderRef, null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void doCopyFolderishContent() {
        session.copy(firstSubFolderRef, secondFolderRef, null);
        coreFeature.waitForAsyncCompletion(); // commit the transaction
    }

    protected void dump() {
        if (Boolean.TRUE.booleanValue()) {
            return;
        }
        System.out.println("\n####################################\n");
        DocumentModelList docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            if (doc.isVersion()) {
                System.out.print(" --version ");
            }
            System.out.print(doc.getId() + " " + doc.getPathAsString());
            if (doc.hasSchema("uid")) {
                System.out.print(" (" + doc.getPropertyValue("uid:major_version") + "."
                        + doc.getPropertyValue("uid:minor_version") + ")");
            }

            if (doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                QuotaAware qa = doc.getAdapter(QuotaAware.class);
                System.out.println(" " + qa.getQuotaInfo());
                // System.out.println(" with Quota facet");
            } else {
                System.out.println(" no Quota facet !!!");
            }
        }

    }

    protected DocumentModel getWorkspace() {
        return session.getDocument(wsRef);
    }

    protected DocumentModel getFirstSubFolder() {
        return session.getDocument(firstSubFolderRef);
    }

    protected DocumentModel getSecondSubFolder() {
        return session.getDocument(secondSubFolderRef);
    }

    protected DocumentModel getFirstFolder() {
        return session.getDocument(firstFolderRef);
    }

    protected DocumentModel getSecondFolder() {
        return session.getDocument(secondFolderRef);
    }

    protected DocumentModel getSecondFile() {
        return session.getDocument(secondFileRef);
    }

    protected DocumentModel getFirstFile() {
        return session.getDocument(firstFileRef);
    }

    protected void createUser(String username) {
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue("username", username);
        userManager.createUser(user);
    }
}
