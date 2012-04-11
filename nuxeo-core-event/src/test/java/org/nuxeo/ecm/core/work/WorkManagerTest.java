/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.Work.Progress;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class WorkManagerTest extends NXRuntimeTestCase {

    private static final String DEFAULT = "default";

    protected WorkManager service;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        service = Framework.getLocalService(WorkManager.class);
        assertNotNull(service);
        service.clearCompletedWork(0);
        assertEquals(0, service.getCompletedWork(DEFAULT).size());
        assertEquals(0, service.getRunningWork(DEFAULT).size());
        assertEquals(0, service.getScheduledWork(DEFAULT).size());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        service.clearCompletedWork(0);
        super.tearDown();
    }

    @Test
    public void testWorkManager() throws Exception {
        DummyWork work = new DummyWork();
        service.schedule(work);

        work.debugWaitReady();
        assertEquals(0, service.getCompletedWork(DEFAULT).size());
        assertEquals(1, service.getRunningWork(DEFAULT).size());
        assertEquals(0, service.getScheduledWork(DEFAULT).size());
        assertEquals("Starting dummy work", work.getStatus());
        assertEquals(Progress.PROGRESS_0_PC, work.getProgress());
        work.debugStart();

        for (int i = 0; i < 50; i++) {
            // System.out.println(work.getStatus() + ": " + work.getProgress());
            Thread.sleep(100);
        }

        work.debugWaitDone();
        assertEquals(0, service.getCompletedWork(DEFAULT).size());
        assertEquals(1, service.getRunningWork(DEFAULT).size());
        assertEquals(0, service.getScheduledWork(DEFAULT).size());
        assertEquals("Finished dummy work", work.getStatus());
        assertEquals(Progress.PROGRESS_100_PC, work.getProgress());
        work.debugFinish();

        Thread.sleep(1000);
        assertEquals(1, service.getCompletedWork(DEFAULT).size());
        assertEquals(0, service.getRunningWork(DEFAULT).size());
        assertEquals(0, service.getScheduledWork(DEFAULT).size());
    }

}
