/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.api.repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceGroup;
import org.nuxeo.runtime.api.ServiceManager;

/**
 * A high-level repository descriptor, from which you get a {@link CoreSession}
 * when calling {@link #open}.
 */
@XObject("repository")
public class Repository {

    @XNode("@isDefault")
    private boolean isDefault;

    @XNode("@name")
    private String name;

    @XNode("@label")
    private String label;

    /** @deprecated unused */
    @Deprecated
    @XNode("@group")
    private String group;

    public Repository() {
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDefault(boolean value) {
        this.isDefault = value;
    }

    protected CoreSession lookupSession() throws Exception {
        CoreSession session;
        if (group != null) {
            ServiceManager mgr = Framework.getLocalService(ServiceManager.class);
            ServiceGroup sg = mgr.getGroup(group);
            if (sg == null) {
                // TODO maybe throw other exception
                throw new ClientException("group '" + group + "' not defined");
            }
            session = sg.getService(CoreSession.class, name);
        } else {
            session = Framework.getService(CoreSession.class, name);
        }
        return session;
    }

    public CoreSession open() throws Exception {
        return open(new HashMap<String, Serializable>());
    }

    public CoreSession open(Map<String, Serializable> context) throws Exception {
        CoreSession session = lookupSession();
        session.connect(name, context);
        return session;
    }

    public static void close(CoreSession session) {
        CoreInstance.getInstance().close(session);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name=" + name + ", label="
                + label + '}';
    }

}
