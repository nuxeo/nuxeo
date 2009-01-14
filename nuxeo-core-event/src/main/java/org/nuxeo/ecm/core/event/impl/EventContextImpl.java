/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EventContextImpl implements EventContext {

    private static final long serialVersionUID = 1L;

    protected static final Object[] EMPTY = new Object[0];
    protected Object[] args;
    protected HashMap<String, Serializable> properties;

    protected transient CoreSession session;
    protected transient NuxeoPrincipal principal;


    /**
     * Constructor to be used by derived classes
     */
    protected EventContextImpl() {
    }

    public EventContextImpl(Object ... args) {
        this(null, null, args);
    }

    public EventContextImpl(CoreSession session, NuxeoPrincipal principal, Object ... args) {
        this.args = args;
        this.session = session;
        this.principal = principal;
    }


    public Object[] getArguments() {
        return args;
    }

    public CoreSession getCoreSession() {
        return session;
    }

    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        return properties;
    }

    public Serializable getProperty(String key) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        return properties.get(key);
    }

    public boolean hasProperty(String key) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        return properties.containsKey(key);
    }

    public void setProperty(String key, Serializable value) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        properties.put(key, value);
    }

    public void setCoreSession(CoreSession session) {
        this.session = session;
    }

    public void setPrincipal(NuxeoPrincipal principal) {
        this.principal = principal;
    }

    public Event event(String name) {
        return new EventImpl(name, this);
    }

    public Event event(String name, int flags) {
        return new EventImpl(name, this, flags);
    }

}
