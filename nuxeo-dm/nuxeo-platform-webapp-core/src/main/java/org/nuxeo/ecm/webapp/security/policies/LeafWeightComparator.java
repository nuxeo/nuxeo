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
 *     "Stephane Lacoin at Nuxeo (aka matic)"
 */
package org.nuxeo.ecm.webapp.security.policies;

import java.util.Comparator;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.runtime.api.Framework;

public class LeafWeightComparator implements Comparator<UserEntry> {

    protected PermissionProvider provider;

    public LeafWeightComparator() {
        try {
        provider = Framework.getService(PermissionProvider.class);
        } catch (Exception e) {
            throw new ClientRuntimeException("Cannot get access to permission provider", e);
        }
    }
    public int compare(UserEntry o1, UserEntry o2) {
        try {
            return countLeafs(o1) - countLeafs(o2);
        } catch (ClientException e) {
            throw new ClientRuntimeException("Cannot explore permisssions", e);
        }
    }

    public int countLeafs(UserEntry e1) throws ClientException {
        int count = 0;
        for (String perm:e1.getPermissions()) {
            count += countLeafs(perm);
        }
        return count;
    }

    public int countLeafs(String perm) throws ClientException {
        int count = 0;
        for (String sub:provider.getSubPermissions(perm) ) {
            count += countLeafs(sub);
        }
        return count+1;
    }

}