/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *   Maxime Hilaire
 *   Florent Guillaume
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
public class PermissionDescriptor implements Cloneable {

    @XNode("@name")
    public String name;

    @XNodeList(value = "group", type = String[].class, componentType = String.class)
    public String[] groups;

    @XNodeList(value = "user", type = String[].class, componentType = String.class)
    public String[] users;

    @Override
    public PermissionDescriptor clone() {
        PermissionDescriptor clone;
        try {
            clone = (PermissionDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        // basic fields are already copied by super.clone()
        clone.groups = groups == null ? null : groups.clone();
        clone.users = users == null ? null : users.clone();
        return clone;
    }
}
