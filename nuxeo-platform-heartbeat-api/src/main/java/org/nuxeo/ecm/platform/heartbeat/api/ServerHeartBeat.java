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
 *
 */
public interface ServerHeartBeat {

    /**
     * stop and restart
     *
     * @throws IllegalStateException
     */
    void reset(long delay) throws IllegalStateException;

    /**
     * Stopping the heart beat updates.
     *
     * @throws IllegalStateException
     */
    void stop() throws IllegalStateException;

    /**
     * Starting the heat beat updates
     *
     * @throws IllegalStateException
     */
    void start(long delay) throws IllegalStateException;

    /**
     * is the heart beat updates process started and running ?
     *
     * @return
     */
    boolean isStarted();

    /**
     *
     * @return
     */
    long getHeartBeatDelay();

    /**
     * Return the list of server running
     *
     * @return
     */
    List<ServerInfo> getInfos();

    /**
     * get the running server informations
     *
     * @return
     * @throws ServerNotFoundException
     */
    ServerInfo getMyInfo() throws ServerNotFoundException;

    /**
     * get the running server informations
     *
     * @return
     */
    ServerInfo getInfo(URI serverURI) throws ServerNotFoundException;

    /**
     * Get the running server uri
     *
     * @return
     */
    URI getMyURI();

}
