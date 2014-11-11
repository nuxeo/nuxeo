/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 *
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * Base session class with helper methods common to all kinds of directory
 * sessions.
 *
 * @author Anahide Tchertchian
 * @since 5.2M4
 */
public abstract class BaseSession implements Session {

    protected static final String POWER_USERS_GROUP = "powerusers";

    protected static final String READONLY_ENTRY_FLAG = "READONLY_ENTRY";

    protected static final String MULTI_TENANT_ID_FORMAT = "tenant_%s_%s";

    private final static Log log = LogFactory.getLog(BaseSession.class);

    protected PermissionDescriptor[] permissions = null;

    /**
     * Check the current user rights for the given permission against the
     * permission descriptor
     * 
     * @return true if the user
     *
     * @since 5.9.6
     */
    public boolean isCurrentUserAllowed(String permissionTocheck) {
        PermissionDescriptor[] permDescriptors = permissions;
        NuxeoPrincipal currentUser = ClientLoginModule.getCurrentPrincipal();

        if (currentUser == null) {
            if (log.isDebugEnabled()) {
                log.debug("Can't get current user to check directory permission. EVERYTHING is allowed by default");
            }
            return true;
        }
        String username = currentUser.getName();
        List<String> userGroups = currentUser.getAllGroups();

        if (username.equalsIgnoreCase(LoginComponent.SYSTEM_USERNAME)) {
            return true;
        }

        if (permDescriptors == null || permDescriptors.length == 0) {
            if (currentUser.isAdministrator()) {
                // By default if nothing is specified, admin is allowed
                return true;
            }
            if (currentUser.isMemberOf(POWER_USERS_GROUP)) {
                return true;
            }

            // Return true for read access to anyone when nothing defined
            if (permissionTocheck.equalsIgnoreCase(SecurityConstants.READ)) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "User '%s', is not allowed for permission '%s' on the directory",
                        currentUser, permissionTocheck));
            }
            // Deny in all other case
            return false;
        }
        boolean allowed = checkPermission(permDescriptors, permissionTocheck,
                username, userGroups);
        if (allowed != true) {
            // If the permission has not been found and if the permission to
            // check is read
            // Then try to check if the current user is allowed, because having
            // write access include read
            if (permissionTocheck.equalsIgnoreCase(SecurityConstants.READ)) {
                allowed = checkPermission(permDescriptors,
                        SecurityConstants.WRITE, username, userGroups);
            }
        }
        if (log.isDebugEnabled() && !allowed) {
            log.debug(String.format(
                    "User '%s', is not allowed for permission '%s' on the directory",
                    currentUser, permissionTocheck));
        }
        return allowed;

    }

    private boolean checkPermission(PermissionDescriptor permDescriptors[],
            String permToChek, String username, List<String> userGroups) {
        for (int i = 0; i < permDescriptors.length; i++) {
            PermissionDescriptor currentDesc = permDescriptors[i];
            if (currentDesc.name.equalsIgnoreCase(permToChek)) {
                if (currentDesc.groups != null) {
                    for (int j = 0; j < currentDesc.groups.length; j++) {
                        String groupName = currentDesc.groups[j];
                        if (userGroups.contains(groupName)) {
                            return true;
                        }
                    }
                }

                if (currentDesc.users != null) {
                    for (int j = 0; j < currentDesc.users.length; j++) {
                        String currentUsername = currentDesc.users[j];
                        if (currentUsername.equals(username)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns a bare document model suitable for directory implementations.
     * <p>
     * Can be used for creation screen.
     *
     * @since 5.2M4
     */
    public static DocumentModel createEntryModel(String sessionId,
            String schema, String id, Map<String, Object> values)
            throws PropertyException {
        DocumentModelImpl entry = new DocumentModelImpl(sessionId, schema, id,
                null, null, null, null, new String[] { schema },
                new HashSet<String>(), null, null);
        DataModel dataModel;
        if (values == null) {
            values = Collections.emptyMap();
        }
        dataModel = new DataModelImpl(schema, values);
        entry.addDataModel(dataModel);
        return entry;
    }

    /**
     * Returns a bare document model suitable for directory implementations.
     * <p>
     * Allow setting the readonly entry flag to {@code Boolean.TRUE}. See
     * {@code Session#isReadOnlyEntry(DocumentModel)}
     *
     * @since 5.3.1
     */
    public static DocumentModel createEntryModel(String sessionId,
            String schema, String id, Map<String, Object> values,
            boolean readOnly) throws PropertyException {
        DocumentModel entry = createEntryModel(sessionId, schema, id, values);
        if (readOnly) {
            setReadOnlyEntry(entry);
        }
        return entry;
    }

    protected static Map<String, Serializable> mkSerializableMap(
            Map<String, Object> map) {
        Map<String, Serializable> serializableMap = null;
        if (map != null) {
            serializableMap = new HashMap<String, Serializable>();
            for (String key : map.keySet()) {
                serializableMap.put(key, (Serializable) map.get(key));
            }
        }
        return serializableMap;
    }

    protected static Map<String, Object> mkObjectMap(
            Map<String, Serializable> map) {
        Map<String, Object> objectMap = null;
        if (map != null) {
            objectMap = new HashMap<String, Object>();
            for (String key : map.keySet()) {
                objectMap.put(key, map.get(key));
            }
        }
        return objectMap;
    }

    /**
     * Test whether entry comes from a read-only back-end directory.
     *
     * @since 5.3.1
     */
    public static boolean isReadOnlyEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        return contextData.getScopedValue(ScopeType.REQUEST,
                READONLY_ENTRY_FLAG) == Boolean.TRUE;
    }

    /**
     * Set the read-only flag of a directory entry. To be used by EntryAdaptor
     * implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadOnlyEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        contextData.putScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG,
                Boolean.TRUE);
    }

    /**
     * Unset the read-only flag of a directory entry. To be used by EntryAdaptor
     * implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadWriteEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        contextData.putScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG,
                Boolean.FALSE);
    }

    /**
     * Compute a multi tenant directory id based on the given {@code tenantId}.
     *
     * @return the computed directory id
     * @since 5.6
     */
    public static String computeMultiTenantDirectoryId(String tenantId,
            String id) {
        return String.format(MULTI_TENANT_ID_FORMAT, tenantId, id);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset)
            throws ClientException, DirectoryException {
        log.info("Call an unoverrided query with offset and limit.");
        DocumentModelList entries = query(filter, fulltext, orderBy,
                fetchReferences);
        int toIndex = offset + limit;
        if (toIndex > entries.size()) {
            toIndex = entries.size();
        }

        return new DocumentModelListImpl(entries.subList(offset, toIndex));
    }

}
