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

/**
 * Common permission descriptor for all directory that use security check
 * 
 * @since 6.0
 */
@XObject(value = "permission")
public class PermissionDescriptor {

    @XNode("@name")
    public String name;

    @XNodeList(value = "group", type = String[].class, componentType = String.class)
    public String[] groups;

    @XNodeList(value = "user", type = String[].class, componentType = String.class)
    public String[] users;

    public PermissionDescriptor clone() {
        PermissionDescriptor clone = new PermissionDescriptor();
        clone.name = name;
        clone.groups = groups;
        clone.users = users;
        return clone;
    }
}
