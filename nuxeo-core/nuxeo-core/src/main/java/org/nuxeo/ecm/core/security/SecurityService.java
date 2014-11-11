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

package org.nuxeo.ecm.core.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;
import org.nuxeo.ecm.core.api.security.impl.SecuritySummaryEntryImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Bogdan Stefanescu
 * @author Olivier Grisel
 * @author Anahide Tchertchian
 *
 */
// TODO: improve caching invalidation
// TODO: remove "implements SecurityConstants" and check that it doesn't break
// anything
public class SecurityService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.security.SecurityService");

    public static final String PERMISSIONS_EXTENSION_POINT = "permissions";

    private static final String PERMISSIONS_VISIBILITY_EXTENSION_POINT = "permissionsVisibility";

    private static final String POLICIES_EXTENSION_POINT = "policies";

    private static final Log log = LogFactory.getLog(SecurityService.class);

    private PermissionProviderLocal permissionProvider;

    private SecurityPolicyService securityPolicyService;

    // private SecurityManager securityManager;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        permissionProvider = new DefaultPermissionProvider();
        securityPolicyService = new SecurityPolicyServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        permissionProvider = null;
        securityPolicyService = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PERMISSIONS_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof PermissionDescriptor) {
            permissionProvider.registerDescriptor((PermissionDescriptor) contribution);
        } else if (PERMISSIONS_VISIBILITY_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof PermissionVisibilityDescriptor) {
            permissionProvider.registerDescriptor((PermissionVisibilityDescriptor) contribution);
        } else if (POLICIES_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof SecurityPolicyDescriptor) {
            securityPolicyService.registerDescriptor((SecurityPolicyDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PERMISSIONS_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof PermissionDescriptor) {
            permissionProvider.unregisterDescriptor((PermissionDescriptor) contribution);
        } else if (PERMISSIONS_VISIBILITY_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof PermissionVisibilityDescriptor) {
            permissionProvider.unregisterDescriptor((PermissionVisibilityDescriptor) contribution);
        } else if (POLICIES_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof SecurityPolicyDescriptor) {
            securityPolicyService.unregisterDescriptor((SecurityPolicyDescriptor) contribution);
        }
    }

    public PermissionProvider getPermissionProvider() {
        return permissionProvider;
    }

    // Never used. Remove ?
    public static void invalidateCache(Session session, String username) {
        session.getRepository().getNuxeoSecurityManager().invalidateCache(
                session);
    }

    public boolean arePoliciesRestrictingPermission(String permission) {
        return securityPolicyService.arePoliciesRestrictingPermission(permission);
    }

    public boolean arePoliciesExpressibleInQuery() {
        return securityPolicyService.arePoliciesExpressibleInQuery();
    }

    public Collection<SQLQuery.Transformer> getPoliciesQueryTransformers() {
        return securityPolicyService.getPoliciesQueryTransformers();
    }

    public boolean checkPermission(Document doc, Principal principal,
            String permission) throws SecurityException {
        String username = principal.getName();

        // system bypass
        // :FIXME: tmp hack
        if (SecurityConstants.SYSTEM_USERNAME.equals(username)) {
            return true;
        }

        // get the security store
        SecurityManager securityManager = doc.getSession().getRepository().getNuxeoSecurityManager();

        // fully check each ACE in turn
        String[] resolvedPermissions = getPermissionsToCheck(permission);
        String[] additionalPrincipals = getPrincipalsToCheck(principal);

        // get the ordered list of ACE
        ACP acp = securityManager.getMergedACP(doc);

        // check pluggable policies
        Access access = securityPolicyService.checkPermission(doc, acp,
                principal, permission, resolvedPermissions,
                additionalPrincipals);
        if (access != null && !Access.UNKNOWN.equals(access)) {
            return access.toBoolean();
        }

        if (acp == null) {
            return false; // no ACP on that doc - by default deny
        }
        access = acp.getAccess(additionalPrincipals, resolvedPermissions);

        return access.toBoolean();
    }

    /**
     * Provides the full list of all permissions or groups of permissions that
     * contain the given one (inclusive).
     * <p>
     * It is exposed remotely through
     * {@link CoreSession#getPermissionsToCheck}.
     *
     * @param permission
     * @return the list, as an array of strings.
     */
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

    public static String[] getPrincipalsToCheck(Principal principal) {
        List<String> userGroups = null;
        if (principal instanceof NuxeoPrincipal) {
            userGroups = ((NuxeoPrincipal) principal).getAllGroups();
        }
        if (userGroups == null) {
            return new String[] { principal.getName(),
                    SecurityConstants.EVERYONE };
        } else {
            int size = userGroups.size();
            String[] groups = new String[size + 2];
            userGroups.toArray(groups);
            groups[size] = principal.getName();
            groups[size + 1] = SecurityConstants.EVERYONE;
            return groups;
        }
    }

    public static List<SecuritySummaryEntry> getSecuritySummary(Document doc,
            Boolean includeParents) {
        List<SecuritySummaryEntry> result = new ArrayList<SecuritySummaryEntry>();

        if (doc == null) {
            return result;
        }

        addChildrenToSecuritySummary(doc, result);
        // TODO: change API to use boolean instead
        if (includeParents) {
            addParentsToSecurirySummary(doc, result);
        }
        return result;
    }

    private static SecuritySummaryEntry createSecuritySummaryEntry(Document doc)
            throws DocumentException {
        return new SecuritySummaryEntryImpl(new IdRef(doc.getUUID()),
                new PathRef(doc.getPath()),
                doc.getSession().getSecurityManager().getACP(doc));
    }

    private static void addParentsToSecurirySummary(Document doc,
            List<SecuritySummaryEntry> summary) {

        Document parent;
        try {
            parent = doc.getParent();
        } catch (DocumentException e) {
            return;
        }

        if (parent == null) {
            return;
        }

        try {
            SecuritySummaryEntry entry = createSecuritySummaryEntry(parent);
            final ACP acp = entry.getAcp();
            if (acp != null) {
                final ACL[] acls = acp.getACLs();
                if (acls != null && acls.length > 0) {
                    summary.add(0, entry);
                }
            }
        } catch (DocumentException e) {
            return;
        }

        addParentsToSecurirySummary(parent, summary);
    }

    private static void addChildrenToSecuritySummary(Document doc,
            List<SecuritySummaryEntry> summary) {
        try {
            SecuritySummaryEntry entry = createSecuritySummaryEntry(doc);
            ACP acp = entry.getAcp();
            if (acp != null && acp.getACLs() != null
                    && acp.getACLs().length > 0) {
                summary.add(entry);
            }
        } catch (DocumentException e) {
            return;
        }
        try {
            Iterator<Document> iter = doc.getChildren();
            while (iter.hasNext()) {
                Document child = iter.next();
                addChildrenToSecuritySummary(child, summary);
            }
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            return;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(PermissionProvider.class)) {
            return (T) permissionProvider;
        } else if (adapter.isAssignableFrom(SecurityPolicyService.class)) {
            return (T) securityPolicyService;
        } else {
            return adapter.cast(this);
        }
    }

}
