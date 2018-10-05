/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     George Lefter
 *     Stéfane Fermigier
 *     Julien Carsique
 *     Anahide Tchertchian
 *     Alexandre Russel
 *     Thierry Delprat
 *     Stéphane Lacoin
 *     Sun Seng David Tan
 *     Thomas Roger
 *     Thierry Martins
 *     Benoit Delbosc
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.api.Framework;

public class NuxeoPrincipalImpl implements NuxeoPrincipal {

    private static final long serialVersionUID = 1L;

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
     * Constructor that sets principal to not anonymous, not administrator, and updates all the principal groups.
     */
    public NuxeoPrincipalImpl(String name) {
        this(name, false, false);
    }

    /**
     * Constructor that sets principal to not administrator, and updates all the principal groups.
     */
    public NuxeoPrincipalImpl(String name, boolean isAnonymous) {
        this(name, isAnonymous, false);
    }

    /**
     * Constructor that updates all the principal groups.
     */
    public NuxeoPrincipalImpl(String name, boolean isAnonymous, boolean isAdministrator) {
        this(name, isAnonymous, isAdministrator, true);
    }

    public NuxeoPrincipalImpl(String name, boolean isAnonymous, boolean isAdministrator, boolean updateAllGroups) {
        DocumentModel documentModelImpl = new SimpleDocumentModel(config.schemaName);
        // schema name hardcoded default when setModel is never called
        // which happens when a principal is created just to encapsulate
        // a username
        setModel(documentModelImpl, updateAllGroups);
        dataModel.setData(config.nameKey, name);
        this.isAnonymous = isAnonymous;
        this.isAdministrator = isAdministrator;
    }

    protected NuxeoPrincipalImpl(NuxeoPrincipalImpl other) {
        config = other.config;
        try {
            model = other.model.clone();
            model.copyContextData(other.model);
        } catch (CloneNotSupportedException cause) {
            throw new NuxeoException("Cannot clone principal " + this);
        }
        dataModel = model.getDataModel(config.schemaName);
        roles.addAll(other.roles);
        allGroups = new ArrayList<>(other.allGroups);
        virtualGroups = new ArrayList<>(other.virtualGroups);
        isAdministrator = other.isAdministrator;
        isAnonymous = other.isAnonymous;
        origUserName = other.origUserName;
        principalId = other.principalId;
    }

    public void setConfig(UserConfig config) {
        this.config = config;
    }

    public UserConfig getConfig() {
        return config;
    }

    @Override
    public String getCompany() {
        try {
            return (String) dataModel.getData(config.companyKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    @Override
    public void setCompany(String company) {
        dataModel.setData(config.companyKey, company);
    }

    @Override
    public String getFirstName() {
        try {
            return (String) dataModel.getData(config.firstNameKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    @Override
    public void setFirstName(String firstName) {
        dataModel.setData(config.firstNameKey, firstName);
    }

    @Override
    public String getLastName() {
        try {
            return (String) dataModel.getData(config.lastNameKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    @Override
    public void setLastName(String lastName) {
        dataModel.setData(config.lastNameKey, lastName);
    }

    // impossible to modify the name - it is PK
    @Override
    public void setName(String name) {
        dataModel.setData(config.nameKey, name);
    }

    @Override
    public void setRoles(List<String> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    @Override
    public void setGroups(List<String> groups) {
        if (virtualGroups != null && !virtualGroups.isEmpty()) {
            List<String> groupsToWrite = new ArrayList<String>();
            for (String group : groups) {
                if (!virtualGroups.contains(group)) {
                    groupsToWrite.add(group);
                }
            }
            dataModel.setData(config.groupsKey, groupsToWrite);
        } else {
            dataModel.setData(config.groupsKey, groups);
        }
    }

    @Override
    public String getName() {
        try {
            return (String) dataModel.getData(config.nameKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
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
    @Override
    public List<String> getRoles() {
        return new ArrayList<String>(roles);
    }

    @Override
    public void setPassword(String password) {
        dataModel.setData(config.passwordKey, password);
    }

    @Override
    public String getPassword() {
        // password should never be read at the UI level for safety reasons
        // + backend directories usually only store hashes that are useless
        // except to check authentication at the directory level
        return null;
    }

    @Override
    public String toString() {
        return (String) dataModel.getData(config.nameKey);
    }

    @Override
    public String getPrincipalId() {
        return principalId;
    }

    @Override
    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    @Override
    public String getEmail() {
        try {
            return (String) dataModel.getData(config.emailKey);
        } catch (PropertyException e) {
            return null;
        }
    }

    @Override
    public void setEmail(String email) {
        dataModel.setData(config.emailKey, email);
    }

    @Override
    public DocumentModel getModel() {
        return model;
    }

    /**
     * Sets model and recomputes all groups.
     */
    public void setModel(DocumentModel model, boolean updateAllGroups) {
        this.model = model;
        dataModel = model.getDataModels().values().iterator().next();
        if (updateAllGroups) {
            updateAllGroups();
        }
    }

    @Override
    public void setModel(DocumentModel model) {
        setModel(model, true);
    }

    @Override
    public boolean isMemberOf(String group) {
        return allGroups.contains(group);
    }

    @Override
    public List<String> getAllGroups() {
        return new ArrayList<String>(allGroups);
    }

    public void updateAllGroups() {
        UserManager userManager = Framework.getService(UserManager.class);
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
                    try {
                        nxGroup = userManager.getGroup(groupName);
                    } catch (DirectoryException de) {
                        if (virtualGroups.contains(groupName)) {
                            // do not fail while retrieving a virtual group
                            log.warn("Failed to get group '" + groupName + "' due to '" + de.getMessage()
                                    + "': permission resolution involving groups may not be correct");
                            nxGroup = null;
                        } else {
                            throw de;
                        }
                    }
                }
                if (nxGroup == null) {
                    if (virtualGroups.contains(groupName)) {
                        // just add the virtual group as is
                        resultingGroups.add(groupName);
                    } else if (userManager != null) {
                        // XXX this should only happens in case of
                        // inconsistency in DB
                        log.error("User " + getName() + " references the " + groupName + " group that does not exists");
                    }
                } else {
                    groupsToProcess.addAll(nxGroup.getParentGroups());
                    // fetch the group name from the returned entry in case
                    // it does not have the same case than the actual entry in
                    // directory (for case insensitive directories)
                    resultingGroups.add(nxGroup.getName());
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
        return new ArrayList<String>(virtualGroups);
    }

    public void setVirtualGroups(List<String> virtualGroups, boolean updateAllGroups) {
        this.virtualGroups = new ArrayList<String>(virtualGroups);
        if (updateAllGroups) {
            updateAllGroups();
        }
    }

    /**
     * Sets virtual groups and recomputes all groups.
     */
    public void setVirtualGroups(List<String> virtualGroups) {
        setVirtualGroups(virtualGroups, true);
    }

    @Override
    public boolean isAdministrator() {
        return isAdministrator;
    }

    @Override
    public String getTenantId() {
        return null;
    }

    @Override
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

    @Override
    public String getOriginatingUser() {
        return origUserName;
    }

    @Override
    public void setOriginatingUser(String originatingUser) {
        origUserName = originatingUser;
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

    protected NuxeoPrincipal cloneTransferable() {
        return new TransferableClone(this);
    }

    /**
     * Provides another implementation which marshall the user id instead of transferring the whole content and resolve
     * it when unmarshalled.
     */
    static protected class TransferableClone extends NuxeoPrincipalImpl {

        protected TransferableClone(NuxeoPrincipalImpl other) {
            super(other);
        }

        static class DataTransferObject implements Serializable {

            private static final long serialVersionUID = 1L;

            final String username;

            final String originatingUser;

            DataTransferObject(NuxeoPrincipal principal) {
                username = principal.getName();
                originatingUser = principal.getOriginatingUser();
            }

            private Object readResolve() throws ObjectStreamException {
                UserManager userManager = Framework.getService(UserManager.class);
                // look up principal as system user to avoid permission checks in directories
                NuxeoPrincipal principal = Framework.doPrivileged(() -> userManager.getPrincipal(username));
                if (principal == null) {
                    throw new NullPointerException("No principal: " + username);
                }
                principal.setOriginatingUser(originatingUser);
                return principal;
            }

        }

        private Object writeReplace() throws ObjectStreamException {
            return new DataTransferObject(this);
        }
    }
}
