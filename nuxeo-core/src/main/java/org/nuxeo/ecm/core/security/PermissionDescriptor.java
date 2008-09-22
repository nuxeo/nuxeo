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

package org.nuxeo.ecm.core.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Bogdan Stefanescu
 * @author Olivier Grisel
 * @author Thierry Delprat
 */
@XObject("permission")
public class PermissionDescriptor implements Serializable{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNodeList(value = "include", type = String[].class, componentType = String.class)
    private String[] includePermissions;

    @XNodeList(value = "remove", type = String[].class, componentType = String.class)
    private String[] removePermissions;

    @XNodeList(value = "alias", type = String[].class, componentType = String.class)
    private String[] aliasPermissions;

    public String getName() {
        return name;
    }

    public List<String> getIncludePermissions() {
        return Arrays.asList(includePermissions);
    }

    public List<String> getRemovePermissions() {
        return Arrays.asList(removePermissions);
    }

    public List<String> getAliasPermissions() {
        return Arrays.asList(aliasPermissions);
    }

    @Override // used to unregistered a PermissionDescriptor out of the list
                // of already registered contributions
    public boolean equals(Object o) {
        if (o instanceof PermissionDescriptor) {
            PermissionDescriptor pd = (PermissionDescriptor) o;
            if (!name.equals(pd.name)) {
                return false;
            }
            if (!getIncludePermissions().equals(pd.getIncludePermissions())) {
                return false;
            }
            if (!getRemovePermissions().equals(pd.getRemovePermissions())) {
                return false;
            }
            if (!getAliasPermissions().equals(pd.getAliasPermissions())) {
                return false;
            }
            // this is an equivalent permission
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("PermissionDescriptor[%s]", name);
    }

}
