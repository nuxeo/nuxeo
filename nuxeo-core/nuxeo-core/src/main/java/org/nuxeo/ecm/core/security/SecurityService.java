/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Bogdan Stefanescu
 * @author Olivier Grisel
 * @author Anahide Tchertchian
 */
// TODO: improve caching invalidation
// TODO: remove "implements SecurityConstants" and check that it doesn't break
// anything
public class SecurityService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.security.SecurityService");

    public static final String PERMISSIONS_EXTENSION_POINT = "permissions";

    private static final String PERMISSIONS_VISIBILITY_EXTENSION_POINT = "permissionsVisibility";

    private static final String POLICIES_EXTENSION_POINT = "policies";

    private static final Log log = LogFactory.getLog(SecurityService.class);

    private PermissionProviderLocal permissionProvider;

    private SecurityPolicyService securityPolicyService;

    // private SecurityManager securityManager;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        permissionProvider = new DefaultPermissionProvider();
        securityPolicyService = new SecurityPolicyServiceImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        permissionProvider = null;
        securityPolicyService = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PERMISSIONS_EXTENSION_POINT.equals(extensionPoint) && contribution instanceof PermissionDescriptor) {
            permissionProvider.registerDescriptor((PermissionDescriptor) contribution);
        } else if (PERMISSIONS_VISIBILITY_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof PermissionVisibilityDescriptor) {
            permissionProvider.registerDescriptor((PermissionVisibilityDescriptor) contribution);
        } else if (POLICIES_EXTENSION_POINT.equals(extensionPoint) && contribution instanceof SecurityPolicyDescriptor) {
            securityPolicyService.registerDescriptor((SecurityPolicyDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PERMISSIONS_EXTENSION_POINT.equals(extensionPoint) && contribution instanceof PermissionDescriptor) {
            permissionProvider.unregisterDescriptor((PermissionDescriptor) contribution);
        } else if (PERMISSIONS_VISIBILITY_EXTENSION_POINT.equals(extensionPoint)
                && contribution instanceof PermissionVisibilityDescriptor) {
            permissionProvider.unregisterDescriptor((PermissionVisibilityDescriptor) contribution);
        } else if (POLICIES_EXTENSION_POINT.equals(extensionPoint) && contribution instanceof SecurityPolicyDescriptor) {
            securityPolicyService.unregisterDescriptor((SecurityPolicyDescriptor) contribution);
        }
    }

    public PermissionProvider getPermissionProvider() {
        return permissionProvider;
    }

    public boolean arePoliciesRestrictingPermission(String permission) {
        return securityPolicyService.arePoliciesRestrictingPermission(permission);
    }

    public boolean arePoliciesExpressibleInQuery(String repositoryName) {
        return securityPolicyService.arePoliciesExpressibleInQuery(repositoryName);
    }

    public Collection<SQLQuery.Transformer> getPoliciesQueryTransformers(String repositoryName) {
        return securityPolicyService.getPoliciesQueryTransformers(repositoryName);
    }

    public boolean checkPermission(Document doc, Principal principal, String permission) {
        if (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAdministrator()) {
            return true;
        }
        // fully check each ACE in turn
        String[] resolvedPermissions = getPermissionsToCheck(permission);
        String[] additionalPrincipals = getPrincipalsToCheck(principal);

        // get the ordered list of ACE
        ACP acp = doc.getSession().getMergedACP(doc);

        // check pluggable policies
        Access access = securityPolicyService.checkPermission(doc, acp, principal, permission, resolvedPermissions,
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
     * Filters the supplied permissions based on whether they are granted to a given principal for a given document.
     *
     * @since 9.1
     */
    public Collection<String> filterGrantedPermissions(Document doc, Principal principal, Collection<String> permissions) {
        if (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAdministrator()) {
            return permissions;
        }

        String[] additionalPrincipals = getPrincipalsToCheck(principal);
        ACP acp = doc.getSession().getMergedACP(doc);

        List<String> result = new ArrayList<>();
        for(String permission : permissions) {
            String[] resolvedPermissions = getPermissionsToCheck(permission);
            Access access = securityPolicyService.checkPermission(doc, acp, principal, permission, resolvedPermissions,
                additionalPrincipals);
            if (access == null || Access.UNKNOWN.equals(access)) {
                access = acp == null ? null : acp.getAccess(additionalPrincipals, resolvedPermissions);
            }
            if (access != null && access.toBoolean()) {
                result.add(permission);
            }
        }
        return result;
    }

    /**
     * Provides the full list of all permissions or groups of permissions that contain the given one (inclusive).
     * <p>
     * It is exposed remotely through {@link CoreSession#getPermissionsToCheck}.
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
            return new String[] { principal.getName(), SecurityConstants.EVERYONE };
        } else {
            int size = userGroups.size();
            String[] groups = new String[size + 2];
            userGroups.toArray(groups);
            groups[size] = principal.getName();
            groups[size + 1] = SecurityConstants.EVERYONE;
            return groups;
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
