/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest.security;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Permission {

    public String name;
    public boolean granted;
    public String permission;

    public Permission(String name, String permission, boolean granted) {
        this.name = name;
        this.permission = permission;
        this.granted = granted;
    }
    
    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the permission.
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * @return the granted.
     */
    public boolean isGranted() {
        return granted;
    }
}
