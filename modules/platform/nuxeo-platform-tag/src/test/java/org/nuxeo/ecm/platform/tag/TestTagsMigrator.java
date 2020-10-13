/*
 * (C) Copyright 2017-2020 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.tag;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_MINUTE;
import static org.awaitility.Duration.ONE_SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_ID;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STATE_FACETS;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STATE_RELATIONS;
import static org.nuxeo.ecm.platform.tag.TagConstants.MIGRATION_STEP_RELATIONS_TO_FACETS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.MigrationStatus;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.notification.api")
@Deploy("org.nuxeo.ecm.platform.notification.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestTagsMigrator {

    protected static final int NDOCS = 100;

    protected static final int TAGS_PER_DOC = 5;

    @Inject
    protected CoreSession session;

    @Inject
    protected MigrationService migrationService;

    @Inject
    protected NotificationManager notificationManager;

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void testMigrationImpl() {
        Migrator migrator = new TagsMigrator();
        List<String> progressLines = new ArrayList<>();
        MigrationContext migrationContext = new MigrationContext() {
            @Override
            public void reportProgress(String message, long num, long total) {
                String line = message + ": " + num + "/" + total;
                progressLines.add(line);
            }

            @Override
            public void requestShutdown() {
                // test
            }

            @Override
            public boolean isShutdownRequested() {
                return false;
            }
        };

        testMigration(() -> migrator.run(MIGRATION_STEP_RELATIONS_TO_FACETS, migrationContext));

        List<String> expectedLines = Arrays.asList( //
                "Initializing: 0/-1", //
                "Creating new tags: 1/100", //
                "Creating new tags: 51/100", //
                "Creating new tags: 100/100", //
                "Deleting old Tagging documents: 1/500", //
                "Deleting old Tagging documents: 51/500", //
                "Deleting old Tagging documents: 101/500", //
                "Deleting old Tagging documents: 151/500", //
                "Deleting old Tagging documents: 201/500", //
                "Deleting old Tagging documents: 251/500", //
                "Deleting old Tagging documents: 301/500", //
                "Deleting old Tagging documents: 351/500", //
                "Deleting old Tagging documents: 401/500", //
                "Deleting old Tagging documents: 451/500", //
                "Deleting old Tagging documents: 500/500", //
                "Deleting old Tag documents: 1/7", //
                "Deleting old Tag documents: 7/7", //
                "Done: 100/100");
        assertEquals(expectedLines, progressLines);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.tag:relation-tag-service-override.xml")
    public void testMigrationThroughService() {
        if (coreFeature.getStorageConfiguration().isVCSPostgreSQL()) {
            // NXP-28753: temporarily ignore against PostgreSQL
            return;
        }

        TagService tagService;

        tagService = Framework.getService(TagService.class);
        assertTrue(tagService.getClass().getName(), tagService instanceof RelationTagService);

        MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_RELATIONS, status.getState());

        testMigration(() -> {
            migrationService.runStep(MIGRATION_ID, MIGRATION_STEP_RELATIONS_TO_FACETS);

            // poll until migration done
            await().pollDelay(ONE_SECOND) // wait a bit for the migration to start
                   .atMost(ONE_MINUTE)
                   .until(() -> !migrationService.getStatus(MIGRATION_ID).isRunning());
        });

        tagService = Framework.getService(TagService.class);
        assertTrue(tagService.getClass().getName(), tagService instanceof FacetedTagService);

        status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_FACETS, status.getState());
    }

    @SuppressWarnings("deprecation")
    protected void testMigration(Runnable migrator) {
        TagService relationTagService = new RelationTagService();
        TagService facetedTagService = new FacetedTagService();

        NuxeoPrincipal principal = session.getPrincipal();
        // create base docs
        String[] docIds = new String[NDOCS];
        for (int i = 0; i < NDOCS; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i, "File");
            doc = session.createDocument(doc);
            notificationManager.addSubscription(principal.getName(), "notification" + i, doc, FALSE, principal,
                    "notification" + i);

            String docId = doc.getId();
            docIds[i] = docId;
            for (int j = 0; j < TAGS_PER_DOC; j++) {
                String label = "tag" + ((i + j) % 7);
                String username = "user" + ((i + j) % 13);
                CoreSession s = CoreInstance.getCoreSession(session.getRepositoryName(), username);
                relationTagService.tag(s, docId, label);
            }
            if (i % 2 == 0) {
                // sometimes create a new-style tag as well
                String label = "newtag" + (i % 5);
                String username = "user" + (i % 4);
                CoreSession s = CoreInstance.getCoreSession(session.getRepositoryName(), username);
                facetedTagService.tag(s, docId, label);
            }
        }
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // migrate
        try (CapturingEventListener listener = new CapturingEventListener(DOCUMENT_UPDATED)) {
            migrator.run();
            List<Event> events = listener.getCapturedEvents();
            for (Event event : events) {
                assertEquals(TRUE, event.getContext().getProperty(DISABLE_NOTIFICATION_SERVICE));
            }
        }

        // check resulting tags
        for (int i = 0; i < NDOCS; i++) {
            String docId = docIds[i];
            Set<String> expectedTags = new HashSet<>();
            for (int j = 0; j < TAGS_PER_DOC; j++) {
                String label = "tag" + ((i + j) % 7);
                expectedTags.add(label);
            }
            if (i % 2 == 0) {
                String label = "newtag" + (i % 5);
                expectedTags.add(label);
            }
            Set<String> tags = facetedTagService.getTags(session, docId);
            assertEquals(expectedTags, tags);
        }
    }

    @Test
    public void testProbe() {
        @SuppressWarnings("deprecation")
        TagService relationTagService = new RelationTagService();
        TagService facetedTagService = new FacetedTagService();

        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "File");
        doc1 = session.createDocument(doc1);
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "File");
        doc2 = session.createDocument(doc2);
        session.save();

        // no tags, detected as already migrated
        Migrator migrator = new TagsMigrator();
        assertEquals(MIGRATION_STATE_FACETS, migrator.probeState());

        // just a relation-based tag, detected as not migrated
        relationTagService.tag(session, doc1.getId(), "foo");
        assertEquals(MIGRATION_STATE_RELATIONS, migrator.probeState());

        // both a relation-based tag and a facet-based tag, detected as not migrated
        facetedTagService.tag(session, doc2.getId(), "bar");
        assertEquals(MIGRATION_STATE_RELATIONS, migrator.probeState());

        // just a faceted-based tag, detected as migrated
        relationTagService.untag(session, doc1.getId(), "foo");
        assertEquals(MIGRATION_STATE_FACETS, migrator.probeState());
    }

}
