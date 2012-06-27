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
import static org.nuxeo.ecm.quota.size.DocumentsCountAndSizeUpdater.DOCUMENTS_SIZE_STATISTICS_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@TransactionalConfig(autoStart = false)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.quota.core")
@LocalDeploy("org.nuxeo.ecm.quota.core:quotastats-size-contrib.xml")
public class TestDocumentsCountAndSizeUpdater {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected FeaturesRunner featureRunner;

    protected DocumentRef wsRef;

    protected DocumentRef firstFolderRef;

    protected DocumentRef secondFolderRef;

    protected DocumentRef firstSubFolderRef;

    protected DocumentRef secondSubFolderRef;

    protected DocumentRef firstFileRef;

    protected DocumentRef secondFileRef;

    protected static final boolean verboseMode = false;

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
        session.save();
        wsRef = ws.getRef();

        DocumentModel firstFolder = session.createDocumentModel(
                ws.getPathAsString(), "folder1", "Folder");
        firstFolder = session.createDocument(firstFolder);
        session.save();
        firstFolderRef = firstFolder.getRef();

        DocumentModel firstSubFolder = session.createDocumentModel(
                firstFolder.getPathAsString(), "subfolder1", "Folder");
        firstSubFolder = session.createDocument(firstSubFolder);
        session.save();
        firstSubFolderRef = firstSubFolder.getRef();

        DocumentModel firstFile = session.createDocumentModel(
                firstSubFolder.getPathAsString(), "file1", "File");
        firstFile.setPropertyValue("file:content",
                (Serializable) getFakeBlob(100));
        firstFile = session.createDocument(firstFile);

        session.save();
        firstFileRef = firstFile.getRef();

        DocumentModel secondFile = session.createDocumentModel(
                firstSubFolder.getPathAsString(), "file2", "File");
        secondFile.setPropertyValue("file:content",
                (Serializable) getFakeBlob(200));

        secondFile = session.createDocument(secondFile);
        session.save();
        secondFileRef = secondFile.getRef();

        DocumentModel secondSubFolder = session.createDocumentModel(
                firstFolder.getPathAsString(), "subfolder2", "Folder");
        secondSubFolder = session.createDocument(secondSubFolder);
        session.save();
        secondSubFolderRef = secondSubFolder.getRef();

        DocumentModel secondFolder = session.createDocumentModel(
                ws.getPathAsString(), "folder2", "Folder");
        secondFolder = session.createDocument(secondFolder);
        session.save();
        secondFolderRef = secondFolder.getRef();

        session.save();
        TransactionHelper.commitOrRollbackTransaction();

        eventService.waitForAsyncCompletion();
    }

    protected void doMoveContent() throws Exception {

        TransactionHelper.startTransaction();

        session.move(firstFileRef, secondSubFolderRef, null);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doMoveFolderishContent() throws Exception {

        TransactionHelper.startTransaction();

        session.move(firstSubFolderRef, secondFolderRef, null);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doUpdateContent() throws Exception {
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
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    protected void doRemoveContent() throws Exception {
        TransactionHelper.startTransaction();
        session.removeDocument(firstFileRef);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doRemoveFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        session.removeDocument(firstSubFolderRef);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doCopyContent() throws Exception {
        TransactionHelper.startTransaction();
        session.copy(firstFileRef, secondSubFolderRef, null);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doCopyFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        session.copy(firstSubFolderRef, secondFolderRef, null);
        session.save();
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

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

        dump();

        doMoveContent();

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

        dump();

        doRemoveContent();

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

        dump();

        doCopyContent();

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

        dump();

        doCopyFolderishContent();

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

        dump();

        doRemoveFolderishContent();

        // do not remove this
        // or invalidations do not work
        session.save();

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

        // do not remove this
        // or invalidations do not work
        session.save();

        dump();

        doMoveFolderishContent();

        // do not remove this
        // or invalidations do not work
        session.save();

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

}
