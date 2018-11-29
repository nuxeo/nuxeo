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

import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.sql.SQLBackendFeature;
import org.nuxeo.runtime.kv.AbstractKeyValueStoreTest;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(SQLBackendFeature.class)
@Deploy("org.nuxeo.runtime.jtajca")
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/sql-keyvalue-test-contrib.xml")
public class TestSQLKeyValueStore extends AbstractKeyValueStoreTest {

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
