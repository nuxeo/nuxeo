/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Jean-Marc Orliaguet, Chalmers
 */

package org.nuxeo.theme.test.webwidgets;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class FakeNuxeoPrincipal implements NuxeoPrincipal {

    private static final long serialVersionUID = 1L;

    private String name;

    private boolean administrator;

    private boolean anonymous;

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public List<String> getAllGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCompany() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getFirstName() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLastName() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getModel() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getOriginatingUser() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPassword() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPrincipalId() {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getRoles() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public boolean isMemberOf(String group) {
        // TODO Auto-generated method stub
        return false;
    }

    public void setCompany(String company) {
        // TODO Auto-generated method stub

    }

    public void setFirstName(String firstName) {
        // TODO Auto-generated method stub

    }

    public void setGroups(List<String> groups) {
        // TODO Auto-generated method stub

    }

    public void setLastName(String lastName) {
        // TODO Auto-generated method stub

    }

    public void setModel(DocumentModel model) throws ClientException {
        // TODO Auto-generated method stub

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOriginatingUser(String originatingUser) {
        // TODO Auto-generated method stub

    }

    public void setPassword(String password) {
        // TODO Auto-generated method stub

    }

    public void setPrincipalId(String principalId) {
        // TODO Auto-generated method stub

    }

    public void setRoles(List<String> roles) {
        // TODO Auto-generated method stub

    }

    public String getName() {
        return name;
    }

}
