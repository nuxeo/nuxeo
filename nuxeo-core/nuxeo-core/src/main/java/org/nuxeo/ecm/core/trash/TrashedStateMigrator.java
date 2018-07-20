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
package org.nuxeo.ecm.core.trash;

import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.trash.PropertyTrashService.SYSPROP_IS_TRASHED;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STEP_LIFECYCLE_TO_PROPERTY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Migrator of trashed state.
 *
 * @since 10.2
 */
public class TrashedStateMigrator implements Migrator {

    private static final Log log = LogFactory.getLog(TrashedStateMigrator.class);

    protected static final int BATCH_SIZE = 50;

    protected MigrationContext migrationContext;

    @Override
    public void notifyStatusChange() {
        TrashServiceImpl trashService = (TrashServiceImpl) Framework.getRuntime().getComponent(TrashServiceImpl.NAME);
        trashService.invalidateTrashServiceImplementation();
    }

    @Override
    public void run(String step, MigrationContext migrationContext) {
        if (!MIGRATION_STEP_LIFECYCLE_TO_PROPERTY.equals(step)) {
            throw new NuxeoException("Unknown migration step: " + step);
        }
        this.migrationContext = migrationContext;
        reportProgress("Initializing", 0, -1); // unknown
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        try {
            repositoryNames.forEach(this::migrateRepository);
            reportProgress("Done", 1, 1);
        } catch (MigrationShutdownException e) {
            return;
        }
    }

    protected void checkShutdownRequested() {
        if (migrationContext.isShutdownRequested()) {
            throw new MigrationShutdownException();
        }
    }

    protected void reportProgress(String message, long num, long total) {
        log.debug(message + ": " + num + "/" + total);
        migrationContext.reportProgress(message, num, total);
    }

    protected void reportProgress(String repositoryName, String message, long num, long total) {
        reportProgress(String.format("[%s] %s", repositoryName, message), num, total);
    }

    protected void migrateRepository(String repositoryName) {
        reportProgress(repositoryName, "Initializing", 0, -1); // unknown
        TransactionHelper.runInTransaction(() -> CoreInstance.doPrivileged(repositoryName, this::migrateSession));
    }

    protected void migrateSession(CoreSession session) {
        // query all 'deleted' documents
        String deletedQuery = "SELECT ecm:uuid FROM Document WHERE ecm:currentLifeCycleState = 'deleted' "
                + "AND ecm:isVersion = 0";
        List<Map<String, Serializable>> deletedMaps = session.queryProjection(deletedQuery, -1, 0);

        checkShutdownRequested();

        // compute all deleted doc id refs
        List<IdRef> deletedRefs = deletedMaps.stream() //
                                             .map(map -> (String) map.get(ECM_UUID))
                                             .map(IdRef::new)
                                             .collect(Collectors.toList());

        checkShutdownRequested();

        // set ecm:isTrashed property by batch
        int size = deletedRefs.size();
        int i = 0;
        reportProgress(session.getRepositoryName(), "Setting isTrashed property", i, size);
        for (IdRef deletedRef : deletedRefs) {
            session.setDocumentSystemProp(deletedRef, SYSPROP_IS_TRASHED, Boolean.TRUE);
            checkShutdownRequested();
            i++;
            if (i % BATCH_SIZE == 0 || i == size) {
                reportProgress(session.getRepositoryName(), "Setting isTrashed property", i, size);
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
        reportProgress(session.getRepositoryName(), "Done", size, size);
    }

    // exception used for simpler flow control
    protected static class MigrationShutdownException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public MigrationShutdownException() {
            super();
        }
    }
}
