/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.annotations;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class FakeNuxeoPrincipal implements NuxeoPrincipal {

    private static final long serialVersionUID = 1L;

    private String name;

    public FakeNuxeoPrincipal(String name) {
        this.name = name;
    }

    public List<String> getAllGroups() {
        return null;
    }

    public String getCompany() {
        return null;
    }

    public String getFirstName() {
        return null;
    }

    public List<String> getGroups() {
        return null;
    }

    public String getLastName() {
        return null;
    }

    public DocumentModel getModel() {
        return null;
    }

    public String getOriginatingUser() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    public String getPrincipalId() {
        return null;
    }

    public List<String> getRoles() {
        return null;
    }

    public boolean isAdministrator() {
        return false;
    }

    public boolean isAnonymous() {
        return false;
    }

    public boolean isMemberOf(String group) {
        return false;
    }

    public void setCompany(String company) {

    }

    public void setFirstName(String firstName) {

    }

    public void setGroups(List<String> groups) {

    }

    public void setLastName(String lastName) {

    }

    public void setModel(DocumentModel model) throws ClientException {

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOriginatingUser(String originatingUser) {
    }

    public void setPassword(String password) {
    }

    public void setPrincipalId(String principalId) {
    }

    public void setRoles(List<String> roles) {
    }

    public String getName() {
        return name;
    }

}
