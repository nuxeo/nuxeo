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
package org.nuxeo.ecm.core.bulk;

import java.time.Duration;

/**
 * API to manage Bulk Computation.
 *
 * @since 10.2
 */
public interface BulkService {

    /**
     * Submits a {@link BulkCommand} that will be processed asynchronously.
     *
     * @param command the command to submit
     * @return a unique bulk command identifier
     */
    String submit(BulkCommand command);

    /**
     * @return the command status corresponding to the command identifier.
     */
    BulkStatus getStatus(String commandId);

    /**
     * Waits for completion of given bulk command.
     *
     * @param commandId the command to wait
     * @param duration the duration to wait
     * @return {@code true} if bulk command completed or {@code false} if computation has not finished after the timeout
     */
    boolean await(String commandId, Duration duration) throws InterruptedException;

    /**
     * Waits for completion of all bulk commands.
     *
     * @param duration the duration to wait
     * @return {@code true} if all bulk commands completed or {@code false} if one or more has not finished after the
     *         timeout
     * @since 10.3
     */
    boolean await(Duration duration) throws InterruptedException;

}
