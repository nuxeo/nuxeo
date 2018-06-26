/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor.SubstringMatchType;
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
public abstract class BaseSession implements Session, EntrySource {

    protected static final String POWER_USERS_GROUP = "powerusers";

    protected static final String READONLY_ENTRY_FLAG = "READONLY_ENTRY";

    protected static final String MULTI_TENANT_ID_FORMAT = "tenant_%s_%s";

    protected static final String TENANT_ID_FIELD = "tenantId";

    private final static Log log = LogFactory.getLog(BaseSession.class);

    protected final Directory directory;

    protected PermissionDescriptor[] permissions = null;

    // needed for test framework to be able to do a full backup of a directory including password
    protected boolean readAllColumns;

    protected String schemaName;

    protected String directoryName;

    protected SubstringMatchType substringMatchType;

    protected Class<? extends Reference> referenceClass;

    protected String passwordHashAlgorithm;

    protected boolean autoincrementId;

    protected boolean computeMultiTenantId;

    protected BaseSession(Directory directory, Class<? extends Reference> referenceClass) {
        this.directory = directory;
        schemaName = directory.getSchema();
        directoryName = directory.getName();

        BaseDirectoryDescriptor desc = directory.getDescriptor();
        substringMatchType = desc.getSubstringMatchType();
        autoincrementId = desc.isAutoincrementIdField();
        permissions = desc.permissions;
        passwordHashAlgorithm = desc.passwordHashAlgorithm;
        this.referenceClass = referenceClass;
        computeMultiTenantId = desc.isComputeMultiTenantId();
    }

    /** To be implemented with a more specific return type. */
    public abstract Directory getDirectory();

    @Override
    public void setReadAllColumns(boolean readAllColumns) {
        this.readAllColumns = readAllColumns;
    }

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
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
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
            return false;
        }
        String username = user.getName();
        if (username.equals(LoginComponent.SYSTEM_USERNAME) || user.isAdministrator()) {
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
                new String[] { schema }, new HashSet<>(), null, null);
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
    public static DocumentModel createEntryModel(String sessionId, String schema, String id, Map<String, Object> values,
            boolean readOnly) throws PropertyException {
        DocumentModel entry = createEntryModel(sessionId, schema, id, values);
        if (readOnly) {
            setReadOnlyEntry(entry);
        }
        return entry;
    }

    /**
     * Test whether entry comes from a read-only back-end directory.
     *
     * @since 5.3.1
     */
    public static boolean isReadOnlyEntry(DocumentModel entry) {
        return Boolean.TRUE.equals(entry.getContextData(READONLY_ENTRY_FLAG));
    }

    /**
     * Set the read-only flag of a directory entry. To be used by EntryAdaptor implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadOnlyEntry(DocumentModel entry) {
        entry.putContextData(READONLY_ENTRY_FLAG, Boolean.TRUE);
    }

    /**
     * Unset the read-only flag of a directory entry. To be used by EntryAdaptor implementations for instance.
     *
     * @since 5.3.2
     */
    public static void setReadWriteEntry(DocumentModel entry) {
        entry.putContextData(READONLY_ENTRY_FLAG, Boolean.FALSE);
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
    public DocumentModel getEntry(String id) throws DirectoryException {
        return getEntry(id, true);
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences) throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return null;
        }
        if (readAllColumns) {
            // bypass cache when reading all columns
            return getEntryFromSource(id, fetchReferences);
        }
        return directory.getCache().getEntry(id, this, fetchReferences);
    }

    @Override
    public DocumentModelList getEntries() throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }
        return query(Collections.emptyMap());
    }

    @Override
    public DocumentModel getEntryFromSource(String id, boolean fetchReferences) throws DirectoryException {
        String idFieldName = directory.getSchemaFieldMap().get(getIdField()).getName().getPrefixedName();
        DocumentModelList result = query(Collections.singletonMap(idFieldName, id), Collections.emptySet(),
                Collections.emptyMap(), true);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public DocumentModel createEntry(DocumentModel documentModel) {
        return createEntry(documentModel.getProperties(schemaName));
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap) throws DirectoryException {
        checkPermission(SecurityConstants.WRITE);
        DocumentModel docModel = createEntryWithoutReferences(fieldMap);

        // Add references fields
        Map<String, Field> schemaFieldMap = directory.getSchemaFieldMap();
        String idFieldName = schemaFieldMap.get(getIdField()).getName().getPrefixedName();
        Object entry = fieldMap.get(idFieldName);
        String sourceId = docModel.getId();
        for (Reference reference : getDirectory().getReferences()) {
            String referenceFieldName = schemaFieldMap.get(reference.getFieldName()).getName().getPrefixedName();
            if (getDirectory().getReferences(reference.getFieldName()).size() > 1) {
                if (log.isWarnEnabled()) {
                    log.warn("Directory " + directoryName + " cannot create field " + reference.getFieldName()
                            + " for entry " + entry + ": this field is associated with more than one reference");
                }
                continue;
            }

            @SuppressWarnings("unchecked")
            List<String> targetIds = (List<String>) fieldMap.get(referenceFieldName);
            if (reference.getClass() == referenceClass) {
                reference.addLinks(sourceId, targetIds, this);
            } else {
                reference.addLinks(sourceId, targetIds);
            }
        }

        getDirectory().invalidateCaches();
        return docModel;
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws DirectoryException {
        checkPermission(SecurityConstants.WRITE);

        String id = docModel.getId();
        if (id == null) {
            throw new DirectoryException("The document cannot be updated because its id is missing");
        }

        // Retrieve the references to update in the document model, and update the rest
        List<String> referenceFieldList = updateEntryWithoutReferences(docModel);

        // update reference fields
        for (String referenceFieldName : referenceFieldList) {
            List<Reference> references = directory.getReferences(referenceFieldName);
            if (references.size() > 1) {
                // not supported
                if (log.isWarnEnabled()) {
                    log.warn("Directory " + getDirectory().getName() + " cannot update field " + referenceFieldName
                            + " for entry " + docModel.getId()
                            + ": this field is associated with more than one reference");
                }
            } else {
                Reference reference = references.get(0);
                @SuppressWarnings("unchecked")
                List<String> targetIds = (List<String>) docModel.getProperty(schemaName, referenceFieldName);
                if (reference.getClass() == referenceClass) {
                    reference.setTargetIdsForSource(docModel.getId(), targetIds, this);
                } else {
                    reference.setTargetIdsForSource(docModel.getId(), targetIds);
                }
            }
        }
        getDirectory().invalidateCaches();
    }

    @Override
    public void deleteEntry(DocumentModel docModel) throws DirectoryException {
        deleteEntry(docModel.getId());
    }

    @Override
    @Deprecated
    public void deleteEntry(String id, Map<String, String> map) throws DirectoryException {
        deleteEntry(id);
    }

    @Override
    public void deleteEntry(String id) throws DirectoryException {

        if (!canDeleteMultiTenantEntry(id)) {
            throw new OperationNotAllowedException("Operation not allowed in the current tenant context",
                    "label.directory.error.multi.tenant.operationNotAllowed", null);
        }

        checkPermission(SecurityConstants.WRITE);
        checkDeleteConstraints(id);

        for (Reference reference : getDirectory().getReferences()) {
            if (reference.getClass() == referenceClass) {
                reference.removeLinksForSource(id, this);
            } else {
                reference.removeLinksForSource(id);
            }
        }
        deleteEntryWithoutReferences(id);
        getDirectory().invalidateCaches();
    }

    protected boolean canDeleteMultiTenantEntry(String entryId) throws DirectoryException {
        if (isMultiTenant()) {
            // can only delete entry from the current tenant
            String tenantId = getCurrentTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                DocumentModel entry = getEntry(entryId);
                String entryTenantId = (String) entry.getProperty(schemaName, TENANT_ID_FIELD);
                if (StringUtils.isBlank(entryTenantId) || !entryTenantId.equals(tenantId)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Trying to delete entry '%s' not part of current tenant '%s'", entryId,
                                tenantId));
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Applies offset and limit to a DocumentModelList
     *
     * @param results the query results without limit and offet
     * @param limit maximum number of results ignored if less than 1
     * @param offset number of rows skipped before starting, will be 0 if less than 0.
     * @return the result with applied limit and offset
     * @since 10.1
     * @see Session#query(Map, Set, Map, boolean, int, int)
     */
    protected DocumentModelList applyQueryLimits(DocumentModelList results, int limit, int offset) {
        offset = Math.max(0, offset);
        int toIndex = limit >= 1 ? Math.min(results.size(), offset + limit) : results.size();
        return new DocumentModelListImpl(results.subList(offset, toIndex));
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter) throws DirectoryException {
        return query(filter, Collections.emptySet());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext) throws DirectoryException {
        return query(filter, fulltext, new HashMap<>());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException {
        return query(filter, fulltext, orderBy, false);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws DirectoryException {
        return query(filter, fulltext, orderBy, fetchReferences, -1, 0);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, String columnName) throws DirectoryException {
        return getProjection(filter, Collections.emptySet(), columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName)
            throws DirectoryException {
        DocumentModelList docList = query(filter, fulltext);
        List<String> result = new ArrayList<>();
        for (DocumentModel docModel : docList) {
            Object obj = docModel.getProperty(schemaName, columnName);
            String propValue = String.valueOf(obj);
            result.add(propValue);
        }
        return result;
    }

    /**
     * Returns {@code true} if this directory supports multi tenancy, {@code false} otherwise.
     */
    protected boolean isMultiTenant() {
        return directory.isMultiTenant();
    }

    /**
     * Returns the tenant id of the logged user if any, {@code null} otherwise.
     */
    protected String getCurrentTenantId() {
        NuxeoPrincipal principal = ClientLoginModule.getCurrentPrincipal();
        return principal != null ? principal.getTenantId() : null;
    }

    /** To be implemented for specific creation. */
    protected abstract DocumentModel createEntryWithoutReferences(Map<String, Object> fieldMap);

    /** To be implemented for specific update. */
    protected abstract List<String> updateEntryWithoutReferences(DocumentModel docModel) throws DirectoryException;

    /** To be implemented for specific deletion. */
    protected abstract void deleteEntryWithoutReferences(String id) throws DirectoryException;

}
