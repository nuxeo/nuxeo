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
 * $Id$
 */

package org.nuxeo.ecm.platform.usermanager;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Describes a detached NuxeoPrincipal.
 *
 * @author Mariana Cedica
 */
public class DetachedNuxeoPrincipal implements NuxeoPrincipal {

    public static DetachedNuxeoPrincipal detach(NuxeoPrincipal principal) {
        DetachedNuxeoPrincipal detachedPrincipal = new DetachedNuxeoPrincipal(
                principal.getPrincipalId());
        detachedPrincipal.name = principal.getName();
        detachedPrincipal.firstName = principal.getFirstName();
        detachedPrincipal.lastName = principal.getLastName();
        detachedPrincipal.password = principal.getPassword();
        detachedPrincipal.company = principal.getCompany();
        detachedPrincipal.groups = principal.getGroups();
        detachedPrincipal.allGroups = principal.getAllGroups();
        detachedPrincipal.roles = principal.getRoles();
        detachedPrincipal.isAdministrator = principal.isAdministrator();
        detachedPrincipal.isAnonymous = principal.isAnonymous();
        detachedPrincipal.email = principal.getEmail();
        return detachedPrincipal;
    }

    protected DetachedNuxeoPrincipal(String principalId) {
        this.principalId = principalId;
    }

    public DetachedNuxeoPrincipal(String principalId, String name,
            String firstName, String lastName, String password, String email,
            String company, List<String> groups, List<String> allGroups,
            List<String> roles, boolean isAdministrator, boolean isAnonymous) {
        this.password = principalId;
        this.name = name;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.email = email;
        this.company = company;
        this.groups = groups;
        this.allGroups = allGroups;
        this.roles = roles;
        this.isAdministrator = isAdministrator;
        this.isAnonymous = isAnonymous;
    }

    private static final long serialVersionUID = 1L;

    protected String principalId;

    protected String name;

    protected String firstName;

    protected String lastName;

    protected String password;

    protected String email;

    protected String company;

    protected List<String> groups;

    protected List<String> allGroups;

    protected List<String> roles;

    protected boolean isAdministrator;

    protected boolean isAnonymous;

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
    }

    public String getCompany() {
        return company;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getAllGroups() {
        return allGroups;
    }

    public boolean isMemberOf(String group) {
        throw new UnsupportedOperationException();
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setName(String name) {
        throw new UnsupportedOperationException();

    }

    public void setFirstName(String firstName) {
        throw new UnsupportedOperationException();

    }

    public void setLastName(String lastName) {
        throw new UnsupportedOperationException();
    }

    public void setGroups(List<String> groups) {
        throw new UnsupportedOperationException();

    }

    public void setRoles(List<String> roles) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    public void setCompany(String company) {
        throw new UnsupportedOperationException();

    }

    public void setPassword(String password) {
        throw new UnsupportedOperationException();

    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getModel() {
        throw new UnsupportedOperationException();
    }

    public void setModel(DocumentModel model) throws ClientException {
        throw new UnsupportedOperationException();

    }

    public boolean isAdministrator() {
        return isAdministrator;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public String getOriginatingUser() {
        throw new UnsupportedOperationException();
    }

    public void setOriginatingUser(String originatingUser) {
        throw new UnsupportedOperationException();
    }
}
