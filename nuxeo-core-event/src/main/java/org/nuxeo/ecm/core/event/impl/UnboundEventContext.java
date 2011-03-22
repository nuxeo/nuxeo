/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */

package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;

/**
 * Minimal eventContext implementation that can be
 * used for events that are not bound to a CoreSession.
 *
 * @author Thierry Delprat
 */
public class UnboundEventContext extends EventContextImpl {

    private static final long serialVersionUID = 1L;

    protected boolean boundToCoreSession = false;

    public UnboundEventContext(Principal principal, Map<String, Serializable> properties) {
        this(null, principal, properties);
    }

    public UnboundEventContext(CoreSession session, Principal principal, Map<String, Serializable> properties) {
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
