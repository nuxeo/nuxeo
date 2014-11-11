/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
            properties = new HashMap<String, Serializable>();
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
            properties = new HashMap<String, Serializable>();
        }
        return properties.get(key);
    }

    @Override
    public boolean hasProperty(String key) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        return properties.containsKey(key);
    }

    @Override
    public void setProperty(String key, Serializable value) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
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
