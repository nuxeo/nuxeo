/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob.binary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Garbage Collector for a {@link BinaryManager}.
 * <p>
 * First, inform the GC that it is started by calling {@link #start}.
 * <p>
 * Then for all binaries to mark, call {@link #mark}.
 * <p>
 * Finally when all binaries have been marked, call{@link #stop} to delete the non-marked binaries.
 * <p>
 * After this, {@link #getStatus} returns information about the binaries remaining and those that have been GCed.
 */
public interface BinaryGarbageCollector {

    static final Logger log = LogManager.getLogger(BinaryGarbageCollector.class);

    /**
     * Gets a unique identifier for this garbage collector. Two garbage collectors that would impact the same files must
     * have the same identifier.
     *
     * @return a unique identifier
     */
    String getId();

    /**
     * Starts the garbage collection process.
     * <p>
     * After this, all active binaries must be fed to the {@link #mark} method.
     */
    void start();

    /**
     * Marks a binary as being in use.
     *
     * @param digest the binary's digest
     */
    void mark(String digest);

    /**
     * Stops the garbage collection process and deletes all binaries that have not been marked (sweep).
     *
     * @param delete {@code true} if actual deletion must be performed, {@code false} if the binaries to delete should
     *            simply be counted in the status
     */
    void stop(boolean delete);

    /**
     * Gets the status of the binaries to GC and of those that won't be.
     * <p>
     * Available after {@link #stop}.
     *
     * @return the status
     */
    BinaryManagerStatus getStatus();

    /**
     * Checks if a GC is in progress.
     * <p>
     * A GC is in progress is {@code #start} has been called but not {@code #stop}.
     * <p>
     * It's only useful to call this from a separate thread from the one that calls {@link #mark}.
     *
     * @return {@code true} if a GC is in progress
     */
    boolean isInProgress();

    /**
     * Reset start time. This is required if the process throws an error before the end not to block the next call to
     * the GC
     *
     * @since 2021.36
     */
    default void reset() {
        log.warn("Reset method is not implemented for {}", this.getClass().getName());
    };

}
