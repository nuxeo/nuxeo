/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     François Maturel
 */

package org.nuxeo.ecm.platform.ui.web.keycloak;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

/**
 * @since 7.4
 */
public class KeycloakUserInfo extends UserIdentificationInfo {

    private static final long serialVersionUID = 6894397878763275157L;

    protected String firstName;

    protected String lastName;

    protected String company;

    protected Set<String> roles;

    private KeycloakUserInfo(String emailAsUserName, String password) {
        super(emailAsUserName, password);
    }

    public KeycloakUserInfo(String emailAsUserName, String password, String firstName, String lastName, String company) {
        super(emailAsUserName, password);

        if (emailAsUserName == null || StringUtils.isEmpty(emailAsUserName)) {
            throw new IllegalStateException("A valid username should always be provided");
        }

        this.firstName = firstName;
        this.lastName = lastName;
        this.company = company;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getCompany() {
        return company;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public static class KeycloakUserInfoBuilder {
        protected String token;

        protected String userName;

        protected String password;

        protected String authPluginName;

        protected String company;

        protected String lastName;

        protected String firstName;

        private KeycloakUserInfoBuilder() {
        }

        public static KeycloakUserInfoBuilder aKeycloakUserInfo() {
            return new KeycloakUserInfoBuilder();
        }

        public KeycloakUserInfoBuilder withToken(String token) {
            this.token = token;
            return this;
        }

        public KeycloakUserInfoBuilder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public KeycloakUserInfoBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public KeycloakUserInfoBuilder withAuthPluginName(String authPluginName) {
            this.authPluginName = authPluginName;
            return this;
        }

        public KeycloakUserInfoBuilder withCompany(String company) {
            this.company = company;
            return this;
        }

        public KeycloakUserInfoBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public KeycloakUserInfoBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public KeycloakUserInfo build() {
            KeycloakUserInfo keycloakUserInfo = new KeycloakUserInfo(userName, password, firstName, lastName, company);
            keycloakUserInfo.setToken(token);
            keycloakUserInfo.setAuthPluginName(authPluginName);
            return keycloakUserInfo;
        }
    }
}