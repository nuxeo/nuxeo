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
import static org.nuxeo.ecm.core.work.api.Work.State.CANCELED;
import static org.nuxeo.ecm.core.work.api.Work.State.COMPLETED;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class WorkManagerTXTest extends NXRuntimeTestCase {

    protected static final String CATEGORY = "SleepWork";

    protected static final String QUEUE = "SleepWork";

    protected WorkManager service;

    protected boolean dontClearCompletedWork;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.event.test",
                "test-workmanager-config.xml");
        service = Framework.getLocalService(WorkManager.class);
        assertNotNull(service);
        service.clearCompletedWork(0);
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        NuxeoContainer.install();
        TransactionHelper.startTransaction();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (!dontClearCompletedWork) {
            service.clearCompletedWork(0);
        }
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        }
        NuxeoContainer.uninstall();
        super.tearDown();
    }

    @Test
    public void testWorkManagerPostCommit() throws Exception {
        int duration = 1000; // 1s
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work, true);
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        Thread.sleep(duration + 1000);
        // still scheduled as tx didn't commit
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(SCHEDULED, work.getWorkInstanceState());

        TransactionHelper.commitOrRollbackTransaction();
        Thread.sleep(duration + 1000);
        // tx commit triggered a release of the scheduled work
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(COMPLETED, work.getWorkInstanceState());
    }

    @Test
    public void testWorkManagerRollback() throws Exception {
        int duration = 1000; // 1s
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work, true);
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        Thread.sleep(duration + 1000);
        // still scheduled as tx didn't commit
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(SCHEDULED, work.getWorkInstanceState());

        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        // tx rollback cancels the task and removes it
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(CANCELED, work.getWorkInstanceState());
    }

}
