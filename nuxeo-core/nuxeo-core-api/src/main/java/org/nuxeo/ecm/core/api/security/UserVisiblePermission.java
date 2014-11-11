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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;

public class UserVisiblePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String permission;

    protected String denyPermission;

    protected String id;

    public UserVisiblePermission(String id, String perm, String denyPerm) {
        permission = perm;
        denyPermission = denyPerm;
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (toString().equals(other.toString())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (denyPermission!=null) {
            return String.format("UserVisiblePermission %s [%s (deny %s)]", id,  permission, denyPermission);
        } else {
            return String.format("UserVisiblePermission %s [%s]",id,  permission);
        }
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getDenyPermission() {
        return denyPermission;
    }

    public void setDenyPermission(String denyPermission) {
        this.denyPermission = denyPermission;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
