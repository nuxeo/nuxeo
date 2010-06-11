/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.util;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Provides helper methods to find extract permissions/principals info from
 * documents. Should be instantiated with
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PrincipalHelper {

    protected UserManager umgr;

    protected PermissionProvider permissionProvider;

    public PrincipalHelper(UserManager umgr,
            PermissionProvider permissionProvider) {
        this.umgr = umgr;
        this.permissionProvider = permissionProvider;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getEmailsForPermission(DocumentModel input,
            String permission, boolean resolveGroups) throws ClientException {
        return (Set<String>) collectObjectsMatchingPermission(input,
                permission, resolveGroups, new EmailCollector());
    }

    @SuppressWarnings("unchecked")
    public Set<NuxeoPrincipal> getPrincipalsForPermission(DocumentModel input,
            String permission, boolean resolveGroups) throws ClientException {
        return (Set<NuxeoPrincipal>) collectObjectsMatchingPermission(input,
                permission, resolveGroups, new PrincipalCollector());
    }

    public Object collectObjectsMatchingPermission(DocumentModel input,
            String permission, boolean resolveGroups, Collector collector)
            throws ClientException {
        String[] perms = getPermissionsToCheck(permission);
        ACP acp = input.getACP();
        for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (ace.isGranted()
                        && permissionMatch(perms, ace.getPermission())) {
                    try {
                        NuxeoPrincipal principal = umgr.getPrincipal(ace.getUsername());
                        collector.collect(principal);
                    } catch (Throwable t) {
                        if (resolveGroups) {
                            resolveGroups(ace.getUsername(), collector);
                        }
                        // else continue - ignore groups
                    }
                }
            }
        }
        return collector.getResult();
    }

    public boolean permissionMatch(String[] perms, String perm) {
        for (String p : perms) {
            if (p.equals(perm)) {
                return true;
            }
        }
        return false;
    }

    public void resolveGroups(String name, Collector collector)
            throws ClientException {
        try {
            NuxeoGroup group = umgr.getGroup(name);
            for (String u : group.getMemberUsers()) {
                try {
                    NuxeoPrincipal principal = umgr.getPrincipal(u);
                    collector.collect(principal);
                } catch (Throwable t) {
                    resolveGroups(u, collector);
                }
            }
        } catch (Throwable t) {
            // ignore missing group
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

    interface Collector {
        void collect(NuxeoPrincipal p);

        Object getResult();
    }

    static class EmailCollector implements Collector {
        protected HashSet<String> result = new HashSet<String>();

        public void collect(NuxeoPrincipal p) {
            String email = ((NuxeoPrincipalImpl) p).getEmail();
            if (email != null && email.length() > 0) {
                result.add(email);
            }
        }

        public Object getResult() {
            return result;
        }
    }

    static class PrincipalCollector implements Collector {
        protected HashSet<NuxeoPrincipal> result = new HashSet<NuxeoPrincipal>();

        public void collect(NuxeoPrincipal p) {
            result.add(p);
        }

        public Object getResult() {
            return result;
        }
    }

}
