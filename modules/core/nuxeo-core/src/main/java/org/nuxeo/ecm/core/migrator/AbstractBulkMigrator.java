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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.migrator;

import static java.util.function.Predicate.isEqual;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.ABORTED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.migrator.AbstractBulkMigrator.MigrationAction.ACTION_FULL_NAME;
import static org.nuxeo.ecm.core.migrator.AbstractBulkMigrator.MigrationAction.ACTION_NAME;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;
import static org.nuxeo.runtime.pubsub.ClusterActionServiceImpl.STREAM_START_PROCESSOR_ACTION;
import static org.nuxeo.runtime.pubsub.ClusterActionServiceImpl.STREAM_STOP_PROCESSOR_ACTION;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.Migration;
import org.nuxeo.runtime.migration.MigrationDescriptor;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.migration.MigrationServiceImpl;
import org.nuxeo.runtime.migration.MigrationServiceImpl.InvalidatorMigrator;
import org.nuxeo.runtime.pubsub.ClusterActionService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 2023.0
 */
public abstract class AbstractBulkMigrator implements Migrator {

    private static final Logger log = LogManager.getLogger(AbstractBulkMigrator.class);

    public static final String PARAM_MIGRATION_ID = "migrationId";

    public static final String PARAM_MIGRATION_STEP = "migrationStep";

    protected static final String MIGRATION_PROCESSOR_NAME = "migration";

    protected final MigrationDescriptor descriptor;

    /**
     * @param descriptor the {@link MigrationDescriptor migration descriptor} this migrator is linked to
     */
    public AbstractBulkMigrator(MigrationDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String probeState() {
        // probe state for each repository
        var states = Framework.getService(RepositoryService.class)
                              .getRepositoryNames()
                              .stream()
                              .map(repo -> TransactionHelper.runInTransaction(
                                      () -> CoreInstance.doPrivileged(repo, this::probeSession)))
                              .collect(Collectors.toSet());
        // assume the states are declared from the former to the newer order
        // return the first state that are probed in a repository
        for (var state : descriptor.getStates().keySet()) {
            if (states.contains(state)) {
                return state;
            }
        }
        throw new NuxeoException("Unable to deduce the state of the migration: " + descriptor.getId()
                + " from the repository states: " + states);
    }

    /**
     * Probes the current state of a {@link CoreSession session} by analyzing persistent data.
     * <p>
     * Assumes no migration step is currently running.
     *
     * @return the probed state, or {@code null} if it cannot be determined
     */
    protected abstract String probeSession(CoreSession session);

    @Override
    public void run(String step, MigrationContext migrationContext) {
        if (!descriptor.getSteps().containsKey(step)) {
            throw new NuxeoException("Unknown migration step: " + step + " for migration: " + descriptor.getId());
        }
        // start the migration processor
        Framework.getService(ClusterActionService.class)
                 .executeAction(STREAM_START_PROCESSOR_ACTION, MIGRATION_PROCESSOR_NAME);

        var bulkService = Framework.getService(BulkService.class);
        migrationContext.reportProgress("Initializing", 0, -1);
        var bulkIds = Framework.getService(RepositoryService.class)
                               .getRepositoryNames()
                               .stream()
                               .map(repoName -> createBulkCommand(repoName, descriptor.getId(), step))
                               .map(bulkService::submit)
                               .peek(bulkId -> log.trace("Bulk command: {} was submitted for migration: {}", bulkId,
                                       descriptor.getId()))
                               .collect(Collectors.toList());
        boolean finish;
        do {
            if (bulkIds.stream().map(bulkService::getStatus).map(BulkStatus::getState).anyMatch(isEqual(ABORTED))) {
                migrationContext.requestShutdown();
            }
            if (migrationContext.isShutdownRequested()) {
                log.warn("Migration: {} is shutting down", descriptor::getId);
                // abort all bulk actions
                bulkIds.forEach(bulkService::abort);
                break;
            }
            try {
                log.trace("Sleep a bit before checking status for migration: {}", descriptor::getId);
                Thread.sleep(Duration.ofSeconds(1).toMillis());
            } catch (InterruptedException e) {
                // don't stop the migration processor, Nuxeo is shutting down
                Thread.currentThread().interrupt();
                throw new NuxeoException(e);
            }
            // check if all bulk actions have finished and compute progress
            finish = true;
            long processed = 0;
            long total = 0;
            for (var bulkId : bulkIds) {
                var bulkStatus = bulkService.getStatus(bulkId);
                finish = finish && bulkStatus.getState() == COMPLETED;
                processed += bulkStatus.getProcessed();
                total += bulkStatus.getTotal();
                if (bulkStatus.hasError()) {
                    // An error occurred and must be reported
                    // migration will be stopped and remain in its initial state
                    // It can be relaunched after the error is fixed
                    var errorMessage = bulkStatus.getErrorMessage();
                    var errorCode = bulkStatus.getErrorCode();
                    migrationContext.reportError(errorMessage, errorCode);
                    throw new NuxeoException(errorMessage, errorCode);
                }
            }
            migrationContext.reportProgress(finish ? "Done" : "Migrating content", processed, total);
        } while (!finish);
        // stop the migration processor
        var noMigrationRunning = Framework.getService(MigrationService.class)
                                          .getMigrations()
                                          .stream()
                                          .filter(m -> !descriptor.getId().equals(m.getId()))
                                          .map(Migration::getStatus)
                                          .noneMatch(MigrationService.MigrationStatus::isRunning);
        if (noMigrationRunning) {
            Framework.getService(ClusterActionService.class)
                     .executeAction(STREAM_STOP_PROCESSOR_ACTION, MIGRATION_PROCESSOR_NAME);
        }
    }

    protected BulkCommand createBulkCommand(String repositoryName, String migrationId, String migrationStep) {
        return new BulkCommand.Builder(ACTION_NAME, getNXQLScrollQuery(), SYSTEM_USERNAME).repository(repositoryName)
                                                                                          .param(PARAM_MIGRATION_ID,
                                                                                                  migrationId)
                                                                                          .param(PARAM_MIGRATION_STEP,
                                                                                                  migrationStep)
                                                                                          .build();
    }

    /**
     * @return the NXQL query to scroll for bulk migration
     */
    protected abstract String getNXQLScrollQuery();

    /**
     * Executes the migration on the given batch. The migration step is given through {@code properties} with the key
     * {@link #PARAM_MIGRATION_STEP}.
     * <p>
     * This method is called by the {@link MigrationComputation}, the transaction is handled by it.
     */
    public abstract void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties);

    public static class MigrationAction implements StreamProcessorTopology {

        public static final String ACTION_NAME = "migration";

        public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

        @Override
        public Topology getTopology(Map<String, String> options) {
            return Topology.builder()
                           .addComputation(MigrationComputation::new, //
                                   List.of(INPUT_1 + ":" + ACTION_FULL_NAME, //
                                           OUTPUT_1 + ":" + STATUS_STREAM))
                           .build();
        }
    }

    public static class MigrationComputation extends AbstractBulkComputation {

        private static final Logger log = LogManager.getLogger(MigrationComputation.class);

        protected String lastCommandId;

        protected AbstractBulkMigrator migrator;

        public MigrationComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            var command = getCurrentCommand();
            if (!command.getId().equals(lastCommandId)) {
                String migrationId = command.getParam(PARAM_MIGRATION_ID);
                log.debug("Command id has changed, retrieve the migrator for migration: {}", migrationId);
                var serviceMigrator = ((MigrationServiceImpl) Framework.getService(MigrationService.class)).getMigrator(
                        migrationId);
                if (serviceMigrator instanceof InvalidatorMigrator) {
                    serviceMigrator = ((InvalidatorMigrator) serviceMigrator).getMigrator();
                }
                if (!(serviceMigrator instanceof AbstractBulkMigrator)) {
                    throw new IllegalArgumentException(
                            "The migrator is not a Bulk Migrator, migration: " + migrationId);
                }
                this.migrator = (AbstractBulkMigrator) serviceMigrator;
                this.lastCommandId = command.getId();
            }
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            migrator.compute(session, ids, properties);
        }
    }
}
