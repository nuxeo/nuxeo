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
package org.nuxeo.ecm.core.test;

import static org.nuxeo.runtime.migration.MigrationServiceImpl.KEYVALUE_STORE_NAME;
import static org.nuxeo.runtime.migration.MigrationServiceImpl.PING_TIME;
import static org.nuxeo.runtime.migration.MigrationServiceImpl.PROGRESS_MESSAGE;
import static org.nuxeo.runtime.migration.MigrationServiceImpl.PROGRESS_NUM;
import static org.nuxeo.runtime.migration.MigrationServiceImpl.PROGRESS_TOTAL;
import static org.nuxeo.runtime.migration.MigrationServiceImpl.START_TIME;
import static org.nuxeo.runtime.migration.MigrationServiceImpl.STEP;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature for migration service.
 *
 * @since 10.2
 */
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.runtime.migration")
public class MigrationFeature implements RunnerFeature {

    protected static final String[] KEYS = { STEP, START_TIME, PING_TIME, PROGRESS_MESSAGE, PROGRESS_NUM,
            PROGRESS_TOTAL };

    protected final Set<String> migrationIds = new HashSet<>();

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        KeyValueStore kv = getKeyValueStore();
        // clear the changed migration states
        migrationIds.stream() //
                    .flatMap(id -> Stream.of(KEYS).map(id::concat))
                    .forEach(key -> kv.put(key, (String) null));
        migrationIds.clear();
    }

    public void changeStatus(String migrationId, String state) {
        KeyValueStore kv = getKeyValueStore();
        // get the initial state - allow to call simulateRunning several times
        migrationIds.add(migrationId);
        kv.put(migrationId + STEP, state);
        kv.put(migrationId + START_TIME, String.valueOf(System.currentTimeMillis()));
        kv.put(migrationId + PING_TIME, String.valueOf(System.currentTimeMillis()));
        kv.put(migrationId + PROGRESS_MESSAGE, "Set from MigrationFeature");
        kv.put(migrationId + PROGRESS_NUM, String.valueOf(0));
        kv.put(migrationId + PROGRESS_TOTAL, String.valueOf(0));
    }

    protected static KeyValueStore getKeyValueStore() {
        KeyValueService service = Framework.getService(KeyValueService.class);
        Objects.requireNonNull(service, "Missing KeyValueService");
        return service.getKeyValueStore(KEYVALUE_STORE_NAME);
    }

}
