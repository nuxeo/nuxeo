/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.kv;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.AbstractKeyValueStoreTest;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.osgi.framework.Bundle;

@RunWith(FeaturesRunner.class)
@Features(TestSQLKeyValueStore.SQLBackendFeature.class)
@Deploy({ "org.nuxeo.runtime.jtajca", //
        "org.nuxeo.runtime.datasource" })
@LocalDeploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/sql-keyvalue-test-contrib.xml")
public class TestSQLKeyValueStore extends AbstractKeyValueStoreTest {

    /** Backported from 10.1 standalone SQLBackendFeature */
    @Features(RuntimeFeature.class)
    @Deploy({ "org.nuxeo.ecm.core.api", //
            "org.nuxeo.ecm.core", //
            "org.nuxeo.ecm.core.schema", //
            "org.nuxeo.ecm.core.event", //
            "org.nuxeo.ecm.core.storage", //
            "org.nuxeo.ecm.core.storage.sql", //
            "org.nuxeo.ecm.platform.el", //
            "org.nuxeo.ecm.core.storage.sql.test.tests" })
    public static class SQLBackendFeature extends SimpleFeature {

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
            TransactionHelper.startTransaction();
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            repositoryService.initRepositories();
        }

        @Override
        public void afterTeardown(FeaturesRunner runner) {
            TransactionHelper.commitOrRollbackTransaction();
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

    @Inject
    protected KeyValueService keyValueService;

    @Override
    protected KeyValueStoreProvider newKeyValueStore() {
        KeyValueStoreProvider store = (KeyValueStoreProvider) keyValueService.getKeyValueStore("default");
        store.clear();
        return store;
    }

    @Override
    protected boolean hasSlowTTLExpiration() {
        return true;
    }

    @Override
    protected void sleepForTTLExpiration() {
        try {
            Thread.sleep(SQLKeyValueStore.TTL_EXPIRATION_FREQUENCY_MS + 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
