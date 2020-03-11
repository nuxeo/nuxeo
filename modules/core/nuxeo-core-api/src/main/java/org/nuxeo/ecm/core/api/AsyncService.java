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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import javax.validation.constraints.NotNull;

/**
 * Interface to be implemented by asynchronous services.
 *
 * @param <K> type of task id
 * @param <S> type of status
 * @param <V> type of result
 * @since 10.3
 */
public interface AsyncService<K extends Serializable, S extends AsyncStatus<K>, V> {

    /**
     * Returns the status of a task.
     */
    @NotNull
    S getStatus(K taskId);

    /**
     * Aborts an asynchronous task. Service should try to cancel the task, if not possible it must try to stop it.
     */
    @NotNull
    S abort(K taskId);

    /**
     * Retrieves the command execution result.
     */
    V getResult(K taskId);
}
