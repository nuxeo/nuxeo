/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Features({ ManagementFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.audit:nxaudit-ds.xml")
public class AuditFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(new BulkAuditWaiter());
    }

    protected class BulkAuditWaiter implements TransactionalFeature.Waiter {
        @Override
        public boolean await(Duration duration) throws InterruptedException {
            return Framework.getService(AuditLogger.class).await(duration.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        clear();
    }

    protected void clear() {
        boolean started = !TransactionHelper.isTransactionActive() && TransactionHelper.startTransaction();
        try {
            doClear();
        } finally {
            if (started) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    public void doClear() {
        EntityManager em = Framework.getService(PersistenceProviderFactory.class)
                                    .newProvider("nxaudit-logs")
                                    .acquireEntityManager();
        try {
            em.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
            em.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
            em.createNativeQuery("delete from nxp_logs").executeUpdate();
        } finally {
            em.close();
        }
    }
}
