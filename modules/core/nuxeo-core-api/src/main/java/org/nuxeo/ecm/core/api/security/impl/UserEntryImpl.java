/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
        granted = new HashSet<>();
        denied = new HashSet<>();
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
