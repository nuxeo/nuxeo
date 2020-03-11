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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Encapsulates some information about a user and how it must be authenticated.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class UserIdentificationInfo implements Serializable {

    private static final long serialVersionUID = 6894397878763275157L;

    protected String userName;

    protected String password;

    /** @since 11.1 */
    protected boolean credentialsChecked;

    protected String token;

    protected String authPluginName;

    public UserIdentificationInfo(String userName) {
        if (isBlank(userName)) {
            throw new NuxeoException("username must not be blank");
        }
        this.userName = userName;
        this.credentialsChecked = true;
    }

    public UserIdentificationInfo(String userName, String password) {
        if (isBlank(userName)) {
            throw new NuxeoException("username must not be blank");
        }
        this.userName = userName;
        this.password = password;
    }

    public UserIdentificationInfo(UserIdentificationInfo savedIdent) {
        this(savedIdent.userName, savedIdent.password);
        credentialsChecked = savedIdent.credentialsChecked;
        token = savedIdent.token;
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
        if (isBlank(userName)) {
            throw new NuxeoException("username must not be blank");
        }
        this.userName = userName;
    }

    /** @since 11.1 */
    public boolean credentialsChecked() {
        return credentialsChecked;
    }

    /** @since 11.1 */
    public void setCredentialsChecked(boolean credentialsChecked)  {
        this.credentialsChecked = credentialsChecked;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
