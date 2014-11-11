/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.features;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Provides helper methods to find extract permissions/principals info from
 * documents.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author Anahide Tchertchian
 */
public class PrincipalHelper {

    protected UserManager userManager;

    protected PermissionProvider permissionProvider;

    public PrincipalHelper(UserManager userManager,
            PermissionProvider permissionProvider) {
        this.userManager = userManager;
        this.permissionProvider = permissionProvider;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getEmailsForPermission(DocumentModel input,
            String permission, boolean ignoreGroups) throws ClientException {
        return (Set<String>) collectObjectsMatchingPermission(input,
                permission, ignoreGroups, true, new EmailCollector(
                        userManager.getUserSchemaName(),
                        userManager.getUserEmailField()));
    }

    /**
     * Resolves the list of identifiers for users and groups who have the given
     * permission on given document.
     *
     * @param input document model to resolve users and groups on.
     * @param permission the permission to check
     * @param ignoreGroups if true, will ignore groups in resolution of ids
     * @param resolveGroups if true, will resolve user members, iterating in
     *            the hierarchy of groups
     * @param prefixIds if true, will prefix identifiers with
     *            {@link NuxeoPrincipal#PREFIX} and {@link NuxeoGroup#PREFIX}
     * @throws ClientException
     */
    @SuppressWarnings("unchecked")
    public Set<String> getUserAndGroupIdsForPermission(DocumentModel input,
            String permission, boolean ignoreGroups, boolean resolveGroups,
            boolean prefixIds) throws ClientException {
        return (Set<String>) collectObjectsMatchingPermission(input,
                permission, ignoreGroups, resolveGroups, new IdCollector(
                        prefixIds));
    }

    @SuppressWarnings("unchecked")
    public Set<NuxeoPrincipal> getPrincipalsForPermission(DocumentModel input,
            String permission, boolean ignoreGroups, boolean resolveGroups)
            throws ClientException {
        return (Set<NuxeoPrincipal>) collectObjectsMatchingPermission(input,
                permission, ignoreGroups, resolveGroups,
                new PrincipalCollector());
    }

    public HashSet<?> collectObjectsMatchingPermission(DocumentModel input,
            String permission, boolean ignoreGroups, boolean resolveGroups,
            Collector<?> collector) throws ClientException {
        String[] perms = getPermissionsToCheck(permission);
        ACP acp = input.getACP();
        for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted()
                        && permissionMatch(perms, ace.getPermission())) {
                    NuxeoGroup group = userManager.getGroup(ace.getUsername());
                    if (group == null) {
                        // this may be a user
                        collector.collect(userManager.getPrincipal(ace.getUsername()));
                    } else if (!ignoreGroups) {
                        if (resolveGroups) {
                            resolveGroups(group, collector);
                        } else {
                            collector.collect(group);
                        }
                    }
                }
            }
        }
        return collector.getResult();
    }

    public void resolveGroups(NuxeoGroup group, Collector<?> collector)
            throws ClientException {
        if (group != null) {
            for (String memberUser : group.getMemberUsers()) {
                collector.collect(userManager.getPrincipal(memberUser));
            }
            for (String subGroup : group.getMemberGroups()) {
                resolveGroups(userManager.getGroup(subGroup), collector);
            }
        }
    }

    public String[] getPermissionsToCheck(String permission) {
        String[] groups = permissionProvider.getPermissionGroups(permission);
        if (groups == null) {
            return new String[] { permission, SecurityConstants.EVERYTHING };
        } else {
            String[] perms = new String[groups.length + 2];
            perms[0] = permission;
            System.arraycopy(groups, 0, perms, 1, groups.length);
            perms[groups.length + 1] = SecurityConstants.EVERYTHING;
            return perms;
        }
    }

    public boolean permissionMatch(String[] perms, String perm) {
        for (String p : perms) {
            if (p.equals(perm)) {
                return true;
            }
        }
        return false;
    }

    interface Collector<T> {

        void collect(NuxeoPrincipal principal) throws ClientException;

        void collect(NuxeoGroup group) throws ClientException;

        HashSet<T> getResult();
    }

    static class EmailCollector implements Collector<String> {

        protected final String userSchemaName;

        protected final String userEmailFieldName;

        protected HashSet<String> result = new HashSet<String>();

        EmailCollector(String userSchemaName, String userEmailFieldName) {
            this.userSchemaName = userSchemaName;
            this.userEmailFieldName = userEmailFieldName;
        }

        public void collect(NuxeoPrincipal principal) throws ClientException {
            if (principal == null) {
                return;
            }
            DocumentModel userEntry = principal.getModel();
            String email = (String) userEntry.getProperty(userSchemaName,
                    userEmailFieldName);
            if (!StringUtils.isEmpty(email)) {
                result.add(email);
            }
        }

        public void collect(NuxeoGroup group) throws ClientException {
            // do nothing
        }

        public HashSet<String> getResult() {
            return result;
        }
    }

    static class PrincipalCollector implements Collector<NuxeoPrincipal> {

        protected HashSet<NuxeoPrincipal> result = new HashSet<NuxeoPrincipal>();

        public void collect(NuxeoPrincipal principal) throws ClientException {
            if (principal == null) {
                return;
            }
            result.add(principal);
        }

        public void collect(NuxeoGroup group) throws ClientException {
            // do nothing
        }

        public HashSet<NuxeoPrincipal> getResult() {
            return result;
        }
    }

    static class IdCollector implements Collector<String> {

        protected final boolean prefixIds;

        protected HashSet<String> result = new HashSet<String>();

        IdCollector(boolean prefixIds) {
            this.prefixIds = prefixIds;
        }

        public void collect(NuxeoPrincipal principal) throws ClientException {
            if (principal != null) {
                String name = principal.getName();
                if (name != null) {
                    if (prefixIds) {
                        result.add(NuxeoPrincipal.PREFIX + name);
                    } else {
                        result.add(name);
                    }
                }
            }
        }

        public void collect(NuxeoGroup group) throws ClientException {
            if (group != null) {
                String name = group.getName();
                if (name != null) {
                    if (prefixIds) {
                        result.add(NuxeoGroup.PREFIX + name);
                    } else {
                        result.add(name);
                    }
                }
            }
        }

        public HashSet<String> getResult() {
            return result;
        }

    }

}
