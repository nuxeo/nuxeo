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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;

/**
 * Minimal eventContext implementation that can be used for events that are not bound to a CoreSession.
 *
 * @author Thierry Delprat
 */
public class UnboundEventContext extends EventContextImpl {

    private static final long serialVersionUID = 1L;

    protected boolean boundToCoreSession = false;

    public UnboundEventContext(NuxeoPrincipal principal, Map<String, Serializable> properties) {
        this(null, principal, properties);
    }

    public UnboundEventContext(CoreSession session, NuxeoPrincipal principal, Map<String, Serializable> properties) {
        super(session, principal);
        setProperties(properties);
        boundToCoreSession = session != null;
    }

    @Override
    public Event newEvent(String name) {
        int flags = boundToCoreSession ? Event.FLAG_NONE : Event.FLAG_IMMEDIATE;
        return newEvent(name, flags);
    }

}
