/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
// TODO: "org.nuxeo.ecm.core.management" is missing but creates test failures
@Deploy("org.nuxeo.runtime.jtajca")
@Deploy("org.nuxeo.ecm.automation.server")
@Deploy("org.nuxeo.ecm.automation.io")
@Deploy("org.nuxeo.ecm.webengine.core")
@Deploy("org.nuxeo.ecm.webengine.jaxrs")
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.core.management")
@Features({ CoreFeature.class, LogFeature.class })
@Deploy("org.nuxeo.elasticsearch.core.test:elastic-search-core-management-tests-component.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class RepositoryElasticSearchFeature implements RunnerFeature {
    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) {
        // make sure there is an active Tx to do the cleanup, so we don't hide previous assertion
        if (!TransactionHelper.isTransactionActive()) {
            TransactionHelper.startTransaction();
        }
    }

    @Override
    public void initialize(FeaturesRunner runner) {
        // Uncomment to use Derby when h2 lucene lib is not aligned with ES
        // DatabaseHelper.setDatabaseForTests(DatabaseDerby.class.getCanonicalName());
    }
}
