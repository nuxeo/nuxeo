/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.convert.plugins")
public class TestSQLBinariesIndexing {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    protected String docId;

    protected DocumentRef docRef;

    protected BlockingWork blockingWork;

    protected void waitForFulltextIndexing() {
        nextTransaction();
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    /** Creates doc, doesn't do a session save. */
    protected void createDocument() {
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(Blobs.createBlob("test"));
        doc = session.createDocument(doc);
        docId = doc.getId();
        docRef = new IdRef(docId);
    }

    protected static CountDownLatch readyLatch;

    protected static CountDownLatch startLatch;

    /**
     * Work that waits in the fulltext updater queue, blocking other indexing work, until the main thread tells it to go
     * ahead.
     */
    public static class BlockingWork extends AbstractWork {

        private static final long serialVersionUID = 1L;

        @Override
        public String getCategory() {
            return "fulltextUpdater";
        }

        @Override
        public String getTitle() {
            return "Blocking Work";
        }

        @Override
        public void work() {
            setStatus("Blocking");
            readyLatch.countDown();
            try {
                startLatch.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                startLatch = null;
            }
            setStatus("Released");
        }
    }

    protected void blockFulltextUpdating() throws InterruptedException {
        startLatch = new CountDownLatch(1);
        readyLatch = new CountDownLatch(1);
        blockingWork = new BlockingWork();
        Framework.getService(WorkManager.class).schedule(blockingWork);
        try {
            readyLatch.await(1, TimeUnit.MINUTES);
        } finally {
            readyLatch = null;
        }
    }

    protected void allowFulltextUpdating() {
        startLatch.countDown();
        blockingWork = null;
        waitForFulltextIndexing();
    }

    protected int indexedDocs() {
        DocumentModelList res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        return res.size();
    }

    protected int jobDocs() {
        String request = String.format("SELECT * from Document where ecm:fulltextJobId = '%s'", docId);
        return session.query(request).size();
    }

    @Test
    public void testBinariesAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            session.save();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());
        } finally {
            allowFulltextUpdating();
        }

        waitForFulltextIndexing();
        assertEquals(0, jobDocs());
        assertEquals(1, indexedDocs());
    }

    @Test
    public void testCopiesAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            session.save();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());

            session.copy(docRef, session.getRootDocument().getRef(), "copy").getRef();

            // check copy is part of requested
            session.save();
            assertEquals(2, jobDocs());
        } finally {
            allowFulltextUpdating();
        }

        // check copy is indexed also
        waitForFulltextIndexing();
        assertEquals(0, jobDocs());
        assertEquals(2, indexedDocs());

        // check copy doesn't stay linked to doc
        DocumentModel doc = session.getDocument(docRef);
        doc.getAdapter(BlobHolder.class).setBlob(Blobs.createBlob("other"));
        session.saveDocument(doc);

        waitForFulltextIndexing();

        assertEquals(1, indexedDocs());
    }

    @Test
    public void testVersionsAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            session.save();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());

            session.checkIn(docRef, null, null);
            session.save();
        } finally {
            allowFulltextUpdating();
        }

        waitForFulltextIndexing();
        assertEquals(2, indexedDocs());
    }

}
