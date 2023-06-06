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
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.model.Repository.CAPABILITY_QUERY_BLOB_KEYS;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.migrator.AbstractBulkMigrator;
import org.nuxeo.ecm.core.model.BaseSession;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationDescriptor;
import org.nuxeo.runtime.migration.MigrationService;

/**
 * Migrator to populate the {@code ecm:blobKeys} field in DBS repositories.
 *
 * @since 2023
 */
public class BlobKeysBulkMigrator extends AbstractBulkMigrator {

    public static final String MIGRATION_ID = "blob-keys-migration";

    public static final String MIGRATION_BEFORE_STATE = "empty";

    public static final String MIGRATION_AFTER_STATE = "populated";

    public static final String MIGRATION_UNSUPPORTED_STATE = "unsupported";

    public static final String MIGRATION_BEFORE_TO_AFTER_STEP = "empty-to-populated";

    public BlobKeysBulkMigrator(MigrationDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    protected String probeSession(CoreSession session) {
        Repository repository = Framework.getService(RepositoryService.class)
                                         .getRepository(session.getRepositoryName());
        if (repository instanceof DBSRepository) {
            boolean isDone = repository.hasCapability(CAPABILITY_QUERY_BLOB_KEYS);
            if (isDone) {
                return MIGRATION_AFTER_STATE;
            } else {
                return MIGRATION_BEFORE_STATE;
            }
        }
        return MIGRATION_UNSUPPORTED_STATE;
    }

    @Override
    protected String getNXQLScrollQuery() {
        return "SELECT * FROM Document, Relation WHERE " + NXQL.ECM_BLOBKEYS + " IS NULL";
    }

    @Override
    public void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
        BaseSession internalSession = (BaseSession) ((AbstractSession) session).getSession();
        if (internalSession instanceof DBSSession dbSSession) {
            for (var id : ids) {
                try {
                    // Load document in transient state
                    dbSSession.getDocumentByUUID(id);
                } catch (DocumentNotFoundException e) {
                    // let's ignore
                }
            }
            // Save all documents in transient states
            dbSSession.save();
        }
    }

    @Override
    public void notifyStatusChange() {
        if (MIGRATION_AFTER_STATE.equals(
                Framework.getService(MigrationService.class).getStatus(MIGRATION_ID).getState())) {
            RepositoryService rs = Framework.getService(RepositoryService.class);
            rs.getRepositoryNames()
              .stream()
              .map(rs::getRepository)
              .filter(DBSRepository.class::isInstance)
              .map(DBSRepository.class::cast)
              .forEach(DBSRepository::updateCapabilities);
        }
    }
}
