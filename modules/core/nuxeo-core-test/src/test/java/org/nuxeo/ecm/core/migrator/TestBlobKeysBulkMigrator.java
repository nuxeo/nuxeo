/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.migrator;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.ONE_MINUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.model.Repository.CAPABILITY_QUERY_BLOB_KEYS;
import static org.nuxeo.ecm.core.storage.dbs.BlobKeysBulkMigrator.MIGRATION_AFTER_STATE;
import static org.nuxeo.ecm.core.storage.dbs.BlobKeysBulkMigrator.MIGRATION_BEFORE_STATE;
import static org.nuxeo.ecm.core.storage.dbs.BlobKeysBulkMigrator.MIGRATION_BEFORE_TO_AFTER_STEP;
import static org.nuxeo.ecm.core.storage.dbs.BlobKeysBulkMigrator.MIGRATION_ID;
import static org.nuxeo.ecm.core.storage.dbs.BlobKeysBulkMigrator.MIGRATION_UNSUPPORTED_STATE;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.capabilities.CapabilitiesService;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2023
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@WithFrameworkProperty(name = "nuxeo.test.repository.disable.blobKeys", value = "true")
public class TestBlobKeysBulkMigrator {

    protected static final int NB_DOCS_WITH_CONTENT = 20;

    protected static final int NB_DOCS_WITHOUT_CONTENT = 2;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected MigrationService migrationService;

    @Inject
    protected TransactionalFeature txFeature;

    protected void createDocuments() {
        for (var i = 0; i < NB_DOCS_WITH_CONTENT; i++) {
            var doc = session.createDocumentModel("/", String.format("File%03d", i), "File");
            doc.setPropertyValue("dc:title", "Content to migrate");
            doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("content-" + i));
            session.createDocument(doc);
        }
        for (var i = NB_DOCS_WITH_CONTENT; i < NB_DOCS_WITH_CONTENT + NB_DOCS_WITHOUT_CONTENT; i++) {
            var doc = session.createDocumentModel("/", String.format("File%03d", i), "File");
            doc.setPropertyValue("dc:title", "Content to migrate");
            session.createDocument(doc);
        }
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testBulkMigrationDBS() {
        assumeTrue("MongoDB feature only", coreFeature.getStorageConfiguration().isDBS());
        createDocuments();
        assertEquals(NB_DOCS_WITH_CONTENT + NB_DOCS_WITHOUT_CONTENT, getNbFilesWithoutBlobKeys());
        Framework.getProperties().put("nuxeo.test.repository.disable.blobKeys", "false");
        assertBlobKeysCapability(false);
        var beforeState = migrationService.probeAndSetState(MIGRATION_ID);
        assertEquals(MIGRATION_BEFORE_STATE, beforeState);

        // run the migration
        migrationService.runStep(MIGRATION_ID, MIGRATION_BEFORE_TO_AFTER_STEP);

        // await its end
        await().atMost(ONE_MINUTE).until(() -> !migrationService.getStatus(MIGRATION_ID).isRunning());

        var afterState = migrationService.getStatus(MIGRATION_ID).getState();
        assertEquals(MIGRATION_AFTER_STATE, afterState);
        assertEquals(NB_DOCS_WITHOUT_CONTENT, getNbFilesWithoutBlobKeys());
        assertBlobKeysCapability(true);
    }

    @Test
    public void testBulkMigrationVCS() {
        assumeTrue("VCS feature only", coreFeature.getStorageConfiguration().isVCS());
        Framework.getProperties().put("nuxeo.test.repository.disable.blobKeys", "false");
        assertBlobKeysCapability(false);
        var beforeState = migrationService.probeAndSetState(MIGRATION_ID);
        assertEquals(MIGRATION_UNSUPPORTED_STATE, beforeState);

        assertThrows(IllegalArgumentException.class, () -> migrationService.probeAndRun(MIGRATION_ID));
        assertFalse(migrationService.getStatus(MIGRATION_ID).isRunning());

        var afterState = migrationService.probeAndSetState(MIGRATION_ID);
        assertEquals(MIGRATION_UNSUPPORTED_STATE, afterState);
        assertBlobKeysCapability(false);
    }

    protected int getNbFilesWithoutBlobKeys() {
        return session.query("SELECT * FROM File WHERE " + NXQL.ECM_BLOBKEYS + " IS NULL").size();
    }

    protected void assertBlobKeysCapability(boolean expected) {
        String repoName = session.getRepositoryName();
        assertEquals(expected,
                Framework.getService(RepositoryService.class)
                         .getRepository(repoName)
                         .hasCapability(CAPABILITY_QUERY_BLOB_KEYS));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) Framework.getService(CapabilitiesService.class)
                                                                 .getCapabilities()
                                                                 .get(Repository.CAPABILITY_REPOSITORY)
                                                                 .get(repoName);
        assertEquals(expected, map.get(CAPABILITY_QUERY_BLOB_KEYS));
    }
}
