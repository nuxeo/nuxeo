/*
 * (C) Copyright 2018-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.core.api;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.Serializable;

/**
 * Interface to be implemented by asynchronous services' statuses.
 *
 * @param <K> type of task id
 * @since 10.3
 */
public interface AsyncStatus<K extends Serializable> extends Serializable {

    /**
     * Gets the asynchronous task id.
     */
    K getId();

    /**
     * Returns true if command is completed.
     */
    boolean isCompleted();

    /**
     * Gets the error message if any.
     *
     * @return the error message if any, {@code null} otherwise
     * @since 11.5
     */
    default String getErrorMessage() {
        return null;
    }

    /**
     * Gets the error code if any.
     *
     * @return the error code if any, {@code 0} otherwise
     * @since 11.5
     */
    default int getErrorCode() {
        return hasError() ? SC_INTERNAL_SERVER_ERROR : 0;
    }

    /**
     * Checks if there is any error.
     *
     * @return {@code true} if there is any error, {@code false} otherwise
     * @since 11.5
     */
    default boolean hasError() {
        return getErrorMessage() != null;
    }
}
