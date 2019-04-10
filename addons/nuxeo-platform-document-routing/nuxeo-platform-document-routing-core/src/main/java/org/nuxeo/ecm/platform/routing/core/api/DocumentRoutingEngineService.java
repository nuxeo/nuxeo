/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.core.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;

/**
 * The DocumentRoutingEngineService is responsible for managing the lifecycle of
 * the DocumentRoute. This is an internal service, you should use method on the
 * {@link DocumentRoutingService} to start a route.
 *
 * @author arussel
 *
 */
public interface DocumentRoutingEngineService {

    /**
     * Start or resume a the route.
     *
     * @param routeInstance
     * @param session
     */
    void start(DocumentRoute routeInstance, CoreSession session);

}
