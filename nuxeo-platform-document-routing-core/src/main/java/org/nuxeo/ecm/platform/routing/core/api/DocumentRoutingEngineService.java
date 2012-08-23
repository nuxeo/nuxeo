/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.api;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;

/**
 * The DocumentRoutingEngineService is responsible for managing the lifecycle of
 * the DocumentRoute. This is an internal service, you should use method on the
 * {@link DocumentRoutingService} to start a route.
 */
public interface DocumentRoutingEngineService {

    /**
     * Starts a route.
     *
     * @param routeInstance the route instance
     * @param map the values to pass as initial workflow variables
     * @param session the session
     * @since 5.6
     */
    void start(DocumentRoute routeInstance, Map<String, Serializable> map,
            CoreSession session);

    /**
     * Resumes a route, optionnally for a given task only.
     *
     * @param routeInstance the route instance
     * @param nodeId the node id to resume on (optional)
     * @param taskId the task id that resumes (optional)
     * @param data the data coming from UI form
     * @param status the name of the button clicked to submit the associated task form
     * @param session the session
     * @since 5.6
     */
    void resume(DocumentRoute routeInstance, String nodeId, String taskId,
            Map<String, Object> data, String status, CoreSession session);

}
