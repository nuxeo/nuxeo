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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.util;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ACLUtils {

    private ACLUtils() {
    }

    public static void removePermission(CoreSession session,
            DocumentRef docRef, String username, String permission)
            throws ClientException {
        ACP acp = session.getACP(docRef);
        if (acp == null) {
            return;
        }
        ACL  acl = acp.getACL(null);
        if (acl == null) {
            return;
        }
        ACE[] aces = acl.getACEs();

        int i = 0;
        for (; i<aces.length; i++) {
            if (permission.equals(aces[i].getPermission())
                    && username.equals(aces[i].getUsername())) {
                break;
            }
        }
        if (i == aces.length) {
            return;
        }
        UserEntry[] entries = new UserEntry[aces.length-1];
        if (i == 0) {
            copyTo(aces, 1, entries, 0, entries.length);
        } else if (i == aces.length-1) {
            copyTo(aces, 0, entries, 0, entries.length);
        } else {
            copyTo(aces, 0, entries, 0, i);
            copyTo(aces, i+1, entries, i, entries.length-i-1);
        }
        acp.setRules(entries, true);
        session.setACP(docRef, acp, true);
    }

    private static void copyTo(ACE[] aces, int s0, UserEntry[] entries, int s1, int len) {
        for (int i=s0,k=s1; i<len; i++,k++) {
            ACE ace = aces[i];
            UserEntry entry = new UserEntryImpl(ace.getUsername());
            entry.addPrivilege(ace.getPermission(), ace.isGranted(), false);
            entries[k] = entry;
        }
    }

}
