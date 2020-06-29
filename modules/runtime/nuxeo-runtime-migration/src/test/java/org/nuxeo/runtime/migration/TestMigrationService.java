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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.runtime.cluster")
@Deploy("org.nuxeo.runtime.migration")
@Deploy("org.nuxeo.runtime.migration.tests:OSGI-INF/dummy-migration.xml")
public class TestMigrationService {

    @Inject
    protected MigrationService migrationService;

    @Test
    public void testGetMigration() {
        assertDummyMigration(migrationService.getMigration("dummy-migration"));
    }

    @Test
    public void testGetMigrations() {
        var migrations = migrationService.getMigrations();
        assertEquals(2, migrations.size());
        assertDummyMigration(migrations.get(0));
        assertDummyMultiMigration(migrations.get(1));
    }

    @Test
    public void testUnknownMigration() {
        assertNull(migrationService.getMigration("fake"));
    }

    @Test
    public void testProbeAndRunMigration() {
        // Migration with 2 sequential steps: before to after, after to reallyAfter
        String dummyMigration = "dummy-migration";
        assertEquals("before", migrationService.getMigration(dummyMigration).getStatus().getState());
        migrationService.probeAndRun(dummyMigration);
        await().atMost(1, SECONDS)
               .until(() -> "after".equals(migrationService.getMigration(dummyMigration).getStatus().getState()));
        migrationService.probeAndRun(dummyMigration);
        await().atMost(1, SECONDS)
               .until(() -> "reallyAfter".equals(migrationService.getMigration(dummyMigration).getStatus().getState()));
        try {
            migrationService.probeAndRun(dummyMigration);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Migration: dummy-migration must have only one runnable step from state: reallyAfter",
                    e.getMessage());
        }

        // Migration with 2 parallel steps: before to after, before to reallyAfter
        String dummyMultiMigration = "dummy-multi-migration";
        assertEquals("before", migrationService.getMigration(dummyMultiMigration).getStatus().getState());
        try {
            migrationService.probeAndRun(dummyMultiMigration);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Migration: dummy-multi-migration must have only one runnable step from state: before",
                    e.getMessage());
        }
    }

    protected void assertDummyMigration(Migration actual) {
        assertEquals("dummy-migration", actual.getId());
        assertEquals("Dummy Migration", actual.getDescription());
        assertEquals("migration.dummy", actual.getDescriptionLabel());
        assertEquals("before", actual.getStatus().getState());
        assertEquals("Migrate dummy state from before to after", actual.getSteps().get(0).getDescription());
    }

    protected void assertDummyMultiMigration(Migration actual) {
        assertEquals("dummy-multi-migration", actual.getId());
        assertEquals("Dummy Multi Migration", actual.getDescription());
        assertEquals("multi.migration.dummy", actual.getDescriptionLabel());
        assertEquals("before", actual.getStatus().getState());
        assertEquals("Migrate multi-dummy state from before to after", actual.getSteps().get(0).getDescription());
    }

}
