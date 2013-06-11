/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.client.adapters;

import org.nuxeo.ecm.automation.client.Session;

/**
 * @since 5.7
 */
public class BusinessService<T> {

    protected Session session;

    public BusinessService(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public T create(T o, String name, String type, String parentPath,
            String adapter) throws Exception {
        return (T) session.newRequest("Operation.BusinessCreateOperation").setInput(
                o).set("name", name).set("type", type).set("parentPath",
                parentPath).set("adapter", adapter).execute();
    }

    public T update(T o, String id, String adapter) throws Exception {
        return (T) session.newRequest("Operation.BusinessUpdateOperation").setInput(
                o).set("id", id).set("adapter", adapter).execute();
    }
}
