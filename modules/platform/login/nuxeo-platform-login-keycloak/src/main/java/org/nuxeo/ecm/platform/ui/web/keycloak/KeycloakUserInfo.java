/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
            keycloakUserInfo.setCredentialsChecked(true);
            keycloakUserInfo.setToken(token);
            return keycloakUserInfo;
        }
    }
}
