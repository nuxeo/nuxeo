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
package org.nuxeo.ecm.platform.heartbeat.core;

import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatInfo;
import org.nuxeo.ecm.platform.heartbeat.api.HeartbeatManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Testing the queue manager.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class TestServerHeartBeat extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.platform.heartbeat.api");
        deployBundle("org.nuxeo.ecm.platform.heartbeat");
        DocumentStoreSessionRunner.setRepositoryName(REPOSITORY_NAME);
        fireFrameworkStarted();
        openSession();
    }

    /**
     * Testing the heart beat service.
     */
    public void testLife() throws Exception {
        HeartbeatManager hb = Framework.getLocalService(HeartbeatManager.class);
        assertTrue("server isn't started", hb.isStarted());

        Thread.sleep(hb.getDelay());
        HeartbeatInfo info1 = hb.getInfo();
        assertNotNull("server info should not be null", info1);

        Thread.sleep(hb.getDelay());
        HeartbeatInfo info2 = hb.getInfo();
        assertNotSame("The server infos time should had changed",
                info1.getUpdateTime(), info2.getUpdateTime());
    }
}
