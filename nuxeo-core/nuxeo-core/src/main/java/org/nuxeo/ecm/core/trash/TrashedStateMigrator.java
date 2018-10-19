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

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.UNDELETE_TRANSITION;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.trash.PropertyTrashService.SYSPROP_IS_TRASHED;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STATE_LIFECYCLE;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.core.trash.TrashServiceImpl.MIGRATION_STEP_LIFECYCLE_TO_PROPERTY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.LifeCycleException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.migrator.AbstractRepositoryMigrator;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Migrator of trashed state.
 *
 * @since 10.2
 */
public class TrashedStateMigrator extends AbstractRepositoryMigrator {

    protected static final String QUERY_DELETED = "SELECT ecm:uuid FROM Document WHERE ecm:currentLifeCycleState = 'deleted' AND ecm:isVersion = 0";

    protected static final int BATCH_SIZE = 50;

    @Override
    public void notifyStatusChange() {
        TrashServiceImpl trashService = (TrashServiceImpl) Framework.getRuntime().getComponent(TrashServiceImpl.NAME);
        trashService.invalidateTrashServiceImplementation();
    }

    @Override
    public String probeState() {
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        if (repositoryNames.stream().map(this::probeRepository).anyMatch(isEqual(MIGRATION_STATE_LIFECYCLE))) {
            return MIGRATION_STATE_LIFECYCLE;
        }
        return MIGRATION_STATE_PROPERTY;
    }

    @Override
    protected String probeSession(CoreSession session) {
        // finds if there are documents in 'deleted' lifecycle state
        List<Map<String, Serializable>> deletedMaps = session.queryProjection(QUERY_DELETED, 1, 0); // limit 1
        if (!deletedMaps.isEmpty()) {
            return MIGRATION_STATE_LIFECYCLE;
        } else {
            return MIGRATION_STATE_PROPERTY;
        }
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

    @Override
    protected void migrateRepository(String repositoryName) {
        reportProgress(repositoryName, "Initializing", 0, -1); // unknown
        super.migrateRepository(repositoryName);
    }

    @Override
    protected void migrateSession(CoreSession session) {
        // query all 'deleted' documents
        List<Map<String, Serializable>> deletedMaps = session.queryProjection(QUERY_DELETED, -1, 0);

        checkShutdownRequested();

        // compute all deleted doc id refs
        List<String> ids = deletedMaps.stream().map(map -> (String) map.get(ECM_UUID)).collect(toList());

        checkShutdownRequested();

        // set ecm:isTrashed property by batch
        int size = ids.size();
        int i = 0;
        reportProgress(session.getRepositoryName(), "Setting isTrashed property", i, size);
        LifeCycleService lifeCycleService = Framework.getService(LifeCycleService.class);
        for (String id : ids) {
            // here we need the low level Document in order to workaround the backward mechanism of followTransition
            // present in the CoreSession
            Document doc = ((AbstractSession) session).getSession().getDocumentByUUID(id);
            // set trash property to true
            doc.setSystemProp(SYSPROP_IS_TRASHED, TRUE);
            // try to follow undelete transition
            try {
                lifeCycleService.followTransition(doc, UNDELETE_TRANSITION);
            } catch (LifeCycleException e) {
                // this is possible if the lifecycle policy doesn't exist anymore
                // force 'project' state
                doc.setCurrentLifeCycleState("project");
            }
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
}
