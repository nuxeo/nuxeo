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
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.versioning.VersioningService;
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
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@TransactionalConfig(autoStart = false)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.quota.core" })
public class TestDocumentsSizeUpdater {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

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
        addContent(false);
    }

    protected void addContent(boolean checkInFirstFile) throws Exception {
        TransactionHelper.startTransaction();
        try {
            DocumentModel ws = session.createDocumentModel("/", "ws",
                    "Workspace");
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
            if (checkInFirstFile) {
                firstFile.checkIn(VersioningOption.MINOR, null);
            }

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
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doMoveContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            session.move(firstFileRef, secondSubFolderRef, null);

        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doMoveFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            session.move(firstSubFolderRef, secondFolderRef, null);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doMoveFileContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            session.move(firstFileRef, secondFolderRef, null);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doUpdateContent() throws Exception {
        if (verboseMode) {
            System.out.println("Update content");
        }
        TransactionHelper.startTransaction();
        try {
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
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        Thread.sleep(1000);
        eventService.waitForAsyncCompletion();
        dispose(session);

    }

    protected void doCheckIn() throws Exception {
        if (verboseMode) {
            System.out.println("CheckIn first file");
        }
        TransactionHelper.startTransaction();
        try {
            DocumentModel firstFile = session.getDocument(firstFileRef);
            firstFile.checkIn(VersioningOption.MINOR, null);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doCheckOut() throws Exception {
        if (verboseMode) {
            System.out.println("CheckOut first file");
        }
        TransactionHelper.startTransaction();
        try {
            DocumentModel firstFile = session.getDocument(firstFileRef);
            firstFile.checkOut();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doUpdateAndVersionContent() throws Exception {
        if (verboseMode) {
            System.out.println("Update content and create version ");
        }
        TransactionHelper.startTransaction();
        try {
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
            // create minor version
            firstFile.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
            firstFile = session.saveDocument(firstFile);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        Thread.sleep(1000);
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doSimpleVersion() throws Exception {
        if (verboseMode) {
            System.out.println("simply create a version ");
        }
        TransactionHelper.startTransaction();
        try {
            DocumentModel firstFile = session.getDocument(firstFileRef);

            firstFile.setPropertyValue("dc:title", "a version");
            firstFile.putContextData(VersioningService.VERSIONING_OPTION,
                    VersioningOption.MINOR);
            firstFile = session.saveDocument(firstFile);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        Thread.sleep(1000);
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doRemoveFirstVersion() throws Exception {
        if (verboseMode) {
            System.out.println("remove first created version ");
        }
        TransactionHelper.startTransaction();
        try {
            List<DocumentModel> versions = session.getVersions(firstFileRef);

            if (verboseMode) {

            }
            session.removeDocument(versions.get(0).getRef());
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        Thread.sleep(1000);
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doRemoveContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            session.removeDocument(firstFileRef);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doRemoveFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        session.removeDocument(firstSubFolderRef);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void doDeleteFileContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            List<DocumentModel> docs = new ArrayList<DocumentModel>();
            docs.add(session.getDocument(firstFileRef));
            Framework.getLocalService(TrashService.class).trashDocuments(docs);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doUndeleteFileContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            List<DocumentModel> docs = new ArrayList<DocumentModel>();
            docs.add(session.getDocument(firstFileRef));
            Framework.getLocalService(TrashService.class).undeleteDocuments(
                    docs);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doCopyContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            session.copy(firstFileRef, secondSubFolderRef, null);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    protected void doCopyFolderishContent() throws Exception {
        TransactionHelper.startTransaction();
        try {
            session.copy(firstSubFolderRef, secondFolderRef, null);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        dispose(session);
    }

    @SuppressWarnings("unused")
    protected void dump() throws Exception {

        if (!verboseMode) {
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
                System.out.print(" ("
                        + doc.getPropertyValue("uid:major_version") + "."
                        + doc.getPropertyValue("uid:minor_version") + ")");
            }

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
        assertEquals("inner:" + innerSize + " total:" + totalSize, "inner:"
                + qa.getInnerSize() + " total:" + qa.getTotalSize());
    }

    protected void assertQuota(DocumentModel doc, long innerSize,
            long totalSize, long trashSize) {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals("inner:" + innerSize + " total:" + totalSize + " trash:"
                + trashSize,
                "inner:" + qa.getInnerSize() + " total:" + qa.getTotalSize()
                        + " trash:" + qa.getTrashSize());
    }

    protected void assertQuota(DocumentModel doc, long innerSize,
            long totalSize, long trashSize, long versionsSize) {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals(
                "inner:" + innerSize + " total:" + totalSize + " trash:"
                        + trashSize + " versions: " + versionsSize,
                "inner:" + qa.getInnerSize() + " total:" + qa.getTotalSize()
                        + " trash:" + qa.getTrashSize() + " versions: "
                        + qa.getVersionsSize());
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
    public void testQuotaInitialCheckIn() throws Exception {

        addContent(true);

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        // checked in: not counted twice in total
        assertQuota(firstFile, 100, 100, 0, 100);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 300, 0, 100);
        assertQuota(firstFolder, 0, 300, 0, 100);
        assertQuota(ws, 0, 300, 0, 100);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // checkout the doc
        doCheckOut();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);

        assertTrue(firstFile.isCheckedOut());
        assertEquals("0.1+", firstFile.getVersionLabel());

        secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 100, 200, 0, 100);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 400, 0, 100);
        assertQuota(firstFolder, 0, 400, 0, 100);
        assertQuota(ws, 0, 400, 0, 100);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    @Test
    public void testQuotaOnCheckInCheckOut() throws Exception {

        addContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertTrue(firstFile.isCheckedOut());
        assertEquals("0.0", firstFile.getVersionLabel());

        assertQuota(firstFile, 100, 100, 0, 0);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 300, 0, 0);
        assertQuota(firstFolder, 0, 300, 0, 0);
        assertQuota(ws, 0, 300, 0, 0);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // checkin doc
        doCheckIn();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);

        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        // checked in: not counted twice in total
        assertQuota(firstFile, 100, 100, 0, 100);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 300, 0, 100);
        assertQuota(firstFolder, 0, 300, 0, 100);
        assertQuota(ws, 0, 300, 0, 100);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // checkout the doc
        doCheckOut();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);

        assertTrue(firstFile.isCheckedOut());
        assertEquals("0.1+", firstFile.getVersionLabel());

        // after checkout total includes all versions
        assertQuota(firstFile, 100, 200, 0, 100);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 400, 0, 100);
        assertQuota(firstFolder, 0, 400, 0, 100);
        assertQuota(ws, 0, 400, 0, 100);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    @Test
    public void testQuotaOnVersions() throws Exception {

        addContent();

        TransactionHelper.startTransaction();

        dump();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);

        assertQuota(firstFile, 100, 100, 0, 0);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 300, 0, 0);
        assertQuota(firstFolder, 0, 300, 0, 0);
        assertQuota(ws, 0, 300, 0, 0);

        TransactionHelper.commitOrRollbackTransaction();

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);

        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.1", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 380, 0, 380);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 580, 0, 380);
        assertQuota(firstFolder, 0, 580, 0, 380);
        assertQuota(ws, 50, 630, 0, 380);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // create another version
        doSimpleVersion();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);

        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.2", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 760, 0, 760);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 960, 0, 760);
        assertQuota(firstFolder, 0, 960, 0, 760);
        assertQuota(ws, 50, 1010, 0, 760);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // remove a version
        doRemoveFirstVersion();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);

        assertFalse(firstFile.isCheckedOut());
        assertEquals("0.2", firstFile.getVersionLabel());

        assertQuota(firstFile, 380, 380, 0, 380);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 580, 0, 380);
        assertQuota(firstFolder, 0, 580, 0, 380);
        assertQuota(ws, 50, 630, 0, 380);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // remove doc and associated version

        doRemoveContent();
        eventService.waitForAsyncCompletion();

        TransactionHelper.startTransaction();

        dump();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        secondFile = session.getDocument(secondFileRef);

        assertFalse(session.exists(firstFileRef));

        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0L, 200L, 0L, 0L);
        assertQuota(firstFolder, 0L, 200L, 0L, 0L);
        assertQuota(ws, 50L, 250L, 0L, 0L);

        TransactionHelper.commitOrRollbackTransaction();

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
        doRemoveContent();
        dump();

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
    public void testQuotaExceededOnVersion() throws Exception {

        addContent();

        dump();

        TransactionHelper.startTransaction();

        // now add quota limit
        DocumentModel ws = session.getDocument(wsRef);
        QuotaAware qa = ws.getAdapter(QuotaAware.class);
        assertNotNull(qa);

        assertEquals(300L, qa.getTotalSize());

        assertEquals(-1L, qa.getMaxQuota());

        // set the quota to 350
        qa.setMaxQuota(350L, true);

        TransactionHelper.commitOrRollbackTransaction();

        dump();

        TransactionHelper.startTransaction();

        // create a version
        DocumentModel firstFile = session.getDocument(firstFileRef);
        firstFile.checkIn(VersioningOption.MINOR, null);

        boolean canNotExceedQuota = false;
        try {
            // now try to checkout
            firstFile.checkOut();
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

        dump();

        assertTrue(canNotExceedQuota);

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

    @Test
    public void testQuotaOnDeleteContent() throws Exception {
        addContent();
        doDeleteFileContent();
        TransactionHelper.startTransaction();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);

        assertQuota(firstFile, 100L, 100L, 100L);
        assertQuota(firstFolder, 0L, 300L, 100L);
        assertQuota(ws, 0L, 300L, 100L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        doUndeleteFileContent();
        TransactionHelper.startTransaction();

        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstFile = session.getDocument(firstFileRef);

        assertQuota(firstFile, 100L, 100L, 0L);
        assertQuota(firstFolder, 0L, 300L, 0L);
        assertQuota(ws, 0L, 300L, 0L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

        // sent file to trash
        doDeleteFileContent();
        // then permanently delete file when file is in trash
        doRemoveContent();

        TransactionHelper.startTransaction();
        ws = session.getDocument(wsRef);
        firstFolder = session.getDocument(firstFolderRef);

        assertFalse(session.exists(firstFileRef));
        assertQuota(firstFolder, 0L, 200L, 0L);
        assertQuota(ws, 0L, 200L, 0L);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
    }

    @Test
    public void testQuotaOnMoveContentWithVersions() throws Exception {

        addContent();
        // update and create a version
        doUpdateAndVersionContent();
        TransactionHelper.startTransaction();

        DocumentModel ws = session.getDocument(wsRef);
        DocumentModel firstFile = session.getDocument(firstFileRef);
        DocumentModel secondFile = session.getDocument(secondFileRef);
        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        DocumentModel secondFolder = session.getDocument(secondFolderRef);

        assertQuota(firstFile, 380, 380, 0, 380);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 580, 0, 380);
        assertQuota(firstFolder, 0, 580, 0, 380);
        assertQuota(ws, 50, 630, 0, 380);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        doMoveFileContent();

        TransactionHelper.startTransaction();

        ws = session.getDocument(wsRef);
        firstFile = session.getDocument(firstFileRef);
        secondFile = session.getDocument(secondFileRef);
        firstFolder = session.getDocument(firstFolderRef);
        firstSubFolder = session.getDocument(firstSubFolderRef);
        secondFolder = session.getDocument(secondFolderRef);

        assertQuota(firstFile, 380, 380, 0, 380);
        assertQuota(secondFile, 200, 200, 0, 0);
        assertQuota(firstSubFolder, 0, 200, 0, 0);
        assertQuota(firstFolder, 0, 200, 0, 0);
        assertQuota(secondFolder, 0, 380, 0, 380);
        assertQuota(ws, 50, 630, 0, 380);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();

    }

    @Test
    public void testAllowSettingMaxQuota() throws Exception {
        addContent();
        TransactionHelper.startTransaction();

        // now add quota limit
        DocumentModel ws = session.getDocument(wsRef);
        QuotaAware qa = ws.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        // set the quota to 400
        qa.setMaxQuota(400L, true);

        DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
        QuotaAware qaFSF = firstSubFolder.getAdapter(QuotaAware.class);
        qaFSF.setMaxQuota(200L, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        dispose(session);
        TransactionHelper.startTransaction();
        DocumentModel secondFolder = session.getDocument(secondFolderRef);
        QuotaAware qaSecFolder = secondFolder.getAdapter(QuotaAware.class);
        boolean canSetQuota = true;
        try {
            qaSecFolder.setMaxQuota(300L, true);
        } catch (QuotaExceededException e) {
            canSetQuota = false;
        }
        assertFalse(canSetQuota);
        qaSecFolder.setMaxQuota(200L, true);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        dispose(session);
        TransactionHelper.startTransaction();

        secondFolder = session.getDocument(secondFolderRef);
        qaSecFolder = secondFolder.getAdapter(QuotaAware.class);
        assertEquals(200L, qaSecFolder.getMaxQuota());

        DocumentModel firstFolder = session.getDocument(firstFolderRef);
        QuotaAware qaFirstFolder = firstFolder.getAdapter(QuotaAware.class);
        canSetQuota = true;
        try {
            qaFirstFolder.setMaxQuota(50L, true);
        } catch (QuotaExceededException e) {
            canSetQuota = false;
        }
        assertFalse(canSetQuota);

        DocumentModel secondSubFolder = session.getDocument(secondSubFolderRef);
        QuotaAware qaSecSubFolder = secondSubFolder.getAdapter(QuotaAware.class);
        canSetQuota = true;
        try {
            qaSecSubFolder.setMaxQuota(50L, true);
        } catch (QuotaExceededException e) {
            canSetQuota = false;
        }
        assertFalse(canSetQuota);
        TransactionHelper.commitOrRollbackTransaction();
    }

}
