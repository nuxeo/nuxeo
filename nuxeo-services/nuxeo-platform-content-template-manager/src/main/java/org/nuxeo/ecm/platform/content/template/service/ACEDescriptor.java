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

package org.nuxeo.ecm.platform.content.template.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * ACE Descriptor. Immutable.
 */
@XObject(value = "ace")
public class ACEDescriptor {

    @XNode("@granted")
    private boolean granted = true;

    @XNode("@principal")
    private String principal;

    @XNode("@permission")
    private String permission;

    public boolean getGranted() {
        return granted;
    }

    public String getPermission() {
        return permission;
    }

    @Deprecated // use getPrincipal() instead
    public String getUserName() {
        return principal;
    }

    @XNode("@userName")
    @Deprecated
    // keep for BBB with old config files
    public void setUserName(String userName) {
        principal = userName;
    }

    public String getPrincipal() {
        return principal;
    }

}
