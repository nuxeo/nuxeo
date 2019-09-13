/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.migrator;

import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.3
 */
public abstract class AbstractRepositoryMigrator implements Migrator {

    private static final Log log = LogFactory.getLog(AbstractRepositoryMigrator.class);

    protected MigrationContext migrationContext;
    
    /** @since 11.1 **/
    protected String step;

    protected String probeRepository(String repositoryName) {
        return TransactionHelper.runInTransaction(() -> CoreInstance.doPrivileged(repositoryName, this::probeSession));
    }

    protected void checkShutdownRequested() {
        checkShutdownRequested(this.migrationContext);
    }

    protected void checkShutdownRequested(MigrationContext migrationContext) {
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
        TransactionHelper.runInTransaction(() -> CoreInstance.doPrivileged(repositoryName,
                (CoreSession session) -> migrateSession(session)));
    }

    /**
     * @since 11.1
     */
    protected void migrateRepository(String step, MigrationContext migrationContext, String repositoryName) {
        migrateRepository(repositoryName);
    }

    /**
     * @since 11.1
     */
    protected void migrateSession(String step, MigrationContext context, CoreSession session) {
        migrateSession(session);
    }

    protected <T> void processBatched(int batchSize, Collection<T> collection, Consumer<T> consumer,
            String progressMessage) {
        processBatched(migrationContext, batchSize, collection, consumer, progressMessage);
    }

    /**
     * Runs a consumer on the collection, committing every BATCH_SIZE elements, reporting progress and checking for
     * shutdown request.
     * 
     * @since 11.1
     */
    protected <T> void processBatched(MigrationContext migrationContext, int batchSize, Collection<T> collection,
            Consumer<T> consumer, String progressMessage) {
        int size = collection.size();
        int i = -1;
        for (T element : collection) {
            consumer.accept(element);
            checkShutdownRequested(migrationContext);
            i++;
            if (i % batchSize == 0 || i == size - 1) {
                reportProgress(progressMessage, i + 1, size);
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }
    }

    protected abstract String probeSession(CoreSession session);

    protected abstract void migrateSession(CoreSession session);

    // exception used for simpler flow control
    protected static class MigrationShutdownException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public MigrationShutdownException() {
            super();
        }
    }

}
