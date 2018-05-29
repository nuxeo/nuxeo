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
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * This object holds the current bulk command execution status.
 * <p/>
 * This aggregates status and metrics of documentSet creation and operation computation.
 *
 * @since 10.2
 */
public class BulkStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Possible states of bulk operation.
     */
    public enum State {
        SCHEDULED, BUILDING, COMPLETED
    }

    protected UUID uuid;

    protected BulkCommand command;

    protected State state;

    protected ZonedDateTime creationDate;

    /**
     * Gets bulk operation id.
     *
     * @return the id
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Sets bulk operation id.
     *
     * @param uuid the id
     */
    protected void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Gets bulk operation command.
     *
     * @return the bulk operation command.
     */
    public BulkCommand getCommand() {
        return command;
    }


    /**
     * Sets bulk operation command.
     *
     * @param command the bulk operation command
     */
    protected void setCommand(BulkCommand command) {
        this.command = command;
    }

    /**
     * Gets bulk operation state. Possible values are SCHEDULED, BUILDING or COMPLETED.
     *
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * Sets bulk operation state.
     *
     * @param state the state
     */
    protected void setState(State state) {
        this.state = state;
    }

    /**
     * Gets bulk operation creation date. This corresponds to the bulk command submission date.
     *
     * @return the creation date
     */
    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    /**
     * Sets bulk operation creation date.
     *
     * @param creationDate the creation date
     */
    protected void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }

}
