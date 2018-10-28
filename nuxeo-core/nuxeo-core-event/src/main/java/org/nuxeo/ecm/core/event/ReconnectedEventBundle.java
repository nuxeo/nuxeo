/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Because {@link EventBundle} can be processed asynchronously, they can be executed:
 * <ul>
 * <li>in a different security context
 * <li>with a different {@link CoreSession}
 * </ul>
 * This interface is used to mark Bundles that supports this kind of processing. This basically means:
 * <ul>
 * <li>Create a JAAS session via {@link org.nuxeo.runtime.api.Framework#login()}
 * <li>Create a new usage {@link CoreSession}
 * <li>refetch any {@link EventContext} args / properties according to new session
 * <li>provide cleanup method
 * </ul>
 *
 * @author tiry
 */
public interface ReconnectedEventBundle extends EventBundle {

    /**
     * Marker to pass and set to true in document models context data when passing it in event properties, to avoid
     * refetching it when reconnecting.
     */
    public static final String SKIP_REFETCH_DOCUMENT_CONTEXT_KEY = "skipRefetchDocument";

    /**
     * Manage cleanup after processing.
     */
    void disconnect();

}
