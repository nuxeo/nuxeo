/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.shindig.gadgets;

import org.apache.shindig.auth.SecurityToken;

/**
 * Create Security Token
 * 
 * @author 10044826
 * 
 */
public class NxSecurityToken implements SecurityToken {

    protected final String viewer;

    protected final String owner;

    protected final String pwd;

    protected final String header;

    public NxSecurityToken(String viewer, String owner, String pwd,
            String header) {
        this.viewer = viewer;
        this.owner = owner;
        this.pwd = pwd;
        this.header = header;
    }

    public String getNuxeoHeader() {
        return header;
    }

    public String getNuxeoPassword() {
        return pwd;
    }

    public String getAppId() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAppUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getModuleId() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getOwnerId() {
        return owner;
    }

    public String getTrustedJson() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUpdatedToken() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getViewerId() {
        return viewer;
    }

    public boolean isAnonymous() {
        if ((pwd != null) || (header != null)) {
            return false;
        }
        return true;
    }

    public String getActiveUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAuthenticationMode() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getContainer() {
        return "default";
    }

}
