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
 *     Nuxeo - initial API and implementation
 *
 * $Id:FakeSearchEnginePlugin.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.core.search;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class FakeNuxeoPrincipal implements NuxeoPrincipal {

    private static final long serialVersionUID = 1546560430542087949L;

    public String getCompany() {
        return null;
    }

    public String getFirstName() {
        return null;
    }

    public List<String> getGroups() {
        List<String> groups = new ArrayList<String>();
        groups.add("foo");
        groups.add("bar");
        return groups;
    }

    public List<String> getAllGroups() {
        // TODO OG: maybe add more undirect groups as well to test more complex
        // cases
        return getGroups();
    }

    public String getLastName() {
        return null;
    }

    public DocumentModel getModel() {
        return null;
    }

    @Override
    public String getEmail() {
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

    public boolean isMemberOf(String groupName) {
        return false;
    }

    @Override
    public void setEmail(String email) {
    }

    public void setCompany(String company) {
    }

    public void setFirstName(String firstName) {
    }

    public void setGroups(List<String> groups) {
    }

    public void setLastName(String lastName) {
    }

    public void setModel(DocumentModel model) {
    }

    public void setName(String name) {
    }

    public void setPassword(String password) {
    }

    public void setPrincipalId(String principalId) {
    }

    public void setRoles(List<String> roles) {
    }

    public String getName() {
        return "foobar";
    }

    public boolean isAdministrator() {
        return false;
    }

    public boolean isAnonymous() {
        return false;
    }

    public String getOriginatingUser() {
        return null;
    }

    public void setOriginatingUser(String originatingUser) {
        // not implemented
    }

}
