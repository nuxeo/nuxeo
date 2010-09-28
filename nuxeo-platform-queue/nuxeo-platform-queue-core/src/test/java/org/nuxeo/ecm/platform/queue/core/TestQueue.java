/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.queue.core;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.management.storage.DocumentStoreManager;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Testing the queue manager.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class TestQueue extends QueueTestCase {

    public static final Log log = LogFactory.getLog(TestQueue.class);


    public void testNuxeoPersister() throws Exception {
        DocumentQueuePersister<FakeContent> persister = new DocumentQueuePersister<FakeContent>("fake", FakeContent.class);
        URI ownerName = new URI("test");
        URI contentName = new URI("test:test1");
        persister.addContent(ownerName, contentName, new FakeContent());

        // Testing the document is in the nuxeo repo
        TestRunner runner = new TestRunner(Framework.getLocalService(
                RepositoryManager.class).getDefaultRepository().getName());
        runner.runUnrestricted();

        // testing retrieving content from the persister
        List<QueueInfo<FakeContent>> items = persister.listKnownItems();
        assertEquals("Single content in queue", items.size(), 1);
        QueueInfo<FakeContent> info = items.get(0);

        // additional info
        assertEquals("Fake content is", "fake", info.getContent().fake);
        // owner
        assertEquals("Owner is", "test",info.getOwnerName().toASCIIString());
        List<QueueInfo<FakeContent>> ownedItems = persister.listByOwner(ownerName);
        assertEquals("owns", 1, ownedItems.size());

         info = persister.setBlacklisted(contentName);

        assertTrue("content is blacklisted", info.isBlacklisted());

        persister.addContent(ownerName, new URI("test:test2"), new FakeContent());

        int count = persister.removeBlacklisted(new Date());

        assertEquals("Removed single content", 1, count);

        assertEquals("Single content in queue", 1, persister.listKnownItems().size());

        persister.removeByOwner(ownerName);

        assertEquals("No content in queue", 0, persister.listKnownItems().size());
    }

    class TestRunner extends UnrestrictedSessionRunner {

        public TestRunner(String repository) {
            super(repository);
        }

        @Override
        public void run() throws ClientException {
            DocumentRef queueRef = DocumentStoreManager.newPath("queues", "fake");
            DocumentModel queueDoc = session.getDocument(queueRef);
            assertNotNull(queueDoc);
            DocumentModel contentDoc = session.getChild(queueRef, "test:test1");
            assertNotNull(contentDoc);
            assertNotNull("server id is not there", contentDoc.getProperty(
                    DocumentQueueConstants.QUEUEITEM_SCHEMA,
                    DocumentQueueConstants.QUEUEITEM_SERVERID));
        }
    }

    /**
     * Having that we have an server running one task but had not yet finished
     * his task. When that server is crashing and restart, the queue manager
     * should see it as orphans.
     *
     * This test doesn't unsure that it is running with multiple servers.
     *
     */
    public void testOrphans() throws Exception {
        HeartbeatManager mgr = Framework.getLocalService(HeartbeatManager.class);
        QueueHandler qh = Framework.getLocalService(QueueHandler.class);
        URI owner = new URI("queueowner:owner1");
        URI name = qh.newName("orphan", "myContent");
        qh.newContentIfUnknown(owner, name,  new OrphanContent());

        mgr.stop();

        // sleep 2 times the defaultheartbeattime to be sure that it is detected
        // as orphan by the queueManager

        long delay = mgr.getDelay() * 2;
        log.info("Sleeping for " + delay);
        Thread.sleep(delay);

        QueueLocator locator = Framework.getLocalService(QueueLocator.class);
        QueueManager<OrphanContent> queueManager = locator.getManager(name);
        List<QueueInfo<OrphanContent>> orphans = queueManager.listOrphanedContent();
        assertNotNull(
                "The queueManager should have a list of orphaned (even empty)",
                orphans);
        assertEquals("An orphaned item should be listed", 1, orphans.size());
        assertEquals("The orphan name is", "nxqueue:orphan#myContent",
                orphans.get(0).getName().toASCIIString());

        mgr.start(mgr.getDelay());
        orphans = queueManager.listOrphanedContent();

        assertEquals("An orphaned item should be listed", 1, orphans.size());
        assertEquals("The orphan name is", "nxqueue:orphan#myContent",
                orphans.get(0).getName().toASCIIString());
    }

    /**
     * Testing retrieving the list of available queues. (should be the ones
     * registered in the test-queue-contrib.xml)
     */
    public void testRegisteredQueues() {
        QueueLocator locator = Framework.getLocalService(QueueLocator.class);
        List<QueueManager<?>> managers = locator.getManagers();

        assertNotNull(managers);
        assertEquals("The number of registered types is", 2, managers.size());

        URI fakeName = locator.newQueueName("fake");
        QueueManager<FakeContent> mgr = locator.getManager(fakeName);
        assertNotNull(mgr);
    }


    public void testService() throws Exception {
        URI owner1 = new URI("queueowner:owner1");
        URI owner2 = new URI("queueowner:owner2");
        // Thread 1 and 2:
        JobRunner jobRunner1 = new JobRunner(owner1);
        JobRunner jobRunner2 = new JobRunner(owner2);

        jobRunner1.start();
        jobRunner2.start();

        jobRunner1.thread.join();
        jobRunner2.thread.join();

        // join 1 and 2
        assertEquals(
                "Should has executed only one task/job (1 succeed, 1 failed)",
                1, FakeProcessor.executed);

        // Make sure that we don't have any job running
        QueueLocator ql = Framework.getLocalService(QueueLocator.class);
        URI name = ql.newQueueName("fake");
        QueueManager<FakeContent> queueManager = ql.getManager(name);
        assertEquals("The number of handled item is", 0,
                queueManager.listHandledContent().size());

    }

    class JobRunner implements Runnable {
        URI owner;

        public JobRunner(URI owner) {
            this.owner = owner;
        }

        @Override
        public void run() {
            QueueHandler qh = Framework.getLocalService(QueueHandler.class);
            URI name = qh.newName("fake", "test");
            qh.newContentIfUnknown(owner, name , new FakeContent());
        }

        Thread thread;

        /**
         * Start the thread with this runner
         */
        public void start() {
            thread = new Thread(this);
            thread.start();
        }

    }


}
