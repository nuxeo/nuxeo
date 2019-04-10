/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

/**
 * Runner responsible to run or undo an element of a route.
 */
public interface ElementRunner {

    /**
     * Run this element.
     */
    void run(CoreSession session, DocumentRouteElement element, Map<String, Serializable> map);

    /**
     * Run this element. If an exception is thrown while doing, it cancels the route.
     */
    void run(CoreSession session, DocumentRouteElement element);

    /**
     * Resumes this graph route on a given node.
     *
     * @since 5.6
     */
    void resume(CoreSession session, DocumentRouteElement element, String nodeId, String taskId,
            Map<String, Object> data, String status);

    /**
     * Run the undo chain on this element. If this element is not a step, then throw an exception.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     */
    @Deprecated
    void undo(CoreSession session, DocumentRouteElement element);

    /**
     * Cancel this element.
     *
     * @see DocumentRoute#cancel(CoreSession)
     */
    void cancel(CoreSession session, DocumentRouteElement element);
}
