/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.cluster;

import java.time.Duration;

import javax.validation.constraints.NotNull;

import org.nuxeo.runtime.RuntimeServiceException;

/**
 * Cluster Service, defining cluster node state and identity.
 *
 * @since 11.1
 */
public interface ClusterService {

    /**
     * Checks if cluster mode is enabled.
     *
     * @return {@code true} if cluster mode is enabled, {@code false} if not
     */
    boolean isEnabled();

    /**
     * Returns the node id. This is never {@code null}.
     *
     * @return the node id
     */
    @NotNull
    String getNodeId();

    /**
     * Runs a {@link Runnable} atomically in a cluster-wide critical section, outside a transaction.
     *
     * @param key the key used to determine atomicity
     * @param duration the duration during which we attempt to acquire the lock
     * @param pollDelay the delay between two subsequent polls of the lock
     * @param runnable the runnable
     * @throws RuntimeServiceException if locking failed
     */
    void runAtomically(String key, Duration duration, Duration pollDelay, Runnable runnable);

}
