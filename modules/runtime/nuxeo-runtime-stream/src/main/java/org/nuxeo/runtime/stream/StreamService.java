/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.stream;

import java.time.Instant;

import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;

/**
 * @since 9.3
 */
public interface StreamService {

    LogManager getLogManager();

    StreamManager getStreamManager();

    /**
     * Gets a {@link LogManager} corresponding to the config name. The service takes care of closing the manager on
     * shutdown you should not do it directly.
     *
     * @deprecated since 11.1 just use {@link #getLogManager()}.
     */
    @Deprecated(since = "11.1")
    default LogManager getLogManager(String configName) {
        return getLogManager();
    }


    /**
     * Gets a {@link StreamManager} that uses a LogManager matching the config name.
     *
     * @deprecated since 11.1 just use {@link #getStreamManager()}.
     */
    @Deprecated(since = "11.1")
    default StreamManager getStreamManager(String configName) {
        return getStreamManager();
    }

    // exposed for test
    void stopProcessors();

    /**
     * Stop computation thread pool immediately.
     *
     * @since 2021.25
     */
    boolean stopComputation(Name computation);

    /**
     * Restart the computation thread pool.
     * Do nothing if the computation thread pool is already started.
     *
     * @since 2021.25
     */
    boolean restartComputation(Name computation);

    /**
     * Moving computation position to the end of stream.
     * The computation thread pool must be stopped using {@link #stopComputation(Name)} before changing its position.
     *
     * @since 2021.25
     */
    boolean setComputationPositionToEnd(Name computation, Name stream);

    /**
     * Moving computation position to the beginning of stream.
     * The computation thread pool must be stopped using {@link #stopComputation(Name)} before changing its position.
     *
     * @since 2021.25
     */
    boolean setComputationPositionToBeginning(Name computation, Name stream);

    /**
     * Moving computation position to a specific offset for a partition.
     * The computation thread pool must be stopped using {@link #stopComputation(Name)} before changing its position.
     *
     * @since 2021.25
     */
    boolean setComputationPositionToOffset(Name computation, Name stream, int partition, long offset);

    /**
     * Moving computation position after a date.
     * The computation thread pool must be stopped using {@link #stopComputation(Name)} before changing its position.
     *
     * @since 2021.25
     */
    boolean setComputationPositionAfterDate(Name computation, Name stream, Instant dateTime);

}
