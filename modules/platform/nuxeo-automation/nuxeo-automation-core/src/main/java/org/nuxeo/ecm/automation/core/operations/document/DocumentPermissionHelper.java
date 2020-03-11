/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACP;

/**
 * Helper for AddPermission and RemovePermission operations.
 *
 * @since 5.8
 * @deprecated since 7.4. Methods to managing permissions are now on ACP / ACL.
 */
@Deprecated
public final class DocumentPermissionHelper {

    private DocumentPermissionHelper() {

    }

    /**
     * @param acp The ACP to modify
     * @param aclName the name of the ACL to target
     * @param userName the name of the principal (user or group)
     * @param permission the permission of the ACE
     * @param blockInheritance Should we block inheritance
     * @param currentPrincipalName the creator
     * @return true if something has changed on the document security
     */
    public static boolean addPermission(ACP acp, String aclName, String userName, String permission,
            boolean blockInheritance, String currentPrincipalName) {
        return addPermission(acp, aclName, userName, permission, blockInheritance, currentPrincipalName, null, null,
                null);
    }

    /**
     * @param acp The ACP to modify
     * @param aclName the name of the ACL to target
     * @param userName the name of the principal (user or group)
     * @param permission the permission of the ACE
     * @param blockInheritance should we block inheritance
     * @param currentPrincipalName the creator
     * @param begin the begin date of the ACE
     * @param end the end date of the ACE
     * @return true if something has changed on the document security
     * @since 7.4
     */
    public static boolean addPermission(ACP acp, String aclName, String userName, String permission,
            boolean blockInheritance, String currentPrincipalName, Calendar begin, Calendar end,
            Map<String, Serializable> contextData) {
        boolean acpChanged = false;
        if (blockInheritance) {
            acpChanged = acp.blockInheritance(aclName, currentPrincipalName);
        }
        acpChanged = acpChanged || acp.addACE(aclName,
                ACE.builder(userName, permission)
                   .creator(currentPrincipalName)
                   .begin(begin)
                   .end(end)
                   .contextData(contextData)
                   .build());
        return acpChanged;
    }

    /**
     * @param acp The ACP to modify
     * @param aclName the name of the ACL to target
     * @param principalName the name of the principal (user or group)
     * @return true if something has changed on the document security
     */
    public static boolean removePermission(ACP acp, String aclName, String principalName) {
        return acp.removeACEsByUsername(aclName, principalName);
    }

    /**
     * Removes an ACE given its id.
     *
     * @param acp The ACP to modify
     * @param aclName the name of the ACL to target
     * @param id the id of the ACE
     * @return true if something has changed on the document security
     * @since 7.3
     */
    public static boolean removePermissionById(ACP acp, String aclName, String id) {
        return acp.removeACE(aclName, ACE.fromId(id));
    }
}
