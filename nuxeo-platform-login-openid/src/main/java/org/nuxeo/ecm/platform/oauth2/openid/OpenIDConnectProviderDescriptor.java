/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("provider")
public class OpenIDConnectProviderDescriptor implements Serializable {
    protected static final long serialVersionUID = 1L;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("name")
    protected String name;

    @XNode("tokenServerURL")
    protected String tokenServerURL;

    @XNode("authorizationServerURL")
    protected String authorizationServerURL;

    @XNode("userInfoURL")
    protected String userInfoURL;

    @XNode("clientId")
    protected String clientId;

    @XNode("clientSecret")
    protected String clientSecret;

    @XNodeList(value = "scope", type = String[].class, componentType = String.class)
    public String[] scopes;

    @XNode("icon")
    protected String icon;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getName() {
        return name;
    }

    public String getTokenServerURL() {
        return tokenServerURL;
    }

    public String getAuthorizationServerURL() {
        return authorizationServerURL;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String[] getScopes() {
        return scopes;
    }

    public String getUserInfoURL() {
        return userInfoURL;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
