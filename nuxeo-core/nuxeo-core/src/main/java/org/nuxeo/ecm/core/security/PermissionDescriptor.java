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
     * Used to unregistered a PermissionDescriptor out of the list
     * of already registered contributions.
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
