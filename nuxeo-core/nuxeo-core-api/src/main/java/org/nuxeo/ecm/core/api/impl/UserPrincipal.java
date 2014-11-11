/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * NuxeoPrincipal stub implementation.
 * <p>
 * TODO this should replace the DetachedNuxeoPrincipal from user manager to
 * minimize principal implementations.
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

    protected String password;

    protected DocumentModel model;

    protected String originatingUser;

    /**
     * @deprecated use {@link #UserPrincipal(String, List, boolean, boolean)}
     *             instead: this constructor assumes that members of the
     *             "administrators" group is an administrator.
     */
    @Deprecated
    public UserPrincipal(String username) {
        this(username, new ArrayList<String>(), false, false);
    }

    /**
     * @deprecated use {@link #UserPrincipal(String, List, boolean, boolean)}
     *             instead: this constructor assumes that members of the
     *             "administrators" group is an administrator.
     */
    @Deprecated
    public UserPrincipal(String username, List<String> groups) {
        // BBB: members of group 'administrators' are considered administrators
        this(username, groups, false, groups != null
                && groups.contains(SecurityConstants.ADMINISTRATORS));
    }

    public UserPrincipal(String username, List<String> groups,
            boolean anonymous, boolean administrator) {
        userName = username;
        List<String> emptyGroups = Collections.emptyList();
        this.groups = groups == null ? emptyGroups : groups;
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
        return false;
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

        // XXX: autogenerated junk, yuck!
        if (company == null ? that.company != null
                : !company.equals(that.company)) {
            return false;
        }
        if (firstName == null ? that.firstName != null
                : !firstName.equals(that.firstName)) {
            return false;
        }
        if (groups == null ? that.groups != null : !groups.equals(that.groups)) {
            return false;
        }
        if (lastName == null ? that.lastName != null
                : !lastName.equals(that.lastName)) {
            return false;
        }
        if (password == null ? that.password != null
                : !password.equals(that.password)) {
            return false;
        }
        if (roles == null ? that.roles != null : !roles.equals(that.roles)) {
            return false;
        }
        if (userName == null ? that.userName != null
                : !userName.equals(that.userName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = userName == null ? 0 : userName.hashCode();
        result = 31 * result + (groups == null ? 0 : groups.hashCode());
        result = 31 * result + (roles == null ? 0 : roles.hashCode());
        result = 31 * result + (firstName == null ? 0 : firstName.hashCode());
        result = 31 * result + (lastName == null ? 0 : lastName.hashCode());
        result = 31 * result + (company == null ? 0 : company.hashCode());
        result = 31 * result + (password == null ? 0 : password.hashCode());
        return result;
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

}
