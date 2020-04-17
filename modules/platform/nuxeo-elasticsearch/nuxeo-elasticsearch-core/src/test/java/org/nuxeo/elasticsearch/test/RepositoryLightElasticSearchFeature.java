/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 11.1
 */
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class RepositoryLightElasticSearchFeature implements RunnerFeature {

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        // make sure there is an active Tx to do the cleanup, so we don't hide previous assertion
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(RepositoryLightElasticSearchFeature::await);
    }

    public static boolean await(Duration duration) throws InterruptedException {
        // Wait for ES indexing
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        if (esa == null) {
            return true;
        }

        try {
            esa.prepareWaitForIndexing().get(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException cause) {
            return false;
        }

        // Explicit refresh
        esa.refresh();

        return true;
    }
}
