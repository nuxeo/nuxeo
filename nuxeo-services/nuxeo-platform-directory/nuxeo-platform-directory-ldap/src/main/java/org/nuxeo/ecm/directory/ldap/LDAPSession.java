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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.directory.ldap;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SimpleTimeZone;

import javax.naming.Context;
import javax.naming.LimitExceededException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.utils.SIDGenerator;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor.SubstringMatchType;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFieldMapper;
import org.nuxeo.ecm.directory.EntryAdaptor;
import org.nuxeo.ecm.directory.EntrySource;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;

/**
 * This class represents a session against an LDAPDirectory.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class LDAPSession extends BaseSession implements EntrySource {

    protected static final String MISSING_ID_LOWER_CASE = "lower";

    protected static final String MISSING_ID_UPPER_CASE = "upper";

    private static final Log log = LogFactory.getLog(LDAPSession.class);

    // set to false for debugging
    private static final boolean HIDE_PASSWORD_IN_LOGS = true;

    protected final String schemaName;

    protected DirContext dirContext;

    protected final String idAttribute;

    protected final String idCase;

    protected final String searchBaseDn;

    protected final Set<String> emptySet = Collections.emptySet();

    protected final String sid;

    protected final Map<String, Field> schemaFieldMap;

    protected SubstringMatchType substringMatchType;

    protected final String rdnAttribute;

    protected final String rdnField;

    protected final String passwordHashAlgorithm;

    public LDAPSession(LDAPDirectory directory) {
        super(directory);
        DirectoryFieldMapper fieldMapper = directory.getFieldMapper();
        idAttribute = fieldMapper.getBackendField(getIdField());
        LDAPDirectoryDescriptor descriptor = directory.getDescriptor();
        idCase = descriptor.getIdCase();
        schemaName = directory.getSchema();
        schemaFieldMap = directory.getSchemaFieldMap();
        sid = String.valueOf(SIDGenerator.next());
        searchBaseDn = descriptor.getSearchBaseDn();
        substringMatchType = descriptor.getSubstringMatchType();
        rdnAttribute = descriptor.getRdnAttribute();
        rdnField = directory.getFieldMapper().getDirectoryField(rdnAttribute);
        passwordHashAlgorithm = descriptor.passwordHashAlgorithm;
        permissions = descriptor.permissions;
    }

    @Override
    public LDAPDirectory getDirectory() {
        return (LDAPDirectory) directory;
    }

    public DirContext getContext() {
        if (dirContext == null) {
            // Initialize directory context lazily
            LDAPDirectory ldapDirectory = (LDAPDirectory) directory;
            ContextProvider testServer = ldapDirectory.getTestServer();
            DirContext context = testServer == null ? ldapDirectory.createContext() : testServer.getContext();
            dirContext = LdapRetryHandler.wrap(context, ldapDirectory.getServer().getRetries());
        }
        return dirContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DocumentModel createEntry(Map<String, Object> fieldMap) {
        checkPermission(SecurityConstants.WRITE);
        LDAPDirectoryDescriptor descriptor = getDirectory().getDescriptor();
        List<String> referenceFieldList = new LinkedList<>();
        try {
            String dn = String.format("%s=%s,%s", rdnAttribute, fieldMap.get(rdnField), descriptor.getCreationBaseDn());
            Attributes attrs = new BasicAttributes();
            Attribute attr;

            List<String> mandatoryAttributes = getMandatoryAttributes();
            for (String mandatoryAttribute : mandatoryAttributes) {
                attr = new BasicAttribute(mandatoryAttribute);
                attr.add(" ");
                attrs.put(attr);
            }

            String[] creationClasses = descriptor.getCreationClasses();
            if (creationClasses.length != 0) {
                attr = new BasicAttribute("objectclass");
                for (String creationClasse : creationClasses) {
                    attr.add(creationClasse);
                }
                attrs.put(attr);
            }

            for (String fieldId : fieldMap.keySet()) {
                String backendFieldId = getDirectory().getFieldMapper().getBackendField(fieldId);
                if (fieldId.equals(getPasswordField())) {
                    attr = new BasicAttribute(backendFieldId);
                    String password = (String) fieldMap.get(fieldId);
                    password = PasswordHelper.hashPassword(password, passwordHashAlgorithm);
                    attr.add(password);
                    attrs.put(attr);
                } else if (getDirectory().isReference(fieldId)) {
                    List<Reference> references = directory.getReferences(fieldId);
                    if (references.size() > 1) {
                        // not supported
                    } else {
                        Reference reference = references.get(0);
                        if (reference instanceof LDAPReference) {
                            attr = new BasicAttribute(((LDAPReference) reference).getStaticAttributeId());
                            attr.add(descriptor.getEmptyRefMarker());
                            attrs.put(attr);
                        }
                    }
                    referenceFieldList.add(fieldId);
                } else if (LDAPDirectory.DN_SPECIAL_ATTRIBUTE_KEY.equals(backendFieldId)) {
                    // ignore special DN field
                    log.warn(String.format("field %s is mapped to read only DN field: ignored", fieldId));
                } else {
                    Object value = fieldMap.get(fieldId);
                    if (value != null && !value.equals("") && !Collections.emptyList().equals(value)) {
                        attrs.put(getAttributeValue(fieldId, value));
                    }
                }
            }

            if (log.isDebugEnabled()) {
                Attributes logAttrs;
                if (HIDE_PASSWORD_IN_LOGS && attrs.get(getPasswordField()) != null) {
                    logAttrs = (Attributes) attrs.clone();
                    logAttrs.put(getPasswordField(), "********"); // hide password in logs
                } else {
                    logAttrs = attrs;
                }
                String idField = getIdField();
                log.debug(String.format("LDAPSession.createEntry(%s=%s): LDAP bind dn='%s' attrs='%s' [%s]", idField,
                        fieldMap.get(idField), dn, logAttrs, this));
            }
            getContext().bind(dn, null, attrs);

            for (String referenceFieldName : referenceFieldList) {
                List<Reference> references = directory.getReferences(referenceFieldName);
                if (references.size() > 1) {
                    // not supported
                } else {
                    Reference reference = references.get(0);
                    List<String> targetIds = (List<String>) fieldMap.get(referenceFieldName);
                    reference.addLinks((String) fieldMap.get(getIdField()), targetIds);
                }
            }
            String dnFieldName = getDirectory().getFieldMapper()
                                               .getDirectoryField(LDAPDirectory.DN_SPECIAL_ATTRIBUTE_KEY);
            if (getDirectory().getSchemaFieldMap().containsKey(dnFieldName)) {
                // add the DN special attribute to the fieldmap of the new
                // entry
                fieldMap.put(dnFieldName, dn);
            }
            getDirectory().invalidateCaches();
            return fieldMapToDocumentModel(fieldMap);
        } catch (NamingException e) {
            handleException(e, "createEntry failed");
            return null;
        }
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
        return directory.getCache().getEntry(id, this, fetchReferences);
    }

    @Override
    public DocumentModel getEntryFromSource(String id, boolean fetchReferences) throws DirectoryException {
        try {
            SearchResult result = getLdapEntry(id, false);
            if (result == null) {
                return null;
            }
            return ldapResultToDocumentModel(result, id, fetchReferences);
        } catch (NamingException e) {
            throw new DirectoryException("getEntry failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasEntry(String id) throws DirectoryException {
        try {
            // TODO: check directory cache first
            return getLdapEntry(id) != null;
        } catch (NamingException e) {
            throw new DirectoryException("hasEntry failed: " + e.getMessage(), e);
        }
    }

    protected SearchResult getLdapEntry(String id) throws NamingException, DirectoryException {
        return getLdapEntry(id, false);
    }

    protected SearchResult getLdapEntry(String id, boolean fetchAllAttributes) throws NamingException {
        if (StringUtils.isEmpty(id)) {
            log.warn("The application should not " + "query for entries with an empty id " + "=> return no results");
            return null;
        }
        String filterExpr;
        String baseFilter = getDirectory().getBaseFilter();
        if (baseFilter.startsWith("(")) {
            filterExpr = String.format("(&(%s={0})%s)", idAttribute, baseFilter);
        } else {
            filterExpr = String.format("(&(%s={0})(%s))", idAttribute, baseFilter);
        }
        String[] filterArgs = { id };
        SearchControls scts = getDirectory().getSearchControls(fetchAllAttributes);

        if (log.isDebugEnabled()) {
            log.debug(String.format("LDAPSession.getLdapEntry(%s, %s): LDAP search base='%s' filter='%s' "
                    + " args='%s' scope='%s' [%s]", id, fetchAllAttributes, searchBaseDn, filterExpr, id,
                    scts.getSearchScope(), this));
        }
        NamingEnumeration<SearchResult> results;
        try {
            results = getContext().search(searchBaseDn, filterExpr, filterArgs, scts);
        } catch (NameNotFoundException nnfe) {
            // sometimes ActiveDirectory have some query fail with: LDAP:
            // error code 32 - 0000208D: NameErr: DSID-031522C9, problem
            // 2001 (NO_OBJECT).
            // To keep the application usable return no results instead of
            // crashing but log the error so that the AD admin
            // can fix the issue.
            log.error("Unexpected response from server while performing query: " + nnfe.getMessage(), nnfe);
            return null;
        }

        if (!results.hasMore()) {
            log.debug("Entry not found: " + id);
            return null;
        }
        SearchResult result = results.next();
        try {
            String dn = result.getNameInNamespace();
            if (results.hasMore()) {
                result = results.next();
                String dn2 = result.getNameInNamespace();
                String msg = String.format("Unable to fetch entry for '%s': found more than one match,"
                        + " for instance: '%s' and '%s'", id, dn, dn2);
                log.error(msg);
                // ignore entries that are ambiguous while giving enough info
                // in the logs to let the LDAP admin be able to fix the issue
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("LDAPSession.getLdapEntry(%s, %s): LDAP search base='%s' filter='%s' "
                        + " args='%s' scope='%s' => found: %s [%s]", id, fetchAllAttributes, searchBaseDn, filterExpr,
                        id, scts.getSearchScope(), dn, this));
            }
        } catch (UnsupportedOperationException e) {
            // ignore unsupported operation thrown by the Apache DS server in
            // the tests in embedded mode
        }
        return result;
    }

    @Override
    public DocumentModelList getEntries() throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }
        try {
            SearchControls scts = getDirectory().getSearchControls(true);
            if (log.isDebugEnabled()) {
                log.debug(String.format("LDAPSession.getEntries(): LDAP search base='%s' filter='%s' "
                        + " args=* scope=%s [%s]", searchBaseDn, getDirectory().getBaseFilter(), scts.getSearchScope(),
                        this));
            }
            NamingEnumeration<SearchResult> results = getContext().search(searchBaseDn, getDirectory().getBaseFilter(),
                    scts);
            // skip reference fetching
            return ldapResultsToDocumentModels(results, false);
        } catch (SizeLimitExceededException e) {
            throw new org.nuxeo.ecm.directory.SizeLimitExceededException(e);
        } catch (NamingException e) {
            throw new DirectoryException("getEntries failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateEntry(DocumentModel docModel) {
        checkPermission(SecurityConstants.WRITE);
        List<String> updateList = new ArrayList<>();
        List<String> referenceFieldList = new LinkedList<>();

        try {
            for (String fieldName : schemaFieldMap.keySet()) {
                if (!docModel.getPropertyObject(schemaName, fieldName).isDirty()) {
                    continue;
                }
                if (getDirectory().isReference(fieldName)) {
                    referenceFieldList.add(fieldName);
                } else {
                    updateList.add(fieldName);
                }
            }

            if (!isReadOnlyEntry(docModel) && !updateList.isEmpty()) {
                Attributes attrs = new BasicAttributes();
                SearchResult ldapEntry = getLdapEntry(docModel.getId());
                if (ldapEntry == null) {
                    throw new DirectoryException(docModel.getId() + " not found");
                }
                Attributes oldattrs = ldapEntry.getAttributes();
                String dn = ldapEntry.getNameInNamespace();
                Attributes attrsToDel = new BasicAttributes();
                for (String f : updateList) {
                    Object value = docModel.getProperty(schemaName, f);
                    String backendField = getDirectory().getFieldMapper().getBackendField(f);
                    if (LDAPDirectory.DN_SPECIAL_ATTRIBUTE_KEY.equals(backendField)) {
                        // skip special LDAP DN field that is readonly
                        log.warn(String.format("field %s is mapped to read only DN field: ignored", f));
                        continue;
                    }
                    if (value == null || value.equals("")) {
                        Attribute objectClasses = oldattrs.get("objectClass");
                        Attribute attr;
                        if (getMandatoryAttributes(objectClasses).contains(backendField)) {
                            attr = new BasicAttribute(backendField);
                            // XXX: this might fail if the mandatory attribute
                            // is typed integer for instance
                            attr.add(" ");
                            attrs.put(attr);
                        } else if (oldattrs.get(backendField) != null) {
                            attr = new BasicAttribute(backendField);
                            attr.add(oldattrs.get(backendField).get());
                            attrsToDel.put(attr);
                        }
                    } else if (f.equals(getPasswordField())) {
                        // The password has been updated, it has to be encrypted
                        Attribute attr = new BasicAttribute(backendField);
                        attr.add(PasswordHelper.hashPassword((String) value, passwordHashAlgorithm));
                        attrs.put(attr);
                    } else {
                        attrs.put(getAttributeValue(f, value));
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("LDAPSession.updateEntry(%s): LDAP modifyAttributes dn='%s' "
                            + "mod_op='REMOVE_ATTRIBUTE' attr='%s' [%s]", docModel, dn, attrsToDel, this));
                }
                getContext().modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE, attrsToDel);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("LDAPSession.updateEntry(%s): LDAP modifyAttributes dn='%s' "
                            + "mod_op='REPLACE_ATTRIBUTE' attr='%s' [%s]", docModel, dn, attrs, this));
                }
                getContext().modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attrs);
            }

            // update reference fields
            for (String referenceFieldName : referenceFieldList) {
                List<Reference> references = directory.getReferences(referenceFieldName);
                if (references.size() > 1) {
                    // not supported
                } else {
                    Reference reference = references.get(0);
                    List<String> targetIds = (List<String>) docModel.getProperty(schemaName, referenceFieldName);
                    reference.setTargetIdsForSource(docModel.getId(), targetIds);
                }
            }
        } catch (NamingException e) {
            handleException(e, "updateEntry failed:");
        }
        getDirectory().invalidateCaches();
    }

    protected void handleException(Exception e, String message) {
        LdapExceptionProcessor processor = getDirectory().getDescriptor().getExceptionProcessor();

        RecoverableClientException userException = processor.extractRecoverableException(e);
        if (userException != null) {
            throw userException;
        }
        throw new DirectoryException(message + " " + e.getMessage(), e);

    }

    @Override
    public void deleteEntry(DocumentModel dm) {
        deleteEntry(dm.getId());
    }

    @Override
    public void deleteEntry(String id) {
        checkPermission(SecurityConstants.WRITE);
        checkDeleteConstraints(id);
        try {
            for (String fieldName : schemaFieldMap.keySet()) {
                if (getDirectory().isReference(fieldName)) {
                    List<Reference> references = directory.getReferences(fieldName);
                    if (references.size() > 1) {
                        // not supported
                    } else {
                        Reference reference = references.get(0);
                        reference.removeLinksForSource(id);
                    }
                }
            }
            SearchResult result = getLdapEntry(id);

            if (log.isDebugEnabled()) {
                log.debug(String.format("LDAPSession.deleteEntry(%s): LDAP destroySubcontext dn='%s' [%s]", id,
                        result.getNameInNamespace(), this));
            }
            getContext().destroySubcontext(result.getNameInNamespace());
        } catch (NamingException e) {
            handleException(e, "deleteEntry failed for: " + id);
        }
        getDirectory().invalidateCaches();
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map) {
        log.warn("Calling deleteEntry extended on LDAP directory");
        deleteEntry(id);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }
        try {
            // building the query using filterExpr / filterArgs to
            // escape special characters and to fulltext search only on
            // the explicitly specified fields
            String[] filters = new String[filter.size()];
            String[] filterArgs = new String[filter.size()];

            if (fulltext == null) {
                fulltext = Collections.emptySet();
            }

            int index = 0;
            for (String fieldName : filter.keySet()) {
                if (getDirectory().isReference(fieldName)) {
                    log.warn(fieldName + " is a reference and will be ignored as a query criterion");
                    continue;
                }

                String backendFieldName = getDirectory().getFieldMapper().getBackendField(fieldName);
                Object fieldValue = filter.get(fieldName);

                StringBuilder currentFilter = new StringBuilder();
                currentFilter.append("(");
                if (fieldValue == null) {
                    currentFilter.append("!(" + backendFieldName + "=*)");
                } else if ("".equals(fieldValue)) {
                    if (fulltext.contains(fieldName)) {
                        currentFilter.append(backendFieldName + "=*");
                    } else {
                        currentFilter.append("!(" + backendFieldName + "=*)");
                    }
                } else {
                    currentFilter.append(backendFieldName + "=");
                    if (fulltext.contains(fieldName)) {
                        switch (substringMatchType) {
                        case subinitial:
                            currentFilter.append("{" + index + "}*");
                            break;
                        case subfinal:
                            currentFilter.append("*{" + index + "}");
                            break;
                        case subany:
                            currentFilter.append("*{" + index + "}*");
                            break;
                        }
                    } else {
                        currentFilter.append("{" + index + "}");
                    }
                }
                currentFilter.append(")");
                filters[index] = currentFilter.toString();
                if (fieldValue != null && !"".equals(fieldValue)) {
                    if (fieldValue instanceof Blob) {
                        // filter arg could be a sequence of \xx where xx is the
                        // hexadecimal value of the byte
                        log.warn("Binary search is not supported");
                    } else {
                        // XXX: what kind of Objects can we get here? Is
                        // toString() enough?
                        filterArgs[index] = fieldValue.toString();
                    }
                }
                index++;
            }
            String filterExpr = "(&" + getDirectory().getBaseFilter() + StringUtils.join(filters) + ')';
            SearchControls scts = getDirectory().getSearchControls(true);

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "LDAPSession.query(...): LDAP search base='%s' filter='%s' args='%s' scope='%s' [%s]",
                        searchBaseDn, filterExpr, StringUtils.join(filterArgs, ","), scts.getSearchScope(), this));
            }
            try {
                NamingEnumeration<SearchResult> results = getContext().search(searchBaseDn, filterExpr, filterArgs,
                        scts);
                DocumentModelList entries = ldapResultsToDocumentModels(results, fetchReferences);

                if (orderBy != null && !orderBy.isEmpty()) {
                    getDirectory().orderEntries(entries, orderBy);
                }
                return applyQueryLimits(entries, limit, offset);
            } catch (NameNotFoundException nnfe) {
                // sometimes ActiveDirectory have some query fail with: LDAP:
                // error code 32 - 0000208D: NameErr: DSID-031522C9, problem
                // 2001 (NO_OBJECT).
                // To keep the application usable return no results instead of
                // crashing but log the error so that the AD admin
                // can fix the issue.
                log.error("Unexpected response from server while performing query: " + nnfe.getMessage(), nnfe);
                return new DocumentModelListImpl();
            }
        } catch (LimitExceededException e) {
            throw new org.nuxeo.ecm.directory.SizeLimitExceededException(e);
        } catch (NamingException e) {
            throw new DirectoryException("executeQuery failed", e);
        }
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter) throws DirectoryException {
        // by default, do not fetch references of result entries
        return query(filter, emptySet, new HashMap<String, String>());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException {
        return query(filter, fulltext, orderBy, false);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext) throws DirectoryException {
        // by default, do not fetch references of result entries
        return query(filter, fulltext, new HashMap<String, String>());
    }

    @Override
    public void close() throws DirectoryException {
        try {
            getContext().close();
        } catch (NamingException e) {
            throw new DirectoryException("close failed", e);
        } finally {
            getDirectory().removeSession(this);
        }
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, String columnName) throws DirectoryException {
        return getProjection(filter, emptySet, columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter, Set<String> fulltext, String columnName)
            throws DirectoryException {
        // XXX: this suboptimal code should be either optimized for LDAP or
        // moved to an abstract class
        List<String> result = new ArrayList<>();
        DocumentModelList docList = query(filter, fulltext);
        String columnNameinDocModel = getDirectory().getFieldMapper().getDirectoryField(columnName);
        for (DocumentModel docModel : docList) {
            Object obj;
            try {
                obj = docModel.getProperty(schemaName, columnNameinDocModel);
            } catch (PropertyException e) {
                throw new DirectoryException(e);
            }
            String propValue;
            if (obj instanceof String) {
                propValue = (String) obj;
            } else {
                propValue = String.valueOf(obj);
            }
            result.add(propValue);
        }
        return result;
    }

    protected DocumentModel fieldMapToDocumentModel(Map<String, Object> fieldMap) throws DirectoryException {
        String id = String.valueOf(fieldMap.get(getIdField()));
        try {
            DocumentModel docModel = BaseSession.createEntryModel(sid, schemaName, id, fieldMap, isReadOnly());
            EntryAdaptor adaptor = getDirectory().getDescriptor().getEntryAdaptor();
            if (adaptor != null) {
                docModel = adaptor.adapt(directory, docModel);
            }
            return docModel;
        } catch (PropertyException e) {
            log.error(e, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected Object getFieldValue(Attribute attribute, String fieldName, String entryId, boolean fetchReferences)
            throws DirectoryException {

        Field field = schemaFieldMap.get(fieldName);
        Type type = field.getType();
        if (type instanceof SimpleTypeImpl) {
            // type with constraint
            type = type.getSuperType();
        }
        Object defaultValue = field.getDefaultValue();
        String typeName = type.getName();
        if (attribute == null) {
            return defaultValue;
        }
        Object value;
        try {
            value = attribute.get();
        } catch (NamingException e) {
            throw new DirectoryException("Could not fetch value for " + attribute, e);
        }
        if (value == null) {
            return defaultValue;
        }
        String trimmedValue = value.toString().trim();
        if ("string".equals(typeName)) {
            return trimmedValue;
        } else if ("integer".equals(typeName) || "long".equals(typeName)) {
            if ("".equals(trimmedValue)) {
                return defaultValue;
            }
            try {
                return Long.valueOf(trimmedValue);
            } catch (NumberFormatException e) {
                log.error(String.format(
                        "field %s of type %s has non-numeric value found on server: '%s' (ignoring and using default value instead)",
                        fieldName, typeName, trimmedValue));
                return defaultValue;
            }
        } else if (type.isListType()) {
            List<String> parsedItems = new LinkedList<>();
            NamingEnumeration<Object> values = null;
            try {
                values = (NamingEnumeration<Object>) attribute.getAll();
                while (values.hasMore()) {
                    parsedItems.add(values.next().toString().trim());
                }
                return parsedItems;
            } catch (NamingException e) {
                log.error(String.format(
                        "field %s of type %s has non list value found on server: '%s' (ignoring and using default value instead)",
                        fieldName, typeName, values != null ? values.toString() : trimmedValue));
                return defaultValue;
            } finally {
                if (values != null) {
                    try {
                        values.close();
                    } catch (NamingException e) {
                        log.error(e, e);
                    }
                }
            }
        } else if ("date".equals(typeName)) {
            if ("".equals(trimmedValue)) {
                return defaultValue;
            }
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
                dateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
                Date date = dateFormat.parse(trimmedValue);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return cal;
            } catch (ParseException e) {
                log.error(String.format(
                        "field %s of type %s has invalid value found on server: '%s' (ignoring and using default value instead)",
                        fieldName, typeName, trimmedValue));
                return defaultValue;
            }
        } else if ("content".equals(typeName)) {
            return Blobs.createBlob((byte[]) value);
        } else {
            throw new DirectoryException("Field type not supported in directories: " + typeName);
        }
    }

    @SuppressWarnings("unchecked")
    protected Attribute getAttributeValue(String fieldName, Object value) throws DirectoryException {
        Attribute attribute = new BasicAttribute(getDirectory().getFieldMapper().getBackendField(fieldName));
        Field field = schemaFieldMap.get(fieldName);
        if (field == null) {
            String message = String.format("Invalid field name '%s' for directory '%s' with schema '%s'", fieldName,
                    directory.getName(), directory.getSchema());
            throw new DirectoryException(message);
        }
        Type type = field.getType();
        if (type instanceof SimpleTypeImpl) {
            // type with constraint
            type = type.getSuperType();
        }
        String typeName = type.getName();

        if ("string".equals(typeName)) {
            attribute.add(value);
        } else if ("integer".equals(typeName) || "long".equals(typeName)) {
            attribute.add(value.toString());
        } else if (type.isListType()) {
            Collection<String> valueItems;
            if (value instanceof String[]) {
                valueItems = Arrays.asList((String[]) value);
            } else if (value instanceof Collection) {
                valueItems = (Collection<String>) value;
            } else {
                throw new DirectoryException(String.format("field %s with value %s does not match type %s", fieldName,
                        value.toString(), type.getName()));
            }
            for (String item : valueItems) {
                attribute.add(item);
            }
        } else if ("date".equals(typeName)) {
            Calendar cal = (Calendar) value;
            Date date = cal.getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
            dateFormat.setTimeZone(new SimpleTimeZone(0, "Z"));
            attribute.add(dateFormat.format(date));
        } else if ("content".equals(typeName)) {
            try {
                attribute.add(((Blob) value).getByteArray());
            } catch (IOException e) {
                throw new DirectoryException("Failed to get ByteArray value", e);
            }
        } else {
            throw new DirectoryException("Field type not supported in directories: " + typeName);
        }

        return attribute;
    }

    protected DocumentModelList ldapResultsToDocumentModels(NamingEnumeration<SearchResult> results,
            boolean fetchReferences) throws DirectoryException, NamingException {
        DocumentModelListImpl list = new DocumentModelListImpl();
        try {
            while (results.hasMore()) {
                SearchResult result = results.next();
                DocumentModel entry = ldapResultToDocumentModel(result, null, fetchReferences);
                if (entry != null) {
                    list.add(entry);
                }
            }
        } catch (SizeLimitExceededException e) {
            if (list.isEmpty()) {
                // the server did no send back the truncated results set,
                // re-throw the exception to that the user interface can display
                // the error message
                throw e;
            }
            // mark the collect results as a truncated result list
            log.debug("SizeLimitExceededException caught," + " return truncated results. Original message: "
                    + e.getMessage() + " explanation: " + e.getExplanation());
            list.setTotalSize(-2);
        } finally {
            results.close();
        }
        log.debug("LDAP search returned " + list.size() + " results");
        return list;
    }

    protected DocumentModel ldapResultToDocumentModel(SearchResult result, String entryId, boolean fetchReferences)
            throws DirectoryException, NamingException {
        Attributes attributes = result.getAttributes();
        String passwordFieldId = getPasswordField();
        Map<String, Object> fieldMap = new HashMap<>();

        Attribute attribute = attributes.get(idAttribute);
        // NXP-2461: check that id field is filled + NXP-2730: make sure that
        // entry id is the one returned from LDAP
        if (attribute != null) {
            Object entry = attribute.get();
            if (entry != null) {
                entryId = entry.toString();
            }
        }
        // NXP-7136 handle id case
        entryId = changeEntryIdCase(entryId, idCase);

        if (entryId == null) {
            // don't bother
            return null;
        }
        for (String fieldName : schemaFieldMap.keySet()) {
            List<Reference> references = directory.getReferences(fieldName);
            if (references != null && references.size() > 0) {
                if (fetchReferences) {
                    Map<String, List<String>> referencedIdsMap = new HashMap<>();
                    for (Reference reference : references) {
                        // reference resolution
                        List<String> referencedIds;
                        if (reference instanceof LDAPReference) {
                            // optim: use the current LDAPSession directly to
                            // provide the LDAP reference with the needed backend entries
                            LDAPReference ldapReference = (LDAPReference) reference;
                            referencedIds = ldapReference.getLdapTargetIds(attributes);
                        } else if (reference instanceof LDAPTreeReference) {
                            // TODO: optimize using the current LDAPSession
                            // directly to provide the LDAP reference with the
                            // needed backend entries (needs to implement getLdapTargetIds)
                            LDAPTreeReference ldapReference = (LDAPTreeReference) reference;
                            referencedIds = ldapReference.getTargetIdsForSource(entryId);
                        } else {
                            referencedIds = reference.getTargetIdsForSource(entryId);
                        }
                        referencedIds = new ArrayList<>(referencedIds);
                        Collections.sort(referencedIds);
                        if (referencedIdsMap.containsKey(fieldName)) {
                            referencedIdsMap.get(fieldName).addAll(referencedIds);
                        } else {
                            referencedIdsMap.put(fieldName, referencedIds);
                        }
                    }
                    fieldMap.put(fieldName, referencedIdsMap.get(fieldName));
                }
            } else {
                // manage directly stored fields
                String attributeId = getDirectory().getFieldMapper().getBackendField(fieldName);
                if (attributeId.equals(LDAPDirectory.DN_SPECIAL_ATTRIBUTE_KEY)) {
                    // this is the special DN readonly attribute
                    try {
                        fieldMap.put(fieldName, result.getNameInNamespace());
                    } catch (UnsupportedOperationException e) {
                        // ignore ApacheDS partial implementation when running
                        // in embedded mode
                    }
                } else {
                    // this is a regular attribute
                    attribute = attributes.get(attributeId);
                    if (fieldName.equals(passwordFieldId)) {
                        // do not try to fetch the password attribute
                        continue;
                    } else {
                        fieldMap.put(fieldName, getFieldValue(attribute, fieldName, entryId, fetchReferences));
                    }
                }
            }
        }
        // check if the idAttribute was returned from the search. If not
        // set it anyway, maybe changing its case if it's a String instance
        String fieldId = getDirectory().getFieldMapper().getDirectoryField(idAttribute);
        Object obj = fieldMap.get(fieldId);
        if (obj == null) {
            fieldMap.put(fieldId, changeEntryIdCase(entryId, getDirectory().getDescriptor().getMissingIdFieldCase()));
        } else if (obj instanceof String) {
            fieldMap.put(fieldId, changeEntryIdCase((String) obj, idCase));
        }
        return fieldMapToDocumentModel(fieldMap);
    }

    protected String changeEntryIdCase(String id, String idFieldCase) {
        if (MISSING_ID_LOWER_CASE.equals(idFieldCase)) {
            return id.toLowerCase();
        } else if (MISSING_ID_UPPER_CASE.equals(idFieldCase)) {
            return id.toUpperCase();
        }
        // returns the unchanged id
        return id;
    }

    @Override
    public boolean authenticate(String username, String password) throws DirectoryException {

        if (password == null || "".equals(password.trim())) {
            // never use anonymous bind as a way to authenticate a user in
            // Nuxeo EP
            return false;
        }

        // lookup the user: fetch its dn
        SearchResult entry;
        try {
            entry = getLdapEntry(username);
        } catch (NamingException e) {
            throw new DirectoryException("failed to fetch the ldap entry for " + username, e);
        }
        if (entry == null) {
            // no such user => authentication failed
            return false;
        }
        String dn = entry.getNameInNamespace();
        Properties env = (Properties) getDirectory().getContextProperties().clone();
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        InitialLdapContext authenticationDirContext = null;
        try {
            // creating a context does a bind
            log.debug(String.format("LDAP bind dn='%s'", dn));
            // noinspection ResultOfObjectAllocationIgnored
            authenticationDirContext = new InitialLdapContext(env, null);
            // force reconnection to prevent from using a previous connection
            // with an obsolete password (after an user has changed his
            // password)
            authenticationDirContext.reconnect(null);
            log.debug("Bind succeeded, authentication ok");
            return true;
        } catch (NamingException e) {
            log.debug("Bind failed: " + e.getMessage());
            // authentication failed
            return false;
        } finally {
            try {
                if (authenticationDirContext != null) {
                    authenticationDirContext.close();
                }
            } catch (NamingException e) {
                log.error("Error closing authentication context when biding dn " + dn, e);
                return false;
            }
        }
    }

    @Override
    public boolean isAuthenticating() throws DirectoryException {
        String password = getPasswordField();
        return schemaFieldMap.containsKey(password);
    }

    public boolean rdnMatchesIdField() {
        return getDirectory().getDescriptor().rdnAttribute.equals(idAttribute);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getMandatoryAttributes(Attribute objectClassesAttribute) throws DirectoryException {
        try {
            List<String> mandatoryAttributes = new ArrayList<>();

            DirContext schema = getContext().getSchema("");
            List<String> objectClasses = new ArrayList<>();
            if (objectClassesAttribute == null) {
                // use the creation classes as reference schema for this entry
                objectClasses.addAll(Arrays.asList(getDirectory().getDescriptor().getCreationClasses()));
            } else {
                // introspec the objectClass definitions to find the mandatory
                // attributes for this entry
                NamingEnumeration<Object> values = null;
                try {
                    values = (NamingEnumeration<Object>) objectClassesAttribute.getAll();
                    while (values.hasMore()) {
                        objectClasses.add(values.next().toString().trim());
                    }
                } catch (NamingException e) {
                    throw new DirectoryException(e);
                } finally {
                    if (values != null) {
                        values.close();
                    }
                }
            }
            objectClasses.remove("top");
            for (String creationClass : objectClasses) {
                Attributes attributes = schema.getAttributes("ClassDefinition/" + creationClass);
                Attribute attribute = attributes.get("MUST");
                if (attribute != null) {
                    NamingEnumeration<String> values = (NamingEnumeration<String>) attribute.getAll();
                    try {
                        while (values.hasMore()) {
                            String value = values.next();
                            mandatoryAttributes.add(value);
                        }
                    } finally {
                        values.close();
                    }
                }
            }
            return mandatoryAttributes;
        } catch (NamingException e) {
            throw new DirectoryException("getMandatoryAttributes failed", e);
        }
    }

    protected List<String> getMandatoryAttributes() throws DirectoryException {
        return getMandatoryAttributes(null);
    }

    @Override
    // useful for the log function
    public String toString() {
        return String.format("LDAPSession '%s' for directory %s", sid, directory.getName());
    }

    @Override
    public DocumentModel createEntry(DocumentModel entry) {
        Map<String, Object> fieldMap = entry.getProperties(directory.getSchema());
        Map<String, Object> simpleNameFieldMap = new HashMap<>();
        for (Map.Entry<String, Object> fieldEntry : fieldMap.entrySet()) {
            String fieldKey = fieldEntry.getKey();
            if (fieldKey.contains(":")) {
                fieldKey = fieldKey.split(":")[1];
            }
            simpleNameFieldMap.put(fieldKey, fieldEntry.getValue());
        }
        return createEntry(simpleNameFieldMap);
    }

}
