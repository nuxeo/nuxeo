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
 *     Razvan Caraghin
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;

/**
 * Attempts to convert the security data received as a list of user entries into
 * a data structure easily displayable.
 *
 * @author Razvan Caraghin
 * @author Florent Guillaume
 */
public class SecurityDataConverter implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SecurityDataConverter.class);

    /**
     * Feeds security data object with user entries.
     */
    public static void convertToSecurityData(ACP acp, SecurityData securityData) {
        if (null == acp || null == securityData) {
            log.error("Null params received, returning...");
            return;
        }

        securityData.clear();

        for (ACL acl : acp.getACLs()) {
            boolean modifiable = acl.getName().equals(ACL.LOCAL_ACL);
            for (ACE entry : acl.getACEs()) {
                if (modifiable) {
                    securityData.addModifiablePrivilege(entry.getUsername(),
                            entry.getPermission(), entry.isGranted());
                } else {
                    securityData.addUnModifiablePrivilege(entry.getUsername(),
                            entry.getPermission(), entry.isGranted());
                }
            }
        }

        // needed so that the user lists are updated
        securityData.rebuildUserLists();
        securityData.setNeedSave(false);
    }

    /**
     * Reverts back the data contained in SecurityData to a list of user
     * entries.
     * <p>
     * This only converts the modifiable permissions to a list of user entries
     * that is related only to the current document.
     * <p>
     * Does all grants before all denies.
     */
    public static List<UserEntry> convertToUserEntries(SecurityData securityData) {
        if (securityData == null) {
            log.error("Null params received, returning...");
            return Collections.emptyList();
        }

        Map<String, List<String>> grants = securityData.getCurrentDocGrant();
        Map<String, List<String>> denies = securityData.getCurrentDocDeny();
        List<UserEntry> entries = new ArrayList<UserEntry>(grants.size() +
                denies.size());

        for (Entry<String, List<String>> e : grants.entrySet()) {
            UserEntry entry = new UserEntryImpl(e.getKey());
            for (String permission : e.getValue()) {
                entry.addPrivilege(permission, true, false);
            }
            entries.add(entry);
        }

        for (Entry<String, List<String>> e : denies.entrySet()) {
            UserEntry entry = new UserEntryImpl(e.getKey());
            for (String permission : e.getValue()) {
                entry.addPrivilege(permission, false, false);
            }
            entries.add(entry);
        }

        return entries;
    }

}
