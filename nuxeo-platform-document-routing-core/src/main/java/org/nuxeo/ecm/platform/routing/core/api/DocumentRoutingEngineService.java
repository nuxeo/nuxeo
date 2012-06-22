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
     * @param session the session
     */
    void start(DocumentRoute routeInstance, CoreSession session);

    /**
     * Resumes a route.
     *
     * @param routeInstance the route instance
     * @param session the session
     * @param nodeId the node id to resume on
     * @param data the data coming from UI form
     * @param status the name of the button clicked to submit the associated task form
     * @since 5.6
     */
    void resume(DocumentRoute routeInstance, CoreSession session,
            String nodeId, Map<String, Object> data, String status);

}
