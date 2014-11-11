/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
