/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentRoutingEngineServiceImpl extends DefaultComponent implements DocumentRoutingEngineService {

    public static final String WORKFLOW_NAME_EVENT_PROPERTY_KEY = "wfName";

    public static final String WORKFLOW_ID_EVENT_PROPERTY_KEY = "wfId";

    @Override
    public void start(DocumentRoute routeInstance, Map<String, Serializable> map, CoreSession session) {
        routeInstance.run(session, map);
    }

    @Override
    public void resume(DocumentRoute routeInstance, String nodeId, String taskId, Map<String, Object> data,
            String status, CoreSession session) {
        routeInstance.resume(session, nodeId, taskId, data, status);
    }

    @Override
    public void cancel(DocumentRoute routeInstance, CoreSession session) {
        final String routeDocId = routeInstance.getDocument().getId();
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel routeDoc = session.getDocument(new IdRef(routeDocId));
                DocumentRoute routeInstance = routeDoc.getAdapter(DocumentRoute.class);
                if (routeInstance == null) {
                    throw new NuxeoException("Document " + routeDoc + " can not be adapted to a DocumentRoute");
                }
                routeInstance.cancel(session);
            }
        }.runUnrestricted();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(WORKFLOW_NAME_EVENT_PROPERTY_KEY, routeInstance.getTitle());
        properties.put(WORKFLOW_ID_EVENT_PROPERTY_KEY, routeDocId);
        EventFirer.fireEvent(session, routeInstance.getAttachedDocuments(session), properties,
                DocumentRoutingConstants.Events.workflowCanceled.name());
    }
}
