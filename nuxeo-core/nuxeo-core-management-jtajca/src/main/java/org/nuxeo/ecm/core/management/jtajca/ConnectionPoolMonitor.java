/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca;

import javax.management.MXBean;

import org.apache.geronimo.connector.outbound.PoolingAttributes;

/**
 * @author matic
 */
@MXBean
public interface ConnectionPoolMonitor extends PoolingAttributes, Monitor {

    public static String NAME = Defaults.instance.name(ConnectionPoolMonitor.class, "%s");

    /**
     * Returns the pool name
     *
     * @since 8.4
     */
    String getName();

    /**
     *
     * Returns the active timeout before the connection being killed.
     *
     * @since 8.4
     */
    int getActiveTimeoutMinutes();

    /**
     * Returns the current killed connection count
     * @since 8.4
     */
    long getKilledActiveConnectionCount();

    /**
     * Kills active timed out connections in the pool. Returns the killed count.
     *
     *
     * @since 8.4
     */
    int killActiveTimedoutConnections();

    /**
     * Destroys the current connection manager and replace it by a new one
     *
     *
     * @since 8.4
     */
    void reset();




}
