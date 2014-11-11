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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class NuxeoPrincipalImpl implements NuxeoPrincipal {

    private static final long serialVersionUID = 1791676740406045594L;

    private static final Log log = LogFactory.getLog(NuxeoPrincipalImpl.class);

    protected UserConfig config = UserConfig.DEFAULT;

    public final List<String> roles = new LinkedList<String>();

    // group not stored in the backend and added at login time
    public List<String> virtualGroups = new LinkedList<String>();

    // transitive closure of the "member of group" relation
    public List<String> allGroups;

    public final boolean isAnonymous;

    public boolean isAdministrator;

    public String principalId;

    public DocumentModel model;

    public DataModel dataModel;

    public String origUserName;

    /**
     * Constructor that sets principal to not anonymous, not administrator, and
     * updates all the principal groups.
     */
    public NuxeoPrincipalImpl(String name) throws ClientException {
        this(name, false, false);
    }

    /**
     * Constructor that sets principal to not administrator, and updates all
     * the principal groups.
     */
    public NuxeoPrincipalImpl(String name, boolean isAnonymous)
            throws ClientException {
        this(name, isAnonymous, false);
    }

    /**
     * Constructor that updates all the principal groups.
     */
    public NuxeoPrincipalImpl(String name, boolean isAnonymous,
            boolean isAdministrator) throws ClientException {
        this(name, isAnonymous, isAdministrator, true);
    }

    public NuxeoPrincipalImpl(String name, boolean isAnonymous,
            boolean isAdministrator, boolean updateAllGroups)
            throws ClientException {
        DocumentModelImpl documentModelImpl = new DocumentModelImpl(
                config.schemaName);
        // schema name hardcoded default when setModel is never called
        // which happens when a principal is created just to encapsulate
        // a username
        documentModelImpl.addDataModel(new DataModelImpl(config.schemaName,
                new HashMap<String, Object>()));
        setModel(documentModelImpl, updateAllGroups);
        dataModel.setData(config.nameKey, name);
        this.isAnonymous = isAnonymous;
        this.isAdministrator = isAdministrator;
    }

    public void setConfig(UserConfig config) {
        this.config = config;
    }

    public UserConfig getConfig() {
        return config;
    }

    public String getCompany() {
        try {
            return (String) dataModel.getData(config.companyKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    public void setCompany(String company) {
        try {
            dataModel.setData(config.companyKey, company);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public String getFirstName() {
        try {
            return (String) dataModel.getData(config.firstNameKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    public void setFirstName(String firstName) {
        try {
            dataModel.setData(config.firstNameKey, firstName);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public String getLastName() {
        try {
            return (String) dataModel.getData(config.lastNameKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    public void setLastName(String lastName) {
        try {
            dataModel.setData(config.lastNameKey, lastName);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // impossible to modify the name - it is PK
    public void setName(String name) {
        try {
            dataModel.setData(config.nameKey, name);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void setRoles(List<String> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public void setGroups(List<String> groups) {
        if (virtualGroups != null && !virtualGroups.isEmpty()) {
            List<String> groupsToWrite = new ArrayList<String>();
            for (String group : groups) {
                if (!virtualGroups.contains(group)) {
                    groupsToWrite.add(group);
                }
            }
            try {
                dataModel.setData(config.groupsKey, groupsToWrite);
            } catch (PropertyException e) {
                throw new ClientRuntimeException(e);
            }
        } else {
            try {
                dataModel.setData(config.groupsKey, groups);
            } catch (PropertyException e) {
                throw new ClientRuntimeException(e);
            }
        }
    }

    public String getName() {
        try {
            return (String) dataModel.getData(config.nameKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getGroups() {
        List<String> groups = new LinkedList<String>();
        List<String> storedGroups;
        try {
            storedGroups = (List<String>) dataModel.getData(config.groupsKey);
        } catch (PropertyException e) {
            return null;
        }
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
        try {
            dataModel.setData(config.passwordKey, password);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public String getPassword() {
        // password should never be read at the UI level for safety reasons
        // + backend directories usually only store hashes that are useless
        // except to check authentication at the directory level
        return null;
    }

    @Override
    public String toString() {
        try {
            return (String) dataModel.getData(config.nameKey);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    public String getEmail() {
        try {
            return (String) dataModel.getData(config.emailKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    public void setEmail(String email) {
        try {
            dataModel.setData(config.emailKey, email);
        } catch (PropertyException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public DocumentModel getModel() {
        return model;
    }

    /**
     * Sets model and recomputes all groups.
     */
    public void setModel(DocumentModel model, boolean updateAllGroups)
            throws ClientException {
        this.model = model;
        dataModel = model.getDataModels().values().iterator().next();
        if (updateAllGroups) {
            updateAllGroups();
        }
    }

    public void setModel(DocumentModel model) throws ClientException {
        setModel(model, true);
    }

    public boolean isMemberOf(String group) {
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
                NuxeoGroup nxGroup = null;
                if (userManager != null) {
                    nxGroup = userManager.getGroup(groupName);
                }
                if (nxGroup == null) {
                    if (virtualGroups.contains(groupName)) {
                        // just add the virtual group as is
                        resultingGroups.add(groupName);
                    } else if (userManager != null) {
                        // XXX this should only happens in case of
                        // inconsistency in DB
                        log.error("User " + getName() + " references the "
                                + groupName + " group that does not exists");
                    }
                } else {
                    groupsToProcess.addAll(nxGroup.getParentGroups());
                    resultingGroups.add(groupName);
                    // XXX: maybe remove group from virtual groups if it
                    // actually exists? otherwise it would be ignored when
                    // setting groups
                }
            }
        }

        allGroups = new ArrayList<String>(resultingGroups);

        // set isAdministrator boolean according to groups declared on user
        // manager
        if (!isAdministrator() && userManager != null) {
            List<String> adminGroups = userManager.getAdministratorsGroups();
            for (String adminGroup : adminGroups) {
                if (allGroups.contains(adminGroup)) {
                    isAdministrator = true;
                    break;
                }
            }
        }
    }

    public List<String> getVirtualGroups() {
        return virtualGroups;
    }

    public void setVirtualGroups(List<String> virtualGroups,
            boolean updateAllGroups) throws ClientException {
        this.virtualGroups = new ArrayList<String>(virtualGroups);
        if (updateAllGroups) {
            updateAllGroups();
        }
    }

    /**
     * Sets virtual groups and recomputes all groups.
     */
    public void setVirtualGroups(List<String> virtualGroups)
            throws ClientException {
        setVirtualGroups(virtualGroups, true);
    }

    public boolean isAdministrator() {
        return isAdministrator
                || SecurityConstants.SYSTEM_USERNAME.equals(getName());
    }

    public boolean isAnonymous() {
        return isAnonymous;
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
