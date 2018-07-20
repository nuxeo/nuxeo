/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.migration;

/**
 * Migration Service.
 *
 * @since 9.3
 */
public interface MigrationService {

    /**
     * Interface for the implementation of a migrator.
     *
     * @since 9.3
     */
    interface Migrator {

        /**
         * Runs a migration step.
         * <p>
         * This method should periodically check for {@link MigrationContext#isShutdownRequested} and
         * {@link Thread#isInterrupted} and return if {@code true}.
         *
         * @param step the migration step to run
         * @param migrationContext the migration context.
         */
        void run(String step, MigrationContext migrationContext);

        /**
         * Allows notification of status change for a running step or new state.
         *
         * @since 10.3
         */
        void notifyStatusChange();
    }

    /**
     * Interface for a migration context, passed to the {@link Migrator}.
     *
     * @since 9.3
     */
    interface MigrationContext {

        /**
         * Notifies the migration context of the current progress.
         *
         * @param message an informative message about what is being migrated
         * @param num the current number of things migrated
         * @param total the total number of things to migrate, or {@code -1} if unknown
         */
        void reportProgress(String message, long num, long total);

        /**
         * Requests a shutdown. Called internally by the migration service when the server shuts down.
         */
        void requestShutdown();

        /**
         * Checks if shutdown has been requested.
         * <p>
         * This should be checked periodically by the migrator, and when {@code true} the migrator should return as soon
         * as possible, even if its work is not complete.
         * <p>
         * This is a "nice" version of thread interruption, which will follow a short while later, and should also be
         * checked by the migrator.
         *
         * @return {@code true} if migration should be stopped as soon as possible
         */
        boolean isShutdownRequested();
    }

    /**
     * The status of a migration.
     * <p>
     * A migration is either running or not. When not running, it just has a state.
     * <p>
     * When running, it has a step, start time, and progress information (message, num, total, last ping time).
     *
     * @since 9.3
     */
    public class MigrationStatus {

        protected final String state;

        protected final String step;

        protected final long startTime;

        protected final long pingTime;

        protected final String progressMessage;

        protected final long progressNum;

        protected final long progressTotal;

        public MigrationStatus(String state) {
            this.state = state;
            step = null;
            startTime = 0;
            pingTime = 0;
            progressMessage = null;
            progressNum = 0;
            progressTotal = 0;
        }

        public MigrationStatus(String step, long startTime, long pingTime, String progressMessage, long progressNum,
                long progressTotal) {
            state = null;
            this.step = step;
            this.startTime = startTime;
            this.pingTime = pingTime;
            this.progressMessage = progressMessage;
            this.progressNum = progressNum;
            this.progressTotal = progressTotal;
        }

        /**
         * Checks whether the migration is running.
         *
         * @return {@code true} if a migration is running, or {@code false} otherwise
         */
        public boolean isRunning() {
            return state == null;
        }

        /**
         * Gets the state of the migration, if it's not running.
         */
        public String getState() {
            return state;
        }

        /**
         * Gets the step of the migration, if it's running.
         */
        public String getStep() {
            return step;
        }

        /**
         * Gets the start time of the migration, if it's running.
         */
        public long getStartTime() {
            return startTime;
        }

        /**
         * Gets the ping time of the migration, if it's running.
         */
        public long getPingTime() {
            return pingTime;
        }

        /**
         * Gets the progress message of the migration, if it's running.
         */
        public String getProgressMessage() {
            return progressMessage;
        }

        /**
         * Gets the progress "num" of the migration, if it's running.
         */
        public long getProgressNum() {
            return progressNum;
        }

        /**
         * Gets the progress "total" of the migration, if it's running.
         */
        public long getProgressTotal() {
            return progressTotal;
        }
    }

    /**
     * Gets the current status for a migration.
     *
     * @param id the migration id
     * @return the status, or {@code null} if the migration is unknown
     */
    MigrationStatus getStatus(String id);

    /**
     * Runs a migration step for a migration.
     * <p>
     * This launches the migration asynchronously. The status of the migration can be checked with {@link #getStatus}.
     *
     * @param id the migration id
     * @param step the step id
     */
    void runStep(String id, String step);

}
