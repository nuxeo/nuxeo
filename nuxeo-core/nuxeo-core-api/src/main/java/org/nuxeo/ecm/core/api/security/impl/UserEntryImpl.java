/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.security.impl;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.security.UserEntry;

public class UserEntryImpl implements UserEntry {

    private static final long serialVersionUID = 1L;

    private final String username;

    private final Set<String> granted;

    private final Set<String> denied;

    public UserEntryImpl(String username) {
        this.username = username;
        granted = new HashSet<String>();
        denied = new HashSet<String>();
    }

    @Override
    public void addPrivilege(String permission) {
        granted.add(permission);
        denied.remove(permission);
    }

    @Override
    public void addPrivilege(String permission, boolean isGranted) {
        if (isGranted) {
            addPrivilege(permission);
        } else {
            granted.remove(permission);
            denied.add(permission);
        }
    }

    @Override
    @Deprecated
    public void addPrivilege(String permission, boolean granted, boolean readOnly) {
        addPrivilege(permission, granted);
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public Set<String> getGrantedPermissions() {
        return granted;
    }

    @Override
    public Set<String> getDeniedPermissions() {
        return denied;
    }

}
