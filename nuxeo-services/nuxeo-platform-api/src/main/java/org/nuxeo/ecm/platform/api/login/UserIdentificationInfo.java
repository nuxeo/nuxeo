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

package org.nuxeo.ecm.platform.api.login;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    protected final Map<String, String> loginParameters = new HashMap<String, String>();

    public UserIdentificationInfo(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public UserIdentificationInfo(UserIdentificationInfo savedIdent) {
        this(savedIdent.userName, savedIdent.password);
        authPluginName = savedIdent.authPluginName;
    }

    /**
     * Returns the name of the Authentication Plugin used to get user identity (FORM,BASIC,CAS2 ...).
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(userName)
                                    .append(password)
                                    .append(token)
                                    .append(authPluginName)
                                    .append(loginPluginName)
                                    .append(loginParameters.hashCode())
                                    .hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof UserIdentificationInfo)) {
            return false;
        }
        UserIdentificationInfo other = (UserIdentificationInfo) object;
        return new EqualsBuilder().append(userName, other.userName)
                                  .append(password, other.password)
                                  .append(token, other.token)
                                  .append(authPluginName, other.authPluginName)
                                  .append(loginPluginName, other.loginPluginName)
                                  .appendSuper(loginParameters.equals(other.loginParameters))
                                  .isEquals();
    }

}
