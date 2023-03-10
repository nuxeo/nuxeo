/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.pubsub;

import java.util.function.Consumer;

/**
 * @since 2023.0
 */
public interface ClusterActionService {

    /**
     * Registers an action for a requested propagation
     */
    void registerAction(String action, Consumer<ClusterActionMessage> consumer);

    /**
     * Propagate an action to all others nodes in the cluster.
     */
    default void executeAction(String action, String param) {
        executeAction(new ClusterActionMessage(action, param));
    }

    /**
     * Propagate an action to all others nodes in the cluster.
     */
    void executeAction(ClusterActionMessage message);
}
