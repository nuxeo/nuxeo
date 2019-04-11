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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.event.Event;
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

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    @Override
    public Serializable getProperty(String key) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties.get(key);
    }

    @Override
    public boolean hasProperty(String key) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties.containsKey(key);
    }

    @Override
    public void setProperty(String key, Serializable value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    @Override
    public Event newEvent(String name) {
        return new EventImpl(name, this);
    }

    @Override
    public Event newEvent(String name, int flags) {
        return new EventImpl(name, this, flags);
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

}
