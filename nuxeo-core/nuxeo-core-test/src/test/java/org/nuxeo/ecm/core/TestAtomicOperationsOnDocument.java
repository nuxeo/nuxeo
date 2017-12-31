/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.LockHelper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.kv")
public class TestAtomicOperationsOnDocument {

    protected static final int NB_THREADS = 5;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    protected class GetOrCreateDocumentThread implements Runnable {

        protected CountDownLatch latch;

        protected DocumentModel documentModel;

        public GetOrCreateDocumentThread(DocumentModel documentModel) {
            this(documentModel, null);
        }

        public GetOrCreateDocumentThread(DocumentModel documentModel, CountDownLatch latch) {
            this.latch = latch;
            this.documentModel = documentModel;
        }

        @Override
        public void run() {

            TransactionHelper.runInTransaction(() -> {
                try (CloseableCoreSession s = CoreInstance.openCoreSession(coreFeature.getRepositoryName())) {
                    documentModel = LockHelper.doAtomically(computeKey(s, documentModel), () -> {
                        DocumentRef ref = documentModel.getRef();
                        if (s.exists(ref)) {
                            return s.getDocument(ref);
                        }
                        if (latch != null) {
                            // Simulate a thread that keeps a document locked
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt(); // restore interrupt
                                throw new NuxeoException(e);
                            }
                        }
                        return s.createDocument(documentModel);
                    });
                } catch (ConcurrentUpdateException e) {
                    if (latch != null) {
                        latch.countDown();
                    }
                    throw e;
                }
            });
        }
    }

    protected String computeKey(CoreSession session, DocumentModel docModel) {
        String repositoryName = docModel.getRepositoryName();
        String parentId = session.getDocument(docModel.getParentRef()).getId();
        String name = docModel.getName();
        return repositoryName + "-" + parentId + "-" + name;
    }

    @Test
    public void testCreateDocument() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        assertFalse(session.exists(doc.getRef()));

        GetOrCreateDocumentThread t = new GetOrCreateDocumentThread(doc);
        Thread thread = new Thread(t);
        thread.start();
        thread.join();

        doc = t.documentModel;

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertTrue(session.exists(doc.getRef()));

    }

    @Test
    public void testGetDocument() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertTrue(session.exists(doc.getRef()));

        GetOrCreateDocumentThread t = new GetOrCreateDocumentThread(doc);
        Thread thread = new Thread(t);
        thread.start();
        thread.join();
        DocumentModel fetchedDoc = t.documentModel;

        assertEquals(doc.getId(), fetchedDoc.getId());
        assertTrue(session.exists(doc.getRef()));

    }

    @Test
    public void testConcurrentUpdateException() throws Exception {

        DocumentModel doc = session.createDocumentModel("/", "file", "File");

        CountDownLatch latch = new CountDownLatch(NB_THREADS - 1);
        MutableObject<String> me = new MutableObject<>();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < NB_THREADS; i++) {
            threads.add(new Thread(new GetOrCreateDocumentThread(doc, latch)));
        }

        // One of the threads will fail with a concurrent update exception thanks to the latch,
        // so check every one of them to keep error message if this exception occurs.
        threads.forEach(t -> {
            t.setUncaughtExceptionHandler((thread, throwable) -> me.setValue(throwable.getMessage()));
            t.start();
        });

        for (Thread t : threads) {
            t.join();
        }

        assertEquals("Failed to acquire the lock on key " + computeKey(session, doc), me.getValue());

    }

}
