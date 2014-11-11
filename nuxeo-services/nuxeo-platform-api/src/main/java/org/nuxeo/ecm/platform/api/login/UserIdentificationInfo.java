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

package org.nuxeo.ecm.platform.api.login;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
* Encapsulates some information about a user and how it must be authenticated.
*
* @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
*/
public class UserIdentificationInfo implements Serializable {

    private static final long serialVersionUID = 6894397878763275157L;

    protected String userName;

    protected String password;

    protected String token;

    protected String authPluginName;

    protected String loginPluginName;

    protected final Map<String, String> loginParameters = new HashMap<String,String>();

    public UserIdentificationInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public UserIdentificationInfo(UserIdentificationInfo savedIdent) {
        this(savedIdent.userName, savedIdent.password);
        authPluginName = savedIdent.authPluginName;
    }

    /**
     * Returns the name of the Authentication Plugin used to get user identity
     * (FORM,BASIC,CAS2 ...).
     */
    public String getAuthPluginName() {
        return authPluginName;
    }

    public void setAuthPluginName(String authPluginName) {
        this.authPluginName = authPluginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean containsValidIdentity() {
        if (userName == null) {
            return false;
        }
        return userName.length() != 0;
    }

    public Map<String, String> getLoginParameters() {
        return loginParameters;
    }

    public void setLoginParameters(Map<String, String> loginParameters) {
        this.loginParameters.putAll(loginParameters);
    }

    /**
     * Returns the name of the LoginModule plugin that must be used to create the Principal.
     */
    public String getLoginPluginName() {
        return loginPluginName;
    }

    public void setLoginPluginName(String loginPluginName) {
        this.loginPluginName = loginPluginName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
