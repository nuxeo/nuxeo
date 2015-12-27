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
public class PermissionDescriptor implements Serializable {

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

    /**
     * Used to unregistered a PermissionDescriptor out of the list of already registered contributions.
     */
    @Override
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
