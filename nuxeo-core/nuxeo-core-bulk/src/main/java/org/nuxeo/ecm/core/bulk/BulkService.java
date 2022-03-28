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

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.AsyncService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;

/**
 * API to manage Bulk Computation.
 * <p>
 * At this level, there is no verification of the user calling the method against command's user.
 * <p>
 * This kind of verification has to be done by caller if needed.
 *
 * @since 10.2
 */
public interface BulkService extends AsyncService<String, BulkStatus, Map<String, Serializable>> {

    /**
     * Submits a {@link BulkCommand} that will be processed asynchronously, even if the current transaction rolls back
     * (the method is not transactional).
     *
     * @param command the command to submit
     * @return a unique bulk command identifier
     */
    String submit(BulkCommand command);

    /**
     * Submits a {@link BulkCommand} that will be processed asynchronously only if the transaction commits
     * successfully (nothing will be submitted in case of transaction rollback).
     * Note that the {@link #getStatus(Serializable)} will return an unknown state until transaction is committed.
     *
     * @since 2021.18
     * @param command the command to submit
     * @return a unique bulk command identifier
     */
    String submitTransactional(BulkCommand command);

    /**
     * Returns the command or null if the command is not found or aborted.
     */
    BulkCommand getCommand(String commandId);

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

    /**
     * Gets the list of action statuses triggered by the given user.
     *
     * @param username the user name
     * @return the list of statuses
     * @since 10.3
     */
    List<BulkStatus> getStatuses(String username);

}
