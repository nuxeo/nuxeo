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

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * Testing the queue
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class TestQueueHandler extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.lock.api");
        deployBundle("org.nuxeo.ecm.platform.lock.core");
        // deploying datasource for tests
        deployTestContrib("org.nuxeo.ecm.platform.lock.core",
                "nxlocks-tests.xml");
        deployBundle("org.nuxeo.ecm.platform.heartbeat.api");
        deployBundle("org.nuxeo.ecm.platform.heartbeat");
        deployBundle("org.nuxeo.ecm.platform.queue");
        deployBundle("org.nuxeo.ecm.platform.queue.test");
        openSession();
    }

    public void testQueueService() throws Exception {

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
                1, FakeExecutor.executed);

    }

    class JobRunner implements Runnable {
        URI owner;

        public JobRunner(URI owner) {
            this.owner = owner;
        }

        public void run() {
            QueueContent content = new QueueContent(owner,
                    "myQueueDestination", "myContent");
            content.setComments("locked by " + owner);
            QueueHandler queuehandler = Framework.getLocalService(QueueHandler.class);

            try {
                queuehandler.handleNewContentIfUnknown(content);
            } catch (QueueException e) {
                throw new Error(e);
            }

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
