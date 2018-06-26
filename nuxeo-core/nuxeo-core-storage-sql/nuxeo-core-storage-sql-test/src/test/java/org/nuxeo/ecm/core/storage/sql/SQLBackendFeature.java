/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.osgi.framework.Bundle;

/**
 * @since 10.1
 */
@TransactionalConfig(autoStart = false)
@Features(TransactionalFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.runtime.migration")
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.storage.sql")
@Deploy("org.nuxeo.ecm.platform.el")
@Deploy("org.nuxeo.ecm.core.storage.sql.test.tests")
public class SQLBackendFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(RuntimeFeature.class).registerHandler(new SQLBackendDeployer());
    }

    @Override
    public void start(FeaturesRunner runner) throws SQLException {
        // first setup the database
        DatabaseHelper.DATABASE.setUp();
        // deploy the test bundle after the default properties have been set
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            Bundle bundle = harness.getOSGiAdapter()
                                   .getRegistry()
                                   .getBundle("org.nuxeo.ecm.core.storage.sql.test.tests");
            URL url = bundle.getEntry("OSGI-INF/test-repo-ds.xml");
            harness.getContext().deploy(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        repositoryService.initRepositories();
    }

    public class SQLBackendDeployer extends HotDeployer.ActionHandler {

        @Override
        public void exec(String action, String... agrs) throws Exception {
            DatabaseHelper.DATABASE.tearDown();
            next.exec(action, agrs);
            DatabaseHelper.DATABASE.setUp();
        }

    }

}
