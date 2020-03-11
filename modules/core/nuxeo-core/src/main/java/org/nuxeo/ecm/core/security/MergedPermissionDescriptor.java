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
 * $Id: MergedPermissionDescriptor.java 28439 2008-01-02 14:35:41Z sfermigier $
 */
package org.nuxeo.ecm.core.security;

import java.util.ArrayList;
import java.util.List;

public class MergedPermissionDescriptor {

    private String name;

    private final List<String> subPermissions = new ArrayList<>();

    private final List<String> aliasPermissions = new ArrayList<>();

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
