/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: MergedPermissionDescriptor.java 28439 2008-01-02 14:35:41Z sfermigier $
 */
package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.List;

public class MergedPermissionDescriptor {

    private String name;

    private final List<String> subPermissions = new ArrayList<String>();

    private final List<String> aliasPermissions = new ArrayList<String>();

    public MergedPermissionDescriptor(PermissionDescriptor pd) {
        mergeDescriptor(pd);
    }

    public void mergeDescriptor(PermissionDescriptor pd) {
        name = pd.getName();
        subPermissions.addAll(pd.getIncludePermissions());
        subPermissions.removeAll(pd.getRemovePermissions());
        aliasPermissions.addAll(pd.getAliasPermissions());
        // no way to remove alias yet (YAGNI?)
    }

    public String getName() {
        return name;
    }

    public List<String> getSubPermissions() {
        return subPermissions;
    }

    public void removeSubPermission(String permissionName) {
        subPermissions.remove(permissionName);
    }

    public List<String> getAliasPermissions() {
        return aliasPermissions;
    }

    @Override
    public String toString() {
        return String.format("MergedPermission[%s]", name);
    }

}
