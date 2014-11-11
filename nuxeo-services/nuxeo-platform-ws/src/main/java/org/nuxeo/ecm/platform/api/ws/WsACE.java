/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.api.ws;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.security.ACE;

public class WsACE {

    private String username;
    private String permission;
    private boolean isGranted;

    public WsACE(String username, String permission, boolean isGranted) {
        this.username = username;
        this.permission = permission;
        this.isGranted = isGranted;
    }

    public WsACE() {
        this(null, null, false);
    }

    public WsACE(ACE ace) {
        this(ace.getUsername(), ace.getPermission(), ace.isGranted());
    }

    public String getUsername() {
        return username;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isGranted() {
        return isGranted;
    }

    public void setGranted(boolean isGranted) {
        this.isGranted = isGranted;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public static WsACE[] wrap(ACE[] aces) {
        List<WsACE> result = new ArrayList<WsACE>();

        for (ACE ace : aces) {
            result.add(new WsACE(ace));
        }
        return result.toArray(new WsACE[result.size()]);
    }

}
