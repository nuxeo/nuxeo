/*******************************************************************************
 *  (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *  
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Lesser General Public License
 *  (LGPL) version 2.1 which accompanies this distribution, and is available at
 *  http://www.gnu.org/licenses/lgpl.html
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.nuxeo.ecm.automation.core.operations.document.AtomicFolderCreator;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.automation.test.RestFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.critical.AtomicGetOrCreateFolderRunner;
import org.nuxeo.ecm.core.api.critical.CriticalSectionRunner;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.TestRepositoryHandler;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.runner.critical-section")
@Features({ RestFeature.class })
@Jetty(port = 18080, propagateNaming=true)
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
public class CanRunCriticalFoldersTest {

    @Inject
    CoreSession session;

    @Inject
    RepositorySettings settings;

    protected class Synchronizer {

        int count = 0;

        final int max;

        protected Synchronizer(int max) {
            this.max = max;
        }

        public synchronized void waitOthers() throws InterruptedException {
            if (++count >= max) {
                this.notifyAll();
            } else {
                this.wait();
            }
        }
    }

    protected class SynchronizedHierarchyCreator implements Runnable {

        protected DocumentRef noteRef;

        protected Throwable error;

        protected final Synchronizer syncher;

        protected final String path;

        protected final String name;

        protected SynchronizedHierarchyCreator(Synchronizer syncher,
                String path, String name) {
            this.syncher = syncher;
            this.path = path;
            this.name = name;
        }

        @Override
        public void run() {
            TransactionHelper.startTransaction();
            try {
                syncher.waitOthers();
                openAndCreate();
            } catch (Throwable e) {
                error = e;
            } finally {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }

        private void openAndCreate() throws ClientException,
                InterruptedException {
            TestRepositoryHandler repo = settings.getRepositoryHandler();
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            final String username = getClass().getSimpleName();
            UserPrincipal principal = new UserPrincipal(username,
                    new ArrayList<String>(), false, true);
            context.put("username", username);
            context.put("principal", principal);
            CoreSession session = repo.openSession(context);
            try {
                AtomicFolderCreator creator = new AtomicFolderCreator(
                        session, path);
                creator.run();
                DocumentModel note = session.createDocumentModel("Note");
                note.setPathInfo(path, name);
                note = session.createDocument(note);
                noteRef = note.getRef();
            } finally {
                repo.releaseSession(session);
            }
        }

    }

    protected void assertCreator(SynchronizedHierarchyCreator creator) {
        assertNotNull(creator.noteRef);
        assertNull(creator.error);
    }

    @Test
    public void canInjectTwoNotesWithConcurrency() throws InterruptedException,
            ClientException {
        Synchronizer syncher = new Synchronizer(2);
        String path = "/oups/la/ops";
        SynchronizedHierarchyCreator creator1 = new SynchronizedHierarchyCreator(
                syncher, path, "note1");
        SynchronizedHierarchyCreator creator2 = new SynchronizedHierarchyCreator(
                syncher, path, "note2");

        Thread t1 = new Thread(creator1, "t1");
        Thread t2 = new Thread(creator2, "t2");

        try {
            t1.start();
            t2.start();
        } finally {
            try {
                t1.join();
            } finally {
                t2.join();
            }
        }

        assertCreator(creator1);
        assertCreator(creator2);
        DocumentModel note1 = session.getDocument(creator1.noteRef);
        DocumentModel parent1 = session.getDocument(note1.getParentRef());
        DocumentModel note2 = session.getDocument(creator2.noteRef);
        DocumentModel parent2 = session.getDocument(note2.getParentRef());

        assertEquals(parent1.getPath(), parent2.getPath());
        assertEquals(parent1.getRef(), parent2.getRef());

    }

    @Inject
    AutomationServer server;

    @Inject
    AutomationService service;


    @Inject
    Session clientSession;

    @Test
    public void singleGetOrCreateThroughAutomation() throws Exception {
        Document doc = (Document) clientSession.newRequest(AtomicGetOrCreateFolderRunner.ID).set(
                "path", "/test").execute();
        assertNotNull(doc);
    }


    @Inject
    HttpAutomationClient client;
    
    protected class ClientRunner implements Runnable {

        Throwable error;
        
        final int maxFolders;

        final Random rand;

        public ClientRunner(int maxFolders, Random rand) {
            this.maxFolders = maxFolders;
            this.rand = rand;
        }

        @Override
        public void run() {
            Session session = client.getSession("Administrator",
                    "Administrator");
            for (int i = 0; i < maxFolders; ++i) {
                int month = rand.nextInt(12);
                int day = rand.nextInt(31);
                String path = "/test/" + month + "/" + day;
                try {
                    session.newRequest(AtomicGetOrCreateFolderRunner.ID).set("path", path).execute();
                } catch (Exception e) {
                    error = e;
                    throw new RuntimeException("Cannot create folder " + path,
                            e);
                }
            }
        }

    }

    @Test
    public void massGetOrCreateThroughAutomation() throws Exception {
        Random rand = new Random(0);
        ClientRunner runner1 = new ClientRunner(100, rand);
        ClientRunner runner2 = new ClientRunner(100, rand);
        ClientRunner runner3 = new ClientRunner(100, rand);

        Thread t1 = new Thread(runner1, "t1");
        Thread t2 = new Thread(runner2, "t2");
        Thread t3 = new Thread(runner3, "t3");

        t1.start();
        t2.start();
        t3.start();

        try {
            t1.join();
        } finally {
            try {
                t2.join();
            } finally {
                t3.join();
            }
        }
        
        assertNull(runner1.error);
        assertNull(runner2.error);
        assertNull(runner3.error);
        
        CriticalSectionRunner.resetTransaction(session);
        assertNoDuplicates();
    }
    
    protected void assertNoDuplicates() throws ClientException {
        for (DocumentModel doc:session.query("SELECT * FROM Folder WHERE ecm:path startswith '/test'")) {
            assertEquals(doc.getName(), doc.getTitle());
        }
    }
}
