/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.binary;


/**
 * A Garbage Collector for a {@link BinaryManager}.
 * <p>
 * First, inform the GC that it is started by calling {@link #start}.
 * <p>
 * Then for all binaries to mark, call {@link #mark}.
 * <p>
 * Finally when all binaries have been marked, call{@link #stop} to delete the
 * non-marked binaries.
 * <p>
 * After this, {@link #getStatus} returns information about the binaries
 * remaining and those that have been GCed.
 */
public interface BinaryGarbageCollector {

    /**
     * Gets a unique identifier for this garbage collector. Two garbage
     * collectors that would impact the same files must have the same
     * identifier.
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
     * Stops the garbage collection process and deletes all binaries that have
     * not been marked (sweep).
     *
     * @param delete {@code true} if actual deletion must be performed,
     *            {@code false} if the binaries to delete should simply be
     *            counted in the status
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
     * A GC is in progress is {@code #start} has been called but not
     * {@code #stop}.
     * <p>
     * It's only useful to call this from a separate thread from the one that
     * calls {@link #mark}.
     *
     * @return {@code true} if a GC is in progress
     */
    boolean isInProgress();

}
