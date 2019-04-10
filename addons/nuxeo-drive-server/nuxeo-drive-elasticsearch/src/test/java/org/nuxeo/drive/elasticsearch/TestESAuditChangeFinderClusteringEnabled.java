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
package org.nuxeo.drive.elasticsearch;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.nuxeo.drive.service.AuditChangeFinderClusteringEnabledTestSuite;
import org.nuxeo.drive.test.ESAuditFeature;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Runs the {@link AuditChangeFinderClusteringEnabledTestSuite} using the {@link ESAuditChangeFinder}.
 *
 * @since 8.2
 */
@Features(ESAuditFeature.class)
public class TestESAuditChangeFinderClusteringEnabled extends AuditChangeFinderClusteringEnabledTestSuite {

    @Inject
    protected ElasticSearchAdmin esa;

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        cleanUpAuditLog();
    }

    protected void cleanUpAuditLog() {
        esa.dropAndInitIndex(ESAuditBackend.IDX_NAME);
    }

    @Override
    protected void waitForAsyncCompletion() throws Exception {
        super.waitForAsyncCompletion();
        // Wait for indexing
        esa.prepareWaitForIndexing().get(20, TimeUnit.SECONDS);
        // Explicit refresh
        esa.refresh();
        // Explicit refresh for the audit index until it is handled by esa.refresh
        esa.getClient().admin().indices().prepareRefresh(ESAuditBackend.IDX_NAME).get();
    }

}
