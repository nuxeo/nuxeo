/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.heartbeat.api;

import java.net.URI;
import java.util.List;

/**
 * The server heart beat service is updating a shared keep alive table. Looking
 * at this table, administrators can know if a server is down or not and make
 * jobs orphans.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public interface ServerHeartBeat {

    /**
     * Stop and restart.
     *
     * @throws IllegalStateException
     */
    void reset(long delay);

    /**
     * Stopping the heart beat updates.
     *
     * @throws IllegalStateException
     */
    void stop();

    /**
     * Starting the heat beat updates.
     *
     * @throws IllegalStateException
     */
    void start(long delay);

    /**
     * Is the heart beat updates process started and running?
     */
    boolean isStarted();

    long getHeartBeatDelay();

    /**
     * Returns the list of server running.
     */
    List<ServerInfo> getInfos();

    /**
     * Get the running server information.
     */
    ServerInfo getMyInfo() throws ServerNotFoundException;

    /**
     * Get the running server information.
     */
    ServerInfo getInfo(URI serverURI) throws ServerNotFoundException;

    /**
     * Get the running server URI.
     */
    URI getMyURI();

}
