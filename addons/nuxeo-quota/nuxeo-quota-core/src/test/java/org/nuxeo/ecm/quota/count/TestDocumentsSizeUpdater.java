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
 */

package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.quota.size.QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocument;
import org.nuxeo.ecm.quota.size.QuotaExceededException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@TransactionalConfig(autoStart = false)
@Deploy("org.nuxeo.ecm.quota.core")
public class TestDocumentsSizeUpdater {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected FeaturesRunner featureRunner;

    @Inject
    protected RuntimeHarness harness;

    protected DocumentRef wsRef;

    protected DocumentRef firstFolderRef;

    protected DocumentRef secondFolderRef;

    protected DocumentRef firstSubFolderRef;

    protected DocumentRef secondSubFolderRef;

    protected DocumentRef firstFileRef;

    protected DocumentRef secondFileRef;

    protected static final boolean verboseMode = false;

    @Before
    public void cleanupSessionAssociationBeforeTest() throws Exception {
        // temp fix to be sure the session tx
        // will be correctly handled in the test
        dispose(session);
    }

    protected void dispose(CoreSession session) throws Exception {
        if (Proxy.isProxyClass(session.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(session);
            if (handler instanceof TransactionalCoreSessionWrapper) {
                Field field = TransactionalCoreSessionWrapper.class.getDeclaredField("session");
                field.setAccessible(true);
                session = (CoreSession) field.get(handler);
            }
        }
        if (!(session instanceof LocalSession)) {
            throw new UnsupportedOperationException(
                    "Cannot dispose session of class " + session.getClass());
        }
        ((LocalSession) session).getSession().dispose();
    }

    protected Blob getFakeBlob(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        Blob blob = new StringBlob(sb.toString());
        blob.setMimeType("text/plain");
        blob.setFilename("FakeBlob_" + size + ".txt");
        return blob;
    }

    protected void addContent() throws Exception {
        TransactionHelper.startTransaction();

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
        firstFile.setPropertyValue("file:content",
                (Serializable) getFakeBlob(100));
        firstFile = session.createDocument(firstFile);

        firstFileRef = firstFile.getRef();

        DocumentModel secondFile = session.createDocumentModel(
                firstSubFolder.getPathAsString(), "file2", "File");
        secondFile.setPropertyValue("file:content",
                (Serializable) getFakeBlob(200));

        secondFile = session.createDocument(secondFile);
        secondFileRef = secondFile.getRef();

        DocumentModel secondSubFolder = session.createDocumentModel(
                firstFolder.getPathAsString(), "subfolder2", "Folder");
        secondSubFolder = session.createDocument(secondSubFolder);
        secondSubFolderRef = secondSubFolder.getRef();

        DocumentModel secondFolder = session.createDocumentModel(
                ws.getPathAsString(), "folder2", "Folder");
        secondFolder = session.createDocument(secondFolder);
        secondFolderRef = secondFolder.getRef();

        TransactionHelper.commitOrRollbackTransaction();

        eventService.waitForAsyncCompletion();
    }

    protected void doMoveContent() throws Exception {

        TransactionHelper.startTransaction();

        session.move(firstFileRef, secondSubFolderRef, null);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doMoveFolderishContent() throws Exception {

        TransactionHelper.startTransaction();

        session.move(firstSubFolderRef, secondFolderRef, null);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doUpdateContent() throws Exception {
        if (verboseMode) {
            System.out.println("Update content");
        }
        TransactionHelper.startTransaction();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);

        ws.setPropertyValue("file:content", (Serializable) getFakeBlob(50));
        ws = session.saveDocument(ws);

        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();

        for (int i = 1; i < 5; i++) {
            Map<String, Serializable> files_entry = new HashMap<String, Serializable>();
            files_entry.put("filename", "fakefile" + i);
            files_entry.put("file", (Serializable) getFakeBlob(70));
            files.add(files_entry);
        }

        firstFile.setPropertyValue("files:files", (Serializable) files);
        firstFile = session.saveDocument(firstFile);

        TransactionHelper.commitOrRollbackTransaction();
        Thread.sleep(1000);
        eventService.waitForAsyncCompletion();

    }

    protected void doRemoveContent() throws Exception {
        TransactionHelper.startTransaction();
        session.removeDocument(firstFileRef);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doRemoveFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        session.removeDocument(firstSubFolderRef);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doCopyContent() throws Exception {
        TransactionHelper.startTransaction();
        session.copy(firstFileRef, secondSubFolderRef, null);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doCopyFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        session.copy(firstSubFolderRef, secondFolderRef, null);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    @SuppressWarnings("unused")
    protected void dump() throws Exception {

        if (!verboseMode) {
            return;
        }
        System.out.println("\n####################################\n");
        DocumentModelList docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            System.out.print(doc.getId() + " " + doc.getPathAsString());
            if (doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                QuotaAware qa = doc.getAdapter(QuotaAware.class);
                System.out.println(" [ quota : " + qa.getTotalSize() + "("
                        + qa.getInnerSize() + ") / " + qa.getMaxQuota() + "]");
                // System.out.println(" with Quota facet");
            } else {
                System.out.println(" no Quota facet !!!");
            }
        }

    }

    protected void assertQuota(DocumentModel doc, long innerSize, long totalSize) {
        assertTrue(doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET));
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(innerSize, qa.getInnerSize());
        assertEquals(totalSize, qa.getTotalSize());
    }

    @Test
    public void testQuotaOnAddContent() throws Exception {

        addContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 300L);
        assertQuota(firstFolder, 0L, 300L);
        assertQuota(ws, 0L, 300L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnAddAndModifyContent() throws Exception {

        addContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 300L);
        assertQuota(firstFolder, 0L, 300L);
        assertQuota(ws, 0L, 300L);

        TransactionHelper.commitOrRollbackTransaction();

        doUpdateContent();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 380L, 380L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 580L);
        assertQuota(firstFolder, 0L, 580L);
        assertQuota(ws, 50L, 630L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnMoveContent() throws Exception {

        addContent();

        dump();

        doMoveContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 200L);
        assertQuota(secondSubFolder, 0L, 100L);
        assertQuota(firstFolder, 0L, 300L);
        assertQuota(ws, 0L, 300L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnRemoveContent() throws Exception {

        addContent();

        dump();

        doRemoveContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertFalse(session.exists(firstFileRef));

        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 200L);
        assertQuota(firstFolder, 0L, 200L);
        assertQuota(ws, 0L, 200L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnCopyContent() throws Exception {

        addContent();

        dump();

        doCopyContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);
        DocumentModel copiedFile = session.getChildren(secondSubFolderRef).get(
                0);

        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(copiedFile, 100L, 100L);
        assertQuota(firstSubFolder, 0L, 300L);
        assertQuota(secondSubFolder, 0L, 100L);
        assertQuota(firstFolder, 0L, 400L);
        assertQuota(ws, 0L, 400L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnCopyFolderishContent() throws Exception {

        addContent();

        dump();

        doCopyFolderishContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);
        DocumentModel copiedFolder = session.getChildren(secondFolderRef).get(0);

        DocumentModel copiedFirstFile = session.getChild(copiedFolder.getRef(),
                "file1");
        DocumentModel copiedSecondFile = session.getChild(
                copiedFolder.getRef(), "file2");

        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 300L);
        assertQuota(firstFolder, 0L, 300L);
        assertQuota(secondFolder, 0L, 300L);
        assertQuota(copiedFolder, 0L, 300L);
        assertQuota(copiedFirstFile, 100L, 100L);
        assertQuota(copiedSecondFile, 200L, 200L);
        assertQuota(ws, 0L, 600L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnRemoveFoldishContent() throws Exception {
        addContent();

        dump();

        doRemoveFolderishContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        assertFalse(session.exists(firstSubFolderRef));

        assertQuota(firstFolder, 0L, 0L);
        assertQuota(ws, 0L, 0L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaOnMoveFoldishContent() throws Exception {

        addContent();

        dump();

        doMoveFolderishContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel secondFolder = session.getDocument(secondFolderRef);

        DocumentModel firstSubFolder = session.getChildren(secondFolderRef).get(
                0);
        DocumentModel firstFile = session.getChild(firstSubFolder.getRef(),
                "file1");
        DocumentModel secondFile = session.getChild(firstSubFolder.getRef(),
                "file2");

        assertEquals(1, session.getChildren(firstFolderRef).size());

        assertQuota(ws, 0L, 300L);
        assertQuota(firstFolder, 0L, 0L);
        assertQuota(secondFolder, 0L, 300L);
        assertQuota(firstSubFolder, 0L, 300L);
        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testQuotaExceeded() throws Exception {

        addContent();

        dump();

        TransactionHelper.startTransaction();

        // now add quota limit
        DocumentModel ws = session.getDocument(wsRef);
        QuotaAware qa = ws.getAdapter(QuotaAware.class);
        assertNotNull(qa);

        assertEquals(300L, qa.getTotalSize());

        assertEquals(-1L, qa.getMaxQuota());

        boolean canNotSetTooSmallQuota = false;

        // try to set the quota to a too small size
        try {
            qa.setMaxQuota(200L, false);
        } catch (QuotaExceededException e) {
            canNotSetTooSmallQuota = true;
        }
        assertTrue(canNotSetTooSmallQuota);

        // set the quota to 400
        qa.setMaxQuota(400L, true);

        TransactionHelper.commitOrRollbackTransaction();

        dump();

        TransactionHelper.startTransaction();

        boolean canNotExceedQuota = false;
        try {
            // now try to update one
            DocumentModel firstFile = session.getDocument(firstFileRef);
            firstFile.setPropertyValue("file:content",
                    (Serializable) getFakeBlob(250));
            firstFile = session.saveDocument(firstFile);
        } catch (Exception e) {
            if (QuotaExceededException.isQuotaExceededException(e)) {
                System.out.println("raised expected Execption "
                        + QuotaExceededException.unwrap(e).getMessage());
                canNotExceedQuota = true;
            }
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        assertTrue(canNotExceedQuota);

        TransactionHelper.startTransaction();

        // now remove the quota limit
        ws = session.getDocument(wsRef);
        qa = ws.getAdapter(QuotaAware.class);
        assertNotNull(qa);

        assertEquals(300L, qa.getTotalSize());
        assertEquals(400L, qa.getMaxQuota());

        // set the quota to -1 / unlimited
        qa.setMaxQuota(-1L, true);

        TransactionHelper.commitOrRollbackTransaction();

        dump();

        TransactionHelper.startTransaction();

        canNotExceedQuota = false;
        try {
            // now try to update one
            DocumentModel firstFile = session.getDocument(firstFileRef);
            firstFile.setPropertyValue("file:content",
                    (Serializable) getFakeBlob(250));
            firstFile = session.saveDocument(firstFile);
        } catch (Exception e) {
            if (QuotaExceededException.isQuotaExceededException(e)) {
                System.out.println("raised expected Execption "
                        + QuotaExceededException.unwrap(e).getMessage());
                canNotExceedQuota = true;
            }
            TransactionHelper.setTransactionRollbackOnly();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        assertFalse(canNotExceedQuota);

    }

    @Test
    public void testComputeInitialStatistics() throws Exception {

        EventServiceAdmin eventAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventAdmin.setListenerEnabledFlag("quotaStatsListener", false);

        addContent();

        dump();

        TransactionHelper.startTransaction();
        DocumentModel ws = session.getDocument(wsRef);
        assertFalse(ws.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET));

        String updaterName = "documentsSizeUpdater";
        quotaStatsService.launchInitialStatisticsComputation(updaterName,
                session.getRepositoryName());
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        String queueId = workManager.getCategoryQueueId(QuotaStatsInitialWork.CATEGORY_QUOTA_INITIAL);

        workManager.awaitCompletion(queueId, 10, TimeUnit.SECONDS);

        session.save(); // process invalidations

        TransactionHelper.commitOrRollbackTransaction();

        session.save();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 100L, 100L);
        assertQuota(secondFile, 200L, 200L);
        assertQuota(firstSubFolder, 0L, 300L);
        assertQuota(firstFolder, 0L, 300L);
        assertQuota(ws, 0L, 300L);

        TransactionHelper.commitOrRollbackTransaction();
        session.save();

        eventAdmin.setListenerEnabledFlag("quotaStatsListener", true);
    }

}
