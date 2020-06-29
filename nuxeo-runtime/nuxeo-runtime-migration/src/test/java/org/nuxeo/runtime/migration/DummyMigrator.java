/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.runtime.migration;

import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;

/**
 * @since 11.2
 */
public class DummyMigrator implements Migrator {

    protected String state;

    @Override
    public void run(String step, MigrationContext migrationContext) {
        state = "after";
    }

    @Override
    public void notifyStatusChange() {
        // Do nothing
    }

}
