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

package org.nuxeo.ecm.directory.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.SearchControls;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.EntryAdaptor;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.Reference;

@XObject(value = "directory")
public class LDAPDirectoryDescriptor {

    public static final Log log = LogFactory.getLog(LDAPDirectoryDescriptor.class);

    public static final int defaultSearchScope = SearchControls.ONELEVEL_SCOPE;

    public static final String defaultSearchClassesFilter = "(objectClass=*)";

    @XNode("@name")
    public String name;

    @XNode("server")
    public String serverName;

    @XNode("schema")
    public String schemaName;

    @XNode("searchBaseDn")
    public String searchBaseDn;

    @XNode("readOnly")
    public boolean readOnly = true;

    @XNode("cacheEntryName")
    public String cacheEntryName = null;

    @XNode("cacheEntryWithoutReferencesName")
    public String cacheEntryWithoutReferencesName = null;

    @XNodeMap(value = "fieldMapping", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> fieldMapping = new HashMap<String, String>();

    public String[] searchClasses;

    public String searchClassesFilter;

    public String searchFilter;

    public int searchScope = defaultSearchScope; // default value: onelevel

    public String substringMatchType;

    @XNode("creationBaseDn")
    public String creationBaseDn;

    @XNodeList(value = "creationClass", componentType = String.class, type = String[].class)
    public String[] creationClasses;

    @XNode("idField")
    public String idField;

    @XNode("rdnAttribute")
    public String rdnAttribute;

    @XNode("passwordField")
    public String passwordField;

    @XNode("passwordHashAlgorithm")
    public String passwordHashAlgorithm;

    @XNodeList(value = "references/ldapReference", type = LDAPReference[].class, componentType = LDAPReference.class)
    private LDAPReference[] ldapReferences;

    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    private InverseReference[] inverseReferences;

    @XNodeList(value = "references/ldapTreeReference", type = LDAPTreeReference[].class, componentType = LDAPTreeReference.class)
    private LDAPTreeReference[] ldapTreeReferences;

    @XNode("emptyRefMarker")
    public String emptyRefMarker = "cn=emptyRef";

    @XNode("missingIdFieldCase")
    public String missingIdFieldCase = "unchanged";

    /**
     * Since 5.4.2: force id case to upper or lower, or leaver it unchanged.
     */
    @XNode("idCase")
    public String idCase = "unchanged";

    @XNode("querySizeLimit")
    private int querySizeLimit = 200;

    @XNode("queryTimeLimit")
    private int queryTimeLimit = 0; // default to wait indefinitely
    
    // Add attribute to allow to ignore referrals resolution
    /**
     * Since 5.9.4
     */
    @XNode("followReferrals")
    protected boolean followReferrals = true; // default to true
    
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
            entryAdaptor.setParameter(paramEntry.getKey(),
                    paramEntry.getValue());
        }
    }

    /**
     * @since 5.7 : allow to contribute custom Exception Handler to extract LDAP
     *        validation error messages
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

    public String getIdField() {
        return idField;
    }

    public String getIdCase() {
        return idCase;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getSearchBaseDn() {
        return searchBaseDn;
    }

    @XNodeList(value = "searchClass", componentType = String.class, type = String[].class)
    public void setSearchClasses(String[] searchClasses) {
        this.searchClasses = searchClasses;
        if (searchClasses == null) {
            // default searchClassesFilter
            searchClassesFilter = defaultSearchClassesFilter;
            return;
        }
        List<String> searchClassFilters = new ArrayList<String>();
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
    public void setSearchScope(String searchScope) throws DirectoryException {
        if (null == searchScope) {
            // restore default search scope
            this.searchScope = defaultSearchScope;
            return;
        }
        Integer scope = LdapScope.getIntegerScope(searchScope);
        if (null == scope) {
            // invalid scope
            throw new DirectoryException("Invalid search scope: " + searchScope
                    + ". Valid options: object, onelevel, subtree");
        }
        this.searchScope = scope.intValue();
    }

    public int getSearchScope() {
        return searchScope;
    }

    public String getSubstringMatchType() {
        return substringMatchType;
    }

    @XNode("substringMatchType")
    public void setSubstringMatchType(String substringMatchType) {
        if (substringMatchType == null) {
            // default behaviour
            this.substringMatchType = LDAPSubstringMatchType.SUBINITIAL;
        } else if (LDAPSubstringMatchType.SUBINITIAL.equals(substringMatchType)
                || LDAPSubstringMatchType.SUBFINAL.equals(substringMatchType)
                || LDAPSubstringMatchType.SUBANY.equals(substringMatchType)) {
            this.substringMatchType = substringMatchType;
        } else {
            log.error("Invalid substring match type: " + substringMatchType
                    + ". Valid options: subinitial, subfinal, subany");
            this.substringMatchType = LDAPSubstringMatchType.SUBINITIAL;
        }
    }

    public String getName() {
        return name;
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

    public String getPasswordField() {
        return passwordField;
    }

    public String getPasswordHashAlgorithmField() {
        return passwordHashAlgorithm;
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public Reference[] getInverseReferences() {
        return inverseReferences;
    }

    public Reference[] getLdapReferences() {
        List<Reference> refs = new ArrayList<Reference>();
        if (ldapReferences != null) {
            refs.addAll(Arrays.asList(ldapReferences));
        }
        if (ldapTreeReferences != null) {
            refs.addAll(Arrays.asList(ldapTreeReferences));
        }
        return refs.toArray(new Reference[] {});
    }

    public boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getEmptyRefMarker() {
        return emptyRefMarker;
    }

    public void setEmptyRefMarker(String emptyRefMarker) {
        this.emptyRefMarker = emptyRefMarker;
    }

    public int getQuerySizeLimit() {
        return querySizeLimit;
    }

    public void setQuerySizeLimit(int querySizeLimit) {
        this.querySizeLimit = querySizeLimit;
    }

    public void setQueryTimeLimit(int queryTimeLimit) {
        this.queryTimeLimit = queryTimeLimit;
    }

    public int getQueryTimeLimit() {
        return queryTimeLimit;
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
                } catch (Exception e) {
                    log.error("Unable to instanciate custom Exception handler",
                            e);
                    exceptionProcessor = new DefaultLdapExceptionProcessor();
                }
            }
        }
        return exceptionProcessor;
    }

}
