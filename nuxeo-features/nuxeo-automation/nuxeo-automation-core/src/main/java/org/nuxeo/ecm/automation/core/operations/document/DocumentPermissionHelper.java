/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper for AddPermission and RemovePermission operations.
 *
 * @since 5.8
 */
public final class DocumentPermissionHelper {

    private DocumentPermissionHelper() {

    }

    /**
     * @param acp The ACP to modify
     * @param aclName the name of the ACL to target
     * @param userName the name of the principal (user or group)
     * @param permissionName
     * @param blockInheritance Should we block inheritance
     * @return true if something has changed on the document security
     *
     */
    public static boolean addPermission(ACP acp, String aclName,
            String userName, String permission, boolean blockInheritance,
            String currentPrincipalName) {
        boolean securityHasChanged = false;

        ACL acl = acp.getOrCreateACL(aclName);
        List<ACE> aceList = getACEAsList(acl.getACEs());

        ACE aceToAdd = new ACE(userName, permission, true);

        if (blockInheritance) {
            if (StringUtils.isEmpty(currentPrincipalName)) {
                throw new IllegalArgumentException(
                        "Can't block inheritance without a current principal");
            }

            aceList.clear();
            aceList.add(aceToAdd);

            if (!userName.equals(currentPrincipalName)) {
                aceList.add(new ACE(currentPrincipalName,
                        SecurityConstants.EVERYTHING, true));
            }

            aceList.addAll(getAdminEverythingACES());
            aceList.add(getBlockInheritanceACE());
            securityHasChanged = true;
        } else {
            if (shouldAddACEToACL(aceList, aceToAdd)) {
                int pos = aceList.indexOf(getBlockInheritanceACE());
                if (pos >= 0) {
                    aceList.add(pos, aceToAdd);
                } else {
                    aceList.add(aceToAdd);
                }
                securityHasChanged = true;
            }
        }

        acl.setACEs(aceList.toArray(new ACE[aceList.size()]));

        // in order to clear the cache
        if (securityHasChanged) {
            acp.addACL(acl);
        }

        return securityHasChanged;

    }

    private static boolean shouldAddACEToACL(List<ACE> aceList, ACE aceToAdd) {
        boolean shouldAddPermission = true;
        for (ACE ace : aceList) {
            if (aceToAdd.equals(ace)) {
                shouldAddPermission = false;
            }
        }
        return shouldAddPermission;
    }

    private static ACE getBlockInheritanceACE() {
        return new ACE(SecurityConstants.EVERYONE,
                SecurityConstants.EVERYTHING, false);
    }

    /**
     * Return a list of ACE giving everything permission to admin groups.
     *
     * @return
     *
     */
    private static List<ACE> getAdminEverythingACES() {
        List<ACE> result = new ArrayList<>();
        UserManager um = Framework.getLocalService(UserManager.class);
        List<String> administratorsGroups = um.getAdministratorsGroups();
        for (String adminGroup : administratorsGroups) {
            result.add(new ACE(adminGroup, SecurityConstants.EVERYTHING, true));
        }
        return result;

    }

    /**
     * Return a mutable list of ACE.
     *
     * @param acEs
     * @return
     *
     */
    private static List<ACE> getACEAsList(ACE[] acEs) {
        List<ACE> aces = new ArrayList<>();
        aces.addAll(Arrays.asList(acEs));
        return aces;
    }

    /**
     * @param acp The ACP to modify
     * @param aclName the name of the ACL to target
     * @param principalName the name of the principal (user or group)
     * @return true if something has changed on the document security
     *
     */
    public static boolean removePermission(ACP acp, String aclName,
            String principalName) {

        boolean securityHasChanged = false;

        ACL acl = acp.getACL(aclName);

        if (acl != null) {
            ACE[] acEs = acl.getACEs();
            for (ACE ace : acEs) {
                if (ace.getUsername().equals(principalName)) {
                    acEs = (ACE[]) ArrayUtils.removeElement(acEs, ace);
                    securityHasChanged = true;
                }
            }
            acl.setACEs(acEs);
        }

        // in order to clear the cache
        if (securityHasChanged) {
            acp.addACL(acl);
        }

        return securityHasChanged;
    }

}
