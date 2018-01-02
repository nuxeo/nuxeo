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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.features;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
 * Provides helper methods to find extract permissions/principals info from documents.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author Anahide Tchertchian
 */
public class PrincipalHelper {

    protected UserManager userManager;

    protected PermissionProvider permissionProvider;

    public PrincipalHelper(UserManager userManager, PermissionProvider permissionProvider) {
        this.userManager = userManager;
        this.permissionProvider = permissionProvider;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getEmailsForPermission(DocumentModel input, String permission, boolean ignoreGroups) {
        return (Set<String>) collectObjectsMatchingPermission(input, permission, ignoreGroups, true,
                new EmailCollector(userManager.getUserSchemaName(), userManager.getUserEmailField()));
    }

    /**
     * Resolves the list of identifiers for users and groups who have the given permission on given document.
     *
     * @param input document model to resolve users and groups on.
     * @param permission the permission to check
     * @param ignoreGroups if true, will ignore groups in resolution of ids
     * @param resolveGroups if true, will resolve user members, iterating in the hierarchy of groups
     * @param prefixIds if true, will prefix identifiers with {@link NuxeoPrincipal#PREFIX} and
     *            {@link NuxeoGroup#PREFIX}
     */
    @SuppressWarnings("unchecked")
    public Set<String> getUserAndGroupIdsForPermission(DocumentModel input, String permission, boolean ignoreGroups,
            boolean resolveGroups, boolean prefixIds) {
        return (Set<String>) collectObjectsMatchingPermission(input, permission, ignoreGroups, resolveGroups,
                new IdCollector(prefixIds));
    }

    @SuppressWarnings("unchecked")
    public Set<NuxeoPrincipal> getPrincipalsForPermission(DocumentModel input, String permission, boolean ignoreGroups,
            boolean resolveGroups) {
        return (Set<NuxeoPrincipal>) collectObjectsMatchingPermission(input, permission, ignoreGroups, resolveGroups,
                new PrincipalCollector());
    }

    public Set<String> getEmailsFromGroup(String groupId, boolean resolveGroups) {
        EmailCollector collector = new EmailCollector(userManager.getUserSchemaName(), userManager.getUserEmailField());
        collectObjectsFromGroup(groupId, resolveGroups, collector);
        return collector.getResult();
    }

    public Set<NuxeoPrincipal> getPrincipalsFromGroup(String groupId, boolean resolveGroups) {
        PrincipalCollector collector = new PrincipalCollector();
        collectObjectsFromGroup(groupId, resolveGroups, collector);
        return collector.getResult();
    }

    public Set<String> getUserNamesFromGroup(String groupId, boolean resolveGroups, boolean prefixIds) {
        IdCollector collector = new IdCollector(prefixIds);
        collectObjectsFromGroup(groupId, resolveGroups, collector);
        return collector.getResult();
    }

    public void collectObjectsFromGroup(String groupId, boolean resolveGroups, Collector<?> collector) {
        NuxeoGroup group = userManager.getGroup(groupId);
        if (group == null) {
            userManager.getPrincipal(groupId);
        } else {
            for (String u : group.getMemberUsers()) {
                NuxeoPrincipal principal = userManager.getPrincipal(u);
                if (principal != null) {
                    collector.collect(principal);
                }
            }
            if (resolveGroups) {
                for (String g : group.getMemberGroups()) {
                    collectObjectsFromGroup(g, resolveGroups, collector);
                }
            }
        }
    }

    public HashSet<?> collectObjectsMatchingPermission(DocumentModel input, String permission, boolean ignoreGroups,
            boolean resolveGroups, Collector<?> collector) {
        String[] perms = getPermissionsToCheck(permission);
        ACP acp = input.getACP();
        for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted() && permissionMatch(perms, ace.getPermission())) {
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

    public void resolveGroups(NuxeoGroup group, Collector<?> collector) {
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

        void collect(NuxeoPrincipal principal);

        void collect(NuxeoGroup group);

        HashSet<T> getResult();
    }

    public static class EmailCollector implements Collector<String> {

        protected final String userSchemaName;

        protected final String userEmailFieldName;

        protected HashSet<String> result = new HashSet<>();

        EmailCollector(String userSchemaName, String userEmailFieldName) {
            this.userSchemaName = userSchemaName;
            this.userEmailFieldName = userEmailFieldName;
        }

        @Override
        public void collect(NuxeoPrincipal principal) {
            if (principal == null) {
                return;
            }
            DocumentModel userEntry = principal.getModel();
            String email = (String) userEntry.getProperty(userSchemaName, userEmailFieldName);
            if (!StringUtils.isEmpty(email)) {
                result.add(email);
            }
        }

        @Override
        public void collect(NuxeoGroup group) {
            // do nothing
        }

        @Override
        public HashSet<String> getResult() {
            return result;
        }
    }

    static class PrincipalCollector implements Collector<NuxeoPrincipal> {

        protected HashSet<NuxeoPrincipal> result = new HashSet<>();

        @Override
        public void collect(NuxeoPrincipal principal) {
            if (principal == null) {
                return;
            }
            result.add(principal);
        }

        @Override
        public void collect(NuxeoGroup group) {
            // do nothing
        }

        @Override
        public HashSet<NuxeoPrincipal> getResult() {
            return result;
        }
    }

    static class IdCollector implements Collector<String> {

        protected final boolean prefixIds;

        protected HashSet<String> result = new HashSet<>();

        IdCollector(boolean prefixIds) {
            this.prefixIds = prefixIds;
        }

        @Override
        public void collect(NuxeoPrincipal principal) {
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

        @Override
        public void collect(NuxeoGroup group) {
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

        @Override
        public HashSet<String> getResult() {
            return result;
        }

    }

}
