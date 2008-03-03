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

package org.nuxeo.ecm.webapp.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
public class SecurityDataConverter implements Serializable {

    private static final long serialVersionUID = -3198252290509915456L;

    private static final Log log = LogFactory.getLog(SecurityDataConverter.class);

    /**
     * Feeds security data object with user entries.
     *
     * @param acp
     * @param securityData
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
    }

    /**
     * Reverts back the data contained in SecurityData to a list of user
     * entries.
     * <p>
     * This only converts the modifiable permissions to a list of user entries
     * that is related only to the current document.
     *
     * @param securityData
     */
    public static List<UserEntry> convertToUserEntries(SecurityData securityData) {
        List<UserEntry> entries = new ArrayList<UserEntry>();

        if (null == securityData) {
            log.error("Null params received, returning...");
            return entries;
        }

        UserEntryImpl entry;
        for (String user : securityData.getCurrentDocGrant().keySet()) {
            entry = new UserEntryImpl(user);

            for (String permission : securityData.getCurrentDocGrant()
                    .get(user)) {
                entry.addPrivilege(permission, true, false);
            }

            entries.add(entry);
        }

        for (String user : securityData.getCurrentDocDeny().keySet()) {
            entry = null;
            for (UserEntry indexEntry : entries) {
                if (indexEntry.getUserName().equals(user)) {
                    entry = (UserEntryImpl) indexEntry;
                    break;
                }
            }

            if (null == entry) {
                entry = new UserEntryImpl(user);
            }

            for (String permission : securityData.getCurrentDocDeny().get(user)) {
                entry.addPrivilege(permission, false, false);
            }

            entries.add(entry);
        }

        return entries;
    }

}
