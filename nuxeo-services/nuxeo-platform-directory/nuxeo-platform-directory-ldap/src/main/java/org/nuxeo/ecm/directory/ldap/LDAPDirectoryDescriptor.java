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
 *     Nuxeo - initial API and implementation
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.SearchControls;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.EntryAdaptor;
import org.nuxeo.ecm.directory.Reference;

@XObject(value = "directory")
public class LDAPDirectoryDescriptor extends BaseDirectoryDescriptor {

    public static final Log log = LogFactory.getLog(LDAPDirectoryDescriptor.class);

    public static final int DEFAULT_SEARCH_SCOPE = SearchControls.ONELEVEL_SCOPE;

    public static final String DEFAULT_SEARCH_CLASSES_FILTER = "(objectClass=*)";

    public static final String DEFAULT_EMPTY_REF_MARKER = "cn=emptyRef";

    public static final String DEFAULT_MISSING_ID_FIELD_CASE = "unchanged";

    public static final String DEFAULT_ID_CASE = "unchanged";

    public static final int DEFAULT_QUERY_SIZE_LIMIT = 200;

    public static final int DEFAULT_QUERY_TIME_LIMIT = 0; // default to wait indefinitely

    public static final boolean DEFAULT_FOLLOW_REFERRALS = true;

    @XNode("server")
    public String serverName;

    @XNode("searchBaseDn")
    public String searchBaseDn;

    @XNodeMap(value = "fieldMapping", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> fieldMapping = new HashMap<>();

    public String[] searchClasses;

    public String searchClassesFilter;

    public String searchFilter;

    public Integer searchScope;

    @XNode("creationBaseDn")
    public String creationBaseDn;

    @XNodeList(value = "creationClass", componentType = String.class, type = String[].class)
    public String[] creationClasses;

    @XNode("rdnAttribute")
    public String rdnAttribute;

    @XNodeList(value = "references/ldapReference", type = LDAPReference[].class, componentType = LDAPReference.class)
    private LDAPReference[] ldapReferences;

    @XNodeList(value = "references/ldapTreeReference", type = LDAPTreeReference[].class, componentType = LDAPTreeReference.class)
    private LDAPTreeReference[] ldapTreeReferences;

    @XNode("emptyRefMarker")
    public String emptyRefMarker;

    @XNode("missingIdFieldCase")
    public String missingIdFieldCase;

    public String getMissingIdFieldCase() {
        return missingIdFieldCase == null ? DEFAULT_MISSING_ID_FIELD_CASE : missingIdFieldCase;
    }

    /**
     * Since 5.4.2: force id case to upper or lower, or leaver it unchanged.
     */
    @XNode("idCase")
    public String idCase = DEFAULT_ID_CASE;

    @XNode("querySizeLimit")
    private Integer querySizeLimit;

    @XNode("queryTimeLimit")
    private Integer queryTimeLimit;

    // Add attribute to allow to ignore referrals resolution
    /**
     * Since 5.9.4
     */
    @XNode("followReferrals")
    protected Boolean followReferrals;

    public boolean getFollowReferrals() {
        return followReferrals == null ? DEFAULT_FOLLOW_REFERRALS : followReferrals.booleanValue();
    }

    protected EntryAdaptor entryAdaptor;

    @XObject(value = "entryAdaptor")
    public static class EntryAdaptorDescriptor {

        @XNode("@class")
        public Class<? extends EntryAdaptor> adaptorClass;

        @XNodeMap(value = "parameter", key = "@name", type = HashMap.class, componentType = String.class)
        public Map<String, String> parameters;

    }

    @XNode("entryAdaptor")
    public void setEntryAdaptor(EntryAdaptorDescriptor adaptorDescriptor)
            throws InstantiationException, IllegalAccessException {
        entryAdaptor = adaptorDescriptor.adaptorClass.newInstance();
        for (Map.Entry<String, String> paramEntry : adaptorDescriptor.parameters.entrySet()) {
            entryAdaptor.setParameter(paramEntry.getKey(), paramEntry.getValue());
        }
    }

    /**
     * @since 5.7 : allow to contribute custom Exception Handler to extract LDAP validation error messages
     */
    @XNode("ldapExceptionHandler")
    protected Class<? extends LdapExceptionProcessor> exceptionProcessorClass;

    protected LdapExceptionProcessor exceptionProcessor;

    // XXX: passwordEncryption?
    // XXX: ignoredFields?
    // XXX: referenceFields?
    public LDAPDirectoryDescriptor() {
    }

    public String getRdnAttribute() {
        return rdnAttribute;
    }

    public String getCreationBaseDn() {
        return creationBaseDn;
    }

    public String[] getCreationClasses() {
        return creationClasses;
    }

    public String getIdCase() {
        return idCase;
    }

    public String getSearchBaseDn() {
        return searchBaseDn;
    }

    @XNodeList(value = "searchClass", componentType = String.class, type = String[].class)
    public void setSearchClasses(String[] searchClasses) {
        this.searchClasses = searchClasses;
        if (searchClasses == null) {
            // default searchClassesFilter
            searchClassesFilter = DEFAULT_SEARCH_CLASSES_FILTER;
            return;
        }
        List<String> searchClassFilters = new ArrayList<>();
        for (String searchClass : searchClasses) {
            searchClassFilters.add("(objectClass=" + searchClass + ')');
        }
        searchClassesFilter = StringUtils.join(searchClassFilters.toArray());

        // logical OR if several classes are provided
        if (searchClasses.length > 1) {
            searchClassesFilter = "(|" + searchClassesFilter + ')';
        }
    }

    public String[] getSearchClasses() {
        return searchClasses;
    }

    @XNode("searchFilter")
    public void setSearchFilter(String searchFilter) {
        if ((searchFilter == null) || searchFilter.equals("(objectClass=*)")) {
            this.searchFilter = null;
            return;
        }
        if (!searchFilter.startsWith("(") && !searchFilter.endsWith(")")) {
            searchFilter = '(' + searchFilter + ')';
        }
        this.searchFilter = searchFilter;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    @XNode("searchScope")
    public void setSearchScope(String searchScope) {
        if (searchScope == null) {
            // restore default search scope
            this.searchScope = null;
            return;
        }
        Integer scope = LdapScope.getIntegerScope(searchScope);
        if (scope == null) {
            // invalid scope
            throw new DirectoryException(
                    "Invalid search scope: " + searchScope + ". Valid options: object, onelevel, subtree");
        }
        this.searchScope = scope;
    }

    public int getSearchScope() {
        return searchScope == null ? DEFAULT_SEARCH_SCOPE : searchScope.intValue();
    }

    public String getServerName() {
        return serverName;
    }

    public String getAggregatedSearchFilter() {
        if (searchFilter == null) {
            return searchClassesFilter;
        }
        return "(&" + searchClassesFilter + searchFilter + ')';
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public Reference[] getLdapReferences() {
        List<Reference> refs = new ArrayList<>();
        if (ldapReferences != null) {
            refs.addAll(Arrays.asList(ldapReferences));
        }
        if (ldapTreeReferences != null) {
            refs.addAll(Arrays.asList(ldapTreeReferences));
        }
        return refs.toArray(new Reference[] {});
    }

    public String getEmptyRefMarker() {
        return emptyRefMarker == null ? DEFAULT_EMPTY_REF_MARKER : emptyRefMarker;
    }

    public void setEmptyRefMarker(String emptyRefMarker) {
        this.emptyRefMarker = emptyRefMarker;
    }

    public int getQuerySizeLimit() {
        return querySizeLimit == null ? DEFAULT_QUERY_SIZE_LIMIT : querySizeLimit.intValue();
    }

    public void setQuerySizeLimit(int querySizeLimit) {
        this.querySizeLimit = Integer.valueOf(querySizeLimit);
    }

    public void setQueryTimeLimit(int queryTimeLimit) {
        this.queryTimeLimit = Integer.valueOf(queryTimeLimit);
    }

    public int getQueryTimeLimit() {
        return queryTimeLimit == null ? DEFAULT_QUERY_TIME_LIMIT : queryTimeLimit.intValue();
    }

    public EntryAdaptor getEntryAdaptor() {
        return entryAdaptor;
    }

    public LdapExceptionProcessor getExceptionProcessor() {
        if (exceptionProcessor == null) {
            if (exceptionProcessorClass == null) {
                exceptionProcessor = new DefaultLdapExceptionProcessor();
            } else {
                try {
                    exceptionProcessor = exceptionProcessorClass.newInstance();
                } catch (ReflectiveOperationException e) {
                    log.error("Unable to instanciate custom Exception handler", e);
                    exceptionProcessor = new DefaultLdapExceptionProcessor();
                }
            }
        }
        return exceptionProcessor;
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof LDAPDirectoryDescriptor) {
            merge((LDAPDirectoryDescriptor) other);
        }
    }

    protected void merge(LDAPDirectoryDescriptor other) {
        if (other.serverName != null) {
            serverName = other.serverName;
        }
        if (other.searchBaseDn != null) {
            searchBaseDn = other.searchBaseDn;
        }
        if (other.fieldMapping != null) {
            fieldMapping.putAll(other.fieldMapping);
        }
        if (other.searchClasses != null && other.searchClasses.length > 0) {
            searchClasses = other.searchClasses.clone();
        }
        if (other.searchClassesFilter != null) {
            searchClassesFilter = other.searchClassesFilter;
        }
        if (other.searchFilter != null) {
            searchFilter = other.searchFilter;
        }
        if (other.searchScope != null) {
            searchScope = other.searchScope;
        }
        if (other.creationBaseDn != null) {
            creationBaseDn = other.creationBaseDn;
        }
        if (other.creationClasses != null && other.creationClasses.length > 0) {
            creationClasses = other.creationClasses.clone();
        }
        if (other.rdnAttribute != null) {
            rdnAttribute = other.rdnAttribute;
        }
        if (other.ldapReferences != null && other.ldapReferences.length > 0) {
            ldapReferences = other.ldapReferences;
        }
        if (other.ldapTreeReferences != null && other.ldapTreeReferences.length > 0) {
            ldapTreeReferences = other.ldapTreeReferences;
        }
        if (other.emptyRefMarker != null) {
            emptyRefMarker = other.emptyRefMarker;
        }
        if (other.missingIdFieldCase != null) {
            missingIdFieldCase = other.missingIdFieldCase;
        }
        if (other.idCase != null) {
            idCase = other.idCase;
        }
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.queryTimeLimit != null) {
            queryTimeLimit = other.queryTimeLimit;
        }
        if (other.followReferrals != null) {
            followReferrals = other.followReferrals;
        }
        if (other.entryAdaptor != null) {
            entryAdaptor = other.entryAdaptor;
        }
        if (other.exceptionProcessorClass != null) {
            exceptionProcessorClass = other.exceptionProcessorClass;
            exceptionProcessor = other.exceptionProcessor;
        }
    }

    @Override
    public LDAPDirectoryDescriptor clone() {
        LDAPDirectoryDescriptor clone = (LDAPDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        if (fieldMapping != null) {
            clone.fieldMapping = new HashMap<>(fieldMapping);
        }
        if (searchClasses != null) {
            clone.searchClasses = searchClasses.clone();
        }
        if (creationClasses != null) {
            creationClasses = creationClasses.clone();
        }
        if (ldapReferences != null) {
            clone.ldapReferences = new LDAPReference[ldapReferences.length];
            for (int i = 0; i < ldapReferences.length; i++) {
                clone.ldapReferences[i] = ldapReferences[i].clone();
            }
        }
        if (ldapTreeReferences != null) {
            clone.ldapTreeReferences = new LDAPTreeReference[ldapTreeReferences.length];
            for (int i = 0; i < ldapTreeReferences.length; i++) {
                clone.ldapTreeReferences[i] = ldapTreeReferences[i].clone();
            }
        }
        return clone;
    }

    @Override
    public LDAPDirectory newDirectory() {
        return new LDAPDirectory(this);
    }

}
