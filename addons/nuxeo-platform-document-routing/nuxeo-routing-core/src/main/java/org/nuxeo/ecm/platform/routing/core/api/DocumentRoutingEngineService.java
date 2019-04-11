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
package org.nuxeo.ecm.platform.routing.core.api;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;

/**
 * The DocumentRoutingEngineService is responsible for managing the lifecycle of the DocumentRoute. This is an internal
 * service, you should use method on the {@link DocumentRoutingService} to start a route.
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
    void start(DocumentRoute routeInstance, Map<String, Serializable> map, CoreSession session);

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
    void resume(DocumentRoute routeInstance, String nodeId, String taskId, Map<String, Object> data, String status,
            CoreSession session);

    /**
     * Cancels a route using an unrestricted session. (@since 5.7.2 the event 'workflowCanceled' is notified.)
     *
     * @param routeInstance the route instance
     * @param session the session
     * @since 5.6
     */
    void cancel(DocumentRoute routeInstance, CoreSession session);
}
