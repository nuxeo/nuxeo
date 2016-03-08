/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class WorkManagerTXTest extends NXRuntimeTestCase {

    protected static final String CATEGORY = "SleepWork";

    protected static final String QUEUE = "SleepWork";

    protected WorkManager service;

    void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        assertEquals(new WorkQueueMetrics(QUEUE, scheduled, running, completed, cancelled), service.getMetrics(QUEUE));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-config.xml");
        fireFrameworkStarted();
        service = Framework.getLocalService(WorkManager.class);
        assertNotNull(service);
        assertMetrics(0, 0, 0, 0);
        TransactionHelper.startTransaction();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        }
        super.tearDown();
    }

    @Test
    public void testWorkManagerPostCommit() throws Exception {
        int duration = 1000; // 1s
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work, true);
        assertMetrics(0, 0, 0, 0);

        TransactionHelper.commitOrRollbackTransaction();

        Thread.sleep(duration + 1000);
        assertMetrics(0, 0, 1, 0);
    }

    @Test
    public void testWorkManagerRollback() throws Exception {
        Assert.assertTrue(TransactionHelper.isTransactionActive());
        int duration = 1000; // 1s
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work, true);
        assertMetrics(0, 0, 0, 0);

        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        assertMetrics(0, 0, 0, 0);

    }

}
