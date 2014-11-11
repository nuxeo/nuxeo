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

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class NuxeoPrincipalImpl implements NuxeoPrincipal {

    private static final String TYPE_NAME = "User";

    private static final String SCHEMA_NAME = "user";

    // TODO: this should be moved to an extension point of the usermanager
    // service

    public static final String USERNAME_COLUMN = "username";

    public static final String FIRSTNAME_COLUMN = "firstName";

    public static final String LASTNAME_COLUMN = "lastName";

    public static final String COMPANY_COLUMN = "company";

    public static final String PASSWORD_COLUMN = "password";

    public static final String EMAIL_COLUMN = "email";

    public static final String GROUPS_COLUMN = "groups";

    private static final long serialVersionUID = 1791676740406045594L;

    private static final Log log = LogFactory.getLog(NuxeoPrincipalImpl.class);

    private final List<String> roles = new LinkedList<String>();

    // group not stored in the backend and added at login time
    private List<String> virtualGroups = new LinkedList<String>();

    // transitive closure of the "member of group" relation
    private List<String> allGroups;

    private boolean anonymous;

    private String principalId;

    private DocumentModel model;

    private DataModel dataModel;

    private String origUserName;

    public NuxeoPrincipalImpl(String name) throws ClientException {
        this(name, false);
    }

    public NuxeoPrincipalImpl(String name, boolean anonymous)
            throws ClientException {
        DocumentModelImpl documentModelImpl = new DocumentModelImpl(TYPE_NAME);
        // schema name hardcoded default when setModel is never called
        // which happens when a principal is created just to encapsulate
        // a username
        documentModelImpl.addDataModel(new DataModelImpl(SCHEMA_NAME,
                new HashMap<String, Object>()));
        setModel(documentModelImpl);
        dataModel.setData(USERNAME_COLUMN, name);
        this.anonymous = anonymous;
    }

    public String getCompany() {
        return (String) dataModel.getData(COMPANY_COLUMN);
    }

    public void setCompany(String company) {
        dataModel.setData(COMPANY_COLUMN, company);
    }

    public String getFirstName() {
        return (String) dataModel.getData(FIRSTNAME_COLUMN);
    }

    public void setFirstName(String firstName) {
        dataModel.setData(FIRSTNAME_COLUMN, firstName);
    }

    public String getLastName() {
        return (String) dataModel.getData(LASTNAME_COLUMN);
    }

    public void setLastName(String lastName) {
        dataModel.setData(LASTNAME_COLUMN, lastName);
    }

    // impossible to modify the name - it is PK
    public void setName(@NotNull String name) {
        dataModel.setData(USERNAME_COLUMN, name);
    }

    public void setRoles(@NotNull List<String> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public void setGroups(@NotNull List<String> groups) {
        if (virtualGroups != null && !virtualGroups.isEmpty()) {
            List<String> groupsToWrite = new ArrayList<String>();
            for (String group : groups) {
                if (!virtualGroups.contains(group)) {
                    groupsToWrite.add(group);
                }
            }
            dataModel.setData(GROUPS_COLUMN, groupsToWrite);
        } else {
            dataModel.setData(GROUPS_COLUMN, groups);
        }
    }

    public String getName() {
        return (String) dataModel.getData(USERNAME_COLUMN);
    }

    @SuppressWarnings("unchecked")
    public List<String> getGroups() {
        List<String> groups = new LinkedList();
        List<String> storedGroups = (List<String>) dataModel.getData(GROUPS_COLUMN);
        if (storedGroups != null) {
            groups.addAll(storedGroups);
        }
        groups.addAll(virtualGroups);
        return groups;
    }

    @Deprecated
    public List<String> getRoles() {
        return roles;
    }

    public void setPassword(String password) {
        dataModel.setData(PASSWORD_COLUMN, password);
    }

    public String getPassword() {
        // password should never be read at the UI level for safety reasons
        // + backend directories usually only store hashes that are useless
        // except to check authentication at the directory level
        return null;
    }

    @Override
    public String toString() {
        return (String) dataModel.getData(USERNAME_COLUMN);
    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    /**
     * @return the email.
     */
    public String getEmail() {
        return (String) dataModel.getData(EMAIL_COLUMN);
    }

    /**
     * @param email the email to set.
     */
    public void setEmail(String email) {
        dataModel.setData(EMAIL_COLUMN, email);
    }

    /**
     * @return the model.
     */
    public DocumentModel getModel() {
        return model;
    }

    public void setModel(DocumentModel model) throws ClientException {
        this.model = model;
        dataModel = model.getDataModels().values().iterator().next();
        updateAllGroups();
    }

    public boolean isMemberOf(String group) throws ClientException {
        return allGroups.contains(group);
    }

    public List<String> getAllGroups() {
        return allGroups;
    }

    public void updateAllGroups() throws ClientException {
        UserManager userManager;
        try {
            userManager = Framework.getService(UserManager.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        Set<String> checkedGroups = new HashSet<String>();
        List<String> groupsToProcess = new ArrayList<String>();
        List<String> resultingGroups = new ArrayList<String>();
        groupsToProcess.addAll(getGroups());

        while (!groupsToProcess.isEmpty()) {
            String groupName = groupsToProcess.remove(0);
            if (!checkedGroups.contains(groupName)) {
                checkedGroups.add(groupName);
                if (virtualGroups.contains(groupName)) {
                    resultingGroups.add(groupName);
                } else {
                    NuxeoGroup nxGroup = userManager.getGroup(groupName);
                    if (nxGroup == null) {
                        // XXX this should only happens in case of inconsistency
                        // in DB
                        log.error("User " + getName() + " references the "
                                + groupName + " group that does not exists");
                    } else {
                        groupsToProcess.addAll(nxGroup.getParentGroups());
                        resultingGroups.add(groupName);
                    }
                }
            }
        }

        allGroups = new ArrayList<String>(resultingGroups);
    }

    public List<String> getVirtualGroups() {
        return virtualGroups;
    }

    public void setVirtualGroups(List<String> virtualGroups)
            throws ClientException {
        this.virtualGroups = new ArrayList<String>(virtualGroups);
        updateAllGroups();
    }

    public boolean isAdministrator() {
        try {
            return isMemberOf("administrators");
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Principal) {
            String name = getName();
            String otherName = ((Principal) other).getName();
            if (name == null) {
                return otherName == null;
            } else {
                return name.equals(otherName);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        String name = getName();
        return name == null ? 0 : name.hashCode();
    }

    public String getOriginatingUser() {
        return origUserName;
    }

    public void setOriginatingUser(String originatingUser) {
        origUserName = originatingUser;
    }

}
