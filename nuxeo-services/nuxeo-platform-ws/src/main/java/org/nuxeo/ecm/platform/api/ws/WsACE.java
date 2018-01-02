/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isGranted ? 1231 : 1237);
        result = prime * result + ((permission == null) ? 0 : permission.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WsACE)) {
            return false;
        }
        WsACE other = (WsACE) obj;
        if (isGranted != other.isGranted) {
            return false;
        }
        if (permission == null) {
            if (other.permission != null) {
                return false;
            }
        } else if (!permission.equals(other.permission)) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

}
