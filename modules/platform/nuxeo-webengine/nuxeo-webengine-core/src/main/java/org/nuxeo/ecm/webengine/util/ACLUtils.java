/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.util;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ACLUtils {

    private ACLUtils() {
    }

    public static void removePermission(CoreSession session, DocumentRef docRef, String username, String permission)
            {
        ACP acp = session.getACP(docRef);
        if (acp == null) {
            return;
        }
        ACL acl = acp.getACL(null);
        if (acl == null) {
            return;
        }
        ACE[] aces = acl.getACEs();

        int i = 0;
        for (; i < aces.length; i++) {
            if (permission.equals(aces[i].getPermission()) && username.equals(aces[i].getUsername())) {
                break;
            }
        }
        if (i == aces.length) {
            return;
        }
        UserEntry[] entries = new UserEntry[aces.length - 1];
        if (i == 0) {
            copyTo(aces, 1, entries, 0, entries.length);
        } else if (i == aces.length - 1) {
            copyTo(aces, 0, entries, 0, entries.length);
        } else {
            copyTo(aces, 0, entries, 0, i);
            copyTo(aces, i + 1, entries, i, entries.length - i - 1);
        }
        acp.setRules(entries, true);
        session.setACP(docRef, acp, true);
    }

    private static void copyTo(ACE[] aces, int s0, UserEntry[] entries, int s1, int len) {
        for (int i = s0, k = s1; i < len; i++, k++) {
            ACE ace = aces[i];
            UserEntry entry = new UserEntryImpl(ace.getUsername());
            entry.addPrivilege(ace.getPermission(), ace.isGranted(), false);
            entries[k] = entry;
        }
    }

}
