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

import java.util.concurrent.TimeUnit;

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
    BulkStatus getStatus(String bulkId);

    /**
     * Waits for completion of given bulk.
     *
     * @param timeout the time to wait
     * @param unit the timeout unit
     * @return {@code true} if bulk completed, or {@code false} if computation has not finished after the timeout
     */
    boolean await(String bulkId, long timeout, TimeUnit unit) throws InterruptedException;
}
