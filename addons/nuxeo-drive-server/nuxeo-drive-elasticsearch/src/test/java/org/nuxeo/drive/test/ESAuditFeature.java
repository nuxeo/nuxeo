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

import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature.Waiter;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Elasticsearch audit feature cleaning up audit log after each test.
 *
 * @since 8.2
 */
@Features({ PlatformFeature.class, RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit",
        "org.nuxeo.elasticsearch.audit.test:elasticsearch-audit-index-test-contrib.xml",
        "org.nuxeo.drive.elasticsearch" })
@LocalDeploy("org.nuxeo.drive.elasticsearch:OSGI-INF/test-nuxeodrive-elasticsearch-contrib.xml")
public class ESAuditFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
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
                esa.getClient()
                   .admin()
                   .indices()
                   .prepareRefresh(esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE))
                   .get();
                return true;
            }

        });
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        cleanUpAuditLog();
    }

    protected void cleanUpAuditLog() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        esa.dropAndInitIndex(esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE));
    }

}
