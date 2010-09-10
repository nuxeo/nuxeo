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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.heartbeat.api.ServerHeartBeat;
import org.nuxeo.ecm.platform.heartbeat.core.NuxeoServerHeartBeat;
import org.nuxeo.ecm.platform.queue.api.QueueHandler;
import org.nuxeo.ecm.platform.queue.api.QueueInfo;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.runtime.api.Framework;

/**
 * Testing the queue manager.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *
 */
public class TestQueueManager extends SQLRepositoryTestCase {

    public static final Log log = LogFactory.getLog(TestQueueManager.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
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
        ServerHeartBeat heartbeat = Framework.getLocalService(ServerHeartBeat.class);
        QueueHandler qh = Framework.getLocalService(QueueHandler.class);
        URI owner = new URI("queueowner:owner1");
        URI name = qh.newName("orphan", "myContent");
        qh.newContentIfUnknown(owner, name,  new OrphanContent());

        heartbeat.stop();

        // sleep 2 times the defaultheartbeattime to be sure that it is detected
        // as orphan by the queueManager

        long delay = heartbeat.getHeartBeatDelay() * 2;
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

        heartbeat.start(NuxeoServerHeartBeat.DEFAULT_HEARTBEAT_DELAY);
        orphans = queueManager.listOrphanedContent();

        assertEquals("An orphaned item should be listed", 1, orphans.size());
        assertEquals("The orphan name is", "nxqueue:orphan#myContent",
                orphans.get(0).getName().toASCIIString());
    }

}
