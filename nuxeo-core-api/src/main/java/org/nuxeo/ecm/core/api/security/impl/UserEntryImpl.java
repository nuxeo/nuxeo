/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public void addPrivilege(String permission, boolean granted, boolean readOnly) {
        accessEntries.put(permission, new UserAccess(granted, readOnly));
    }

    public String getUserName() {
        return username;
    }

    public String[] getPermissions() {
        return accessEntries.keySet().toArray(new String[accessEntries.size()]);
    }

    public UserAccess getAccess(String permission) {
        return accessEntries.get(permission);
    }

}
