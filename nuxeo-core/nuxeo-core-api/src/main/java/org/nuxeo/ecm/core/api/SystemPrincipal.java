/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.core.api;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SystemPrincipal implements NuxeoPrincipal {

    private static final long serialVersionUID = -3381784063138281706L;

    private static final char[] SYS_PASSWORD = null;

    private static final List<String> SYS_GROUPS = Collections.unmodifiableList(Arrays.asList(SecurityConstants.ADMINISTRATORS));

    private static final List<String> SYS_ROLES = Collections.unmodifiableList(new ArrayList<String>());

    private String origUserName;

    private int hash;

    public SystemPrincipal(String origUserName) {
        this.origUserName = origUserName == null ? LoginComponent.SYSTEM_USERNAME : origUserName;
        computeHash();
    }

    private void computeHash() {
        if (origUserName != null) {
            hash = (LoginComponent.SYSTEM_USERNAME + "-" + origUserName).hashCode();
        } else {
            hash = LoginComponent.SYSTEM_USERNAME.hashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SystemPrincipal) {
            if (origUserName == null) {
                return ((SystemPrincipal) other).origUserName == null;
            } else {
                return origUserName.equals(((SystemPrincipal) other).origUserName);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public String getCompany() {
        return "Nuxeo";
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public void setEmail(String email) {

    }

    public String getFirstName() {
        return "System";
    }

    public String getLastName() {
        return "System";
    }

    public String getName() {
        return LoginComponent.SYSTEM_USERNAME;
    }

    public List<String> getGroups() {
        return SYS_GROUPS;
    }

    public List<String> getAllGroups() {
        return SYS_GROUPS;
    }

    public List<String> getRoles() {
        return SYS_ROLES;
    }

    public String getPassword() {
        if (SYS_PASSWORD == null) {
            return null;
        }
        return new String(SYS_PASSWORD);
    }

    public String getPrincipalId() {
        return "";
    }

    public String getOriginatingUser() {
        return origUserName;
    }

    public void setOriginatingUser(String originatingUser) {
        origUserName = originatingUser;
        computeHash();
    }

    @Override
    public String getActingUser() {
        return getOriginatingUser() == null ? getName() : getOriginatingUser();
    }

    public DocumentModel getModel() {
        return null;
    }

    public void setCompany(String company) {
    }

    public void setFirstName(String firstName) {
    }

    public void setLastName(String lastName) {
    }

    public void setName(String userName) {
    }

    public void setGroups(List<String> groups) {
    }

    public void setRoles(List<String> roles) {
    }

    public void setPassword(String password) {
    }

    public void setPrincipalId(String principalId) {
    }

    public void setModel(DocumentModel model) {
    }

    public boolean isMemberOf(String group) {
        return SYS_GROUPS.contains(group);
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isAdministrator() {
        return true;
    }

    @Override
    public String getTenantId() {
        return null;
    }

    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }
}
