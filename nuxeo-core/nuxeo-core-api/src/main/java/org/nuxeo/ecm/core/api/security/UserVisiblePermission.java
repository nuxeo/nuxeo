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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;

public class UserVisiblePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String permission;

    protected final String denyPermission;

    protected final String id;

    public UserVisiblePermission(String id, String perm, String denyPerm) {
        permission = perm;
        denyPermission = denyPerm;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserVisiblePermission other = (UserVisiblePermission) o;

        if (denyPermission != null ? !denyPermission.equals(other.denyPermission) : other.denyPermission != null) {
            return false;
        }
        if (id != null ? !id.equals(other.id) : other.id != null) {
            return false;
        }
        if (permission != null ? !permission.equals(other.permission) : other.permission != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = permission != null ? permission.hashCode() : 0;
        result = 31 * result + (denyPermission != null ? denyPermission.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (denyPermission != null) {
            return String.format("UserVisiblePermission %s [%s (deny %s)]", id, permission, denyPermission);
        } else {
            return String.format("UserVisiblePermission %s [%s]", id, permission);
        }
    }

    public String getPermission() {
        return permission;
    }

    public String getDenyPermission() {
        return denyPermission;
    }

    public String getId() {
        return id;
    }

}
