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
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.heartbeat.api.ServerHeartBeat;
import org.nuxeo.ecm.platform.heartbeat.core.NuxeoServerHeartBeat;
import org.nuxeo.ecm.platform.queue.api.QueueContent;
import org.nuxeo.ecm.platform.queue.api.QueueException;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueItem;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueueManagerLocator;
import org.nuxeo.runtime.api.Framework;

/**
 * Testing the queue manager.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class TestQueueManager extends SQLRepositoryTestCase {

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
        deployBundle("org.nuxeo.ecm.platform.queue.api");
        deployBundle("org.nuxeo.ecm.platform.queue");
        deployBundle("org.nuxeo.ecm.platform.queue.test");
        super.fireFrameworkStarted();
        openSession();
    }

    /**
     * Having that we have an server running one task but had not yet finished
     * his task. When that server is crashing and restart, the queue manager
     * should see it as orphans.
     * 
     * This test doesn't unsure that it is running with multiple servers.
     * 
     */
    public void testOrphansQueueItem() throws Exception {
        URI owner = new URI("queueowner:owner1");
        ServerHeartBeat heartbeat = Framework.getLocalService(ServerHeartBeat.class);

        QueueContent content = new QueueContent(owner, "testOrphansQueue",
                "myContent");
        content.setComments("locked by " + owner);
        QueueHandler queuehandler = Framework.getLocalService(QueueHandler.class);

        try {
            queuehandler.handleNewContentIfUnknown(content);
        } catch (QueueException e) {
            throw new Error(e);
        }

        heartbeat.stop();

        // sleep 2 times the defaultheartbeattime to be sure that it is detected
        // as orphan by the queueManager
        Thread.sleep(heartbeat.getHeartBeatDelay() * 2);

        QueueManagerLocator queueManagerLocator = Framework.getLocalService(QueueManagerLocator.class);
        QueueManager queueManager = queueManagerLocator.locateQueue(content);
        List<QueueItem> orphans = queueManager.listOrphanedItems();
        assertNotNull(
                "The queueManager should have a list of orphaned (even empty)",
                orphans);
        assertEquals("An orphaned item should be listed", 1, orphans.size());
        assertEquals("The orphan name is", "myContent",
                orphans.get(0).getHandledContent().getName());

        heartbeat.start(NuxeoServerHeartBeat.DEFAULT_HEARTBEAT_DELAY);
        orphans = queueManager.listOrphanedItems();

        assertEquals("An orphaned item should be listed", 1, orphans.size());
        assertEquals("The orphan name is", "myContent",
                orphans.get(0).getHandledContent().getName());

    }
}
