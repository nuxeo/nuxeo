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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentRoutingEngineServiceImpl extends DefaultComponent
        implements DocumentRoutingEngineService {

    @Override
    public void start(DocumentRoute routeInstance,
            Map<String, Serializable> map, CoreSession session) {
        routeInstance.run(session, map);
    }

    @Override
    public void resume(DocumentRoute routeInstance, String nodeId,
            String taskId, Map<String, Object> data, String status,
            CoreSession session) {
        routeInstance.resume(session, nodeId, taskId, data, status);
    }

}
