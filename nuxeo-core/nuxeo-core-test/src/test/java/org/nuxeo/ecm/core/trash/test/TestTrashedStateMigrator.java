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

package org.nuxeo.ecm.core.trash.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_ID;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STATE_LIFECYCLE;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STEP_LIFECYCLE_TO_PROPERTY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.trash.LifeCycleTrashService;
import org.nuxeo.ecm.core.trash.PropertyTrashService;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.trash.TrashedStateMigrator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestTrashedStateMigrator {

    protected static final int NDOCS = 100;

    @Inject
    protected CoreSession session;

    @Inject
    protected MigrationService migrationService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMigrationImpl() {
        Migrator migrator = new TrashedStateMigrator();
        List<String> progressLines = new ArrayList<>();
        MigrationContext migrationContext = new MigrationContext() {
            @Override
            public void reportProgress(String message, long num, long total) {
                String line = message + ": " + num + "/" + total;
                progressLines.add(line);
            }

            @Override
            public void requestShutdown() {
            }

            @Override
            public boolean isShutdownRequested() {
                return false;
            }
        };

        testMigration(() -> migrator.run(MIGRATION_STEP_LIFECYCLE_TO_PROPERTY, migrationContext));

        List<String> expectedLines = Arrays.asList( //
                "Initializing: 0/-1", //
                "[test] Initializing: 0/-1", //
                "[test] Setting isTrashed property: 0/100", //
                "[test] Setting isTrashed property: 50/100", //
                "[test] Setting isTrashed property: 100/100", //
                "[test] Done: 100/100", //
                "Done: 1/1");
        assertEquals(expectedLines, progressLines);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-trash-service-lifecycle-override.xml")
    @SuppressWarnings("deprecation")
    public void testMigrationThroughService() {
        TrashService trashService = Framework.getService(TrashService.class);
        assertTrue(trashService.getClass().getName(), trashService instanceof LifeCycleTrashService);

        MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_LIFECYCLE, status.getState());

        testMigration(() -> {
            migrationService.runStep(MIGRATION_ID, MIGRATION_STEP_LIFECYCLE_TO_PROPERTY);

            // wait a bit for the migration to start
            sleep(1000);

            // poll until migration done
            long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
            while (System.currentTimeMillis() < deadline) {
                if (!migrationService.getStatus(MIGRATION_ID).isRunning()) {
                    break;
                }
                sleep(100);
            }
        });

        trashService = Framework.getService(TrashService.class);
        assertTrue(trashService.getClass().getName(), trashService instanceof PropertyTrashService);

        status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_PROPERTY, status.getState());
    }

    @SuppressWarnings("deprecation")
    protected void testMigration(Runnable migrator) {
        TrashService lifeCycleService = new LifeCycleTrashService();
        TrashService propertyService = new PropertyTrashService();

        // create base docs
        List<DocumentModel> docs = IntStream.range(0, NDOCS)
                                            .mapToObj(i -> session.createDocumentModel("/", "doc" + i, "File"))
                                            .map(session::createDocument)
                                            .collect(Collectors.toList());
        lifeCycleService.trashDocuments(docs);
        transactionalFeature.nextTransaction();

        // migrate
        migrator.run();

        // check resulting state
        docs.forEach(doc -> assertTrue("ecm:isTrashed property has not been set on '" + doc.getName() + "'",
                propertyService.isTrashed(session, doc.getRef())));
    }

}
