/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    protected Principal principal;

    protected transient LoginContext loginContext;

    public CachableUserIdentificationInfo(String userName, String password) {
        userInfo = new UserIdentificationInfo(userName, password);
    }

    public CachableUserIdentificationInfo(UserIdentificationInfo uii) {
        userInfo = uii;
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
