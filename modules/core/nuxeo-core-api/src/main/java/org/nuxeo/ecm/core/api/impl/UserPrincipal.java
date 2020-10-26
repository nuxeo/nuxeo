/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.api.impl;

import static java.util.Objects.requireNonNullElseGet;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * NuxeoPrincipal stub implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UserPrincipal implements NuxeoPrincipal, Serializable {

    private static final long serialVersionUID = 2013321088068583749L;

    protected boolean anonymous;

    protected boolean administrator;

    protected String userName;

    protected List<String> groups;

    protected List<String> roles;

    protected String firstName;

    protected String lastName;

    protected String email;

    protected String company;

    protected transient String password;

    protected DocumentModel model;

    protected String originatingUser;

    public UserPrincipal(String username, List<String> groups, boolean anonymous, boolean administrator) {
        userName = username;
        this.groups = requireNonNullElseGet(groups, Collections::emptyList);
        this.anonymous = anonymous;
        this.administrator = administrator;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getCompany() {
        return company;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setCompany(String company) {
        this.company = company;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public void setName(String name) {
        userName = name;
    }

    @Override
    public String getName() {
        return userName;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    // TODO OG: this is not the true semantics but is it really a problem here?
    @Override
    public List<String> getAllGroups() {
        return groups;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    @Override
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getPrincipalId() {
        return null;
    }

    @Override
    public void setPrincipalId(String principalId) {
    }

    @Override
    public DocumentModel getModel() {
        return model;
    }

    @Override
    public void setModel(DocumentModel model) {
        this.model = model;
    }

    @Override
    public boolean isMemberOf(String group) {
        return groups.contains(group);
    }

    @Override
    public boolean isAdministrator() {
        return administrator;
    }

    @Override
    public String getTenantId() {
        return null;
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public String getOriginatingUser() {
        return originatingUser;
    }

    @Override
    public void setOriginatingUser(String originatingUser) {
        this.originatingUser = originatingUser;
    }

    @Override
    public String getActingUser() {
        return getOriginatingUser() == null ? getName() : getOriginatingUser();
    }

    @Override
    public boolean isTransient() {
        String name = getName();
        return name != null && name.startsWith(TRANSIENT_USER_PREFIX);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(userName)
                                    .append(groups)
                                    .append(roles)
                                    .append(firstName)
                                    .append(lastName)
                                    .append(company)
                                    .append(password)
                                    .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal that = (UserPrincipal) o;
        return new EqualsBuilder().append(userName, that.userName)
                                  .append(groups, that.groups)
                                  .append(roles, that.roles)
                                  .append(firstName, that.firstName)
                                  .append(lastName, that.lastName)
                                  .append(company, that.company)
                                  .append(password, that.password)
                                  .isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public void setIsComplete(boolean complete) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
