/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature.Waiter;

/**
 * Elasticsearch audit feature cleaning up audit log after each test.
 *
 * @since 8.2
 */
@Features({ AutomationFeature.class, AuditFeature.class, RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.admin.center")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy("org.nuxeo.elasticsearch.audit.test:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.drive.elasticsearch")
@Deploy("org.nuxeo.drive.elasticsearch:nxuidsequencer-ds.xml")
public class ESAuditFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(new Waiter() {

            @Override
            public boolean await(long deadline) throws InterruptedException {
                if (!Framework.getService(AuditLogger.class).await(deadline - System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS)) {
                    return false;
                }
                ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
                if (esa == null) {
                    return true;
                }
                // Wait for indexing
                try {
                    esa.prepareWaitForIndexing().get(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException cause) {
                    return false;
                }
                // Explicit refresh
                esa.refresh();
                // Explicit refresh for the audit index until it is handled by esa.refresh
                esa.getClient().refresh(esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE));
                return true;
            }

        });
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) {
        cleanUpAuditLog();
    }

    protected void cleanUpAuditLog() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        esa.dropAndInitIndex(esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE));
    }

}
