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
import java.util.Set;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.Event.Flag;
import org.nuxeo.ecm.core.event.EventContext;

/**
 * Base class to be used to create new context events.
 * <p>
 * This class handles context properties and event creation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractEventContext implements EventContext {

    private static final long serialVersionUID = 1L;

    protected static final Object[] EMPTY = new Object[0];
    protected Object[] args;
    protected Map<String, Serializable> properties;

    protected String repositoryName;

    /**
     * Constructor to be used by derived classes
     */
    protected AbstractEventContext() {
    }

    protected AbstractEventContext(Object... args) {
        this.args = args == null || (args.length == 1 && args[0] == null) ? EMPTY : args;
    }

    public Object[] getArguments() {
        return args;
    }

    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
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

    public Event newEvent(String name) {
        return new EventImpl(name, this);
    }

    public Event newEvent(String name, Set<Flag> flags) {
        return new EventImpl(name, this, flags);
    }

    public String getRepositoryName() {
        return repositoryName;
    }

}
