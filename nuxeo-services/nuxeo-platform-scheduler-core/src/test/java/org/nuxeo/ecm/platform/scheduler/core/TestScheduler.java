/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: $
 */
package org.nuxeo.ecm.platform.scheduler.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestScheduler extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestScheduler.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // scheduler service
        deployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler-service.xml");
        // our event listener
        deployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-eventlistener.xml");
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Override
    public void tearDown() throws Exception {
        undeployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler-service.xml");
        super.tearDown();
    }

    public void testScheduleRegistration() throws Exception {
        Whiteboard whiteboard = Whiteboard.getWhiteboard();
        whiteboard.setCount(0);

        deployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler.xml");
        try {
            Thread.sleep(2000); // 1s so that job is called at least once
        } catch (InterruptedException e) {
            log.error(e);
            fail("Timer failed");
        }

        Integer count = whiteboard.getCount();
        log.info("count " + count);
        undeployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler.xml");
        assertTrue("count " + count, count >= 1);
    }

}
