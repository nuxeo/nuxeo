/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A DocumentRoute model or instance. A route is a set of step that processes documents. If a method is called that
 * change the state of this object, the {@link #save(CoreSession)} method should be called to persist its state.
 *
 * @author arussel
 */
public interface DocumentRoute extends DocumentRouteStepsContainer {

    /**
     * Set the list of id of documents attached to this instance of RouteModel replacing the previous list if any.
     *
     * @param documents
     */
    void setAttachedDocuments(List<String> documentIds);

    /**
     * The list of documents processed by this route.
     *
     * @return
     */
    List<String> getAttachedDocuments();


    /**
     * Get the username of the initiator.
     *
     * @since 7.2
     */
    String getInitiator();

    /**
     * @since 7.2
     */
    String getModelId();

    /**
     * @since 7.2
     */
    String getModelName();

}
