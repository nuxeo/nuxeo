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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import java.io.Serializable;
import java.security.Principal;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

public class CachableUserIdentificationInfo implements Serializable {

    private static final long serialVersionUID = 13278976543651L;

    protected final UserIdentificationInfo userInfo;

    protected boolean alreadyAuthenticated;

    protected Principal principal;

    protected transient LoginContext loginContext;

    public CachableUserIdentificationInfo(String userName, String password) {
        userInfo = new UserIdentificationInfo(userName, password);
        alreadyAuthenticated = false;
    }

    public CachableUserIdentificationInfo(UserIdentificationInfo uii) {
        userInfo = uii;
        alreadyAuthenticated = false;
    }

    public Boolean getAlreadyAuthenticated() {
        return alreadyAuthenticated;
    }

    public void setAlreadyAuthenticated(boolean alreadyAuthenticated) {
        this.alreadyAuthenticated = alreadyAuthenticated;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public LoginContext getLoginContext() {
        return loginContext;
    }

    public void setLoginContext(LoginContext loginContext) {
        this.loginContext = loginContext;
    }

    public UserIdentificationInfo getUserInfo() {
        return userInfo;
    }

}
