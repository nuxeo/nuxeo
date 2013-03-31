/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestSQLBinariesIndexing extends TXSQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestSQLBinariesIndexing.class);

    protected String docId;

    protected DocumentRef docRef;

    protected BlockingWork blockingWork;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        waitForFulltextIndexing();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
    }

    /** Creates doc, doesn't do a session save. */
    protected void createDocument() throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("test"));
        doc = session.createDocument(doc);
        docId = doc.getId();
        docRef = new IdRef(docId);
    }

    /**
     * Work that waits in the fulltext updater queue, blocking other indexing
     * work, until the main thread tells it to go ahead.
     */
    public static class BlockingWork extends AbstractWork {

        protected CountDownLatch readyLatch = new CountDownLatch(1);

        protected CountDownLatch startLatch = new CountDownLatch(1);

        @Override
        public String getCategory() {
            return "fulltextUpdater";
        }

        @Override
        public String getTitle() {
            return "Blocking Work";
        }

        @Override
        public void work() throws Exception {
            setStatus("Blocking");
            readyLatch.countDown();
            startLatch.await();
            setStatus("Released");
        }
    }

    protected void blockFulltextUpdating() throws InterruptedException {
        blockingWork = new BlockingWork();
        Framework.getLocalService(WorkManager.class).schedule(blockingWork);
        blockingWork.readyLatch.await();
    }

    protected void allowFulltextUpdating() throws ClientException {
        blockingWork.startLatch.countDown();
        blockingWork = null;
        waitForFulltextIndexing();
    }

    protected void flush() throws ClientException {
        session.save();
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        openSession();
    }

    protected int indexedDocs() throws ClientException {
        DocumentModelList res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        return res.size();
    }

    protected int jobDocs() throws ClientException {
        String request = String.format(
                "SELECT * from Document where ecm:fulltextJobId = '%s'", docId);
        return session.query(request).size();
    }

    @Override
    public void waitForFulltextIndexing() {
        try {
            flush(); // also starts a new tx, which will allow progress
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        super.waitForFulltextIndexing();
    }

    @Test
    public void testBinariesAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            flush();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());
        } finally {
            allowFulltextUpdating();
        }

        flush();
        assertEquals(0, jobDocs());
        assertEquals(1, indexedDocs());
    }

    @Test
    public void testCopiesAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            flush();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());

            session.copy(docRef, session.getRootDocument().getRef(), "copy").getRef();

            // check copy is part of requested
            flush();
            assertEquals(2, jobDocs());
        } finally {
            allowFulltextUpdating();
        }

        // check copy is indexed also
        flush();
        assertEquals(0, jobDocs());
        assertEquals(2, indexedDocs());

        // check copy doesn't stay linked to doc
        DocumentModel doc = session.getDocument(docRef);
        doc.getAdapter(BlobHolder.class).setBlob(new StringBlob("other"));
        session.saveDocument(doc);

        waitForFulltextIndexing();

        assertEquals(1, indexedDocs());
    }

    @Test
    public void testVersionsAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            flush();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());

            session.checkIn(docRef, null, null);
            flush();
        } finally {
            allowFulltextUpdating();
        }

        assertEquals(2, indexedDocs());
    }

}
