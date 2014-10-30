/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *  mhilaire
 *
 */

package org.nuxeo.ecm.directory;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Session.Right;

/**
 * Common permission descriptor for all directory that use security check
 *
 * @since 5.9.6
 */
@XObject(value = "permission")
public class PermissionDescriptor {

    @XNode("@name")
    public Right right;

    @XNodeList(value = "group", type = String[].class, componentType = String.class)
    public String[] groups = {};

    @XNodeList(value = "user", type = String[].class, componentType = String.class)
    public String[] users = {};

    @Override
    public PermissionDescriptor clone() {
        PermissionDescriptor clone = new PermissionDescriptor();
        clone.right = right;
        clone.groups = groups;
        clone.users = users;
        return clone;
    }

    public boolean isAllowed(NuxeoPrincipal principal, Right tocheck) {
        if (!right.equals(tocheck)) {
            return false;
        }
        // checks users
        final String user = principal.getName();
        for (String other : users) {
            if (user.equals(other)) {
                return true;
            }
        }
        // check groups
        for (String each : principal.getGroups()) {
            for (String other:groups) {
                if (each.equals(other)) {
                    return true;
                }
            }
        }
        return false;
    }
}
