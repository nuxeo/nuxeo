/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security.impl;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.UserAccess;
import org.nuxeo.ecm.core.api.security.UserEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UserEntryImpl implements UserEntry {

    private static final long serialVersionUID = -4831432501486395944L;

    private final String username;

    private final Map<String, UserAccess> accessEntries;


    public UserEntryImpl(String username) {
        this.username = username;
        accessEntries = new HashMap<String, UserAccess>();
    }

    @Override
    public void addPrivilege(String permission, boolean granted, boolean readOnly) {
        accessEntries.put(permission, new UserAccess(granted, readOnly));
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String[] getPermissions() {
        return accessEntries.keySet().toArray(new String[accessEntries.size()]);
    }

    @Override
    public UserAccess getAccess(String permission) {
        return accessEntries.get(permission);
    }

}
