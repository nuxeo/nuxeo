/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.directory.AbstractReference;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryEntryNotFoundException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFieldMapper;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.ldap.filter.FilterExpressionCorrector;
import org.nuxeo.ecm.directory.ldap.filter.FilterExpressionCorrector.FilterJobs;

import com.sun.jndi.ldap.LdapURL;

/**
 * Implementation of the directory Reference interface that leverage two common ways of storing relationships in LDAP
 * directories:
 * <ul>
 * <li>the static attribute strategy where a multi-valued attribute store the exhaustive list of distinguished names of
 * the refereed entries (eg. the uniqueMember attribute of the groupOfUniqueNames objectclass)</li>
 * <li>the dynamic attribute strategy where a potentially multi-valued attribute stores a ldap urls intensively
 * describing the refereed LDAP entries (eg. the memberURLs attribute of the groupOfURLs objectclass)</li>
 * </ul>
 * <p>
 * Please note that both static and dynamic references are resolved in read mode whereas only the static attribute
 * strategy is used when creating new references or when deleting existing ones (write / update mode).
 * <p>
 * Some design considerations behind the implementation of such reference can be found at:
 * http://jira.nuxeo.org/browse/NXP-1506
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
@XObject(value = "ldapReference")
public class LDAPReference extends AbstractReference {

    private static final Log log = LogFactory.getLog(LDAPReference.class);

    @XNodeList(value = "dynamicReference", type = LDAPDynamicReferenceDescriptor[].class, componentType = LDAPDynamicReferenceDescriptor.class)
    private LDAPDynamicReferenceDescriptor[] dynamicReferences;

    @XNode("@forceDnConsistencyCheck")
    public boolean forceDnConsistencyCheck;

    protected LDAPDirectoryDescriptor targetDirectoryDescriptor;

    /**
     * Resolve staticAttributeId as distinguished names (true by default) such as in the uniqueMember field of
     * groupOfUniqueNames. Set to false to resolve as simple id (as in memberUID of posixGroup for instance).
     */
    @XNode("@staticAttributeIdIsDn")
    private boolean staticAttributeIdIsDn = true;

    @XNode("@staticAttributeId")
    protected String staticAttributeId;

    @XNode("@dynamicAttributeId")
    protected String dynamicAttributeId;

    @XNode("@field")
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public static final List<String> EMPTY_STRING_LIST = Collections.emptyList();

    private LDAPFilterMatcher getFilterMatcher() {
        return new LDAPFilterMatcher();
    }

    /**
     * @return true if the reference should resolve statically refereed entries (identified by dn-valued attribute)
     * @throws DirectoryException
     */
    public boolean isStatic() throws DirectoryException {
        return getStaticAttributeId() != null;
    }

    public String getStaticAttributeId() throws DirectoryException {
        return getStaticAttributeId(null);
    }

    public String getStaticAttributeId(DirectoryFieldMapper sourceFM) throws DirectoryException {
        if (staticAttributeId != null) {
            // explicitly provided attributeId
            return staticAttributeId;
        }

        // sourceFM can be passed to avoid infinite loop in LDAPDirectory init
        if (sourceFM == null) {
            sourceFM = ((LDAPDirectory) getSourceDirectory()).getFieldMapper();
        }
        String backendFieldId = sourceFM.getBackendField(fieldName);
        if (fieldName.equals(backendFieldId)) {
            // no specific backendField found and no staticAttributeId found
            // either, this reference should not be statically resolved
            return null;
        } else {
            // BBB: the field mapper has been explicitly used to specify the
            // staticAttributeId value as this was the case before the
            // introduction of the staticAttributeId dynamicAttributeId duality
            log.warn(String.format("implicit static attribute definition through fieldMapping is deprecated, "
                    + "please update your setup with "
                    + "<ldapReference field=\"%s\" directory=\"%s\" staticAttributeId=\"%s\">", fieldName,
                    sourceDirectoryName, backendFieldId));
            return backendFieldId;
        }
    }

    public List<LDAPDynamicReferenceDescriptor> getDynamicAttributes() {
        return Arrays.asList(dynamicReferences);
    }

    public String getDynamicAttributeId() {
        return dynamicAttributeId;
    }

    /**
     * @return true if the reference should resolve dynamically refereed entries (identified by a LDAP url-valued
     *         attribute)
     */
    public boolean isDynamic() {
        return dynamicAttributeId != null;
    }

    @Override
    @XNode("@directory")
    public void setTargetDirectoryName(String targetDirectoryName) {
        this.targetDirectoryName = targetDirectoryName;
    }

    @Override
    public Directory getSourceDirectory() throws DirectoryException {

        Directory sourceDir = super.getSourceDirectory();
        if (sourceDir instanceof LDAPDirectory) {
            return sourceDir;
        } else {
            throw new DirectoryException(sourceDirectoryName
                    + " is not a LDAPDirectory and thus cannot be used in a reference for " + fieldName);
        }
    }

    @Override
    public Directory getTargetDirectory() throws DirectoryException {
        Directory targetDir = super.getTargetDirectory();
        if (targetDir instanceof LDAPDirectory) {
            return targetDir;
        } else {
            throw new DirectoryException(targetDirectoryName
                    + " is not a LDAPDirectory and thus cannot be referenced as target by " + fieldName);
        }
    }

    protected LDAPDirectory getTargetLDAPDirectory() throws DirectoryException {
        return (LDAPDirectory) getTargetDirectory();
    }

    protected LDAPDirectory getSourceLDAPDirectory() throws DirectoryException {
        return (LDAPDirectory) getSourceDirectory();
    }

    protected LDAPDirectoryDescriptor getTargetDirectoryDescriptor() throws DirectoryException {
        if (targetDirectoryDescriptor == null) {
            targetDirectoryDescriptor = getTargetLDAPDirectory().getDescriptor();
        }
        return targetDirectoryDescriptor;
    }

    /**
     * Store new links using the LDAP staticAttributeId strategy.
     *
     * @see org.nuxeo.ecm.directory.Reference#addLinks(String, List)
     */
    @Override
    public void addLinks(String sourceId, List<String> targetIds) throws DirectoryException {

        if (targetIds.isEmpty()) {
            // optim: nothing to do, return silently without further creating
            // session instances
            return;
        }

        LDAPDirectory ldapTargetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectory ldapSourceDirectory = (LDAPDirectory) getSourceDirectory();
        String attributeId = getStaticAttributeId();
        if (attributeId == null) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("trying to edit a non-static reference from %s in directory %s: ignoring",
                        sourceId, ldapSourceDirectory.getName()));
            }
            return;
        }
        try (LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession();
                LDAPSession sourceSession = (LDAPSession) ldapSourceDirectory.getSession()) {
            // fetch the entry to be able to run the security policy
            // implemented in an entry adaptor
            DocumentModel sourceEntry = sourceSession.getEntry(sourceId, false);
            if (sourceEntry == null) {
                throw new DirectoryException(String.format("could not add links from unexisting %s in directory %s",
                        sourceId, ldapSourceDirectory.getName()));
            }
            if (!BaseSession.isReadOnlyEntry(sourceEntry)) {
                SearchResult ldapEntry = sourceSession.getLdapEntry(sourceId);

                String sourceDn = ldapEntry.getNameInNamespace();
                Attribute storedAttr = ldapEntry.getAttributes().get(attributeId);
                String emptyRefMarker = ldapSourceDirectory.getDescriptor().getEmptyRefMarker();
                Attribute attrToAdd = new BasicAttribute(attributeId);
                for (String targetId : targetIds) {
                    if (staticAttributeIdIsDn) {
                        // TODO optim: avoid LDAP search request when targetDn
                        // can be forged client side (rdnAttribute = idAttribute and scope is onelevel)
                        ldapEntry = targetSession.getLdapEntry(targetId);
                        if (ldapEntry == null) {
                            log.warn(String.format(
                                    "entry '%s' in directory '%s' not found: could not add link from '%s' in directory '%s' for '%s'",
                                    targetId, ldapTargetDirectory.getName(), sourceId, ldapSourceDirectory.getName(),
                                    this));
                            continue;
                        }
                        String dn = ldapEntry.getNameInNamespace();
                        if (storedAttr == null || !storedAttr.contains(dn)) {
                            attrToAdd.add(dn);
                        }
                    } else {
                        if (storedAttr == null || !storedAttr.contains(targetId)) {
                            attrToAdd.add(targetId);
                        }
                    }
                }
                if (attrToAdd.size() > 0) {
                    try {
                        // do the LDAP request to store missing dns
                        Attributes attrsToAdd = new BasicAttributes();
                        attrsToAdd.put(attrToAdd);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("LDAPReference.addLinks(%s, [%s]): LDAP modifyAttributes dn='%s' "
                                    + "mod_op='ADD_ATTRIBUTE' attrs='%s' [%s]", sourceId,
                                    StringUtils.join(targetIds, ", "), sourceDn, attrsToAdd, this));
                        }
                        sourceSession.getContext().modifyAttributes(sourceDn, DirContext.ADD_ATTRIBUTE, attrsToAdd);

                        // robustly clean any existing empty marker now that we are sure that the list in not empty
                        if (storedAttr.contains(emptyRefMarker)) {
                            Attributes cleanAttrs = new BasicAttributes(attributeId, emptyRefMarker);

                            if (log.isDebugEnabled()) {
                                log.debug(String.format(
                                        "LDAPReference.addLinks(%s, [%s]): LDAP modifyAttributes dn='%s'"
                                                + " mod_op='REMOVE_ATTRIBUTE' attrs='%s' [%s]", sourceId,
                                        StringUtils.join(targetIds, ", "), sourceDn, cleanAttrs, this));
                            }
                            sourceSession.getContext().modifyAttributes(sourceDn, DirContext.REMOVE_ATTRIBUTE,
                                    cleanAttrs);
                        }
                    } catch (SchemaViolationException e) {
                        if (isDynamic()) {
                            // we are editing an entry that has no static part
                            log.warn(String.format("cannot update dynamic reference in field %s for source %s",
                                    getFieldName(), sourceId));
                        } else {
                            // this is a real schema configuration problem,
                            // wrap up the exception
                            throw new DirectoryException(e);
                        }
                    }
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("addLinks failed: " + e.getMessage(), e);
        }
    }

    /**
     * Store new links using the LDAP staticAttributeId strategy.
     *
     * @see org.nuxeo.ecm.directory.Reference#addLinks(List, String)
     */
    @Override
    public void addLinks(List<String> sourceIds, String targetId) throws DirectoryException {
        String attributeId = getStaticAttributeId();
        if (attributeId == null && !sourceIds.isEmpty()) {
            log.warn("trying to edit a non-static reference: ignoring");
            return;
        }
        LDAPDirectory ldapTargetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectory ldapSourceDirectory = (LDAPDirectory) getSourceDirectory();

        String emptyRefMarker = ldapSourceDirectory.getDescriptor().getEmptyRefMarker();
        try (LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession();
                LDAPSession sourceSession = (LDAPSession) ldapSourceDirectory.getSession()) {
            if (!sourceSession.isReadOnly()) {
                // compute the target dn to add to all the matching source
                // entries
                SearchResult ldapEntry = targetSession.getLdapEntry(targetId);
                if (ldapEntry == null) {
                    throw new DirectoryException(String.format("could not add links to unexisting %s in directory %s",
                            targetId, ldapTargetDirectory.getName()));
                }
                String targetAttributeValue;
                if (staticAttributeIdIsDn) {
                    targetAttributeValue = ldapEntry.getNameInNamespace();
                } else {
                    targetAttributeValue = targetId;
                }

                for (String sourceId : sourceIds) {
                    // fetch the entry to be able to run the security policy
                    // implemented in an entry adaptor
                    DocumentModel sourceEntry = sourceSession.getEntry(sourceId, false);
                    if (sourceEntry == null) {
                        log.warn(String.format(
                                "entry %s in directory %s not found: could not add link to %s in directory %s",
                                sourceId, ldapSourceDirectory.getName(), targetId, ldapTargetDirectory.getName()));
                        continue;
                    }
                    if (BaseSession.isReadOnlyEntry(sourceEntry)) {
                        // skip this entry since it cannot be edited to add the
                        // reference to targetId
                        log.warn(String.format(
                                "entry %s in directory %s is readonly: could not add link to %s in directory %s",
                                sourceId, ldapSourceDirectory.getName(), targetId, ldapTargetDirectory.getName()));
                        continue;
                    }
                    ldapEntry = sourceSession.getLdapEntry(sourceId);
                    String sourceDn = ldapEntry.getNameInNamespace();
                    Attribute storedAttr = ldapEntry.getAttributes().get(attributeId);
                    if (storedAttr.contains(targetAttributeValue)) {
                        // no need to readd
                        continue;
                    }
                    try {
                        // add the new dn
                        Attributes attrs = new BasicAttributes(attributeId, targetAttributeValue);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("LDAPReference.addLinks([%s], %s): LDAP modifyAttributes dn='%s'"
                                    + " mod_op='ADD_ATTRIBUTE' attrs='%s' [%s]", StringUtils.join(sourceIds, ", "),
                                    targetId, sourceDn, attrs, this));
                        }
                        sourceSession.getContext().modifyAttributes(sourceDn, DirContext.ADD_ATTRIBUTE, attrs);

                        // robustly clean any existing empty marker now that we
                        // are sure that the list in not empty
                        if (storedAttr.contains(emptyRefMarker)) {
                            Attributes cleanAttrs = new BasicAttributes(attributeId, emptyRefMarker);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("LDAPReference.addLinks(%s, %s): LDAP modifyAttributes dn='%s'"
                                        + " mod_op='REMOVE_ATTRIBUTE' attrs='%s' [%s]",
                                        StringUtils.join(sourceIds, ", "), targetId, sourceDn, cleanAttrs.toString(),
                                        this));
                            }
                            sourceSession.getContext().modifyAttributes(sourceDn, DirContext.REMOVE_ATTRIBUTE,
                                    cleanAttrs);
                        }
                    } catch (SchemaViolationException e) {
                        if (isDynamic()) {
                            // we are editing an entry that has no static part
                            log.warn(String.format("cannot add dynamic reference in field %s for target %s",
                                    getFieldName(), targetId));
                        } else {
                            // this is a real schema configuration problem,
                            // wrap the exception
                            throw new DirectoryException(e);
                        }
                    }
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("addLinks failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch both statically and dynamically defined references and merge the results.
     *
     * @see org.nuxeo.ecm.directory.Reference#getSourceIdsForTarget(String)
     */
    @Override
    public List<String> getSourceIdsForTarget(String targetId) throws DirectoryException {

        // container to hold merged references
        Set<String> sourceIds = new TreeSet<>();
        SearchResult targetLdapEntry = null;
        String targetDn = null;

        // fetch all attributes when dynamic groups are used
        boolean fetchAllAttributes = isDynamic();

        // step #1: resolve static references
        String staticAttributeId = getStaticAttributeId();
        if (staticAttributeId != null) {
            // step #1.1: fetch the dn of the targetId entry in the target
            // directory by the static dn valued strategy
            LDAPDirectory targetDir = getTargetLDAPDirectory();

            if (staticAttributeIdIsDn) {
                try (LDAPSession targetSession = (LDAPSession) targetDir.getSession()) {
                    targetLdapEntry = targetSession.getLdapEntry(targetId, fetchAllAttributes);
                    if (targetLdapEntry == null) {
                        String msg = String.format("Failed to perform inverse lookup on LDAPReference"
                                + " resolving field '%s' of '%s' to entries of '%s'"
                                + " using the static content of attribute '%s':"
                                + " entry '%s' cannot be found in '%s'", fieldName, sourceDirectory,
                                targetDirectoryName, staticAttributeId, targetId, targetDirectoryName);
                        throw new DirectoryEntryNotFoundException(msg);
                    }
                    targetDn = pseudoNormalizeDn(targetLdapEntry.getNameInNamespace());

                } catch (NamingException e) {
                    throw new DirectoryException("error fetching " + targetId + " from " + targetDirectoryName + ": "
                            + e.getMessage(), e);
                }
            }

            // step #1.2: search for entries that reference that dn in the
            // source directory and collect their ids
            LDAPDirectory ldapSourceDirectory = getSourceLDAPDirectory();

            String filterExpr = String.format("(&(%s={0})%s)", staticAttributeId, ldapSourceDirectory.getBaseFilter());
            String[] filterArgs = new String[1];

            if (staticAttributeIdIsDn) {
                filterArgs[0] = targetDn;
            } else {
                filterArgs[0] = targetId;
            }

            String searchBaseDn = ldapSourceDirectory.getDescriptor().getSearchBaseDn();
            SearchControls sctls = ldapSourceDirectory.getSearchControls();
            try (LDAPSession sourceSession = (LDAPSession) ldapSourceDirectory.getSession()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("LDAPReference.getSourceIdsForTarget(%s): LDAP search search base='%s'"
                            + " filter='%s' args='%s' scope='%s' [%s]", targetId, searchBaseDn, filterExpr,
                            StringUtils.join(filterArgs, ", "), sctls.getSearchScope(), this));
                }
                NamingEnumeration<SearchResult> results = sourceSession.getContext().search(searchBaseDn, filterExpr,
                        filterArgs, sctls);

                try {
                    while (results.hasMore()) {
                        Attributes attributes = results.next().getAttributes();
                        // NXP-2461: check that id field is filled
                        Attribute attr = attributes.get(sourceSession.idAttribute);
                        if (attr != null) {
                            Object value = attr.get();
                            if (value != null) {
                                sourceIds.add(value.toString());
                            }
                        }
                    }
                } finally {
                    results.close();
                }
            } catch (NamingException e) {
                throw new DirectoryException("error during reference search for " + filterArgs[0], e);
            }
        }
        // step #2: resolve dynamic references
        String dynamicAttributeId = this.dynamicAttributeId;
        if (dynamicAttributeId != null) {

            LDAPDirectory ldapSourceDirectory = getSourceLDAPDirectory();
            LDAPDirectory ldapTargetDirectory = getTargetLDAPDirectory();
            String searchBaseDn = ldapSourceDirectory.getDescriptor().getSearchBaseDn();

            try (LDAPSession sourceSession = (LDAPSession) ldapSourceDirectory.getSession();
                    LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession()) {
                // step #2.1: fetch the target entry to apply the ldap url
                // filters of the candidate sources on it
                if (targetLdapEntry == null) {
                    // only fetch the entry if not already fetched by the
                    // static
                    // attributes references resolution
                    targetLdapEntry = targetSession.getLdapEntry(targetId, fetchAllAttributes);
                }
                if (targetLdapEntry == null) {
                    String msg = String.format("Failed to perform inverse lookup on LDAPReference"
                            + " resolving field '%s' of '%s' to entries of '%s'"
                            + " using the dynamic content of attribute '%s':" + " entry '%s' cannot be found in '%s'",
                            fieldName, ldapSourceDirectory, targetDirectoryName, dynamicAttributeId, targetId,
                            targetDirectoryName);
                    throw new DirectoryException(msg);
                }
                targetDn = pseudoNormalizeDn(targetLdapEntry.getNameInNamespace());
                Attributes targetAttributes = targetLdapEntry.getAttributes();

                // step #2.2: find the list of entries that hold candidate
                // dynamic links in the source directory
                SearchControls sctls = new SearchControls();
                sctls.setSearchScope(ldapSourceDirectory.getDescriptor().getSearchScope());
                sctls.setReturningAttributes(new String[] { sourceSession.idAttribute, dynamicAttributeId });
                String filterExpr = String.format("%s=*", dynamicAttributeId);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("LDAPReference.getSourceIdsForTarget(%s): LDAP search search base='%s'"
                            + " filter='%s' scope='%s' [%s]", targetId, searchBaseDn, filterExpr,
                            sctls.getSearchScope(), this));
                }
                NamingEnumeration<SearchResult> results = sourceSession.getContext().search(searchBaseDn, filterExpr,
                        sctls);
                try {
                    while (results.hasMore()) {
                        // step #2.3: for each sourceId and each ldapUrl test
                        // whether the current target entry matches the
                        // collected
                        // URL
                        Attributes sourceAttributes = results.next().getAttributes();

                        NamingEnumeration<?> ldapUrls = sourceAttributes.get(dynamicAttributeId).getAll();
                        try {
                            while (ldapUrls.hasMore()) {
                                LdapURL ldapUrl = new LdapURL(ldapUrls.next().toString());
                                String candidateDN = pseudoNormalizeDn(ldapUrl.getDN());
                                // check base URL
                                if (!targetDn.endsWith(candidateDN)) {
                                    continue;
                                }

                                // check onelevel scope constraints
                                if ("onelevel".equals(ldapUrl.getScope())) {
                                    int targetDnSize = new LdapName(targetDn).size();
                                    int urlDnSize = new LdapName(candidateDN).size();
                                    if (targetDnSize - urlDnSize > 1) {
                                        // target is not a direct child of the
                                        // DN of the
                                        // LDAP URL
                                        continue;
                                    }
                                }

                                // check that the target entry matches the
                                // filter
                                if (getFilterMatcher().match(targetAttributes, ldapUrl.getFilter())) {
                                    // the target match the source url, add it
                                    // to the
                                    // collected ids
                                    sourceIds.add(sourceAttributes.get(sourceSession.idAttribute).get().toString());
                                }
                            }
                        } finally {
                            ldapUrls.close();
                        }
                    }
                } finally {
                    results.close();
                }
            } catch (NamingException e) {
                throw new DirectoryException("error during reference search for " + targetId, e);
            }
        }

        /*
         * This kind of reference is not supported because Active Directory use filter expression not yet supported by
         * LDAPFilterMatcher. See NXP-4562
         */
        if (dynamicReferences != null && dynamicReferences.length > 0) {
            log.error("This kind of reference is not supported.");
        }

        return new ArrayList<>(sourceIds);
    }

    /**
     * Fetches both statically and dynamically defined references and merges the results.
     *
     * @see org.nuxeo.ecm.directory.Reference#getSourceIdsForTarget(String)
     */
    @Override
    // XXX: broken, use getLdapTargetIds for a proper implementation
    @SuppressWarnings("unchecked")
    public List<String> getTargetIdsForSource(String sourceId) throws DirectoryException {
        String schemaName = getSourceDirectory().getSchema();
        try (Session session = getSourceDirectory().getSession()) {
            try {
                return (List<String>) session.getEntry(sourceId).getProperty(schemaName, fieldName);
            } catch (PropertyException e) {
                throw new DirectoryException(e);
            }
        }
    }

    /**
     * Simple helper that replaces ", " by "," in the provided dn and returns the lower case version of the result for
     * comparison purpose.
     *
     * @param dn the raw unnormalized dn
     * @return lowercase version without whitespace after commas
     * @throws InvalidNameException
     */
    protected static String pseudoNormalizeDn(String dn) throws InvalidNameException {
        LdapName ldapName = new LdapName(dn);
        List<String> rdns = new ArrayList<>();
        for (Rdn rdn : ldapName.getRdns()) {
            String value = rdn.getValue().toString().toLowerCase().replaceAll(",", "\\\\,");
            String rdnStr = rdn.getType().toLowerCase() + "=" + value;
            rdns.add(0, rdnStr);
        }
        return StringUtils.join(rdns, ',');
    }

    /**
     * Optimized method to spare a LDAP request when the caller is a LDAPSession object that has already fetched the
     * LDAP Attribute instances.
     * <p>
     * This method should return the same results as the sister method: org.nuxeo
     * .ecm.directory.Reference#getTargetIdsForSource(java.lang.String)
     *
     * @return target reference ids
     * @throws DirectoryException
     */
    public List<String> getLdapTargetIds(Attributes attributes) throws DirectoryException {

        Set<String> targetIds = new TreeSet<>();

        LDAPDirectory ldapTargetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectoryDescriptor targetDirconfig = getTargetDirectoryDescriptor();
        String emptyRefMarker = ldapTargetDirectory.getDescriptor().getEmptyRefMarker();
        try (LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession()) {
            String baseDn = pseudoNormalizeDn(targetDirconfig.getSearchBaseDn());

            // step #1: fetch ids referenced by static attributes
            String staticAttributeId = getStaticAttributeId();
            Attribute staticAttribute = null;
            if (staticAttributeId != null) {
                staticAttribute = attributes.get(staticAttributeId);
            }

            if (staticAttribute != null && !staticAttributeIdIsDn) {
                NamingEnumeration<?> staticContent = staticAttribute.getAll();
                try {
                    while (staticContent.hasMore()) {
                        String value = staticContent.next().toString();
                        if (!emptyRefMarker.equals(value)) {
                            targetIds.add(value);
                        }
                    }
                } finally {
                    staticContent.close();
                }
            }

            if (staticAttribute != null && staticAttributeIdIsDn) {
                NamingEnumeration<?> targetDns = staticAttribute.getAll();
                try {
                    while (targetDns.hasMore()) {
                        String targetDn = targetDns.next().toString();

                        if (!pseudoNormalizeDn(targetDn).endsWith(baseDn)) {
                            // optim: avoid network connections when obvious
                            if (log.isTraceEnabled()) {
                                log.trace(String.format("ignoring: dn='%s' (does not match '%s') for '%s'", targetDn,
                                        baseDn, this));
                            }
                            continue;
                        }
                        // find the id of the referenced entry
                        String id = null;

                        if (targetSession.rdnMatchesIdField()) {
                            // optim: do not fetch the entry to get its true id
                            // but
                            // guess it by reading the targetDn
                            LdapName name = new LdapName(targetDn);
                            String rdn = name.get(name.size() - 1);
                            int pos = rdn.indexOf("=");
                            id = rdn.substring(pos + 1);
                        } else {
                            id = getIdForDn(targetSession, targetDn);
                            if (id == null) {
                                log.warn(String.format(
                                        "ignoring target '%s' (missing attribute '%s') while resolving reference '%s'",
                                        targetDn, targetSession.idAttribute, this));
                                continue;
                            }
                        }
                        if (forceDnConsistencyCheck) {
                            // check that the referenced entry is actually part
                            // of
                            // the target directory (takes care of the filters
                            // and
                            // the scope)
                            // this check can be very expensive on large groups
                            // and thus not enabled by default
                            if (!targetSession.hasEntry(id)) {
                                if (log.isTraceEnabled()) {
                                    log.trace(String.format(
                                            "ignoring target '%s' when resolving '%s' (not part of target"
                                                    + " directory by forced DN consistency check)", targetDn, this));
                                }
                                continue;
                            }
                        }
                        // NXP-2461: check that id field is filled
                        if (id != null) {
                            targetIds.add(id);
                        }
                    }
                } finally {
                    targetDns.close();
                }
            }
            // step #2: fetched dynamically referenced ids
            String dynamicAttributeId = this.dynamicAttributeId;
            Attribute dynamicAttribute = null;
            if (dynamicAttributeId != null) {
                dynamicAttribute = attributes.get(dynamicAttributeId);
            }
            if (dynamicAttribute != null) {
                NamingEnumeration<?> rawldapUrls = dynamicAttribute.getAll();
                try {
                    while (rawldapUrls.hasMore()) {
                        LdapURL ldapUrl = new LdapURL(rawldapUrls.next().toString());
                        String linkDn = pseudoNormalizeDn(ldapUrl.getDN());
                        String directoryDn = pseudoNormalizeDn(targetDirconfig.getSearchBaseDn());
                        int scope = SearchControls.ONELEVEL_SCOPE;
                        String scopePart = ldapUrl.getScope();
                        if (scopePart != null && scopePart.toLowerCase().startsWith("sub")) {
                            scope = SearchControls.SUBTREE_SCOPE;
                        }
                        if (!linkDn.endsWith(directoryDn) && !directoryDn.endsWith(linkDn)) {
                            // optim #1: if the dns do not match, abort
                            continue;
                        } else if (directoryDn.endsWith(linkDn) && linkDn.length() < directoryDn.length()
                                && scope == SearchControls.ONELEVEL_SCOPE) {
                            // optim #2: the link dn is pointing to elements
                            // that at
                            // upperlevel than directory elements
                            continue;
                        } else {

                            // Search for references elements
                            targetIds.addAll(getReferencedElements(attributes, directoryDn, linkDn,
                                    ldapUrl.getFilter(), scope));

                        }
                    }
                } finally {
                    rawldapUrls.close();
                }
            }

            if (dynamicReferences != null && dynamicReferences.length > 0) {

                // Only the first Dynamic Reference is used
                LDAPDynamicReferenceDescriptor dynAtt = dynamicReferences[0];

                Attribute baseDnsAttribute = attributes.get(dynAtt.baseDN);
                Attribute filterAttribute = attributes.get(dynAtt.filter);

                if (baseDnsAttribute != null && filterAttribute != null) {

                    NamingEnumeration<?> baseDns = null;
                    NamingEnumeration<?> filters = null;

                    try {
                        // Get the BaseDN value from the descriptor
                        baseDns = baseDnsAttribute.getAll();
                        String linkDnValue = baseDns.next().toString();
                        baseDns.close();
                        linkDnValue = pseudoNormalizeDn(linkDnValue);

                        // Get the filter value from the descriptor
                        filters = filterAttribute.getAll();
                        String filterValue = filters.next().toString();
                        filters.close();

                        // Get the scope value from the descriptor
                        int scope = "subtree".equalsIgnoreCase(dynAtt.type) ? SearchControls.SUBTREE_SCOPE
                                : SearchControls.ONELEVEL_SCOPE;

                        String directoryDn = pseudoNormalizeDn(targetDirconfig.getSearchBaseDn());

                        // if the dns match, and if the link dn is pointing to
                        // elements that at upperlevel than directory elements
                        if ((linkDnValue.endsWith(directoryDn) || directoryDn.endsWith(linkDnValue))
                                && !(directoryDn.endsWith(linkDnValue) && linkDnValue.length() < directoryDn.length() && scope == SearchControls.ONELEVEL_SCOPE)) {

                            // Correct the filter expression
                            filterValue = FilterExpressionCorrector.correctFilter(filterValue, FilterJobs.CORRECT_NOT);

                            // Search for references elements
                            targetIds.addAll(getReferencedElements(attributes, directoryDn, linkDnValue, filterValue,
                                    scope));

                        }
                    } finally {
                        if (baseDns != null) {
                            baseDns.close();
                        }

                        if (filters != null) {
                            filters.close();
                        }
                    }

                }

            }
            // return merged attributes
            return new ArrayList<String>(targetIds);
        } catch (NamingException e) {
            throw new DirectoryException("error computing LDAP references", e);
        }
    }

    protected String getIdForDn(LDAPSession session, String dn) {
        // the entry id is not based on the rdn, we thus need to
        // fetch the LDAP entry to grab it
        String[] attributeIdsToCollect = { session.idAttribute };
        Attributes entry;
        try {

            if (log.isDebugEnabled()) {
                log.debug(String.format("LDAPReference.getIdForDn(session, %s): LDAP get dn='%s'"
                        + " attribute ids to collect='%s' [%s]", dn, dn, StringUtils.join(attributeIdsToCollect, ", "),
                        this));
            }

            Name name = new CompositeName().add(dn);
            entry = session.getContext().getAttributes(name, attributeIdsToCollect);
        } catch (NamingException e) {
            return null;
        }
        // NXP-2461: check that id field is filled
        Attribute attr = entry.get(session.idAttribute);
        if (attr != null) {
            try {
                return attr.get().toString();
            } catch (NamingException e) {
            }
        }
        return null;
    }

    /**
     * Retrieve the elements referenced by the filter/BaseDN/Scope request.
     *
     * @param attributes Attributes of the referencer element
     * @param directoryDn Dn of the Directory
     * @param linkDn Dn specified in the parent
     * @param filter Filter expression specified in the parent
     * @param scope scope for the search
     * @return The list of the referenced elements.
     * @throws DirectoryException
     * @throws NamingException
     */
    private Set<String> getReferencedElements(Attributes attributes, String directoryDn, String linkDn, String filter,
            int scope) throws DirectoryException, NamingException {

        Set<String> targetIds = new TreeSet<>();

        LDAPDirectoryDescriptor targetDirconfig = getTargetDirectoryDescriptor();
        LDAPDirectory ldapTargetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession();

        // use the most specific scope between the one specified in the
        // Directory and the specified in the Parent
        String dn = directoryDn.endsWith(linkDn) && directoryDn.length() > linkDn.length() ? directoryDn : linkDn;

        // combine the ldapUrl search query with target
        // directory own constraints
        SearchControls scts = new SearchControls();

        // use the most specific scope
        scts.setSearchScope(Math.min(scope, targetDirconfig.getSearchScope()));

        // only fetch the ids of the targets
        scts.setReturningAttributes(new String[] { targetSession.idAttribute });

        // combine the filter of the target directory with the
        // provided filter if any
        String targetFilter = targetDirconfig.getSearchFilter();
        if (filter == null || filter.length() == 0) {
            filter = targetFilter;
        } else if (targetFilter != null && targetFilter.length() > 0) {
            filter = String.format("(&(%s)(%s))", targetFilter, filter);
        }

        // perform the request and collect the ids
        if (log.isDebugEnabled()) {
            log.debug(String.format("LDAPReference.getLdapTargetIds(%s): LDAP search dn='%s' "
                    + " filter='%s' scope='%s' [%s]", attributes, dn, dn, scts.getSearchScope(), this));
        }

        Name name = new CompositeName().add(dn);
        NamingEnumeration<SearchResult> results = targetSession.getContext().search(name, filter, scts);
        try {
            while (results.hasMore()) {
                // NXP-2461: check that id field is filled
                Attribute attr = results.next().getAttributes().get(targetSession.idAttribute);
                if (attr != null) {
                    String collectedId = attr.get().toString();
                    if (collectedId != null) {
                        targetIds.add(collectedId);
                    }
                }

            }
        } finally {
            results.close();
        }

        return targetIds;
    }

    /**
     * Remove existing statically defined links for the given source id (dynamic references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#removeLinksForSource(String)
     */
    @Override
    public void removeLinksForSource(String sourceId) throws DirectoryException {
        LDAPDirectory ldapTargetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectory ldapSourceDirectory = (LDAPDirectory) getSourceDirectory();
        String attributeId = getStaticAttributeId();
        try (LDAPSession sourceSession = (LDAPSession) ldapSourceDirectory.getSession();
                LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession()) {
            if (sourceSession.isReadOnly() || attributeId == null) {
                // do not try to do anything on a read only server or to a
                // purely dynamic reference
                return;
            }
            // get the dn of the entry that matches sourceId
            SearchResult sourceLdapEntry = sourceSession.getLdapEntry(sourceId);
            if (sourceLdapEntry == null) {
                throw new DirectoryException(String.format(
                        "cannot edit the links hold by missing entry '%s' in directory '%s'", sourceId,
                        ldapSourceDirectory.getName()));
            }
            String sourceDn = pseudoNormalizeDn(sourceLdapEntry.getNameInNamespace());

            Attribute oldAttr = sourceLdapEntry.getAttributes().get(attributeId);
            if (oldAttr == null) {
                // consider it as an empty attribute to simplify the following
                // code
                oldAttr = new BasicAttribute(attributeId);
            }
            Attribute attrToRemove = new BasicAttribute(attributeId);

            NamingEnumeration<?> oldAttrs = oldAttr.getAll();
            String targetBaseDn = pseudoNormalizeDn(ldapTargetDirectory.getDescriptor().getSearchBaseDn());
            try {
                while (oldAttrs.hasMore()) {
                    String targetKeyAttr = oldAttrs.next().toString();

                    if (staticAttributeIdIsDn) {
                        String dn = pseudoNormalizeDn(targetKeyAttr);
                        if (forceDnConsistencyCheck) {
                            String id = getIdForDn(targetSession, dn);
                            if (id != null && targetSession.hasEntry(id)) {
                                // this is an entry managed by the current
                                // reference
                                attrToRemove.add(dn);
                            }
                        } else if (dn.endsWith(targetBaseDn)) {
                            // this is an entry managed by the current
                            // reference
                            attrToRemove.add(dn);
                        }
                    } else {
                        attrToRemove.add(targetKeyAttr);
                    }
                }
            } finally {
                oldAttrs.close();
            }
            try {
                if (attrToRemove.size() == oldAttr.size()) {
                    // use the empty ref marker to avoid empty attr
                    String emptyRefMarker = ldapSourceDirectory.getDescriptor().getEmptyRefMarker();
                    Attributes emptyAttribute = new BasicAttributes(attributeId, emptyRefMarker);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "LDAPReference.removeLinksForSource(%s): LDAP modifyAttributes key='%s' "
                                        + " mod_op='REPLACE_ATTRIBUTE' attrs='%s' [%s]", sourceId, sourceDn,
                                emptyAttribute, this));
                    }
                    sourceSession.getContext().modifyAttributes(sourceDn, DirContext.REPLACE_ATTRIBUTE, emptyAttribute);
                } else if (attrToRemove.size() > 0) {
                    // remove the attribute managed by the current reference
                    Attributes attrsToRemove = new BasicAttributes();
                    attrsToRemove.put(attrToRemove);
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "LDAPReference.removeLinksForSource(%s): LDAP modifyAttributes dn='%s' "
                                        + " mod_op='REMOVE_ATTRIBUTE' attrs='%s' [%s]", sourceId, sourceDn,
                                attrsToRemove, this));
                    }
                    sourceSession.getContext().modifyAttributes(sourceDn, DirContext.REMOVE_ATTRIBUTE, attrsToRemove);
                }
            } catch (SchemaViolationException e) {
                if (isDynamic()) {
                    // we are editing an entry that has no static part
                    log.warn(String.format("cannot remove dynamic reference in field %s for source %s", getFieldName(),
                            sourceId));
                } else {
                    // this is a real schma configuration problem, wrapup the
                    // exception
                    throw new DirectoryException(e);
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("removeLinksForSource failed: " + e.getMessage(), e);
        }
    }

    /**
     * Remove existing statically defined links for the given target id (dynamic references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#removeLinksForTarget(String)
     */
    @Override
    public void removeLinksForTarget(String targetId) throws DirectoryException {
        if (!isStatic()) {
            // nothing to do: dynamic references cannot be updated
            return;
        }
        LDAPDirectory ldapTargetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectory ldapSourceDirectory = (LDAPDirectory) getSourceDirectory();
        String attributeId = getStaticAttributeId();
        try (LDAPSession targetSession = (LDAPSession) ldapTargetDirectory.getSession();
                LDAPSession sourceSession = (LDAPSession) ldapSourceDirectory.getSession()) {
            if (!sourceSession.isReadOnly()) {
                // get the dn of the target that matches targetId
                String targetAttributeValue;

                if (staticAttributeIdIsDn) {
                    SearchResult targetLdapEntry = targetSession.getLdapEntry(targetId);
                    if (targetLdapEntry == null) {
                        String rdnAttribute = ldapTargetDirectory.getDescriptor().getRdnAttribute();
                        if (!rdnAttribute.equals(targetSession.idAttribute)) {
                            log.warn(String.format(
                                    "cannot remove links to missing entry %s in directory %s for reference %s",
                                    targetId, ldapTargetDirectory.getName(), this));
                            return;
                        }
                        // the entry might have already been deleted, try to
                        // re-forge it if possible (might not work if scope is
                        // subtree)
                        targetAttributeValue = String.format("%s=%s,%s", rdnAttribute, targetId,
                                ldapTargetDirectory.getDescriptor().getSearchBaseDn());
                    } else {
                        targetAttributeValue = pseudoNormalizeDn(targetLdapEntry.getNameInNamespace());
                    }
                } else {
                    targetAttributeValue = targetId;
                }

                // build a LDAP query to find entries that point to the target
                String searchFilter = String.format("(%s=%s)", attributeId, targetAttributeValue);
                String sourceFilter = ldapSourceDirectory.getBaseFilter();

                if (sourceFilter != null && !"".equals(sourceFilter)) {
                    searchFilter = String.format("(&(%s)(%s))", searchFilter, sourceFilter);
                }

                SearchControls scts = new SearchControls();
                scts.setSearchScope(ldapSourceDirectory.getDescriptor().getSearchScope());
                scts.setReturningAttributes(new String[] { attributeId });

                // find all source entries that point to the target key and
                // clean
                // those references
                if (log.isDebugEnabled()) {
                    log.debug(String.format("LDAPReference.removeLinksForTarget(%s): LDAP search baseDn='%s' "
                            + " filter='%s' scope='%s' [%s]", targetId, sourceSession.searchBaseDn, searchFilter,
                            scts.getSearchScope(), this));
                }
                NamingEnumeration<SearchResult> results = sourceSession.getContext().search(sourceSession.searchBaseDn,
                        searchFilter, scts);
                String emptyRefMarker = ldapSourceDirectory.getDescriptor().getEmptyRefMarker();
                Attributes emptyAttribute = new BasicAttributes(attributeId, emptyRefMarker);

                try {
                    while (results.hasMore()) {
                        SearchResult result = results.next();
                        Attributes attrs = result.getAttributes();
                        Attribute attr = attrs.get(attributeId);
                        try {
                            if (attr.size() == 1) {
                                // the attribute holds the last reference, put
                                // the
                                // empty ref. marker before removing the
                                // attribute
                                // since empty attribute are often not allowed
                                // by
                                // the server schema
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format(
                                            "LDAPReference.removeLinksForTarget(%s): LDAP modifyAttributes key='%s' "
                                                    + "mod_op='ADD_ATTRIBUTE' attrs='%s' [%s]", targetId,
                                            result.getNameInNamespace(), attrs, this));
                                }
                                sourceSession.getContext().modifyAttributes(result.getNameInNamespace(),
                                        DirContext.ADD_ATTRIBUTE, emptyAttribute);
                            }
                            // remove the reference to the target key
                            attrs = new BasicAttributes();
                            attr = new BasicAttribute(attributeId);
                            attr.add(targetAttributeValue);
                            attrs.put(attr);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format(
                                        "LDAPReference.removeLinksForTarget(%s): LDAP modifyAttributes key='%s' "
                                                + "mod_op='REMOVE_ATTRIBUTE' attrs='%s' [%s]", targetId,
                                        result.getNameInNamespace(), attrs, this));
                            }
                            sourceSession.getContext().modifyAttributes(result.getNameInNamespace(),
                                    DirContext.REMOVE_ATTRIBUTE, attrs);
                        } catch (SchemaViolationException e) {
                            if (isDynamic()) {
                                // we are editing an entry that has no static
                                // part
                                log.warn(String.format("cannot remove dynamic reference in field %s for target %s",
                                        getFieldName(), targetId));
                            } else {
                                // this is a real schema configuration problem,
                                // wrapup the exception
                                throw new DirectoryException(e);
                            }
                        }
                    }
                } finally {
                    results.close();
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("removeLinksForTarget failed: " + e.getMessage(), e);
        }
    }

    /**
     * Edit the list of statically defined references for a given target (dynamic references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#setSourceIdsForTarget(String, List)
     */
    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds) throws DirectoryException {
        removeLinksForTarget(targetId);
        addLinks(sourceIds, targetId);
    }

    /**
     * Set the list of statically defined references for a given source (dynamic references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#setTargetIdsForSource(String, List)
     */
    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds) throws DirectoryException {
        removeLinksForSource(sourceId);
        addLinks(sourceId, targetIds);
    }

    @Override
    // to build helpful debug logs
    public String toString() {
        return String.format("LDAPReference to resolve field='%s' of sourceDirectory='%s'"
                + " with targetDirectory='%s'" + " and staticAttributeId='%s', dynamicAttributeId='%s'", fieldName,
                sourceDirectoryName, targetDirectoryName, staticAttributeId, dynamicAttributeId);
    }

    /**
     * @since 5.6
     */
    @Override
    public LDAPReference clone() {
        LDAPReference clone = (LDAPReference) super.clone();
        // basic fields are already copied by super.clone()
        return clone;
    }

}
