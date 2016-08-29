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
 *     Anahide Tchertchian
 *
 */

package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.ArrayList;
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
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * Base session class with helper methods common to all kinds of directory sessions.
 *
 * @author Anahide Tchertchian
 * @since 5.2M4
 */
public abstract class BaseSession implements Session {

    protected static final String POWER_USERS_GROUP = "powerusers";

    protected static final String READONLY_ENTRY_FLAG = "READONLY_ENTRY";

    protected static final String MULTI_TENANT_ID_FORMAT = "tenant_%s_%s";

    private final static Log log = LogFactory.getLog(BaseSession.class);

    protected final Directory directory;

    protected PermissionDescriptor[] permissions = null;

    protected BaseSession(Directory directory) {
        this.directory = directory;
    }

    /** To be implemented with a more specific return type. */
    public abstract Directory getDirectory();

    @Override
    public String getIdField() {
        return directory.getIdField();
    }

    @Override
    public String getPasswordField() {
        return directory.getPasswordField();
    }

    @Override
    public boolean isAuthenticating() {
        return directory.getPasswordField() != null;
    }

    @Override
    public boolean isReadOnly() {
        return directory.isReadOnly();
    }

    /**
     * Checks the current user rights for the given permission against the read-only flag and the permission descriptor.
     * <p>
     * Throws {@link DirectorySecurityException} if the user does not have adequate privileges.
     *
     * @throws DirectorySecurityException if access is denied
     * @since 8.3
     */
    public void checkPermission(String permission) {
        if (hasPermission(permission)) {
            return;
        }
        if (permission.equals(SecurityConstants.WRITE) && isReadOnly()) {
            throw new DirectorySecurityException("Directory is read-only");
        } else {
            NuxeoPrincipal user = ClientLoginModule.getCurrentPrincipal();
            throw new DirectorySecurityException("User " + user + " does not have " + permission + " permission");
        }
    }

    /**
     * Checks that there are no constraints for deleting the given entry id.
     *
     * @since 8.4
     */
    public void checkDeleteConstraints(String entryId) {
        List<DirectoryDeleteConstraint> deleteConstraints = directory.getDirectoryDeleteConstraints();
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        if (deleteConstraints != null && !deleteConstraints.isEmpty()) {
            for (DirectoryDeleteConstraint deleteConstraint : deleteConstraints) {
                if (!deleteConstraint.canDelete(directoryService, entryId)) {
                    throw new DirectoryDeleteConstraintException("This entry is referenced in another vocabulary.");
                }
            }
        }
    }

    /**
     * Checks the current user rights for the given permission against the read-only flag and the permission descriptor.
     * <p>
     * Returns {@code false} if the user does not have adequate privileges.
     *
     * @return {@code false} if access is denied
     * @since 8.3
     */
    public boolean hasPermission(String permission) {
        if (permission.equals(SecurityConstants.WRITE) && isReadOnly()) {
            if (log.isTraceEnabled()) {
                log.trace("Directory is read-only");
            }
            return false;
        }
        NuxeoPrincipal user = ClientLoginModule.getCurrentPrincipal();
        if (user == null) {
            return true;
        }
        String username = user.getName();
        if (username.equals(LoginComponent.SYSTEM_USERNAME)) {
            return true;
        }

        if (permissions == null || permissions.length == 0) {
            if (user.isAdministrator()) {
                return true;
            }
            if (user.isMemberOf(POWER_USERS_GROUP)) {
                return true;
            }
            // Return true for read access to anyone when nothing defined
            if (permission.equals(SecurityConstants.READ)) {
                return true;
            }
            // Deny in all other cases
            if (log.isTraceEnabled()) {
                log.trace("User " + user + " does not have " + permission + " permission");
            }
            return false;
        }

        List<String> groups = new ArrayList<>(user.getAllGroups());
        groups.add(SecurityConstants.EVERYONE);
        boolean allowed = hasPermission(permission, username, groups);
        if (!allowed) {
            // if the permission Read is not explicitly granted, check Write which includes it
            if (permission.equals(SecurityConstants.READ)) {
                allowed = hasPermission(SecurityConstants.WRITE, username, groups);
            }
        }
        if (!allowed && log.isTraceEnabled()) {
            log.trace("User " + user + " does not have " + permission + " permission");
        }
        return allowed;
    }

    protected boolean hasPermission(String permission, String username, List<String> groups) {
        for (PermissionDescriptor desc : permissions) {
            if (!desc.name.equals(permission)) {
                continue;
            }
            if (desc.groups != null) {
                for (String group : desc.groups) {
                    if (groups.contains(group)) {
                        return true;
                    }
                }
            }
            if (desc.users != null) {
                for (String user : desc.users) {
                    if (user.equals(username)) {
                        return true;
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
    public static DocumentModel createEntryModel(String sessionId, String schema, String id, Map<String, Object> values)
            throws PropertyException {
        DocumentModelImpl entry = new DocumentModelImpl(sessionId, schema, id, null, null, null, null,
                new String[] { schema }, new HashSet<String>(), null, null);
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
     * Allow setting the readonly entry flag to {@code Boolean.TRUE}. See {@code Session#isReadOnlyEntry(DocumentModel)}
     *
     * @since 5.3.1
     */
    public static DocumentModel createEntryModel(String sessionId, String schema, String id,
            Map<String, Object> values, boolean readOnly) throws PropertyException {
        DocumentModel entry = createEntryModel(sessionId, schema, id, values);
        if (readOnly) {
            setReadOnlyEntry(entry);
        }
        return entry;
    }

    protected static Map<String, Serializable> mkSerializableMap(Map<String, Object> map) {
        Map<String, Serializable> serializableMap = null;
        if (map != null) {
            serializableMap = new HashMap<String, Serializable>();
            for (String key : map.keySet()) {
                serializableMap.put(key, (Serializable) map.get(key));
            }
        }
        return serializableMap;
    }

    protected static Map<String, Object> mkObjectMap(Map<String, Serializable> map) {
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
        return contextData.getScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG) == Boolean.TRUE;
    }

    /**
     * Set the read-only flag of a directory entry. To be used by EntryAdaptor implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadOnlyEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        contextData.putScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG, Boolean.TRUE);
    }

    /**
     * Unset the read-only flag of a directory entry. To be used by EntryAdaptor implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadWriteEntry(DocumentModel entry) {
        ScopedMap contextData = entry.getContextData();
        contextData.putScopedValue(ScopeType.REQUEST, READONLY_ENTRY_FLAG, Boolean.FALSE);
    }

    /**
     * Compute a multi tenant directory id based on the given {@code tenantId}.
     *
     * @return the computed directory id
     * @since 5.6
     */
    public static String computeMultiTenantDirectoryId(String tenantId, String id) {
        return String.format(MULTI_TENANT_ID_FORMAT, tenantId, id);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException {
        log.info("Call an unoverrided query with offset and limit.");
        DocumentModelList entries = query(filter, fulltext, orderBy, fetchReferences);
        int toIndex = offset + limit;
        if (toIndex > entries.size()) {
            toIndex = entries.size();
        }

        return new DocumentModelListImpl(entries.subList(offset, toIndex));
    }

}
