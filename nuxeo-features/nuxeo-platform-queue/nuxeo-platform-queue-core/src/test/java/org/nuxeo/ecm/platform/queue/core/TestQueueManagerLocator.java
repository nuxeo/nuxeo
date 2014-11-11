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

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.queue.api.QueueManagerLocator;
import org.nuxeo.runtime.api.Framework;

/**
 * Testing the queue manager.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class TestQueueManagerLocator extends SQLRepositoryTestCase {

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
     * Testing retrieving the list of available queues. (should be the ones
     * registered in the test-queue-contrib.xml)
     */
    public void testRegisteredQueues() {
        QueueManagerLocator locator = Framework.getLocalService(QueueManagerLocator.class);
        List<String> queues = locator.getAvailableQueues();

        assertNotNull(queues);
        assertEquals("The number of registered queues is", 2, queues.size());
        Collections.sort(queues);
        assertEquals("The first queue name is", "myQueueDestination",
                queues.get(0));
        assertEquals("The second queue name is", "testOrphansQueue",
                queues.get(1));
    }

}
